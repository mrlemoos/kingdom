package dev.leo.kingdom.election;

import dev.leo.kingdom.model.parliament.BillType;
import dev.leo.kingdom.model.parliament.VoteChoice;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
        table.put(BillType.FISCAL, Map.of(
                "farmer", VoteChoice.NAY,
                "librarian", VoteChoice.AYE,
                "armorer", VoteChoice.NAY,
                "weaponsmith", VoteChoice.NAY,
                "toolsmith", VoteChoice.AYE));
        table.put(BillType.BUDGET, Map.of(
                "farmer", VoteChoice.AYE,
                "librarian", VoteChoice.AYE,
                "armorer", VoteChoice.NAY,
                "cleric", VoteChoice.AYE));
        table.put(BillType.SPEND_MINT, Map.of(
                "farmer", VoteChoice.AYE,
                "librarian", VoteChoice.AYE,
                "armorer", VoteChoice.NAY,
                "cartographer", VoteChoice.AYE));
        table.put(BillType.SPEND_STIPEND, Map.of(
                "farmer", VoteChoice.AYE,
                "librarian", VoteChoice.NAY,
                "armorer", VoteChoice.NAY,
                "shepherd", VoteChoice.AYE));
        return new ProfessionVoteBias(table);
    }

    public static ProfessionVoteBias fromPluginConfig(org.bukkit.configuration.file.FileConfiguration config) {
        var section = config.getConfigurationSection("election.profession-vote-bias");
        if (section == null) {
            return defaults();
        }
        Map<BillType, Map<String, VoteChoice>> table = new EnumMap<>(BillType.class);
        for (String billKey : section.getKeys(false)) {
            BillType billType = BillType.valueOf(billKey.toUpperCase(Locale.ROOT));
            var profSection = section.getConfigurationSection(billKey);
            if (profSection == null) {
                continue;
            }
            Map<String, VoteChoice> profMap = new HashMap<>();
            for (String profession : profSection.getKeys(false)) {
                profMap.put(
                        profession.toLowerCase(Locale.ROOT),
                        VoteChoice.valueOf(profSection.getString(profession, "abstain").toUpperCase(Locale.ROOT)));
            }
            table.put(billType, profMap);
        }
        return table.isEmpty() ? defaults() : new ProfessionVoteBias(table);
    }

    private static Map<BillType, Map<String, VoteChoice>> deepCopy(Map<BillType, Map<String, VoteChoice>> source) {
        Map<BillType, Map<String, VoteChoice>> copy = new EnumMap<>(BillType.class);
        for (Map.Entry<BillType, Map<String, VoteChoice>> entry : source.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return copy;
    }
}
