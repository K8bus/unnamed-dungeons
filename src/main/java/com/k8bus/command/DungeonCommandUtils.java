package com.k8bus.command;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;

public class DungeonCommandUtils {

    public static BlockHitResult getLookedAtBlock(ServerPlayer player) {
        double reachDistance = 8.0D;

        return player.level().clip(new ClipContext(
                player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(reachDistance)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
    }
}
