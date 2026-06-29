package dev.mrlemoos.kingdom.locate;

import java.util.Locale;

public final class LocateCoordFormatter {

    private LocateCoordFormatter() {
    }

    public static String format(int x, int y, int z) {
        return x + ", " + y + ", " + z;
    }

    public static String format(double x, double y, double z) {
        return format((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
}
