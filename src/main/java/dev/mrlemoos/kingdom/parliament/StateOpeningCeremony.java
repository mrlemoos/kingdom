package dev.mrlemoos.kingdom.parliament;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.election.ProfessionConstituencyResolver;
import dev.mrlemoos.kingdom.resignation.ResignationAuthority;
import dev.mrlemoos.kingdom.service.ChamberPresence;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentResult;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Runs the State Opening in the world: summons the realm to the House of Lords, lets the Crown
 * declare the session open from the throne, then returns everyone whence they came.
 */
public final class StateOpeningCeremony {

    private static final long RETURN_DELAY_TICKS = 20L;

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final StateOpeningService stateOpeningService;
    private final YamlKingdomStore store;
    private final SpeechFromThroneItem speechItem;
    private final Map<String, Map<UUID, Location>> summonedOrigins = new ConcurrentHashMap<>();

    public StateOpeningCeremony(
            JavaPlugin plugin,
            KingdomService kingdomService,
            StateOpeningService stateOpeningService,
            YamlKingdomStore store,
            SpeechFromThroneItem speechItem) {
        this.plugin = plugin;
        this.kingdomService = kingdomService;
        this.stateOpeningService = stateOpeningService;
        this.store = store;
        this.speechItem = speechItem;
    }

    public StateOpeningService stateOpeningService() {
        return stateOpeningService;
    }

    /** Announces that Parliament awaits the Crown and hands the speech to whoever must open it. */
    public void announceSummons(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            Bukkit.broadcastMessage(
                    c("&6Parliament of " + kingdom.getDisplayName() + " awaits the Crown to open the session."));
            deliverSpeech(kingdomId);
        });
    }

    public void deliverSpeech(String kingdomId) {
        ResignationAuthority.monarchOrRegent(kingdomId, kingdomService).ifPresent(crownId -> {
            Player crown = Bukkit.getPlayer(crownId);
            if (crown != null) {
                giveSpeech(crown, kingdomId);
            }
        });
    }

    public void deliverSpeechIfMissingOnJoin(Player player) {
        kingdomService.getMembership(player.getUniqueId()).ifPresent(membership -> {
            String kingdomId = membership.getKingdomId();
            if (!stateOpeningService.isAwaitingStateOpening(kingdomId)) {
                return;
            }
            if (!stateOpeningService.canOpen(kingdomId, player.getUniqueId())) {
                return;
            }
            if (!hasSpeech(player, kingdomId)) {
                giveSpeech(player, kingdomId);
            }
        });
    }

    public boolean hasSummoned(String kingdomId) {
        return summonedOrigins.containsKey(kingdomId);
    }

    public boolean isInLords(Player player, String kingdomId) {
        Optional<ChamberSite> lords =
                kingdomService.getKingdom(kingdomId).flatMap(k -> k.getParliamentSites().lords());
        if (lords.isEmpty()) {
            return false;
        }
        Location location = player.getLocation();
        return ChamberPresence.withinChamber(
                lords.get(),
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                ChamberPresence.DEFAULT_RADIUS);
    }

    /** Gathers every online member of the kingdom around the Lords chamber. */
    public void summon(Player crown, String kingdomId) {
        Optional<ChamberSite> lords =
                kingdomService.getKingdom(kingdomId).flatMap(k -> k.getParliamentSites().lords());
        if (lords.isEmpty()) {
            crown.sendMessage(c("&cNo House of Lords is set for your kingdom."));
            return;
        }
        World world = Bukkit.getWorld(lords.get().worldName());
        if (world == null) {
            crown.sendMessage(c("&cThe House of Lords is in a world that is not loaded."));
            return;
        }

        List<Player> summoned = onlineMembers(kingdomId);
        List<int[]> offsets = SafeChamberLanding.ringOffsets(summoned.size());
        Map<UUID, Location> origins = new HashMap<>();

        for (int i = 0; i < summoned.size(); i++) {
            Player member = summoned.get(i);
            origins.put(member.getUniqueId(), member.getLocation().clone());
            member.teleport(landingFor(world, lords.get(), offsets.get(i), member.getLocation()));
            member.playSound(member.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            member.sendTitle(c("&6State Opening"), c("&eThe Crown summons Parliament"), 10, 60, 20);
        }
        summonedOrigins.put(kingdomId, origins);

        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> Bukkit.broadcastMessage(
                c("&6The Crown summons the realm of " + kingdom.getDisplayName() + " to the House of Lords.")));
    }

    /** The Crown declares the session open, then the realm is returned home. */
    public void declareOpen(Player crown, String kingdomId) {
        if (!hasSummoned(kingdomId)) {
            crown.sendMessage(c("&cSummon the realm before opening Parliament."));
            return;
        }
        if (!isInLords(crown, kingdomId)) {
            crown.sendMessage(c("&cYou must stand in the House of Lords to open Parliament."));
            return;
        }

        ParliamentResult opened = stateOpeningService.open(kingdomId, crown.getUniqueId());
        if (opened instanceof ParliamentResult.Failure failure) {
            crown.sendMessage(c("&c" + failure.message()));
            return;
        }

        store.saveFrom(kingdomService);
        removeSpeeches(crown, kingdomId);
        speakFromThrone(kingdomId);
        Bukkit.getScheduler().runTaskLater(plugin, () -> returnSummoned(kingdomId), RETURN_DELAY_TICKS);
    }

    /** Cleans up after the session was opened by royal commission rather than in person. */
    public void commissionOpened(String kingdomId) {
        returnSummoned(kingdomId);
        for (Player online : onlineMembers(kingdomId)) {
            removeSpeeches(online, kingdomId);
        }
    }

    private void speakFromThrone(String kingdomId) {
        Kingdom kingdom = kingdomService.getKingdom(kingdomId).orElseThrow();
        Bukkit.broadcastMessage(c("&6The Parliament of " + kingdom.getDisplayName() + " is open."));
        Bukkit.broadcastMessage(c("&eMy government is formed: " + describePremier(kingdom) + "."));
        Bukkit.broadcastMessage(c("&e" + describeSeats(kingdom)));
        for (Player member : onlineMembers(kingdomId)) {
            member.playSound(member.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    private String describePremier(Kingdom kingdom) {
        for (PlayerMembership membership : kingdomService.getMembershipsView().values()) {
            if (kingdom.getId().equals(membership.getKingdomId()) && membership.getRank() == NobleRank.PREMIER) {
                return "Premier " + Bukkit.getOfflinePlayer(membership.getPlayerId()).getName();
            }
        }
        OptionalInt villagerSeat = kingdom.getElectionState().premierVillagerSeatIndex();
        if (villagerSeat.isPresent()) {
            String profession = kingdom.getElectionState()
                    .seat(villagerSeat.getAsInt())
                    .flatMap(seat -> seat.profession())
                    .orElse("none");
            return "a Premier villager for " + ProfessionConstituencyResolver.displayLabel(profession);
        }
        return "no Premier is seated";
    }

    private String describeSeats(Kingdom kingdom) {
        int players = 0;
        int villagers = 0;
        for (var seat : kingdom.getElectionState().seatsView().values()) {
            if (!seat.isOccupied()) {
                continue;
            }
            if (seat.kind() == MpSeatKind.PLAYER) {
                players++;
            } else {
                villagers++;
            }
        }
        return "The Commons seats " + players + " player MP(s) and " + villagers + " profession MP(s).";
    }

    private void returnSummoned(String kingdomId) {
        Map<UUID, Location> origins = summonedOrigins.remove(kingdomId);
        if (origins == null) {
            return;
        }
        origins.forEach((playerId, origin) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && origin.getWorld() != null) {
                player.teleport(origin);
            }
        });
    }

    private Location landingFor(World world, ChamberSite lords, int[] offset, Location fallbackFacing) {
        int x = (int) Math.floor(lords.x()) + offset[0];
        int z = (int) Math.floor(lords.z()) + offset[1];
        int startY = (int) Math.floor(lords.y());
        OptionalInt feetY = SafeChamberLanding.findFeetY(
                (bx, by, bz) -> world.getBlockAt(bx, by, bz).isPassable(),
                x,
                startY,
                z,
                world.getMinHeight(),
                world.getMaxHeight());
        int y = feetY.orElseGet(() -> world.getHighestBlockYAt(x, z) + 1);
        return new Location(world, x + 0.5, y, z + 0.5, fallbackFacing.getYaw(), fallbackFacing.getPitch());
    }

    private List<Player> onlineMembers(String kingdomId) {
        List<Player> members = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            kingdomService.getMembership(online.getUniqueId()).ifPresent(membership -> {
                if (kingdomId.equals(membership.getKingdomId())) {
                    members.add(online);
                }
            });
        }
        return members;
    }

    private void giveSpeech(Player player, String kingdomId) {
        String name = kingdomService.getKingdom(kingdomId).map(Kingdom::getDisplayName).orElse(kingdomId);
        removeSpeeches(player, kingdomId);
        ItemStack speech = speechItem.create(kingdomId, name);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(speech);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage(c("&eYour inventory was full. The Speech from the Throne was dropped at your feet."));
        }
        player.sendMessage(c("&6Parliament awaits your State Opening."));
        player.sendMessage(c("&7Right-click the Speech from the Throne to open Parliament."));
    }

    private boolean hasSpeech(Player player, String kingdomId) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (speechItem.kingdomId(stack).filter(kingdomId::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private void removeSpeeches(Player player, String kingdomId) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (speechItem.kingdomId(contents[slot]).filter(kingdomId::equals).isPresent()) {
                player.getInventory().setItem(slot, null);
            }
        }
    }
}
