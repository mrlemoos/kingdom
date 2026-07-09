package dev.mrlemoos.kingdom.police;

/**
 * Domain facts for a block place/break action inside a kingdom's jurisdiction.
 * Bukkit listeners supply these; no world or entity UUIDs.
 */
public record BlockActionFacts(String jurisdictionKingdomId, BlockActionType actionType) {

    public BlockActionFacts {
        if (jurisdictionKingdomId == null || jurisdictionKingdomId.isBlank()) {
            throw new IllegalArgumentException("jurisdictionKingdomId must not be blank");
        }
        if (actionType == null) {
            throw new IllegalArgumentException("actionType must not be null");
        }
    }

    public static BlockActionFacts breakBlock(String jurisdictionKingdomId) {
        return new BlockActionFacts(jurisdictionKingdomId, BlockActionType.BREAK);
    }

    public static BlockActionFacts placeBlock(String jurisdictionKingdomId) {
        return new BlockActionFacts(jurisdictionKingdomId, BlockActionType.PLACE);
    }

    public enum BlockActionType {
        BREAK,
        PLACE
    }
}
