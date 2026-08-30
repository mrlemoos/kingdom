package dev.mrlemoos.kingdom.church;

import dev.mrlemoos.kingdom.model.church.ChurchSite;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Who is standing at the altar, in the world: the same question the rites and the mass both ask. */
public final class ChurchPresence {

    private ChurchPresence() {}

    public static boolean atChurch(ChurchService churchService, String kingdomId, Player player) {
        Optional<ChurchSite> site = churchService.church(kingdomId);
        if (site.isEmpty() || player == null) {
            return false;
        }
        Location location = player.getLocation();
        String worldName = location.getWorld() == null ? "" : location.getWorld().getName();
        return ChurchProximity.isAtChurch(
                site.get(), worldName, location.getX(), location.getY(), location.getZ());
    }

    /** The sworn priest counts as presiding only while he is standing at his own church. */
    public static boolean priestAtChurch(ChurchService churchService, String kingdomId) {
        Optional<UUID> priestId = churchService.priest(kingdomId);
        if (priestId.isEmpty()) {
            return false;
        }
        Player priest = Bukkit.getPlayer(priestId.get());
        return priest != null && atChurch(churchService, kingdomId, priest);
    }

    public static Celebrant presiding(ChurchService churchService, String kingdomId) {
        return churchService.presidingCelebrant(kingdomId, priestAtChurch(churchService, kingdomId));
    }
}
