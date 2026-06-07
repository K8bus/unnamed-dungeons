package com.k8bus.visual_engine;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import java.util.List;

public class ActiveAnimation {
    private final Display.ItemDisplay entity; // To jest Twoja encja
    private final AnimationSequence sequence;
    private int currentFrameIndex = 0;
    private int currentFrameTick = 0;
    private int age = 0; // Teraz będzie używane
    private boolean finished = false;

    public ActiveAnimation(Display.ItemDisplay entity, AnimationSequence sequence) {
        this.entity = entity;
        this.sequence = sequence;
        updateEntityModel();
    }

    public void tick() {
        age++; // Zwiększamy wiek encji w każdym ticku

        if (finished || !entity.isAlive()) {
            if (entity.isAlive()) entity.discard(); // Bezpieczne usunięcie
            return;
        }

        currentFrameTick++;
        AnimationFrame currentFrame = sequence.frames().get(currentFrameIndex);

        if (currentFrameTick >= currentFrame.durationTicks()) {
            currentFrameIndex++;
            currentFrameTick = 0;

            if (currentFrameIndex >= sequence.frames().size()) {
                if (sequence.loop()) {
                    currentFrameIndex = 0;
                } else {
                    finished = true;
                    entity.discard();
                    return;
                }
            }
            updateEntityModel();
        }
    }

    private void updateEntityModel() {
        AnimationFrame frame = sequence.frames().get(currentFrameIndex);
        ItemStack stack = new ItemStack(Items.PAPER);

        // Ustawienie CustomModelData zgodnie z nowym systemem komponentów
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of((float) frame.customModelData()),
                List.of(), List.of(), List.of()
        ));

        entity.setItemStack(stack);
    }

    public boolean isFinished() {
        // Poprawka: sprawdzamy 'entity' zamiast nieistniejącego 'display'
        return finished || !entity.isAlive();
    }
}