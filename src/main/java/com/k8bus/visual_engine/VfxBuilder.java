package com.k8bus.visual_engine;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.Arrays;

public final class VfxBuilder {

    private final VfxDisplayType type;
    private int[]        itemCMDs;
    private BlockState[] blockStates;
    private int     ticksPerFrame = 2;
    private boolean loopFrames   = false;
    private float sx = 1f, sy = 1f, sz = 1f;
    private int fadeInTicks  = 4;
    private int holdTicks    = 0;
    private int fadeOutTicks = 4;
    private Display.BillboardConstraints billboard = Display.BillboardConstraints.CENTER;
    private boolean fullBright    = true;
    private float   viewRange     = 1.0f;
    private float   shadowRadius  = 0f;
    private float   shadowStrength = 0f;
    private Vector3f translateTo  = null;  // <<< NOWE

    private VfxBuilder(VfxDisplayType type) { this.type = type; }

    // --- Fabryki ---

    public static VfxBuilder item(int... cmds) {
        VfxBuilder b = new VfxBuilder(VfxDisplayType.ITEM);
        b.itemCMDs = cmds;
        return b;
    }

    public static VfxBuilder block(Block... blocks) {
        VfxBuilder b = new VfxBuilder(VfxDisplayType.BLOCK);
        b.blockStates = Arrays.stream(blocks).map(Block::defaultBlockState).toArray(BlockState[]::new);
        return b;
    }

    public static VfxBuilder block(BlockState... states) {
        VfxBuilder b = new VfxBuilder(VfxDisplayType.BLOCK);
        b.blockStates = states;
        return b;
    }

    // --- Animacja klatek ---
    public VfxBuilder ticks(int t)  { ticksPerFrame = t; return this; }
    public VfxBuilder loop()        { loopFrames = true; return this; }

    // --- Skala ---
    public VfxBuilder scale(float s)                    { sx = sy = sz = s; return this; }
    public VfxBuilder scale(float x, float y, float z)  { sx=x; sy=y; sz=z; return this; }

    // --- Cykl życia ---
    public VfxBuilder fadeIn(int t)  { fadeInTicks  = t; return this; }
    public VfxBuilder hold(int t)    { holdTicks    = t; return this; }
    public VfxBuilder fadeOut(int t) { fadeOutTicks = t; return this; }

    // --- Translacja (animacja pozycji) ---

    /** Animuje display w górę o podaną liczbę bloków podczas fade-in. */
    public VfxBuilder riseY(float blocks) {
        translateTo = new Vector3f(0f, blocks, 0f);
        return this;
    }

    /** Dowolny cel translacji XYZ animowany razem z fade-in. */
    public VfxBuilder translateTo(float x, float y, float z) {
        translateTo = new Vector3f(x, y, z);
        return this;
    }

    // --- Display ---
    public VfxBuilder billboard(Display.BillboardConstraints b) { billboard = b; return this; }
    public VfxBuilder fixed()    { billboard = Display.BillboardConstraints.FIXED;    return this; }
    public VfxBuilder vertical() { billboard = Display.BillboardConstraints.VERTICAL; return this; }
    public VfxBuilder bright(boolean b) { fullBright = b; return this; }
    public VfxBuilder viewRange(float r) { viewRange = r; return this; }
    public VfxBuilder shadow(float r, float s) { shadowRadius=r; shadowStrength=s; return this; }

    // --- Build ---
    public VfxSequence build(String id) {
        return new VfxSequence(
                id, type, itemCMDs, blockStates,
                ticksPerFrame, loopFrames,
                new Vector3f(sx, sy, sz),
                fadeInTicks, holdTicks, fadeOutTicks,
                billboard, fullBright, viewRange,
                shadowRadius, shadowStrength,
                translateTo
        );
    }
}