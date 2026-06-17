package dev.leo.kingdom.model;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.ChatColor;

public final class PlayerMembership {
    private final UUID playerId;
    private String kingdomId;
    private NobleRank rank;
    private TitleStyle titleStyle;

    public PlayerMembership(UUID playerId, String kingdomId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getKingdomId() {
        return kingdomId;
    }

    public void setKingdomId(String kingdomId) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        clearTitle();
    }

    public NobleRank getRank() {
        return rank;
    }

    public TitleStyle getTitleStyle() {
        return titleStyle;
    }

    public boolean hasNobleTitle() {
        return rank != null;
    }

    public String chatPrefix() {
        return formattedTitle(null);
    }

    public String coloredChatPrefix() {
        if (rank == null) {
            return "";
        }
        return formattedTitle(rank.chatColor());
    }

    private String formattedTitle(ChatColor color) {
        if (rank == null) {
            return "";
        }
        TitleStyle style = titleStyle != null ? titleStyle : TitleStyle.MASCULINE;
        String title = rank.displayTitle(style).toUpperCase(Locale.ROOT);
        StringBuilder prefix = new StringBuilder();
        if (color != null) {
            prefix.append(color);
        }
        prefix.append(ChatColor.BOLD).append(title).append(' ');
        return prefix.toString();
    }

    public void assignTitle(NobleRank rank, TitleStyle style) {
        this.rank = Objects.requireNonNull(rank, "rank");
        this.titleStyle = style != null ? style : TitleStyle.MASCULINE;
    }

    public void clearTitle() {
        this.rank = null;
        this.titleStyle = null;
    }
}
