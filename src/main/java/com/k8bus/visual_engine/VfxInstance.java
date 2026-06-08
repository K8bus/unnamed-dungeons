package com.k8bus.visual_engine;

import com.mojang.math.Transformation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class VfxInstance {

    private enum Phase { FIRST_TICK, FADE_IN, HOLD, FADE_OUT, DONE }

    private final Display   display;
    private final VfxSequence seq;

    private Phase phase     = Phase.FIRST_TICK;
    private int   phaseTick = 0;
    private int   frameIndex = 0;
    private int   frameTick  = 0;

    public VfxInstance(Display display, VfxSequence seq) {
        this.display = display;
        this.seq     = seq;

        display.setBillboardConstraints(seq.billboard());
        display.setViewRange(seq.viewRange());
        display.setShadowRadius(seq.shadowRadius());
        display.setShadowStrength(seq.shadowStrength());

        if (seq.fullBright()) {
            // Zamiast ręcznej maski bitowej, użyj obiektu Brightness
            // Konstruktor przyjmuje (blockLight, skyLight)
            display.setBrightnessOverride(new Brightness(15, 15));
        } else {
            // Pamiętaj o wyczyszczeniu, jeśli sekwencja zmienia stan
            display.setBrightnessOverride(null);
        }

        // Skala 0 + translacja 0 — klient dostanie encję niewidoczną
        display.setTransformation(transform(Vec3.ZERO.toVector3f(), Vec3.ZERO.toVector3f()));
        updateContent();
    }

    // -------------------------------------------------------------------------

    public void tick() {
        if (phase == Phase.DONE || !display.isAlive()) {
            if (display.isAlive()) display.discard();
            phase = Phase.DONE;
            return;
        }

        switch (phase) {

            case FIRST_TICK -> {
                // Klient już ma encję ze stanem zerowym — teraz odpalamy interpolację
                if (seq.fadeInTicks() > 0) {
                    setInterpolation(0, seq.fadeInTicks());
                    display.setTransformation(transform(peakTranslate(), seq.peakScale()));
                    phase = Phase.FADE_IN;
                } else {
                    display.setTransformation(transform(peakTranslate(), seq.peakScale()));
                    phase = Phase.HOLD;
                }
            }

            case FADE_IN -> {
                if (++phaseTick >= seq.fadeInTicks()) {
                    phase    = Phase.HOLD;
                    phaseTick = 0;
                }
            }

            case HOLD -> {
                tickFrames();
                boolean framesDone  = !seq.loopFrames() && frameIndex >= seq.frameCount() - 1;
                boolean holdExpired = seq.holdTicks() > 0 && ++phaseTick >= seq.holdTicks();
                if (holdExpired || (seq.holdTicks() == 0 && framesDone)) beginFadeOut();
            }

            case FADE_OUT -> {
                if (++phaseTick >= seq.fadeOutTicks()) {
                    phase = Phase.DONE;
                    display.discard();
                }
            }
        }
    }

    public void stop() {
        if (phase != Phase.DONE && phase != Phase.FADE_OUT) beginFadeOut();
    }

    public boolean isFinished() {
        return phase == Phase.DONE || !display.isAlive();
    }

    // -------------------------------------------------------------------------

    private void tickFrames() {
        if (seq.frameCount() <= 1) return;
        if (++frameTick >= seq.ticksPerFrame()) {
            frameTick = 0;
            frameIndex++;
            if (frameIndex >= seq.frameCount())
                frameIndex = seq.loopFrames() ? 0 : seq.frameCount() - 1;
            updateContent();
        }
    }

    private void beginFadeOut() {
        phase     = Phase.FADE_OUT;
        phaseTick = 0;
        if (seq.fadeOutTicks() > 0) {
            setInterpolation(0, seq.fadeOutTicks());
            // Fade-out: wróć do skali 0 i translacji 0
            display.setTransformation(transform(Vec3.ZERO.toVector3f(), Vec3.ZERO.toVector3f()));
        } else {
            phase = Phase.DONE;
            display.discard();
        }
    }

    // -------------------------------------------------------------------------

    /** Buduje Transformation łącząc translację i skalę. */
    private Transformation transform(Vector3f translation, Vector3f scale) {
        return new Transformation(translation, new Quaternionf(), scale, new Quaternionf());
    }

    /** Zwraca cel translacji (lub zero jeśli nie ustawiono). */
    private Vector3f peakTranslate() {
        return seq.translateTo() != null ? seq.translateTo() : new Vector3f(0f, 0f, 0f);
    }

    private void setInterpolation(int startDelay, int duration) {
        // Ustawienie opóźnienia startu transformacji
        display.setTransformationInterpolationDelay(startDelay);

        // Ustawienie czasu trwania transformacji
        display.setTransformationInterpolationDuration(duration);
    }

    private void updateContent() {
        switch (seq.type()) {
            case ITEM -> {
                if (seq.itemCMDs() == null || seq.itemCMDs().length == 0) return;
                int cmd = seq.itemCMDs()[Math.min(frameIndex, seq.itemCMDs().length - 1)];
                ItemStack stack = new ItemStack(Items.PAPER);
                stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                        List.of((float) cmd), List.of(), List.of(), List.of()
                ));
                ((Display.ItemDisplay) display).setItemStack(stack);
            }
            case BLOCK -> {
                if (seq.blockStates() == null || seq.blockStates().length == 0) return;
                var state = seq.blockStates()[Math.min(frameIndex, seq.blockStates().length - 1)];
                ((Display.BlockDisplay) display).setBlockState(state);
            }
        }
    }
}