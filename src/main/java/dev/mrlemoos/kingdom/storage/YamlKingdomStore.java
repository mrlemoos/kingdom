package dev.mrlemoos.kingdom.storage;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.calendar.ReignRecord;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.election.CandidateDeclaration;
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
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.parliament.ConductProvision;
import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import dev.mrlemoos.kingdom.model.parliament.ParliamentState;
import dev.mrlemoos.kingdom.model.parliament.PreparedPublicWork;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import dev.mrlemoos.kingdom.parliament.DivisionBloc;
import dev.mrlemoos.kingdom.parliament.DivisionBlocKind;
import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import dev.mrlemoos.kingdom.parliament.HansardRecord;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.model.church.FuneralRecord;
import dev.mrlemoos.kingdom.model.church.KingdomChurchState;
import dev.mrlemoos.kingdom.model.church.Marriage;
import dev.mrlemoos.kingdom.model.church.VillagerFuneralRecord;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePost.GazetteCurfewWindow;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;
import dev.mrlemoos.kingdom.model.police.ArrestReward;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.model.police.WarrantStatus;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.granary.FamineWatch;
import dev.mrlemoos.kingdom.granary.HungerLedgerStore;
import dev.mrlemoos.kingdom.granary.ShortfallWatch;
import dev.mrlemoos.kingdom.hearth.ColdLedgerStore;
import dev.mrlemoos.kingdom.loyalty.LoyaltyStore;
import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.loyalty.MoraleStore;
import dev.mrlemoos.kingdom.loyalty.RecoveryMark;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.model.war.OnDutyState;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.levy.LevyArrears;
import dev.mrlemoos.kingdom.war.levy.LevyArrearsStore;
import dev.mrlemoos.kingdom.war.roster.StandingRosterStore;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private LoyaltyStore loyaltyStore;
    private MoraleStore moraleStore;
    private WarService warService;
    private StandingRosterStore standingRosterStore;
    private LevyArrearsStore levyArrearsStore;
    private ColdLedgerStore coldLedgerStore;
    private HungerLedgerStore hungerLedgerStore;
    private FamineWatch famineWatch;
    private MechanicalJusticeService mechanicalJusticeService;
    private RealmCalendarService calendarService;
    private ShortfallWatch shortfallWatch;

    public YamlKingdomStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void setLoyaltyStore(LoyaltyStore loyaltyStore) {
        this.loyaltyStore = loyaltyStore;
    }

    public void setMoraleStore(MoraleStore moraleStore) {
        this.moraleStore = moraleStore;
    }

    public void setWarService(WarService warService) {
        this.warService = warService;
    }

    public void setStandingRosterStore(StandingRosterStore standingRosterStore) {
        this.standingRosterStore = standingRosterStore;
    }

    public void setLevyArrearsStore(LevyArrearsStore levyArrearsStore) {
        this.levyArrearsStore = levyArrearsStore;
    }

    public void setColdLedgerStore(ColdLedgerStore coldLedgerStore) {
        this.coldLedgerStore = coldLedgerStore;
    }

    /** The run of hungry days behind each villager, kept apart from the cold ledger. */
    public void setHungerLedgerStore(HungerLedgerStore hungerLedgerStore) {
        this.hungerLedgerStore = hungerLedgerStore;
    }

    /** Keeps the day each realm's famine was announced, so a restart never announces it twice. */
    public void setFamineWatch(FamineWatch famineWatch) {
        this.famineWatch = famineWatch;
    }

    public void setMechanicalJusticeService(MechanicalJusticeService mechanicalJusticeService) {
        this.mechanicalJusticeService = mechanicalJusticeService;
    }

    public void setCalendarService(RealmCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /** Keeps the day the granary shortfall was last cried, so a restart never cries it twice. */
    public void setShortfallWatch(ShortfallWatch shortfallWatch) {
        this.shortfallWatch = shortfallWatch;
    }

    /** Restores the realm clock; pins the epoch on first run. */
    public void loadCalendar() {
        if (calendarService == null) {
            return;
        }
        if (!dataFile.exists()) {
            calendarService.restore(-1L, 0L);
            return;
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        calendarService.restore(
                data.getLong("calendar.epoch-world-day", -1L), data.getLong("calendar.last-seen-realm-day", 0L));
        calendarService.seasonTurn().restore(data.getLong("calendar.last-season-turn-day", -1L));
        if (shortfallWatch != null) {
            shortfallWatch.restore(data.getLong("granary.last-shortfall-warning-day", -1L));
        }
    }

    static List<ReignRecord> readReigns(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        List<ReignRecord> reigns = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String monarchId = entry.getString("monarch");
            String monarchName = entry.getString("name");
            if (monarchId == null || monarchName == null) {
                continue;
            }
            reigns.add(new ReignRecord(
                    monarchId,
                    monarchName,
                    entry.getString("title", "King"),
                    entry.getInt("ordinal", 1),
                    entry.getLong("accession-day", 0L),
                    entry.getLong("end-day", ReignRecord.OPEN)));
        }
        reigns.sort(java.util.Comparator.comparingLong(ReignRecord::accessionDay));
        return reigns;
    }

    private static void writeReigns(FileConfiguration data, String path, List<ReignRecord> reigns) {
        for (int i = 0; i < reigns.size(); i++) {
            ReignRecord reign = reigns.get(i);
            String entry = path + "." + i;
            data.set(entry + ".monarch", reign.monarchId());
            data.set(entry + ".name", reign.monarchName());
            data.set(entry + ".title", reign.title());
            data.set(entry + ".ordinal", reign.ordinal());
            data.set(entry + ".accession-day", reign.accessionDay());
            data.set(entry + ".end-day", reign.endDay());
        }
    }

    /** Loads persisted warrants after {@link MechanicalJusticeService} is constructed. */
    public void loadWarrants() {
        if (mechanicalJusticeService == null || !dataFile.exists()) {
            return;
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection kingdomSection = data.getConfigurationSection("kingdoms");
        if (kingdomSection == null) {
            return;
        }
        List<Warrant> loaded = new ArrayList<>();
        for (String id : kingdomSection.getKeys(false)) {
            ConfigurationSection entry = kingdomSection.getConfigurationSection(id);
            if (entry == null) {
                continue;
            }
            ConfigurationSection policeSection = entry.getConfigurationSection("police");
            if (policeSection == null) {
                continue;
            }
            loaded.addAll(readWarrants(policeSection.getConfigurationSection("warrants"), id));
        }
        mechanicalJusticeService.replaceWarrants(loaded);
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
                kingdom.setGranaryRegion(entry.getString("granary-region"));
                kingdom.setGranaryWheat(entry.getInt("granary-wheat", 0));
                kingdom.replaceTeleports(readTeleports(entry.getConfigurationSection("teleports")));
                readParliament(entry.getConfigurationSection("parliament"), kingdom);
                readPolice(entry.getConfigurationSection("police"), kingdom);
                readCity(entry.getConfigurationSection("city"), kingdom);
                readChurch(entry.getConfigurationSection("church"), kingdom);
                kingdom.getReignHistory().replaceAll(readReigns(entry.getConfigurationSection("reigns")));
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
        if (loyaltyStore != null) {
            loyaltyStore.replaceAll(readLoyalty(data.getConfigurationSection("loyalty")));
            loyaltyStore.replaceAllMarks(readLoyaltyMarks(data.getConfigurationSection("loyalty-clocks")));
        }
        if (moraleStore != null) {
            moraleStore.replaceAll(readMorale(data.getConfigurationSection("morale")));
            moraleStore.replaceAllMarks(readMoraleMarks(data.getConfigurationSection("morale-clocks")));
        }
        if (warService != null) {
            warService.replaceActiveWars(readWars(data.getConfigurationSection("wars")));
        }
        if (standingRosterStore != null) {
            standingRosterStore.replaceAllRosters(readRosters(data.getConfigurationSection("standing-roster")));
            standingRosterStore.replaceAllOnDutyStates(readOnDutyStates(data.getConfigurationSection("on-duty")));
        }
        if (levyArrearsStore != null) {
            levyArrearsStore.replaceAll(readLevyArrears(data.getConfigurationSection("levy-arrears")));
        }
        if (coldLedgerStore != null) {
            coldLedgerStore.replaceAll(readColdDays(data.getConfigurationSection("cold-days")));
        }
        if (hungerLedgerStore != null) {
            hungerLedgerStore.replaceAll(readHungryDays(data.getConfigurationSection("hungry-days")));
        }
        if (famineWatch != null) {
            famineWatch.replaceAll(readFamineDays(data.getConfigurationSection("granary.famine-announced")));
        }
    }

    public void saveFrom(KingdomService service) {
        FileConfiguration data = new YamlConfiguration();

        for (Kingdom kingdom : service.listKingdoms()) {
            String path = "kingdoms." + kingdom.getId();
            data.set(path + ".display-name", kingdom.getDisplayName());
            data.set(path + ".world", kingdom.getWorldName());
            data.set(path + ".worldguard-region", kingdom.getWorldGuardRegion());
            data.set(path + ".granary-region", kingdom.getGranaryRegion());
            if (kingdom.getGranaryWheat() > 0) {
                data.set(path + ".granary-wheat", kingdom.getGranaryWheat());
            }
            writeTeleports(data, path + ".teleports", kingdom.getTeleportsView());
            writeParliament(data, path + ".parliament", kingdom);
            writePolice(data, path + ".police", kingdom);
            writeCity(data, path + ".city", kingdom);
            writeChurch(data, path + ".church", kingdom);
            writeReigns(data, path + ".reigns", kingdom.getReignHistory().view());
            if (mechanicalJusticeService != null) {
                writeWarrants(
                        data,
                        path + ".police.warrants",
                        mechanicalJusticeService.warrantsView().stream()
                                .filter(warrant -> kingdom.getId().equals(warrant.kingdomId()))
                                .toList());
            }
        }

        for (PlayerMembership membership : service.getMembershipsView().values()) {
            String path = "players." + membership.getPlayerId();
            data.set(path + ".kingdom", membership.getKingdomId());
            if (membership.hasNobleTitle()) {
                data.set(path + ".rank", membership.getRank().name().toLowerCase());
                data.set(path + ".title-style", membership.getTitleStyle().name().toLowerCase());
            }
        }

        if (calendarService != null) {
            data.set("calendar.epoch-world-day", calendarService.epochWorldDay());
            data.set("calendar.last-seen-realm-day", calendarService.currentRealmDay());
            data.set("calendar.last-season-turn-day", calendarService.seasonTurn().lastAnnouncedDay());
        }
        if (shortfallWatch != null) {
            data.set("granary.last-shortfall-warning-day", shortfallWatch.lastWarnedDay());
        }
        if (loyaltyStore != null) {
            writeLoyalty(data, "loyalty", loyaltyStore.allTiersView());
            writeLoyaltyMarks(data, "loyalty-clocks", loyaltyStore.allMarksView());
        }
        if (moraleStore != null) {
            writeMorale(data, "morale", moraleStore.allTiersView());
            writeMoraleMarks(data, "morale-clocks", moraleStore.allMarksView());
        }
        if (warService != null) {
            writeWars(data, "wars", warService.activeWarsView());
        }
        if (standingRosterStore != null) {
            writeRosters(data, "standing-roster", standingRosterStore.allRostersView());
            writeOnDutyStates(data, "on-duty", standingRosterStore.allOnDutyStatesView());
        }
        if (levyArrearsStore != null) {
            writeLevyArrears(data, "levy-arrears", levyArrearsStore.allView());
        }
        if (coldLedgerStore != null) {
            writeColdDays(data, "cold-days", coldLedgerStore.allView());
        }
        if (hungerLedgerStore != null) {
            writeHungryDays(data, "hungry-days", hungerLedgerStore.allView());
        }
        if (famineWatch != null) {
            writeFamineDays(data, "granary.famine-announced", famineWatch.allView());
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
        sites.speakerChair().ifPresent(chair -> writeChamber(config, path + ".speaker-chair", chair));
        sites.bar().ifPresent(bar -> writeChamber(config, path + ".bar", bar));
        sites.registrar().ifPresent(registrar -> writeRegistrar(config, path + ".registrar", registrar));

        ParliamentState state = kingdom.getParliamentState();
        config.set(path + ".session-open", state.isSessionOpen());
        config.set(
                path + ".state-opening-pending-since-mc-day",
                state.stateOpeningPendingSinceMcDay().orElse(-1L));
        config.set(
                path + ".last-premier-questions-mc-day",
                state.lastPremierQuestionsMcDay().orElse(-1L));
        config.set(
                path + ".confidence-cooldown-until-mc-day",
                state.confidenceCooldownUntilMcDay().orElse(-1L));
        state.pendingMotionSecond().ifPresent(pending -> {
            config.set(path + ".pending-motion-second.bill", pending.billId());
            config.set(path + ".pending-motion-second.moved-by", pending.proposedBy().toString());
            config.set(path + ".pending-motion-second.offered-at", pending.offeredAtMs());
        });
        config.set(
                path + ".speaker-villager",
                state.speakerVillagerEntityId().map(UUID::toString).orElse(null));
        state.preparedMint().ifPresent(mint -> {
            String mintPath = path + ".prepared-mint";
            config.set(mintPath + ".world", mint.worldName());
            config.set(mintPath + ".x", mint.x());
            config.set(mintPath + ".y", mint.y());
            config.set(mintPath + ".z", mint.z());
            config.set(mintPath + ".yaw", (double) mint.yaw());
        });
        state.preparedPublicWork().ifPresent(work -> {
            String workPath = path + ".prepared-public-work";
            config.set(workPath + ".type", work.estateType().configKey());
            config.set(workPath + ".world", work.worldName());
            config.set(workPath + ".x", work.x());
            config.set(workPath + ".y", work.y());
            config.set(workPath + ".z", work.z());
        });
        state.currentBill().ifPresent(bill -> writeBill(config, path + ".current-bill", bill));
        writeActs(config, path + ".acts", state.assentedActsView());
        writeHansard(config, path + ".hansard", state.hansardView());
        writeElection(config, path, kingdom);
        writeFlag(config, path + ".flag", kingdom.getFlag());
    }

    static void writeFlag(FileConfiguration config, String path, Optional<KingdomFlag> flag) {
        if (flag.isEmpty()) {
            return;
        }
        KingdomFlag design = flag.get();
        config.set(path + ".base", design.baseMaterial());
        List<Map<String, Object>> layers = new ArrayList<>();
        for (KingdomFlag.Layer layer : design.layers()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("pattern", layer.patternId());
            entry.put("colour", layer.colour());
            layers.add(entry);
        }
        config.set(path + ".layers", layers);
    }

    static void readParliament(ConfigurationSection section, Kingdom kingdom) {
        if (section == null) {
            return;
        }
        var sites = kingdom.getParliamentSites();
        readChamber(section.getConfigurationSection("commons")).ifPresent(sites::setCommons);
        readChamber(section.getConfigurationSection("lords")).ifPresent(sites::setLords);
        readChamber(section.getConfigurationSection("speaker-chair")).ifPresent(sites::setSpeakerChair);
        readChamber(section.getConfigurationSection("bar")).ifPresent(sites::setBar);
        readRegistrar(section.getConfigurationSection("registrar")).ifPresent(sites::setRegistrar);

        ParliamentState state = kingdom.getParliamentState();
        state.setSessionOpen(section.getBoolean("session-open", true));
        long pendingSince = section.getLong("state-opening-pending-since-mc-day", -1L);
        if (pendingSince >= 0) {
            state.awaitStateOpening(pendingSince);
        } else {
            state.clearStateOpeningPending();
        }
        long lastQuestions = section.getLong("last-premier-questions-mc-day", -1L);
        if (lastQuestions >= 0) {
            state.recordPremierQuestions(lastQuestions);
        } else {
            state.clearPremierQuestions();
        }
        long cooldownUntil = section.getLong("confidence-cooldown-until-mc-day", -1L);
        if (cooldownUntil >= 0) {
            state.startConfidenceCooldown(cooldownUntil);
        } else {
            state.clearConfidenceCooldown();
        }
        ConfigurationSection pendingSecond = section.getConfigurationSection("pending-motion-second");
        if (pendingSecond != null && pendingSecond.getString("bill") != null) {
            state.setPendingMotionSecond(new dev.mrlemoos.kingdom.model.parliament.PendingMotionSecond(
                    pendingSecond.getString("bill"),
                    UUID.fromString(pendingSecond.getString("moved-by")),
                    pendingSecond.getLong("offered-at")));
        } else {
            state.clearPendingMotionSecond();
        }
        String speakerVillager = section.getString("speaker-villager");
        state.setSpeakerVillagerEntityId(speakerVillager != null ? UUID.fromString(speakerVillager) : null);
        readMint(section.getConfigurationSection("prepared-mint")).ifPresent(state::setPreparedMint);
        readPreparedPublicWork(section.getConfigurationSection("prepared-public-work"))
                .ifPresent(state::setPreparedPublicWork);
        readBill(section.getConfigurationSection("current-bill")).ifPresent(state::setCurrentBill);
        state.replaceAssentedActs(readActs(section.getConfigurationSection("acts")));
        state.replaceHansard(readHansard(section.getConfigurationSection("hansard")));
        readElection(section, kingdom);
        readFlag(section.getConfigurationSection("flag")).ifPresent(kingdom::setFlag);
    }

    static Optional<KingdomFlag> readFlag(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        String base = section.getString("base");
        if (base == null || base.isBlank()) {
            return Optional.empty();
        }
        List<KingdomFlag.Layer> layers = new ArrayList<>();
        List<?> rawLayers = section.getList("layers");
        if (rawLayers != null) {
            for (Object raw : rawLayers) {
                if (!(raw instanceof Map<?, ?> map)) {
                    continue;
                }
                Object pattern = map.get("pattern");
                Object colour = map.get("colour");
                if (pattern == null || colour == null) {
                    continue;
                }
                String patternId = String.valueOf(pattern).trim();
                String colourName = String.valueOf(colour).trim();
                if (patternId.isEmpty() || colourName.isEmpty()) {
                    continue;
                }
                layers.add(new KingdomFlag.Layer(patternId, colourName));
            }
        }
        return Optional.of(new KingdomFlag(base.trim(), layers));
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
            if (seat.returnCount().isPresent()) {
                config.set(seatPath + ".returned", seat.returnCount().getAsInt());
            } else {
                config.set(seatPath + ".returned", null);
            }
            if (seat.declaration().isPresent()) {
                var declared = seat.declaration().get();
                config.set(seatPath + ".manifesto", declared.manifesto());
                config.set(seatPath + ".party", declared.partyName());
                config.set(seatPath + ".party-colour", declared.partyColour());
            } else {
                config.set(seatPath + ".manifesto", null);
                config.set(seatPath + ".party", null);
                config.set(seatPath + ".party-colour", null);
            }
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
        for (var declared : election.declarationsView().entrySet()) {
            String declarationPath = electionPath + ".declarations." + declared.getKey();
            config.set(declarationPath + ".manifesto", declared.getValue().manifesto());
            config.set(declarationPath + ".party", declared.getValue().partyName());
            config.set(declarationPath + ".party-colour", declared.getValue().partyColour());
        }
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
                if (seatSection.contains("returned")) {
                    seat.setReturnCount(seatSection.getInt("returned"));
                }
                if (seatSection.contains("manifesto") || seatSection.contains("party")) {
                    seat.setDeclaration(new CandidateDeclaration(
                            seatSection.getString("manifesto", ""),
                            seatSection.getString("party", ""),
                            seatSection.getString("party-colour", CandidateDeclaration.DEFAULT_PARTY_COLOUR)));
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
        Map<UUID, CandidateDeclaration> declarations = new HashMap<>();
        ConfigurationSection declarationsSection = electionSection.getConfigurationSection("declarations");
        if (declarationsSection != null) {
            for (String candidate : declarationsSection.getKeys(false)) {
                declarations.put(
                        UUID.fromString(candidate),
                        new CandidateDeclaration(
                                declarationsSection.getString(candidate + ".manifesto", ""),
                                declarationsSection.getString(candidate + ".party", ""),
                                declarationsSection.getString(
                                        candidate + ".party-colour", CandidateDeclaration.DEFAULT_PARTY_COLOUR)));
            }
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
                speakerTieChoice,
                declarations);
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
                section.getInt("z"),
                (float) section.getDouble("yaw"),
                null));
    }

    private static Optional<PreparedPublicWork> readPreparedPublicWork(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        String world = section.getString("world");
        Optional<WealthBlockType> type = WealthBlockType.fromConfigKey(section.getString("type"));
        if (world == null || type.isEmpty()) {
            return Optional.empty();
        }
        WealthBlockType estateType = type.get();
        if (!estateType.isEstate()) {
            return Optional.empty();
        }
        return Optional.of(new PreparedPublicWork(
                estateType,
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
        writeConductProvisions(config, path + ".conduct", bill.conductProvisions());
        for (Map.Entry<UUID, VoteChoice> vote : bill.votesView().entrySet()) {
            config.set(path + ".votes." + vote.getKey() + ".choice", vote.getValue().name().toLowerCase());
        }
        bill.speakerCastingVote()
                .ifPresent(choice -> config.set(path + ".speaker-casting-vote", choice.name().toLowerCase()));
        bill.divisionClosesOnMcDay().ifPresent(day -> config.set(path + ".division-closes-on-mc-day", day));
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
        List<ConductProvision> conduct = readConductProvisions(section.get("conduct"));
        Bill bill = new Bill(id, kingdomId, type, title, state, proposer, payload, tabledAt, conduct);
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
        long closesOn = section.getLong("division-closes-on-mc-day", -1L);
        if (closesOn >= 0) {
            bill.setDivisionClosesOnMcDay(closesOn);
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
                config.set(path + ".villager-wallet-interest", fiscal.rates().villagerWalletInterest());
                config.set(path + ".tariff", fiscal.rates().tariff());
            }
            case BillPayload.Budget budget -> config.set(path + ".amount", budget.amount());
            case BillPayload.SpendMint mint -> {
                config.set(path + ".world", mint.mintLocation().worldName());
                config.set(path + ".x", mint.mintLocation().x());
                config.set(path + ".y", mint.mintLocation().y());
                config.set(path + ".z", mint.mintLocation().z());
                config.set(path + ".yaw", (double) mint.mintLocation().yaw());
                config.set(path + ".cost", mint.cost());
            }
            case BillPayload.SpendPublicWork work -> {
                config.set(path + ".type", work.estateType().configKey());
                config.set(path + ".world", work.worldName());
                config.set(path + ".x", work.x());
                config.set(path + ".y", work.y());
                config.set(path + ".z", work.z());
                config.set(path + ".cost", work.cost());
            }
            case BillPayload.SpendStipend stipend -> {
                config.set(path + ".recipient", stipend.recipientId().toString());
                config.set(path + ".amount", stipend.amount());
                config.set(path + ".reason", stipend.reason());
            }
            case BillPayload.War war -> {
                config.set(path + ".target", war.targetKingdomId());
                config.set(path + ".aim", war.aim().name().toLowerCase(Locale.ROOT));
                config.set(path + ".outcome", war.outcome().name().toLowerCase(Locale.ROOT));
                config.set(path + ".muster-deadline-mc-days", war.musterDeadlineMcDays());
            }
            case BillPayload.Peace peace -> config.set(path + ".war-id", peace.warId());
            case BillPayload.NoConfidence motion -> config.set(path + ".moved-by", motion.proposerId().toString());
            case BillPayload.Referendum referendum -> {
                config.set(path + ".question", referendum.question());
                config.set(path + ".called-by", referendum.calledBy().toString());
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
                    section.getDouble("villager-wallet-interest", 0.0),
                    section.getDouble("tariff", 0.0),
                    FiscalRates.defaults().rankModifiers()));
            case BUDGET -> new BillPayload.Budget(section.getDouble("amount"));
            case SPEND_MINT -> new BillPayload.SpendMint(
                    new MintLocation(
                            section.getString("world"),
                            section.getInt("x"),
                            section.getInt("y"),
                            section.getInt("z"),
                            (float) section.getDouble("yaw"),
                            null),
                    section.getDouble("cost"));
            case SPEND_PUBLIC_WORK -> {
                Optional<WealthBlockType> estateType =
                        WealthBlockType.fromConfigKey(section.getString("type"));
                if (estateType.isEmpty() || !estateType.get().isEstate()) {
                    throw new IllegalArgumentException(
                            "Invalid public work estate type: " + section.getString("type"));
                }
                yield new BillPayload.SpendPublicWork(
                        estateType.get(),
                        section.getString("world"),
                        section.getInt("x"),
                        section.getInt("y"),
                        section.getInt("z"),
                        section.getDouble("cost"));
            }
            case SPEND_STIPEND -> new BillPayload.SpendStipend(
                    UUID.fromString(section.getString("recipient")),
                    section.getDouble("amount"),
                    section.getString("reason"));
            case WAR -> new BillPayload.War(
                    section.getString("target"),
                    WarAim.valueOf(section.getString("aim", "territory_threshold").toUpperCase(Locale.ROOT)),
                    WarOutcome.valueOf(section.getString("outcome", "annexation").toUpperCase(Locale.ROOT)),
                    section.getInt("muster-deadline-mc-days"));
            case PEACE -> new BillPayload.Peace(section.getString("war-id"));
            case NO_CONFIDENCE -> new BillPayload.NoConfidence(UUID.fromString(section.getString("moved-by")));
            case REFERENDUM -> new BillPayload.Referendum(
                    section.getString("question", ""),
                    UUID.fromString(section.getString("called-by")));
        };
    }

    private static void writeHansard(FileConfiguration config, String path, List<HansardRecord> records) {
        config.set(path, null);
        for (int index = 0; index < records.size(); index++) {
            HansardRecord record = records.get(index);
            String recordPath = path + "." + index;
            config.set(recordPath + ".title", record.title());
            config.set(recordPath + ".business", record.business());
            config.set(recordPath + ".carried", record.carried());
            config.set(recordPath + ".aye", record.aye());
            config.set(recordPath + ".nay", record.nay());
            config.set(recordPath + ".abstain", record.abstain());
            config.set(recordPath + ".electorate", record.electorate());
            config.set(recordPath + ".mc-day", record.decidedOnMcDay());
            List<DivisionBloc> blocs = record.blocs();
            for (int blocIndex = 0; blocIndex < blocs.size(); blocIndex++) {
                DivisionBloc bloc = blocs.get(blocIndex);
                String blocPath = recordPath + ".blocs." + blocIndex;
                config.set(blocPath + ".kind", bloc.kind().name().toLowerCase(Locale.ROOT));
                config.set(blocPath + ".label", bloc.label());
                config.set(blocPath + ".colour", bloc.colour());
                config.set(blocPath + ".aye", bloc.aye());
                config.set(blocPath + ".nay", bloc.nay());
                config.set(blocPath + ".abstain", bloc.abstain());
            }
        }
    }

    private static List<HansardRecord> readHansard(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        List<String> keys = new ArrayList<>(section.getKeys(false));
        keys.sort(Comparator.comparingInt(Integer::parseInt));
        List<HansardRecord> records = new ArrayList<>();
        for (String key : keys) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            List<DivisionBloc> blocs = new ArrayList<>();
            ConfigurationSection blocSection = entry.getConfigurationSection("blocs");
            if (blocSection != null) {
                List<String> blocKeys = new ArrayList<>(blocSection.getKeys(false));
                blocKeys.sort(Comparator.comparingInt(Integer::parseInt));
                for (String blocKey : blocKeys) {
                    ConfigurationSection bloc = blocSection.getConfigurationSection(blocKey);
                    if (bloc == null) {
                        continue;
                    }
                    blocs.add(new DivisionBloc(
                            DivisionBlocKind.valueOf(
                                    bloc.getString("kind", "party").toUpperCase(Locale.ROOT)),
                            bloc.getString("label", ""),
                            bloc.getString("colour", ""),
                            bloc.getInt("aye"),
                            bloc.getInt("nay"),
                            bloc.getInt("abstain")));
                }
            }
            records.add(new HansardRecord(
                    entry.getString("title", ""),
                    entry.getString("business", ""),
                    entry.getBoolean("carried"),
                    entry.getInt("aye"),
                    entry.getInt("nay"),
                    entry.getInt("abstain"),
                    entry.getInt("electorate"),
                    blocs,
                    entry.getLong("mc-day")));
        }
        return List.copyOf(records);
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
            writeConductProvisions(config, actPath + ".conduct", act.conductProvisions());
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
                    entry.getInt("shelf.slot"),
                    readConductProvisions(entry.get("conduct"))));
        }
        return acts;
    }

    private static void writeConductProvisions(
            FileConfiguration config, String path, List<ConductProvision> provisions) {
        if (provisions == null || provisions.isEmpty()) {
            return;
        }
        List<String> kinds = new ArrayList<>();
        for (ConductProvision provision : provisions) {
            kinds.add(provision.kind().name().toLowerCase(Locale.ROOT));
        }
        config.set(path, kinds);
    }

    private static List<ConductProvision> readConductProvisions(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<ConductProvision> provisions = new ArrayList<>();
        for (Object entry : list) {
            if (entry == null) {
                continue;
            }
            String kindName = entry.toString().trim().toUpperCase(Locale.ROOT);
            if (kindName.isEmpty()) {
                continue;
            }
            provisions.add(new ConductProvision(ConductKind.valueOf(kindName)));
        }
        return List.copyOf(provisions);
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

    static void writePolice(FileConfiguration config, String path, Kingdom kingdom) {
        var police = kingdom.getPoliceState();
        if (!police.constablesView().isEmpty()) {
            config.set(path + ".constables", police.constablesView().stream().map(id -> id.toString()).toList());
        }
        if (!police.judgesView().isEmpty()) {
            config.set(path + ".judges", police.judgesView().stream().map(id -> id.toString()).toList());
        }
        for (var entry : police.cellsView().entrySet()) {
            PrisonCellLocation cell = entry.getValue();
            String cellPath = path + ".cells." + entry.getKey();
            config.set(cellPath + ".world", cell.worldName());
            config.set(cellPath + ".x", cell.x());
            config.set(cellPath + ".y", cell.y());
            config.set(cellPath + ".z", cell.z());
        }
        police.court().ifPresent(court -> {
            config.set(path + ".court.world", court.worldName());
            config.set(path + ".court.x", court.x());
            config.set(path + ".court.y", court.y());
            config.set(path + ".court.z", court.z());
            config.set(path + ".court.yaw", (double) court.yaw());
        });
        police.judgeEntityId().ifPresent(id -> config.set(path + ".judge-entity", id.toString()));
        if (!police.patrolGolemsView().isEmpty()) {
            config.set(path + ".patrol-golems", police.patrolGolemsView().stream().map(id -> id.toString()).toList());
        }
        if (!police.guardGolemsView().isEmpty()) {
            config.set(path + ".guard-golems", police.guardGolemsView().stream().map(id -> id.toString()).toList());
        }
    }

    static void readPolice(ConfigurationSection section, Kingdom kingdom) {
        if (section == null) {
            return;
        }
        var police = kingdom.getPoliceState();
        Set<UUID> constables = new LinkedHashSet<>();
        for (String id : section.getStringList("constables")) {
            constables.add(UUID.fromString(id));
        }
        police.replaceConstables(constables);

        Set<UUID> judges = new LinkedHashSet<>();
        for (String id : section.getStringList("judges")) {
            judges.add(UUID.fromString(id));
        }
        police.replaceJudges(judges);

        ConfigurationSection cellsSection = section.getConfigurationSection("cells");
        if (cellsSection != null) {
            Map<Integer, PrisonCellLocation> cells = new HashMap<>();
            for (String key : cellsSection.getKeys(false)) {
                ConfigurationSection cellSection = cellsSection.getConfigurationSection(key);
                if (cellSection == null) {
                    continue;
                }
                String world = cellSection.getString("world");
                if (world == null) {
                    continue;
                }
                cells.put(
                        Integer.parseInt(key),
                        new PrisonCellLocation(
                                world,
                                cellSection.getInt("x"),
                                cellSection.getInt("y"),
                                cellSection.getInt("z")));
            }
            police.replaceCells(cells);
        }

        ConfigurationSection courtSection = section.getConfigurationSection("court");
        if (courtSection != null) {
            String world = courtSection.getString("world");
            if (world != null) {
                police.setCourt(new CourtLocation(
                        world,
                        courtSection.getInt("x"),
                        courtSection.getInt("y"),
                        courtSection.getInt("z"),
                        (float) courtSection.getDouble("yaw")));
            }
        }

        String judgeEntity = section.getString("judge-entity");
        if (judgeEntity != null && !judgeEntity.isBlank()) {
            police.setJudgeEntityId(UUID.fromString(judgeEntity));
        }

        Set<UUID> patrolGolems = new LinkedHashSet<>();
        for (String id : section.getStringList("patrol-golems")) {
            patrolGolems.add(UUID.fromString(id));
        }
        police.replacePatrolGolems(patrolGolems);

        Set<UUID> guardGolems = new LinkedHashSet<>();
        for (String id : section.getStringList("guard-golems")) {
            guardGolems.add(UUID.fromString(id));
        }
        police.replaceGuardGolems(guardGolems);
    }

    static void writeChurch(FileConfiguration config, String path, Kingdom kingdom) {
        KingdomChurchState church = kingdom.getChurchState();
        Optional<ChurchSite> site = church.church();
        if (site.isPresent()) {
            ChurchSite altar = site.get();
            config.set(path + ".site.world", altar.worldName());
            config.set(path + ".site.x", altar.x());
            config.set(path + ".site.y", altar.y());
            config.set(path + ".site.z", altar.z());
            config.set(path + ".site.yaw", (double) altar.yaw());
            config.set(path + ".site.pitch", (double) altar.pitch());
            config.set(path + ".consecrated", church.isConsecrated());
        }
        church.priestId().ifPresent(priest -> config.set(path + ".priest", priest.toString()));
        church.clericEntityId().ifPresent(cleric -> config.set(path + ".cleric-entity", cleric.toString()));
        church.crownedMonarchId().ifPresent(monarch -> config.set(path + ".crowned", monarch.toString()));
        church.lastMassDay().ifPresent(day -> config.set(path + ".last-mass", day));

        List<Marriage> marriages = church.marriagesView();
        for (int i = 0; i < marriages.size(); i++) {
            String marriagePath = path + ".marriages." + i;
            config.set(marriagePath + ".first", marriages.get(i).first().toString());
            config.set(marriagePath + ".second", marriages.get(i).second().toString());
            config.set(marriagePath + ".wedded-at", marriages.get(i).weddedAtMs());
        }
        for (var entry : church.funeralRecordsView().entrySet()) {
            String recordPath = path + ".funerals." + entry.getKey();
            config.set(recordPath + ".experience", entry.getValue().heldExperience());
            config.set(recordPath + ".died-on-day", entry.getValue().diedOnDay());
        }
        for (var entry : church.villagerFuneralRecordsView().entrySet()) {
            String recordPath = path + ".villager-funerals." + entry.getKey();
            config.set(recordPath + ".balance", entry.getValue().heldBalance());
            config.set(recordPath + ".died-on-day", entry.getValue().diedOnDay());
        }
    }

    static void readChurch(ConfigurationSection section, Kingdom kingdom) {
        if (section == null) {
            return;
        }
        KingdomChurchState church = kingdom.getChurchState();
        ConfigurationSection site = section.getConfigurationSection("site");
        if (site != null) {
            String world = site.getString("world");
            if (world != null) {
                church.setChurch(new ChurchSite(
                        world,
                        site.getDouble("x"),
                        site.getDouble("y"),
                        site.getDouble("z"),
                        (float) site.getDouble("yaw"),
                        (float) site.getDouble("pitch")));
                church.restoreConsecration(section.getBoolean("consecrated", false));
            }
        }
        String priest = section.getString("priest");
        if (priest != null && !priest.isBlank()) {
            church.swearPriest(UUID.fromString(priest));
        }
        String cleric = section.getString("cleric-entity");
        if (cleric != null && !cleric.isBlank()) {
            church.setClericEntityId(UUID.fromString(cleric));
        }
        String crowned = section.getString("crowned");
        if (crowned != null && !crowned.isBlank()) {
            church.crown(UUID.fromString(crowned));
        }
        if (section.contains("last-mass")) {
            church.setLastMassDay(section.getLong("last-mass"));
        }

        ConfigurationSection marriages = section.getConfigurationSection("marriages");
        if (marriages != null) {
            List<Marriage> loaded = new ArrayList<>();
            for (String key : marriages.getKeys(false)) {
                ConfigurationSection entry = marriages.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                String first = entry.getString("first");
                String second = entry.getString("second");
                if (first == null || second == null) {
                    continue;
                }
                loaded.add(new Marriage(
                        UUID.fromString(first), UUID.fromString(second), entry.getLong("wedded-at")));
            }
            church.replaceMarriages(loaded);
        }

        ConfigurationSection funerals = section.getConfigurationSection("funerals");
        if (funerals != null) {
            Map<UUID, FuneralRecord> loaded = new LinkedHashMap<>();
            for (String key : funerals.getKeys(false)) {
                ConfigurationSection entry = funerals.getConfigurationSection(key);
                if (entry != null) {
                    loaded.put(
                            UUID.fromString(key),
                            new FuneralRecord(entry.getInt("experience"), entry.getLong("died-on-day")));
                }
            }
            church.replaceFuneralRecords(loaded);
        }

        ConfigurationSection villagerFunerals = section.getConfigurationSection("villager-funerals");
        if (villagerFunerals != null) {
            Map<UUID, VillagerFuneralRecord> loaded = new LinkedHashMap<>();
            for (String key : villagerFunerals.getKeys(false)) {
                ConfigurationSection entry = villagerFunerals.getConfigurationSection(key);
                if (entry != null) {
                    loaded.put(
                            UUID.fromString(key),
                            new VillagerFuneralRecord(
                                    entry.getDouble("balance"), entry.getLong("died-on-day")));
                }
            }
            church.replaceVillagerFuneralRecords(loaded);
        }
    }

    static void writeCity(FileConfiguration config, String path, Kingdom kingdom) {
        var city = kingdom.getCityState();
        Optional<CapitalLocation> capital = city.capital();
        if (capital.isPresent()) {
            CapitalLocation seat = capital.get();
            config.set(path + ".capital.world", seat.worldName());
            config.set(path + ".capital.x", seat.x());
            config.set(path + ".capital.y", seat.y());
            config.set(path + ".capital.z", seat.z());
            config.set(path + ".capital.yaw", (double) seat.yaw());
            config.set(path + ".capital.pitch", (double) seat.pitch());
        }
        Optional<UUID> mayor = city.lordMayorEntityId();
        if (mayor.isPresent()) {
            config.set(path + ".lord-mayor-entity", mayor.get().toString());
        }
        Optional<UUID> crier = city.townCrierEntityId();
        if (crier.isPresent()) {
            config.set(path + ".town-crier-entity", crier.get().toString());
        }
        Optional<CapitalLocation> crierStand = city.townCrierStand();
        if (crierStand.isPresent()) {
            CapitalLocation stand = crierStand.get();
            config.set(path + ".town-crier.world", stand.worldName());
            config.set(path + ".town-crier.x", stand.x());
            config.set(path + ".town-crier.y", stand.y());
            config.set(path + ".town-crier.z", stand.z());
            config.set(path + ".town-crier.yaw", (double) stand.yaw());
            config.set(path + ".town-crier.pitch", (double) stand.pitch());
        }
        for (var entry : city.permitsView().entrySet()) {
            config.set(path + ".permits." + entry.getKey(), entry.getValue());
        }
        for (var entry : city.horsePermitsView().entrySet()) {
            config.set(path + ".horse-permits." + entry.getKey(), entry.getValue().toString());
        }
        List<GazettePost> posts = city.gazettePostsView();
        for (int i = 0; i < posts.size(); i++) {
            GazettePost post = posts.get(i);
            String postPath = path + ".gazette." + i;
            config.set(postPath + ".title", post.title());
            config.set(postPath + ".body", post.body());
            config.set(postPath + ".author", post.authorUuid().toString());
            config.set(postPath + ".mc-day", post.mcDay());
            config.set(postPath + ".kind", post.kind().name().toLowerCase(Locale.ROOT));
            Optional<GazetteCurfewWindow> curfew = post.curfew();
            if (curfew.isPresent()) {
                config.set(postPath + ".curfew.start", curfew.get().startTick());
                config.set(postPath + ".curfew.end", curfew.get().endTick());
            }
        }
        Optional<CurfewEnforcementConfig> decreeCurfew = city.decreeCurfew();
        if (decreeCurfew.isPresent()) {
            CurfewEnforcementConfig window = decreeCurfew.get();
            config.set(path + ".decree-curfew.enabled", window.enabled());
            config.set(path + ".decree-curfew.start", window.windowStartTick());
            config.set(path + ".decree-curfew.end", window.windowEndTick());
        }
    }

    static void readCity(ConfigurationSection section, Kingdom kingdom) {
        if (section == null) {
            return;
        }
        var city = kingdom.getCityState();
        ConfigurationSection capitalSection = section.getConfigurationSection("capital");
        if (capitalSection != null) {
            String world = capitalSection.getString("world");
            if (world != null) {
                city.setCapital(new CapitalLocation(
                        world,
                        capitalSection.getDouble("x"),
                        capitalSection.getDouble("y"),
                        capitalSection.getDouble("z"),
                        (float) capitalSection.getDouble("yaw"),
                        (float) capitalSection.getDouble("pitch")));
            }
        }

        String mayorEntity = section.getString("lord-mayor-entity");
        if (mayorEntity != null && !mayorEntity.isBlank()) {
            city.setLordMayorEntityId(UUID.fromString(mayorEntity));
        }

        String crierEntity = section.getString("town-crier-entity");
        if (crierEntity != null && !crierEntity.isBlank()) {
            city.setTownCrierEntityId(UUID.fromString(crierEntity));
        }

        ConfigurationSection crierStandSection = section.getConfigurationSection("town-crier");
        if (crierStandSection != null) {
            String world = crierStandSection.getString("world");
            if (world != null) {
                city.setTownCrierStand(new CapitalLocation(
                        world,
                        crierStandSection.getDouble("x"),
                        crierStandSection.getDouble("y"),
                        crierStandSection.getDouble("z"),
                        (float) crierStandSection.getDouble("yaw"),
                        (float) crierStandSection.getDouble("pitch")));
            }
        }

        ConfigurationSection permitsSection = section.getConfigurationSection("permits");
        if (permitsSection != null) {
            Map<UUID, Long> permits = new LinkedHashMap<>();
            for (String key : permitsSection.getKeys(false)) {
                permits.put(UUID.fromString(key), permitsSection.getLong(key));
            }
            city.replacePermits(permits);
        }

        ConfigurationSection horsePermitsSection = section.getConfigurationSection("horse-permits");
        if (horsePermitsSection != null) {
            Map<UUID, UUID> horsePermits = new LinkedHashMap<>();
            for (String key : horsePermitsSection.getKeys(false)) {
                String ownerId = horsePermitsSection.getString(key);
                if (ownerId != null && !ownerId.isBlank()) {
                    horsePermits.put(UUID.fromString(key), UUID.fromString(ownerId));
                }
            }
            city.replaceHorsePermits(horsePermits);
        }

        ConfigurationSection gazetteSection = section.getConfigurationSection("gazette");
        if (gazetteSection != null) {
            List<String> keys = new ArrayList<>(gazetteSection.getKeys(false));
            keys.sort(Comparator.comparingInt(key -> {
                try {
                    return Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    return Integer.MAX_VALUE;
                }
            }));
            List<GazettePost> posts = new ArrayList<>();
            for (String key : keys) {
                ConfigurationSection postSection = gazetteSection.getConfigurationSection(key);
                if (postSection == null) {
                    continue;
                }
                String author = postSection.getString("author");
                if (author == null || author.isBlank()) {
                    continue;
                }
                GazettePostKind kind = parseGazetteKind(postSection.getString("kind"));
                Optional<GazetteCurfewWindow> curfew = Optional.empty();
                ConfigurationSection curfewSection = postSection.getConfigurationSection("curfew");
                if (curfewSection != null) {
                    curfew = Optional.of(new GazetteCurfewWindow(
                            curfewSection.getLong("start"), curfewSection.getLong("end")));
                }
                posts.add(new GazettePost(
                        postSection.getString("title", ""),
                        postSection.getString("body", ""),
                        UUID.fromString(author),
                        postSection.getLong("mc-day"),
                        kind,
                        curfew));
            }
            city.replaceGazettePosts(posts);
        }

        ConfigurationSection decreeCurfewSection = section.getConfigurationSection("decree-curfew");
        if (decreeCurfewSection != null) {
            boolean enabled = decreeCurfewSection.getBoolean("enabled", true);
            long start = decreeCurfewSection.getLong("start", 13_000L);
            long end = decreeCurfewSection.getLong("end", 23_000L);
            city.setDecreeCurfew(enabled
                    ? CurfewEnforcementConfig.enabled(start, end)
                    : CurfewEnforcementConfig.disabled(start, end));
        }
    }

    private static GazettePostKind parseGazetteKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return GazettePostKind.ANNOUNCEMENT;
        }
        try {
            return GazettePostKind.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return GazettePostKind.ANNOUNCEMENT;
        }
    }

    static void writeWarrants(FileConfiguration config, String path, List<Warrant> warrants) {
        if (warrants == null || warrants.isEmpty()) {
            return;
        }
        for (Warrant warrant : warrants) {
            String warrantPath = path + "." + warrant.id();
            config.set(warrantPath + ".suspect", warrant.suspectId().toString());
            config.set(warrantPath + ".act-bill-id", warrant.actBillId());
            config.set(warrantPath + ".provision-kind", warrant.provisionKind().name());
            config.set(warrantPath + ".status", warrant.status().name());
            config.set(warrantPath + ".opened-at-ms", warrant.openedAtMs());
            warrant.approvedBy().ifPresent(crownId ->
                    config.set(warrantPath + ".approved-by", crownId.toString()));
            warrant.arrestReward().ifPresent(reward -> {
                config.set(warrantPath + ".arrest-reward.poster", reward.posterId().toString());
                config.set(warrantPath + ".arrest-reward.amount", reward.amount());
            });
        }
    }

    static List<Warrant> readWarrants(ConfigurationSection section, String kingdomId) {
        if (section == null) {
            return List.of();
        }
        List<Warrant> warrants = new ArrayList<>();
        for (String warrantId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(warrantId);
            if (entry == null) {
                continue;
            }
            String suspect = entry.getString("suspect");
            String actBillId = entry.getString("act-bill-id");
            String provisionKind = entry.getString("provision-kind");
            String statusName = entry.getString("status");
            if (suspect == null || actBillId == null || provisionKind == null || statusName == null) {
                continue;
            }
            WarrantStatus status;
            try {
                status = WarrantStatus.valueOf(statusName);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConductKind kind;
            try {
                kind = ConductKind.valueOf(provisionKind);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            Warrant warrant = new Warrant(
                    warrantId,
                    kingdomId,
                    UUID.fromString(suspect),
                    actBillId,
                    kind,
                    status,
                    entry.getLong("opened-at-ms"));
            ConfigurationSection rewardSection = entry.getConfigurationSection("arrest-reward");
            if (rewardSection != null) {
                String poster = rewardSection.getString("poster");
                double amount = rewardSection.getDouble("amount");
                if (poster != null && amount > 0) {
                    warrant.setArrestReward(new ArrestReward(UUID.fromString(poster), amount));
                }
            }
            String approvedBy = entry.getString("approved-by");
            if (approvedBy != null && !approvedBy.isBlank()) {
                warrant.setApprovedBy(UUID.fromString(approvedBy));
            }
            warrants.add(warrant);
        }
        return warrants;
    }

    static void writeWars(FileConfiguration config, String path, Collection<ActiveWar> wars) {
        for (ActiveWar war : wars) {
            String warPath = path + "." + war.id();
            config.set(warPath + ".attacker", war.attackerKingdomId());
            config.set(warPath + ".defender", war.defenderKingdomId());
            config.set(warPath + ".aim", war.aim().name().toLowerCase(Locale.ROOT));
            config.set(warPath + ".outcome", war.outcome().name().toLowerCase(Locale.ROOT));
            config.set(warPath + ".started-at", war.startedAtMs());
            config.set(warPath + ".muster-deadline-at", war.musterDeadlineAtMs());
        }
    }

    static List<ActiveWar> readWars(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        List<ActiveWar> wars = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String attacker = entry.getString("attacker");
            String defender = entry.getString("defender");
            if (attacker == null || defender == null) {
                continue;
            }
            wars.add(new ActiveWar(
                    key,
                    attacker,
                    defender,
                    WarAim.valueOf(entry.getString("aim", "territory_threshold").toUpperCase(Locale.ROOT)),
                    WarOutcome.valueOf(entry.getString("outcome", "annexation").toUpperCase(Locale.ROOT)),
                    entry.getLong("started-at"),
                    entry.getLong("muster-deadline-at")));
        }
        return wars;
    }

    static void writeLoyalty(FileConfiguration config, String path, Map<UUID, LoyaltyTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, LoyaltyTier> entry : tiers.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (entry.getValue() == LoyaltyTier.FAITHFUL) {
                continue;
            }
            config.set(path + "." + entry.getKey(), entry.getValue().name().toLowerCase(Locale.ROOT));
        }
    }

    static Map<UUID, LoyaltyTier> readLoyalty(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<UUID, LoyaltyTier> tiers = new HashMap<>();
        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String tierName = section.getString(key);
                if (tierName == null || tierName.isBlank()) {
                    continue;
                }
                LoyaltyTier tier = LoyaltyTier.valueOf(tierName.toUpperCase(Locale.ROOT));
                if (tier != LoyaltyTier.FAITHFUL) {
                    tiers.put(playerId, tier);
                }
            } catch (IllegalArgumentException ignored) {
                // Skip malformed player or tier entries.
            }
        }
        return tiers;
    }

    static void writeMorale(FileConfiguration config, String path, Map<UUID, MoraleTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, MoraleTier> entry : tiers.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            config.set(path + "." + entry.getKey(), entry.getValue().name().toLowerCase(Locale.ROOT));
        }
    }

    static Map<UUID, MoraleTier> readMorale(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<UUID, MoraleTier> tiers = new HashMap<>();
        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String tierName = section.getString(key);
                if (tierName == null || tierName.isBlank()) {
                    continue;
                }
                tiers.put(playerId, MoraleTier.valueOf(tierName.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed player or tier entries.
            }
        }
        return tiers;
    }

    // Recovery clocks are written as a single "<tier>:<in-game day>" scalar per player, keeping the
    // existing flat tier sections untouched so old data.yml files load unchanged.
    static void writeLoyaltyMarks(
            FileConfiguration config, String path, Map<UUID, RecoveryMark<LoyaltyTier>> marks) {
        writeMarks(config, path, marks);
    }

    static Map<UUID, RecoveryMark<LoyaltyTier>> readLoyaltyMarks(ConfigurationSection section) {
        return readMarks(section, LoyaltyTier.class);
    }

    static void writeMoraleMarks(
            FileConfiguration config, String path, Map<UUID, RecoveryMark<MoraleTier>> marks) {
        writeMarks(config, path, marks);
    }

    static Map<UUID, RecoveryMark<MoraleTier>> readMoraleMarks(ConfigurationSection section) {
        return readMarks(section, MoraleTier.class);
    }

    private static <T extends Enum<T>> void writeMarks(
            FileConfiguration config, String path, Map<UUID, RecoveryMark<T>> marks) {
        if (marks == null || marks.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, RecoveryMark<T>> entry : marks.entrySet()) {
            RecoveryMark<T> mark = entry.getValue();
            if (entry.getKey() == null || mark == null || mark.tier() == null) {
                continue;
            }
            config.set(
                    path + "." + entry.getKey(),
                    mark.tier().name().toLowerCase(Locale.ROOT) + ":" + mark.mcDay());
        }
    }

    private static <T extends Enum<T>> Map<UUID, RecoveryMark<T>> readMarks(
            ConfigurationSection section, Class<T> tierType) {
        if (section == null) {
            return Map.of();
        }
        Map<UUID, RecoveryMark<T>> marks = new HashMap<>();
        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String raw = section.getString(key);
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                int separator = raw.lastIndexOf(':');
                if (separator < 0) {
                    continue;
                }
                T tier = Enum.valueOf(tierType, raw.substring(0, separator).toUpperCase(Locale.ROOT));
                marks.put(playerId, new RecoveryMark<>(tier, Long.parseLong(raw.substring(separator + 1))));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed player, tier, or day entries.
            }
        }
        return marks;
    }

    static void writeLevyArrears(FileConfiguration config, String path, Map<String, LevyArrears> arrears) {
        if (arrears == null || arrears.isEmpty()) {
            return;
        }
        for (Map.Entry<String, LevyArrears> entry : arrears.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String entryPath = path + "." + entry.getKey();
            config.set(entryPath + ".amount", entry.getValue().amount());
            config.set(entryPath + ".last-paid-day", entry.getValue().lastPaidDay());
            config.set(entryPath + ".warned", entry.getValue().warned());
        }
    }

    static Map<String, LevyArrears> readLevyArrears(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, LevyArrears> arrears = new HashMap<>();
        for (String kingdomId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(kingdomId);
            if (entry == null) {
                continue;
            }
            arrears.put(
                    kingdomId,
                    new LevyArrears(
                            entry.getDouble("amount", 0.0),
                            entry.getLong("last-paid-day", 0L),
                            entry.getBoolean("warned", false)));
        }
        return arrears;
    }

    /**
     * The run of cold days behind each villager. Nothing else of the hearths is kept: a hearth is a
     * fact about the world and the world holds it.
     */
    static void writeColdDays(FileConfiguration config, String path, Map<String, Map<UUID, Integer>> coldDays) {
        if (coldDays == null || coldDays.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Map<UUID, Integer>> kingdom : coldDays.entrySet()) {
            if (kingdom.getKey() == null || kingdom.getValue() == null) {
                continue;
            }
            for (Map.Entry<UUID, Integer> villager : kingdom.getValue().entrySet()) {
                if (villager.getKey() == null || villager.getValue() == null || villager.getValue().intValue() <= 0) {
                    continue;
                }
                config.set(path + "." + kingdom.getKey() + "." + villager.getKey(), villager.getValue());
            }
        }
    }

    static Map<String, Map<UUID, Integer>> readColdDays(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, Map<UUID, Integer>> coldDays = new HashMap<>();
        for (String kingdomId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(kingdomId);
            if (entry == null) {
                continue;
            }
            Map<UUID, Integer> villagers = new HashMap<>();
            for (String uuidString : entry.getKeys(false)) {
                try {
                    int days = entry.getInt(uuidString, 0);
                    if (days > 0) {
                        villagers.put(UUID.fromString(uuidString), Integer.valueOf(days));
                    }
                } catch (IllegalArgumentException ignored) {
                    // Skip malformed villager entries.
                }
            }
            if (!villagers.isEmpty()) {
                coldDays.put(kingdomId, villagers);
            }
        }
        return coldDays;
    }

    /**
     * The run of hungry days behind each villager, under a section of its own. Cold and hunger are
     * two privations and are answered separately, so their ledgers never share a key.
     */
    static void writeHungryDays(FileConfiguration config, String path, Map<String, Map<UUID, Integer>> hungryDays) {
        writeColdDays(config, path, hungryDays);
    }

    static Map<String, Map<UUID, Integer>> readHungryDays(ConfigurationSection section) {
        return readColdDays(section);
    }

    /** The day each realm's famine was announced, so no realm hears of the same famine twice. */
    static void writeFamineDays(FileConfiguration config, String path, Map<String, Long> famineDays) {
        if (famineDays == null || famineDays.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Long> entry : famineDays.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            config.set(path + "." + entry.getKey(), entry.getValue());
        }
    }

    static Map<String, Long> readFamineDays(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, Long> famineDays = new HashMap<>();
        for (String kingdomId : section.getKeys(false)) {
            famineDays.put(kingdomId, Long.valueOf(section.getLong(kingdomId, -1L)));
        }
        return famineDays;
    }

    static void writeRosters(FileConfiguration config, String path, Map<String, Set<UUID>> rosters) {
        if (rosters == null || rosters.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Set<UUID>> entry : rosters.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            config.set(
                    path + "." + entry.getKey(),
                    entry.getValue().stream().map(UUID::toString).toList());
        }
    }

    static Map<String, Set<UUID>> readRosters(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, Set<UUID>> rosters = new HashMap<>();
        for (String kingdomId : section.getKeys(false)) {
            Set<UUID> roster = new LinkedHashSet<>();
            for (String id : section.getStringList(kingdomId)) {
                try {
                    roster.add(UUID.fromString(id));
                } catch (IllegalArgumentException ignored) {
                    // Skip malformed player entries.
                }
            }
            if (!roster.isEmpty()) {
                rosters.put(kingdomId, roster);
            }
        }
        return rosters;
    }

    static void writeOnDutyStates(FileConfiguration config, String path, Map<UUID, OnDutyState> states) {
        if (states == null || states.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, OnDutyState> entry : states.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String statePath = path + "." + entry.getKey();
            config.set(statePath + ".morale", entry.getValue().moraleTier().name().toLowerCase(Locale.ROOT));
            config.set(statePath + ".hardened-service", entry.getValue().hardenedService());
        }
    }

    static Map<UUID, OnDutyState> readOnDutyStates(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<UUID, OnDutyState> states = new HashMap<>();
        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                MoraleTier tier = MoraleTier.valueOf(
                        entry.getString("morale", "steadfast").toUpperCase(Locale.ROOT));
                boolean hardenedService = entry.getBoolean("hardened-service", false);
                states.put(playerId, new OnDutyState(tier, hardenedService));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed player or morale entries.
            }
        }
        return states;
    }
}
