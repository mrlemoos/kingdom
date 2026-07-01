package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicePrefixTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CONSTABLE = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID JUDGE = UUID.fromString("00000000-0000-0000-0000-000000000005");

    private PoliceService policeService;

    @BeforeEach
    void setUp() {
        KingdomService kingdomService = new KingdomService();
        policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(CONSTABLE, "northmarch");
        kingdomService.joinKingdom(JUDGE, "northmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE);
        policeService.appointJudge("northmarch", NobleRank.KING, JUDGE);
    }

    @Test
    void colouredSwornPrefixForConstable() {
        String prefix = policeService.colouredSwornChatPrefix("northmarch", CONSTABLE);

        assertTrue(prefix.contains("[Constable]"));
        assertEquals(PoliceService.CONSTABLE_CHAT_COLOR + "[Constable] ", prefix);
    }

    @Test
    void colouredSwornPrefixForJudge() {
        String prefix = policeService.colouredSwornChatPrefix("northmarch", JUDGE);

        assertTrue(prefix.contains("[Judge]"));
        assertEquals(PoliceService.JUDGE_CHAT_COLOR + "[Judge] ", prefix);
    }

    @Test
    void swornPrefixEmptyForUnswornMember() {
        assertEquals("", policeService.swornChatPrefix("northmarch", KING));
        assertEquals("", policeService.colouredSwornChatPrefix("northmarch", KING));
    }
}
