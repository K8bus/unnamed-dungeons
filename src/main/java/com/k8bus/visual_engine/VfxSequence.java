package com.k8bus.visual_engine;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public record VfxSequence(
        String id,
        VfxDisplayType type,
        int[] itemCMDs,
        BlockState[] blockStates,
        int ticksPerFrame,
        boolean loopFrames,
        Vector3f peakScale,
        int fadeInTicks,
        int holdTicks,
        int fadeOutTicks,
        Display.BillboardConstraints billboard,
        boolean fullBright,
        float viewRange,
        float shadowRadius,
        float shadowStrength,
        Vector3f translateTo   // <<< NOWE: cel translacji, null = statyczny
) {
    public int frameCount() {
        if (type == VfxDisplayType.ITEM)  return itemCMDs   != null ? itemCMDs.length   : 1;
        if (type == VfxDisplayType.BLOCK) return blockStates != null ? blockStates.length : 1;
        return 1;
    }
}