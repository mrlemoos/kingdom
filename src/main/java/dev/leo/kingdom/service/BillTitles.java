package dev.leo.kingdom.service;

import dev.leo.kingdom.model.parliament.BillType;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class BillTitles {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.UK)
            .withZone(ZoneOffset.UTC);

    private BillTitles() {}

    public static String defaultTitle(BillType type, String kingdomId, long tabledAtMs) {
        String date = DATE.format(Instant.ofEpochMilli(tabledAtMs));
        String label = switch (type) {
            case FISCAL -> "Fiscal Act";
            case BUDGET -> "Budget Act";
            case SPEND_MINT -> "Supply Act (Mint)";
            case SPEND_STIPEND -> "Supply Act";
        };
        return label + " — " + kingdomId + " — " + date;
    }

    public static String resolve(BillType type, String kingdomId, long tabledAtMs, String optionalTitle) {
        if (optionalTitle != null && !optionalTitle.isBlank()) {
            return optionalTitle.trim();
        }
        return defaultTitle(type, kingdomId, tabledAtMs);
    }
}
