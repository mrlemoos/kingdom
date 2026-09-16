package dev.mrlemoos.kingdom.election;

import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * How each profession bench leans on the business before the House. Every bench that can be
 * returned has a stance on every bill it divides on, so a division is decided by the trades of the
 * realm rather than lost to a chamber of abstentions. Confidence in the Premier is deliberately
 * absent: that question belongs to the elected benches alone.
 */
public final class ProfessionVoteBias {

    private final Map<BillType, Map<String, VoteChoice>> biasTable;

    public ProfessionVoteBias(Map<BillType, Map<String, VoteChoice>> biasTable) {
        this.biasTable = biasTable != null ? deepCopy(biasTable) : Map.of();
    }

    public VoteChoice resolve(BillType billType, String profession) {
        if (billType == null || profession == null) {
            return VoteChoice.ABSTAIN;
        }
        Map<String, VoteChoice> byProfession = biasTable.get(billType);
        if (byProfession == null) {
            return VoteChoice.ABSTAIN;
        }
        VoteChoice choice = byProfession.get(profession.toLowerCase(Locale.ROOT));
        return choice != null ? choice : VoteChoice.ABSTAIN;
    }

    public static ProfessionVoteBias defaults() {
        Map<BillType, Map<String, VoteChoice>> table = new EnumMap<>(BillType.class);
        // Aye on a fiscal bill is the bench that wants the rates cut; nay the bench that wants them held up.
        table.put(BillType.FISCAL, bench(
                "armorer=nay,butcher=nay,cartographer=aye,cleric=nay,farmer=nay,fisherman=aye,fletcher=aye,"
                        + "leatherworker=nay,librarian=aye,mason=aye,nitwit=nay,none=aye,shepherd=aye,"
                        + "toolsmith=aye,weaponsmith=nay"));
        table.put(BillType.BUDGET, bench(
                "armorer=nay,butcher=aye,cartographer=nay,cleric=aye,farmer=aye,fisherman=aye,fletcher=aye,"
                        + "leatherworker=aye,librarian=aye,mason=aye,nitwit=nay,none=aye,shepherd=aye,"
                        + "toolsmith=nay,weaponsmith=nay"));
        table.put(BillType.SPEND_MINT, bench(
                "armorer=nay,butcher=nay,cartographer=aye,cleric=aye,farmer=aye,fisherman=nay,fletcher=aye,"
                        + "leatherworker=aye,librarian=aye,mason=aye,nitwit=nay,none=aye,shepherd=nay,"
                        + "toolsmith=aye,weaponsmith=nay"));
        table.put(BillType.SPEND_STIPEND, bench(
                "armorer=nay,butcher=aye,cartographer=nay,cleric=aye,farmer=aye,fisherman=aye,fletcher=nay,"
                        + "leatherworker=aye,librarian=nay,mason=nay,nitwit=aye,none=aye,shepherd=aye,"
                        + "toolsmith=nay,weaponsmith=nay"));
        table.put(BillType.SPEND_PUBLIC_WORK, bench(
                "armorer=nay,butcher=nay,cartographer=aye,cleric=aye,farmer=aye,fisherman=nay,fletcher=nay,"
                        + "leatherworker=nay,librarian=aye,mason=aye,nitwit=nay,none=aye,shepherd=aye,"
                        + "toolsmith=aye,weaponsmith=nay"));
        // The trades that arm the realm want the war; the trades that feed it want the peace.
        table.put(BillType.WAR, bench(
                "armorer=aye,butcher=aye,cartographer=aye,cleric=nay,farmer=nay,fisherman=nay,fletcher=aye,"
                        + "leatherworker=aye,librarian=nay,mason=nay,nitwit=nay,none=nay,shepherd=nay,"
                        + "toolsmith=aye,weaponsmith=aye"));
        table.put(BillType.PEACE, bench(
                "armorer=nay,butcher=nay,cartographer=nay,cleric=aye,farmer=aye,fisherman=aye,fletcher=nay,"
                        + "leatherworker=nay,librarian=aye,mason=aye,nitwit=aye,none=aye,shepherd=aye,"
                        + "toolsmith=nay,weaponsmith=nay"));
        table.put(BillType.TREATY, bench(
                "armorer=nay,butcher=aye,cartographer=aye,cleric=aye,farmer=aye,fisherman=aye,fletcher=nay,"
                        + "leatherworker=aye,librarian=aye,mason=aye,nitwit=nay,none=aye,shepherd=aye,"
                        + "toolsmith=aye,weaponsmith=nay"));
        return new ProfessionVoteBias(table);
    }

    /**
     * Reads the stances the server has configured, laid over the defaults: a config that names only
     * a few benches leaves the rest with their standing stance rather than muting them.
     */
    public static ProfessionVoteBias fromPluginConfig(org.bukkit.configuration.file.FileConfiguration config) {
        var section = config.getConfigurationSection("election.profession-vote-bias");
        if (section == null) {
            return defaults();
        }
        Map<BillType, Map<String, VoteChoice>> table = new EnumMap<>(defaults().biasTable);
        for (String billKey : section.getKeys(false)) {
            BillType billType = BillType.valueOf(billKey.toUpperCase(Locale.ROOT));
            var profSection = section.getConfigurationSection(billKey);
            if (profSection == null) {
                continue;
            }
            Map<String, VoteChoice> profMap = new HashMap<>(table.getOrDefault(billType, Map.of()));
            for (String profession : profSection.getKeys(false)) {
                profMap.put(
                        profession.toLowerCase(Locale.ROOT),
                        VoteChoice.valueOf(profSection.getString(profession, "abstain").toUpperCase(Locale.ROOT)));
            }
            table.put(billType, profMap);
        }
        return new ProfessionVoteBias(table);
    }

    /** One bench row, written as {@code profession=choice} pairs. */
    private static Map<String, VoteChoice> bench(String spec) {
        Map<String, VoteChoice> stances = new HashMap<>();
        for (String entry : spec.split(",")) {
            String[] parts = entry.split("=");
            stances.put(parts[0], VoteChoice.valueOf(parts[1].toUpperCase(Locale.ROOT)));
        }
        return Map.copyOf(stances);
    }

    private static Map<BillType, Map<String, VoteChoice>> deepCopy(Map<BillType, Map<String, VoteChoice>> source) {
        Map<BillType, Map<String, VoteChoice>> copy = new EnumMap<>(BillType.class);
        for (Map.Entry<BillType, Map<String, VoteChoice>> entry : source.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return copy;
    }
}
