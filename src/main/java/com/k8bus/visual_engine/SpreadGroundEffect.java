package com.k8bus.visual_engine;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SpreadGroundEffect {

    private SpreadGroundEffect() {}

    public static void spawn(ServerLevel level, Vec3 pos, Block block,
                             float spreadSize, float thickness,
                             int fadeInTicks, int holdTicks, int fadeOutTicks) {
        BlockState state = block.defaultBlockState();
        float half = spreadSize / 2f;
        for (float yawDeg : new float[]{ 0f, 30f, 60f }) {
            spawnLayer(level, pos, state, half, thickness, yawDeg, fadeInTicks, holdTicks, fadeOutTicks);
        }
    }

    private static void spawnLayer(ServerLevel level, Vec3 pos, BlockState state,
                                   float half, float thickness, float yawDeg,
                                   int fadeIn, int hold, int fadeOut) {
        var display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        display.setPos(pos.x, pos.y, pos.z);
        display.setBlockState(state);
        display.setBillboardConstraints(Display.BillboardConstraints.FIXED);
        display.setViewRange(1.5f);
        display.setShadowRadius(0f);
        display.setBrightnessOverride(new Brightness(15, 15));

        float rad = (float) Math.toRadians(yawDeg);
        Quaternionf rot = new Quaternionf(new AxisAngle4f(rad, 0f, 1f, 0f));
        Vector3f offset = new Vector3f(-half, -0.05f, -half);

        display.setTransformation(new Transformation(offset, rot, new Vector3f(0f), new Quaternionf()));
        level.addFreshEntity(display);

        display.setTransformationInterpolationDelay(0);
        display.setTransformationInterpolationDuration(fadeIn);
        display.setTransformation(new Transformation(
                offset, rot,
                new Vector3f(half * 2f, thickness, half * 2f),
                new Quaternionf()
        ));

        WebRemovalTask.schedule(display, hold, fadeOut);
    }
}