package dev.mrlemoos.kingdom.hub;

import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.city.gui.GazetteLiveState;
import dev.mrlemoos.kingdom.city.gui.GazetteLiveStateReader;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.model.election.PendingResignation;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.resignation.ResignationAuthority;
import dev.mrlemoos.kingdom.resignation.ResignationSummaries;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentService;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.war.oath.OathService;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.muster.MusterAnswer;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.conscription.ConscriptionService;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capital.CapitalRegion;
import dev.mrlemoos.kingdom.war.capital.CapitalService;
import dev.mrlemoos.kingdom.war.siege.SiegePresence;
import dev.mrlemoos.kingdom.war.siege.SiegePresenceService;
import dev.mrlemoos.kingdom.war.siege.SiegeStatusLine;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Reads the realm as it stands and hands the Realm Hub one plain snapshot. Every optional
 * collaborator may be absent — the hub then simply says a thing is not available rather than failing.
 */
public final class RealmHubSnapshotFactory {

    private final KingdomService kingdomService;
    private final GazetteLiveStateReader liveStateReader;
    private CityService cityService;
    private EconomyService economyService;
    private MechanicalJusticeService justiceService;
    private ParliamentService parliamentService;
    private StandingRosterService standingRosterService;
    private OathService oathService;
    private MoraleService moraleService;
    private MusterService musterService;
    private WarService warService;
    private ConscriptionService conscriptionService;
    private SiegePresenceService siegePresenceService;
    private ChunkCaptureService chunkCaptureService;
    private CapitalService capitalService;
    private boolean warEnabled;

    public RealmHubSnapshotFactory(KingdomService kingdomService, GazetteLiveStateReader liveStateReader) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.liveStateReader = liveStateReader;
    }

    public RealmHubSnapshotFactory withCityService(CityService cityService) {
        this.cityService = cityService;
        return this;
    }

    public RealmHubSnapshotFactory withEconomyService(EconomyService economyService) {
        this.economyService = economyService;
        return this;
    }

    public RealmHubSnapshotFactory withJusticeService(MechanicalJusticeService justiceService) {
        this.justiceService = justiceService;
        return this;
    }

    public RealmHubSnapshotFactory withParliamentService(ParliamentService parliamentService) {
        this.parliamentService = parliamentService;
        return this;
    }

    public RealmHubSnapshotFactory withStandingRosterService(StandingRosterService standingRosterService, boolean warEnabled) {
        this.standingRosterService = standingRosterService;
        this.warEnabled = warEnabled;
        return this;
    }

    public RealmHubSnapshotFactory withOathService(OathService oathService, MoraleService moraleService) {
        this.oathService = oathService;
        this.moraleService = moraleService;
        return this;
    }

    public RealmHubSnapshotFactory withMusterService(MusterService musterService, WarService warService) {
        this.musterService = musterService;
        this.warService = warService;
        return this;
    }

    public RealmHubSnapshotFactory withConscriptionService(ConscriptionService conscriptionService) {
        this.conscriptionService = conscriptionService;
        return this;
    }

    public RealmHubSnapshotFactory withSiegePresenceService(SiegePresenceService siegePresenceService) {
        this.siegePresenceService = siegePresenceService;
        return this;
    }

    public RealmHubSnapshotFactory withChunkCaptureService(ChunkCaptureService chunkCaptureService) {
        this.chunkCaptureService = chunkCaptureService;
        return this;
    }

    public RealmHubSnapshotFactory withCapitalService(CapitalService capitalService) {
        this.capitalService = capitalService;
        return this;
    }

    /** The realm as this subject sees it, right now. */
    public RealmHubSnapshot of(Player player) {
        RealmHubSnapshot.Builder builder = RealmHubSnapshot.builder().viewer(pointOf(player.getLocation()));
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            addOath(builder, player.getUniqueId());
            if (oathService != null) {
                oathService.swornOutsiderStore().find(player.getUniqueId()).flatMap(outsider -> kingdomService
                                .getKingdom(outsider.kingdomId()))
                        .ifPresent(kingdom -> addChurchAndPoliceSites(builder, kingdom));
            }
            return builder.member(false).build();
        }
        Optional<Kingdom> found = kingdomService.getKingdom(membership.get().getKingdomId());
        if (found.isEmpty()) {
            return builder.member(false).build();
        }
        Kingdom kingdom = found.get();
        PlayerMembership member = membership.get();
        builder.member(true)
                .kingdomName(kingdom.getDisplayName())
                .rank(member.getRank())
                .titleLabel(styleOf(member))
                .granaryRegion(kingdom.getGranaryRegion())
                .warCapitalRegion(warCapitalRegionOf(kingdom.getId()))
                .live(liveStateOf(kingdom));

        addCitySites(builder, kingdom, player.getUniqueId());
        addChurchAndPoliceSites(builder, kingdom);
        addParliamentSites(builder, kingdom);
        addMints(builder, kingdom);
        addStandingRoster(builder, kingdom, player.getUniqueId());
        builder.conscriptionEnabled(conscriptionService != null && conscriptionService.config().enabled());
        addOath(builder, player.getUniqueId());
        addMuster(builder, kingdom.getId(), player.getUniqueId());
        addSiege(builder, kingdom);
        addLiveBusiness(builder, kingdom, member, player.getUniqueId());
        return builder.build();
    }

    private String warCapitalRegionOf(String kingdomId) {
        if (capitalService == null) {
            return "";
        }
        Optional<CapitalRegion> capital = capitalService.getCapital(kingdomId);
        if (capital.isEmpty()) {
            return "";
        }
        return capital.get().regionId();
    }

    private void addMuster(RealmHubSnapshot.Builder builder, String kingdomId, UUID playerId) {
        if (musterService == null || warService == null) return;
        warService.activeWarFor(kingdomId).ifPresent(war -> {
            if (!musterService.isEligible(war.id(), playerId)) return;
            Optional<MusterAnswer> answer = musterService.answerOf(war.id(), playerId);
            builder.musterStatus(answer.isPresent()
                    ? "Your muster answer: " + answer.get().name().toLowerCase(java.util.Locale.UK) + "."
                    : "Your realm calls you to answer its muster.");
        });
    }

    private void addSiege(RealmHubSnapshot.Builder builder, Kingdom kingdom) {
        if (warService == null || !warService.config().enabled() || siegePresenceService == null
                || !siegePresenceService.config().enabled()) return;
        warService.activeWarFor(kingdom.getId()).ifPresent(war -> {
            SiegePresence present = siegePresenceService.presenceFor(war.id());
            int captured = chunkCaptureService == null
                    ? 0
                    : chunkCaptureService.capturedBy(war.id(), war.attackerKingdomId()).size();
            builder.siegeStatus(SiegeStatusLine.format(
                    kingdomDisplay(war.attackerKingdomId()),
                    present.attackerCount(),
                    kingdomDisplay(war.defenderKingdomId()),
                    present.defenderCount(),
                    captured));
        });
    }

    private String kingdomDisplay(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        return kingdom.isPresent() ? kingdom.get().getDisplayName() : kingdomId;
    }

    private void addOath(RealmHubSnapshot.Builder builder, UUID playerId) {
        builder.warEnabled(warEnabled);
        if (moraleService == null) {
            return;
        }
        Optional<dev.mrlemoos.kingdom.model.war.MoraleTier> tier = moraleService.tierOf(playerId);
        builder.oathOfService(tier.isPresent(), tier.map(value -> value.name().toLowerCase(java.util.Locale.UK)).orElse(""));
    }

    private void addStandingRoster(RealmHubSnapshot.Builder builder, Kingdom kingdom, UUID playerId) {
        if (standingRosterService == null) {
            builder.warEnabled(false).standingRoster(0, 0, false);
            return;
        }
        var roster = standingRosterService.rosterView(kingdom.getId());
        builder.warEnabled(warEnabled)
                .standingRoster(roster.size(), standingRosterService.config().rosterCap(), roster.contains(playerId));
    }

    private GazetteLiveState liveStateOf(Kingdom kingdom) {
        if (liveStateReader == null) {
            return new GazetteLiveState("", "none proclaimed", 0, kingdom.getCityState().permitCount(), 0d);
        }
        return liveStateReader.read(kingdom);
    }

    private static String styleOf(PlayerMembership membership) {
        if (membership.getRank() == null) {
            return "Citizen";
        }
        return membership.getRank().displayTitle(membership.getTitleStyle());
    }

    private void addCitySites(RealmHubSnapshot.Builder builder, Kingdom kingdom, UUID playerId) {
        Optional<CapitalLocation> capital = kingdom.getCityState().capital();
        if (capital.isPresent()) {
            builder.site(RealmHubTopic.PLACE_CAPITAL, pointOf(capital.get()));
        }
        Optional<CapitalLocation> crier = kingdom.getCityState().crierStand();
        if (crier.isPresent()) {
            builder.site(RealmHubTopic.PLACE_TOWN_CRIER, pointOf(crier.get()));
        }
        builder.hasBuildPermit(kingdom.getCityState().hasPermit(playerId));
        builder.buildEnforcementActive(
                cityService == null ? capital.isPresent() : cityService.enforcementActive(kingdom.getId()));
    }

    private void addChurchAndPoliceSites(RealmHubSnapshot.Builder builder, Kingdom kingdom) {
        Optional<ChurchSite> church = kingdom.getChurchState().church();
        if (church.isPresent()) {
            ChurchSite site = church.get();
            builder.site(
                    RealmHubTopic.PLACE_CHURCH,
                    new SitePoint(
                            site.worldName(),
                            (int) Math.floor(site.x()),
                            (int) Math.floor(site.y()),
                            (int) Math.floor(site.z())));
        }
        Optional<CourtLocation> court = kingdom.getPoliceState().court();
        if (court.isPresent()) {
            CourtLocation site = court.get();
            builder.site(RealmHubTopic.PLACE_COURT, new SitePoint(site.worldName(), site.x(), site.y(), site.z()));
        }
        for (Map.Entry<Integer, PrisonCellLocation> cell :
                new java.util.TreeMap<>(kingdom.getPoliceState().cellsView()).entrySet()) {
            PrisonCellLocation site = cell.getValue();
            builder.site(RealmHubTopic.PLACE_PRISON, new SitePoint(site.worldName(), site.x(), site.y(), site.z()));
        }
    }

    private void addParliamentSites(RealmHubSnapshot.Builder builder, Kingdom kingdom) {
        Optional<ChamberSite> commons = kingdom.getParliamentSites().commons();
        if (commons.isPresent()) {
            builder.site(RealmHubTopic.PLACE_COMMONS, pointOf(commons.get()));
        }
        Optional<ChamberSite> lords = kingdom.getParliamentSites().lords();
        if (lords.isPresent()) {
            builder.site(RealmHubTopic.PLACE_LORDS, pointOf(lords.get()));
        }
        Optional<ChamberSite> chair = kingdom.getParliamentSites().speakerChair();
        if (chair.isPresent()) {
            builder.site(RealmHubTopic.PLACE_SPEAKER_CHAIR, pointOf(chair.get()));
        }
    }

    private void addMints(RealmHubSnapshot.Builder builder, Kingdom kingdom) {
        if (economyService == null) {
            return;
        }
        KingdomEconomy economy = economyService.kingdomEconomies().get(kingdom.getId());
        if (economy == null) {
            return;
        }
        for (MintLocation mint : economy.mintLocations()) {
            builder.site(RealmHubTopic.PLACE_MINTS, new SitePoint(mint.worldName(), mint.x(), mint.y(), mint.z()));
        }
    }

    private void addLiveBusiness(
            RealmHubSnapshot.Builder builder, Kingdom kingdom, PlayerMembership membership, UUID playerId) {
        var election = kingdom.getElectionState().election();
        builder.electionOpen(election.isActive());
        if (election.isActive()) {
            builder.electionLabel("The realm is at the polls ("
                    + election.phase().name().toLowerCase(java.util.Locale.UK).replace('_', ' ') + ")");
        }

        if (parliamentService != null) {
            builder.pollingOpen(parliamentService.isPollingOpen(kingdom.getId()));
            Optional<String> question = parliamentService.referendumQuestion(kingdom.getId());
            if (question.isPresent()) {
                builder.referendumQuestion(question.get());
            }
        }

        Optional<Bill> bill = kingdom.getParliamentState().currentBill();
        boolean divisionOpen = bill.isPresent() && bill.get().state() == BillState.DIVISION_OPEN;
        boolean seated = kingdom.getElectionState().seatIndexForPlayer(playerId).isPresent();
        builder.divisionAwaitingVote(
                divisionOpen && seated && !bill.get().votesView().containsKey(playerId));

        if (justiceService != null) {
            builder.wanted(justiceService.hasActiveWarrant(kingdom.getId(), playerId));
        }

        Optional<PendingResignation> resignation = kingdom.getElectionState().pendingResignation();
        boolean mayResolve =
                ResignationAuthority.canResolveResignation(kingdom.getId(), kingdomService, membership.getRank());
        builder.resignationToReview(resignation.isPresent() && mayResolve);
        if (resignation.isPresent() && mayResolve) {
            builder.resignationSummary(ResignationSummaries.describe(resignation.get()));
        }
    }

    private static SitePoint pointOf(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new SitePoint(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    private static SitePoint pointOf(CapitalLocation location) {
        return new SitePoint(
                location.worldName(),
                (int) Math.floor(location.x()),
                (int) Math.floor(location.y()),
                (int) Math.floor(location.z()));
    }

    private static SitePoint pointOf(ChamberSite site) {
        return new SitePoint(
                site.worldName(), (int) Math.floor(site.x()), (int) Math.floor(site.y()), (int) Math.floor(site.z()));
    }
}
