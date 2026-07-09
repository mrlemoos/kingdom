package dev.mrlemoos.kingdom.war.conscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryConscriptionStore implements ConscriptionStore {

    private final Map<UUID, PressedVillager> pressedByVillager = new LinkedHashMap<>();

    @Override
    public void press(PressedVillager pressed) {
        Objects.requireNonNull(pressed, "pressed");
        pressedByVillager.put(pressed.villagerId(), pressed);
    }

    @Override
    public Optional<PressedVillager> find(UUID villagerId) {
        return Optional.ofNullable(pressedByVillager.get(villagerId));
    }

    @Override
    public Collection<PressedVillager> findByKingdom(String kingdomId) {
        List<PressedVillager> matches = new ArrayList<>();
        for (PressedVillager pressed : pressedByVillager.values()) {
            if (pressed.kingdomId().equals(kingdomId)) {
                matches.add(pressed);
            }
        }
        return List.copyOf(matches);
    }

    @Override
    public Collection<PressedVillager> allView() {
        return List.copyOf(pressedByVillager.values());
    }

    @Override
    public void release(UUID villagerId) {
        pressedByVillager.remove(villagerId);
    }
}
