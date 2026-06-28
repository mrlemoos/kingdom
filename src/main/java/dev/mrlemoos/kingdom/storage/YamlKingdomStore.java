package dev.mrlemoos.kingdom.storage;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.election.ElectionPhase;
import dev.mrlemoos.kingdom.model.election.ElectionType;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import dev.mrlemoos.kingdom.model.election.MpSeatLocation;
import dev.mrlemoos.kingdom.model.election.PendingResignation;
import dev.mrlemoos.kingdom.model.election.ResignationSubject;
import dev.mrlemoos.kingdom.model.election.ResignationSubjectKind;
import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.parliament.AssentedAct;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.parliament.ParliamentState;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlKingdomStore {

    private final JavaPlugin plugin;
    private final File dataFile;

    public YamlKingdomStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void loadInto(KingdomService service) {
        if (!dataFile.exists()) {
            seedFromConfig(service);
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        Map<String, Kingdom> kingdoms = new HashMap<>();
        ConfigurationSection kingdomSection = data.getConfigurationSection("kingdoms");
        if (kingdomSection != null) {
            for (String id : kingdomSection.getKeys(false)) {
                ConfigurationSection entry = kingdomSection.getConfigurationSection(id);
                if (entry == null) {
                    continue;
                }
                Kingdom kingdom = new Kingdom(id, entry.getString("display-name", id));
                kingdom.setWorldName(entry.getString("world"));
                kingdom.setWorldGuardRegion(entry.getString("worldguard-region"));
                kingdom.replaceTeleports(readTeleports(entry.getConfigurationSection("teleports")));
                readParliament(entry.getConfigurationSection("parliament"), kingdom);
                kingdoms.put(kingdom.getId(), kingdom);
            }
        }

        Map<UUID, PlayerMembership> memberships = new HashMap<>();
        ConfigurationSection playerSection = data.getConfigurationSection("players");
        if (playerSection != null) {
            for (String uuidString : playerSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    ConfigurationSection entry = playerSection.getConfigurationSection(uuidString);
                    if (entry == null) {
                        continue;
                    }
                    String kingdomId = entry.getString("kingdom");
                    if (kingdomId == null || !kingdoms.containsKey(Kingdom.normaliseId(kingdomId))) {
                        plugin.getLogger().warning("Skipping player " + uuidString + " with unknown kingdom.");
                        continue;
                    }
                    PlayerMembership membership = new PlayerMembership(playerId, Kingdom.normaliseId(kingdomId));
                    String rankName = entry.getString("rank");
                    if (rankName != null) {
                        NobleRank rank = NobleRank.fromCommand(rankName);
                        TitleStyle style = TitleStyle.MASCULINE;
                        String styleName = entry.getString("title-style");
                        if (styleName != null) {
                            style = TitleStyle.valueOf(styleName.toUpperCase());
                        }
                        membership.assignTitle(rank, style);
                    }
                    memberships.put(playerId, membership);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "Skipping invalid player entry: " + uuidString, ex);
                }
            }
        }

        service.replaceState(kingdoms, memberships);
    }

    public void saveFrom(KingdomService service) {
        FileConfiguration data = new YamlConfiguration();

        for (Kingdom kingdom : service.listKingdoms()) {
            String path = "kingdoms." + kingdom.getId();
            data.set(path + ".display-name", kingdom.getDisplayName());
            data.set(path + ".world", kingdom.getWorldName());
            data.set(path + ".worldguard-region", kingdom.getWorldGuardRegion());
            writeTeleports(data, path + ".teleports", kingdom.getTeleportsView());
            writeParliament(data, path + ".parliament", kingdom);
        }

        for (PlayerMembership membership : service.getMembershipsView().values()) {
            String path = "players." + membership.getPlayerId();
            data.set(path + ".kingdom", membership.getKingdomId());
            if (membership.hasNobleTitle()) {
                data.set(path + ".rank", membership.getRank().name().toLowerCase());
                data.set(path + ".title-style", membership.getTitleStyle().name().toLowerCase());
            }
        }

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder.");
            }
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kingdom data.", ex);
        }
    }

    private void seedFromConfig(KingdomService service) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection kingdomSection = config.getConfigurationSection("kingdoms");
        if (kingdomSection == null) {
            return;
        }
        for (String id : kingdomSection.getKeys(false)) {
            ConfigurationSection entry = kingdomSection.getConfigurationSection(id);
            String displayName = entry != null ? entry.getString("display-name", id) : id;
            service.createKingdom(id, displayName);
        }
    }

    static void writeTeleports(FileConfiguration config, String path, Map<String, TeleportPlace> teleports) {
        for (TeleportPlace place : teleports.values()) {
            String placePath = path + "." + place.name();
            config.set(placePath + ".world", place.worldName());
            config.set(placePath + ".x", place.x());
            config.set(placePath + ".y", place.y());
            config.set(placePath + ".z", place.z());
            config.set(placePath + ".yaw", place.yaw());
            config.set(placePath + ".pitch", place.pitch());
        }
    }

    static Map<String, TeleportPlace> readTeleports(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }

        Map<String, TeleportPlace> teleports = new HashMap<>();
        for (String name : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(name);
            if (entry == null) {
                continue;
            }
            String worldName = entry.getString("world");
            if (worldName == null) {
                continue;
            }
            teleports.put(
                    Kingdom.normaliseId(name),
                    TeleportPlace.of(
                            name,
                            worldName,
                            entry.getDouble("x"),
                            entry.getDouble("y"),
                            entry.getDouble("z"),
                            (float) entry.getDouble("yaw"),
                            (float) entry.getDouble("pitch")));
        }
        return teleports;
    }

    static void writeParliament(FileConfiguration config, String path, Kingdom kingdom) {
        var sites = kingdom.getParliamentSites();
        sites.commons().ifPresent(commons -> writeChamber(config, path + ".commons", commons));
        sites.lords().ifPresent(lords -> writeChamber(config, path + ".lords", lords));
        sites.registrar().ifPresent(registrar -> writeRegistrar(config, path + ".registrar", registrar));

        ParliamentState state = kingdom.getParliamentState();
        state.preparedMint().ifPresent(mint -> {
            String mintPath = path + ".prepared-mint";
            config.set(mintPath + ".world", mint.worldName());
            config.set(mintPath + ".x", mint.x());
            config.set(mintPath + ".y", mint.y());
            config.set(mintPath + ".z", mint.z());
        });
        state.currentBill().ifPresent(bill -> writeBill(config, path + ".current-bill", bill));
        writeActs(config, path + ".acts", state.assentedActsView());
        writeElection(config, path, kingdom);
    }

    static void readParliament(ConfigurationSection section, Kingdom kingdom) {
        if (section == null) {
            return;
        }
        var sites = kingdom.getParliamentSites();
        readChamber(section.getConfigurationSection("commons")).ifPresent(sites::setCommons);
        readChamber(section.getConfigurationSection("lords")).ifPresent(sites::setLords);
        readRegistrar(section.getConfigurationSection("registrar")).ifPresent(sites::setRegistrar);

        ParliamentState state = kingdom.getParliamentState();
        readMint(section.getConfigurationSection("prepared-mint")).ifPresent(state::setPreparedMint);
        readBill(section.getConfigurationSection("current-bill")).ifPresent(state::setCurrentBill);
        state.replaceAssentedActs(readActs(section.getConfigurationSection("acts")));
        readElection(section, kingdom);
    }

    private static void writeElection(FileConfiguration config, String path, Kingdom kingdom) {
        var electionState = kingdom.getElectionState();
        config.set(path + ".last-general-election-mc-day", electionState.lastGeneralElectionMcDay());
        if (electionState.premierVillagerSeatIndex().isPresent()) {
            config.set(path + ".premier-villager-seat", electionState.premierVillagerSeatIndex().getAsInt());
        } else {
            config.set(path + ".premier-villager-seat", null);
        }
        config.set(path + ".pending-inaugural-fiscal", electionState.pendingInauguralFiscal());
        config.set(path + ".pending-inaugural-budget", electionState.pendingInauguralBudget());
        electionState.pendingResignation().ifPresent(pending -> {
            String resignationPath = path + ".pending-resignation";
            config.set(resignationPath + ".kind", pending.subject().kind().name().toLowerCase(Locale.ROOT));
            config.set(resignationPath + ".offered-by", pending.offeredBy().toString());
            config.set(resignationPath + ".offered-at-ms", pending.offeredAtMs());
            pending.subject().playerId().ifPresent(id -> config.set(resignationPath + ".player", id.toString()));
            pending.subject().seatIndex().ifPresent(index -> config.set(resignationPath + ".seat", index));
        });
        if (electionState.pendingResignation().isEmpty()) {
            config.set(path + ".pending-resignation", null);
        }

        for (var entry : electionState.seatsView().entrySet()) {
            var seat = entry.getValue();
            if (!seat.isOccupied()) {
                continue;
            }
            String seatPath = path + ".mp-seats." + entry.getKey();
            config.set(seatPath + ".kind", seat.kind().name().toLowerCase(Locale.ROOT));
            seat.playerId().ifPresent(id -> config.set(seatPath + ".holder", id.toString()));
            seat.profession().ifPresent(prof -> config.set(seatPath + ".profession", prof));
            seat.entityId().ifPresent(id -> config.set(seatPath + ".entity", id.toString()));
            seat.originLocation().ifPresent(origin -> {
                config.set(seatPath + ".origin.world", origin.worldName());
                config.set(seatPath + ".origin.x", origin.x());
                config.set(seatPath + ".origin.y", origin.y());
                config.set(seatPath + ".origin.z", origin.z());
                config.set(seatPath + ".origin.yaw", origin.yaw());
                config.set(seatPath + ".origin.pitch", origin.pitch());
            });
        }

        for (var entry : electionState.seatLocationsView().entrySet()) {
            MpSeatLocation location = entry.getValue();
            String locPath = path + ".mp-seat-locations." + entry.getKey();
            config.set(locPath + ".world", location.worldName());
            config.set(locPath + ".x", location.x());
            config.set(locPath + ".y", location.y());
            config.set(locPath + ".z", location.z());
            config.set(locPath + ".yaw", location.yaw());
            config.set(locPath + ".pitch", location.pitch());
        }

        var election = electionState.election();
        if (!election.isActive() && election.type().isEmpty()) {
            return;
        }
        String electionPath = path + ".election";
        election.type().ifPresent(type -> config.set(electionPath + ".type", type.name().toLowerCase(Locale.ROOT)));
        config.set(electionPath + ".phase", election.phase().name().toLowerCase(Locale.ROOT));
        config.set(electionPath + ".ends-at-ms", election.endsAtMs());
        election.byElectionSeatIndex().ifPresent(index -> config.set(electionPath + ".by-election-seat", index));
        config.set(electionPath + ".nominations", election.nominationsView().stream().map(UUID::toString).toList());
        for (var vote : election.votesView().entrySet()) {
            config.set(electionPath + ".votes." + vote.getKey() + ".candidate", vote.getValue().toString());
        }
        if (!election.speakerTieCandidatesView().isEmpty()) {
            config.set(
                    electionPath + ".speaker-tie-candidates",
                    election.speakerTieCandidatesView().stream().map(UUID::toString).toList());
        }
        election.speakerTieChoice().ifPresent(id -> config.set(electionPath + ".speaker-tie-choice", id.toString()));
    }

    private static void readElection(ConfigurationSection section, Kingdom kingdom) {
        if (section == null) {
            return;
        }
        var electionState = kingdom.getElectionState();
        electionState.setLastGeneralElectionMcDay(section.getLong("last-general-election-mc-day", 0L));
        if (section.contains("premier-villager-seat")) {
            electionState.setPremierVillagerSeatIndex(section.getInt("premier-villager-seat"));
        } else {
            electionState.clearPremierVillager();
        }
        electionState.setPendingInauguralFiscal(section.getBoolean("pending-inaugural-fiscal", false));
        electionState.setPendingInauguralBudget(section.getBoolean("pending-inaugural-budget", false));
        readPendingResignation(section.getConfigurationSection("pending-resignation"), electionState);

        ConfigurationSection seatsSection = section.getConfigurationSection("mp-seats");
        if (seatsSection != null) {
            Map<Integer, MpSeat> loadedSeats = new HashMap<>();
            for (String key : seatsSection.getKeys(false)) {
                ConfigurationSection seatSection = seatsSection.getConfigurationSection(key);
                if (seatSection == null) {
                    continue;
                }
                int index = Integer.parseInt(key);
                MpSeat seat = new MpSeat(index);
                MpSeatKind kind = MpSeatKind.valueOf(seatSection.getString("kind", "player").toUpperCase(Locale.ROOT));
                if (kind == MpSeatKind.PLAYER) {
                    seat.assignPlayer(UUID.fromString(seatSection.getString("holder")));
                } else {
                    String profession = seatSection.getString("profession", "none");
                    UUID entity = seatSection.contains("entity")
                            ? UUID.fromString(seatSection.getString("entity"))
                            : null;
                    seat.assignVillager(profession, entity);
                    ConfigurationSection originSection = seatSection.getConfigurationSection("origin");
                    if (originSection != null) {
                        seat.setOriginLocation(new MpSeatLocation(
                                originSection.getString("world"),
                                originSection.getDouble("x"),
                                originSection.getDouble("y"),
                                originSection.getDouble("z"),
                                (float) originSection.getDouble("yaw"),
                                (float) originSection.getDouble("pitch")));
                    }
                }
                loadedSeats.put(index, seat);
            }
            electionState.replaceSeats(loadedSeats);
        }

        ConfigurationSection locationsSection = section.getConfigurationSection("mp-seat-locations");
        if (locationsSection != null) {
            Map<Integer, MpSeatLocation> locations = new HashMap<>();
            for (String key : locationsSection.getKeys(false)) {
                ConfigurationSection loc = locationsSection.getConfigurationSection(key);
                if (loc == null) {
                    continue;
                }
                locations.put(
                        Integer.parseInt(key),
                        new MpSeatLocation(
                                loc.getString("world"),
                                loc.getDouble("x"),
                                loc.getDouble("y"),
                                loc.getDouble("z"),
                                (float) loc.getDouble("yaw"),
                                (float) loc.getDouble("pitch")));
            }
            electionState.replaceSeatLocations(locations);
        }

        ConfigurationSection electionSection = section.getConfigurationSection("election");
        if (electionSection == null) {
            return;
        }
        ElectionType type = ElectionType.valueOf(
                electionSection.getString("type", "general").toUpperCase(Locale.ROOT));
        ElectionPhase phase = ElectionPhase.valueOf(
                electionSection.getString("phase", "closed").toUpperCase(Locale.ROOT));
        long endsAt = electionSection.getLong("ends-at-ms");
        Integer byElectionSeat = electionSection.contains("by-election-seat")
                ? electionSection.getInt("by-election-seat")
                : null;
        List<UUID> nominations = electionSection.getStringList("nominations").stream()
                .map(UUID::fromString)
                .toList();
        Map<UUID, UUID> votes = new HashMap<>();
        ConfigurationSection votesSection = electionSection.getConfigurationSection("votes");
        if (votesSection != null) {
            for (String voter : votesSection.getKeys(false)) {
                votes.put(UUID.fromString(voter), UUID.fromString(votesSection.getString(voter + ".candidate")));
            }
        }
        Set<UUID> speakerTieCandidates = new LinkedHashSet<>();
        for (String id : electionSection.getStringList("speaker-tie-candidates")) {
            speakerTieCandidates.add(UUID.fromString(id));
        }
        UUID speakerTieChoice = electionSection.contains("speaker-tie-choice")
                ? UUID.fromString(electionSection.getString("speaker-tie-choice"))
                : null;
        electionState.election().restore(
                type,
                phase,
                endsAt,
                byElectionSeat,
                nominations,
                Map.of(),
                votes,
                speakerTieCandidates,
                speakerTieChoice);
    }

    private static void writeChamber(FileConfiguration config, String path, ChamberSite site) {
        config.set(path + ".world", site.worldName());
        config.set(path + ".x", site.x());
        config.set(path + ".y", site.y());
        config.set(path + ".z", site.z());
    }

    private static Optional<ChamberSite> readChamber(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        String world = section.getString("world");
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(ChamberSite.of(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z")));
    }

    private static void writeRegistrar(FileConfiguration config, String path, RegistrarSite site) {
        config.set(path + ".world", site.worldName());
        config.set(path + ".x", site.blockX());
        config.set(path + ".y", site.blockY());
        config.set(path + ".z", site.blockZ());
    }

    private static Optional<RegistrarSite> readRegistrar(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        String world = section.getString("world");
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(RegistrarSite.of(
                world,
                section.getInt("x"),
                section.getInt("y"),
                section.getInt("z")));
    }

    private static Optional<MintLocation> readMint(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        String world = section.getString("world");
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new MintLocation(
                world,
                section.getInt("x"),
                section.getInt("y"),
                section.getInt("z")));
    }

    private static void writeBill(FileConfiguration config, String path, Bill bill) {
        config.set(path + ".id", bill.id());
        config.set(path + ".kingdom", bill.kingdomId());
        config.set(path + ".type", bill.type().name().toLowerCase());
        config.set(path + ".title", bill.title());
        config.set(path + ".state", bill.state().name().toLowerCase());
        config.set(path + ".proposer", bill.proposerId().toString());
        config.set(path + ".tabled-at", bill.tabledAtMs());
        writePayload(config, path + ".payload", bill.type(), bill.payload());
        for (Map.Entry<UUID, VoteChoice> vote : bill.votesView().entrySet()) {
            config.set(path + ".votes." + vote.getKey() + ".choice", vote.getValue().name().toLowerCase());
        }
        bill.speakerCastingVote()
                .ifPresent(choice -> config.set(path + ".speaker-casting-vote", choice.name().toLowerCase()));
    }

    private static Optional<Bill> readBill(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        String id = section.getString("id");
        String kingdomId = section.getString("kingdom");
        if (id == null) {
            return Optional.empty();
        }
        BillType type = BillType.valueOf(section.getString("type", "fiscal").toUpperCase());
        String title = section.getString("title", id);
        BillState state = BillState.valueOf(section.getString("state", "tabled").toUpperCase());
        UUID proposer = UUID.fromString(section.getString("proposer"));
        long tabledAt = section.getLong("tabled-at");
        BillPayload payload = readPayload(section.getConfigurationSection("payload"), type);
        if (payload == null) {
            return Optional.empty();
        }
        if (kingdomId == null) {
            int dash = id.indexOf('-');
            kingdomId = dash > 0 ? id.substring(0, dash) : id;
        }
        Bill bill = new Bill(id, kingdomId, type, title, state, proposer, payload, tabledAt);
        ConfigurationSection votes = section.getConfigurationSection("votes");
        if (votes != null) {
            Map<UUID, VoteChoice> loadedVotes = new HashMap<>();
            for (String voter : votes.getKeys(false)) {
                loadedVotes.put(UUID.fromString(voter), VoteChoice.valueOf(
                        votes.getString(voter + ".choice", "abstain").toUpperCase()));
            }
            bill.replaceVotes(loadedVotes);
        }
        String casting = section.getString("speaker-casting-vote");
        if (casting != null) {
            bill.setSpeakerCastingVote(VoteChoice.valueOf(casting.toUpperCase()));
        }
        return Optional.of(bill);
    }

    private static void writePayload(
            FileConfiguration config, String path, BillType type, BillPayload payload) {
        switch (payload) {
            case BillPayload.Fiscal fiscal -> {
                config.set(path + ".base-rate", fiscal.rates().baseRate());
                config.set(path + ".foreign-surcharge", fiscal.rates().foreignSurcharge());
                config.set(path + ".transfer-fee", fiscal.rates().transferFee());
                config.set(path + ".cross-fee", fiscal.rates().crossKingdomTransferFee());
            }
            case BillPayload.Budget budget -> config.set(path + ".amount", budget.amount());
            case BillPayload.SpendMint mint -> {
                config.set(path + ".world", mint.mintLocation().worldName());
                config.set(path + ".x", mint.mintLocation().x());
                config.set(path + ".y", mint.mintLocation().y());
                config.set(path + ".z", mint.mintLocation().z());
                config.set(path + ".cost", mint.cost());
            }
            case BillPayload.SpendStipend stipend -> {
                config.set(path + ".recipient", stipend.recipientId().toString());
                config.set(path + ".amount", stipend.amount());
                config.set(path + ".reason", stipend.reason());
            }
        }
    }

    private static BillPayload readPayload(ConfigurationSection section, BillType type) {
        if (section == null) {
            return null;
        }
        return switch (type) {
            case FISCAL -> new BillPayload.Fiscal(new FiscalRates(
                    section.getDouble("base-rate"),
                    section.getDouble("foreign-surcharge"),
                    section.getDouble("transfer-fee"),
                    section.getDouble("cross-fee"),
                    FiscalRates.defaults().rankModifiers()));
            case BUDGET -> new BillPayload.Budget(section.getDouble("amount"));
            case SPEND_MINT -> new BillPayload.SpendMint(
                    new MintLocation(
                            section.getString("world"),
                            section.getInt("x"),
                            section.getInt("y"),
                            section.getInt("z")),
                    section.getDouble("cost"));
            case SPEND_STIPEND -> new BillPayload.SpendStipend(
                    UUID.fromString(section.getString("recipient")),
                    section.getDouble("amount"),
                    section.getString("reason"));
        };
    }

    private static void writeActs(FileConfiguration config, String path, List<AssentedAct> acts) {
        for (int index = 0; index < acts.size(); index++) {
            AssentedAct act = acts.get(index);
            String actPath = path + "." + index;
            config.set(actPath + ".bill-id", act.billId());
            config.set(actPath + ".title", act.title());
            config.set(actPath + ".type", act.type().name().toLowerCase());
            config.set(actPath + ".assented-at", act.assentedAtMs());
            config.set(actPath + ".pages", act.bookPages());
            config.set(actPath + ".shelf.world", act.shelfWorld());
            config.set(actPath + ".shelf.x", act.shelfBlockX());
            config.set(actPath + ".shelf.y", act.shelfBlockY());
            config.set(actPath + ".shelf.z", act.shelfBlockZ());
            config.set(actPath + ".shelf.slot", act.shelfSlot());
            for (Map.Entry<UUID, VoteChoice> vote : act.divisionVotes().entrySet()) {
                config.set(actPath + ".votes." + vote.getKey() + ".choice", vote.getValue().name().toLowerCase());
            }
            if (act.speakerCastingVote() != null) {
                config.set(actPath + ".speaker-casting-vote", act.speakerCastingVote().name().toLowerCase());
            }
        }
    }

    private static List<AssentedAct> readActs(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        List<AssentedAct> acts = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            Map<UUID, VoteChoice> votes = new HashMap<>();
            ConfigurationSection voteSection = entry.getConfigurationSection("votes");
            if (voteSection != null) {
                for (String voter : voteSection.getKeys(false)) {
                    votes.put(
                            UUID.fromString(voter),
                            VoteChoice.valueOf(voteSection.getString(voter + ".choice", "abstain").toUpperCase()));
                }
            }
            VoteChoice casting = null;
            String castingName = entry.getString("speaker-casting-vote");
            if (castingName != null) {
                casting = VoteChoice.valueOf(castingName.toUpperCase());
            }
            acts.add(new AssentedAct(
                    entry.getString("bill-id"),
                    entry.getString("title"),
                    BillType.valueOf(entry.getString("type", "fiscal").toUpperCase()),
                    entry.getLong("assented-at"),
                    entry.getStringList("pages"),
                    votes,
                    casting,
                    entry.getString("shelf.world"),
                    entry.getInt("shelf.x"),
                    entry.getInt("shelf.y"),
                    entry.getInt("shelf.z"),
                    entry.getInt("shelf.slot")));
        }
        return acts;
    }

    private static void readPendingResignation(ConfigurationSection section, KingdomElectionState electionState) {
        electionState.clearPendingResignation();
        if (section == null) {
            return;
        }
        ResignationSubjectKind kind = ResignationSubjectKind.valueOf(
                section.getString("kind", "player_mp").toUpperCase(Locale.ROOT));
        UUID offeredBy = UUID.fromString(section.getString("offered-by"));
        long offeredAtMs = section.getLong("offered-at-ms");
        Optional<UUID> playerId = Optional.ofNullable(section.getString("player")).map(UUID::fromString);
        Optional<Integer> seatIndex = section.contains("seat") ? Optional.of(section.getInt("seat")) : Optional.empty();

        ResignationSubject subject = switch (kind) {
            case PLAYER_PREMIER -> ResignationSubject.playerPremier(playerId.orElseThrow());
            case PLAYER_MP -> ResignationSubject.playerMp(playerId.orElseThrow(), seatIndex.orElseThrow());
            case VILLAGER_PREMIER -> ResignationSubject.villagerSeat(seatIndex.orElseThrow(), true);
            case VILLAGER_MP -> ResignationSubject.villagerSeat(seatIndex.orElseThrow(), false);
        };
        electionState.setPendingResignation(new PendingResignation(subject, offeredBy, offeredAtMs));
    }
}
