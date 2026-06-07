package com.k8bus.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Display.class)
public interface DisplayAccessor {

    @Accessor("DATA_INTERPOLATION_START_DELTA")
    static EntityDataAccessor<Integer> getInterpolationStartDelta() {
        throw new AssertionError();
    }

    @Accessor("DATA_INTERPOLATION_DURATION")
    static EntityDataAccessor<Integer> getInterpolationDuration() {
        throw new AssertionError();
    }
}