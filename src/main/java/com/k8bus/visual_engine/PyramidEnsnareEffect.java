package com.k8bus.visual_engine;

import com.mojang.math.Transformation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Brightness;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public final class PyramidEnsnareEffect {

    public static final int PYRAMID_CMD     = 8230;
    public static final int DEFAULT_HOLD_TICKS = 60;

    private PyramidEnsnareEffect() {}

    public static void apply(LivingEntity target, int holdTicks) {
        if (!(target.level() instanceof ServerLevel level)) return;

        float height     = target.getBbHeight() * 0.55f;
        float baseSpread = Math.max(1.2f, target.getBbWidth() * 1.8f);
        Vec3  feet       = target.position();

        for (int i = 0; i < 4; i++) {
            spawnFace(level, feet, baseSpread, height, i * 90f, holdTicks);
        }

        target.addEffect(new MobEffectInstance(
                MobEffects.SLOWNESS, holdTicks, 10, false, false, false
        ));
    }

    private static void spawnFace(ServerLevel level, Vec3 feet,
                                  float baseSpread, float height,
                                  float yawDeg, int holdTicks) {
        var display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
        display.setPos(feet.x, feet.y, feet.z);
        display.setItemTransform(net.minecraft.world.item.ItemDisplayContext.FIXED);
        display.setBillboardConstraints(Display.BillboardConstraints.FIXED);
        display.setViewRange(1.5f);
        display.setShadowRadius(0f);
        display.setBrightnessOverride(new Brightness(15, 15));

        setModel(display, PYRAMID_CMD);

        float rad = (float) Math.toRadians(yawDeg);
        Quaternionf rot = new Quaternionf().rotationY(rad);
        Vector3f offset = rot.transform(new Vector3f(0f, 0f, -baseSpread / 2f));

        display.setTransformation(new Transformation(offset, rot, new Vector3f(0f), new Quaternionf()));
        level.addFreshEntity(display);

        display.setTransformationInterpolationDelay(0);
        display.setTransformationInterpolationDuration(7);
        display.setTransformation(new Transformation(
                offset, rot,
                new Vector3f(baseSpread, height, 0.08f),
                new Quaternionf()
        ));

        WebRemovalTask.schedule(display, holdTicks, 8);
    }

    private static void setModel(Display.ItemDisplay display, int cmd) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of((float) cmd), List.of(), List.of(), List.of()
        ));
        display.setItemStack(stack);
    }

    public static void apply(LivingEntity target) {
        apply(target, DEFAULT_HOLD_TICKS);
    }
}