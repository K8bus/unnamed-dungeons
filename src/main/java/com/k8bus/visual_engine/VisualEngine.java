package com.k8bus.visual_engine;

import com.k8bus.mixin.DisplayAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisualEngine {
    private static final VisualEngine INSTANCE = new VisualEngine();
    private final Map<String, AnimationSequence> registry = new HashMap<>();
    private final List<ActiveAnimation> activeAnimations = new ArrayList<>();

    private VisualEngine() {}

    public static VisualEngine getInstance() {
        return INSTANCE;
    }

    public void registerPattern(AnimationSequence sequence) {
        registry.put(sequence.id(), sequence);
    }

    public void tick() {
        // Bezpieczne i wydajne czyszczenie bez alokacji nowych list w każdym ticku
        for (int i = activeAnimations.size() - 1; i >= 0; i--) {
            ActiveAnimation anim = activeAnimations.get(i);
            anim.tick();
            if (anim.isFinished()) {
                activeAnimations.remove(i);
            }
        }
    }

    // TYP 1: Efekt obszarowy na ziemi
    public void spawnEffect(String sequenceId, ServerLevel world, Vec3 pos, Vector3f startScale, Vector3f targetScale, int interpolationTicks) {
        AnimationSequence seq = registry.get(sequenceId);
        if (seq == null) return;

        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, world);
        display.setPos(pos.x, pos.y, pos.z);
        display.setItemTransform(ItemDisplayContext.FIXED);

        // Ustawiamy skalę początkową
        display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), startScale, new Quaternionf()));
        world.addFreshEntity(display);

        // Wstrzyknięcie interpolacji przez system EntityData za pomocą Mixinu
        if (interpolationTicks > 0) {
            display.getEntityData().set(DisplayAccessor.getInterpolationStartDelta(), 0);
            display.getEntityData().set(DisplayAccessor.getInterpolationDuration(), interpolationTicks);

            // Klient płynnie zinterpoluje transformację do skali docelowej
            display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), targetScale, new Quaternionf()));
        }

        activeAnimations.add(new ActiveAnimation(display, seq));
    }

    // TYP 2: Ikony/Dymki nad głową
    public void spawnAttachedEffect(String sequenceId, Entity anchor, Vector3f scale) {
        AnimationSequence seq = registry.get(sequenceId);
        if (seq == null || !anchor.isAlive()) return;

        ServerLevel world = (ServerLevel) anchor.level();
        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, world);
        display.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        display.setItemTransform(ItemDisplayContext.FIXED);
        display.setBillboardConstraints(Display.BillboardConstraints.CENTER);

        float floatHeight = anchor.getBbHeight() + 0.4f;

        // Start od skali 0 (niewidoczny)
        display.setTransformation(new Transformation(new Vector3f(0f, floatHeight, 0f), new Quaternionf(), new Vector3f(0f, 0f, 0f), new Quaternionf()));
        world.addFreshEntity(display);
        display.startRiding(anchor);

        // Odpalenie animacji wyskakiwania przez 3 ticki
        display.getEntityData().set(DisplayAccessor.getInterpolationStartDelta(), 0);
        display.getEntityData().set(DisplayAccessor.getInterpolationDuration(), 3);

        display.setTransformation(new Transformation(new Vector3f(0f, floatHeight, 0f), new Quaternionf(), scale, new Quaternionf()));

        activeAnimations.add(new ActiveAnimation(display, seq));
    }

    // Zachowany oryginalny kod dla kompatybilności wstecznej
    public void spawnEffect(String sequenceId, ServerLevel world, Vec3 pos, Vector3f scale) {
        spawnEffect(sequenceId, world, pos, scale, scale, 0);
    }

    public void spawnDirectionalEffect(String sequenceId, ServerLevel world, Vec3 pos, Vec3 direction, Vector3f scale) {
        AnimationSequence seq = registry.get(sequenceId);
        if (seq == null) return;

        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, world);
        display.setPos(pos.x + (direction.x * 0.5), pos.y + (direction.y * 0.5), pos.z + (direction.z * 0.5));
        display.setItemTransform(ItemDisplayContext.FIXED);

        // Wyliczamy kąty yaw i pitch na podstawie wektora kierunku
        float yaw = (float) Math.atan2(direction.x, direction.z);

        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float pitch = (float) Math.atan2(-direction.y, horizontalDistance);

        // Składamy kwaternion w kolejności YXZ (Yaw, Pitch, Roll)
        // Dodajemy Math.PI / 2 (90 stopni) do Roll (Z) lub Pitch (X), żeby postawić model prostopadle
        Quaternionf rotation = new Quaternionf()
                .rotationYXZ(yaw, pitch, (float) (Math.PI / 2)); // Obrót wokół osi Z (Roll) stawia płaski model do pionu

        display.setTransformation(new Transformation(new Vector3f(), rotation, scale, new Quaternionf()));

        world.addFreshEntity(display);
        activeAnimations.add(new ActiveAnimation(display, seq));
    }

    public void spawnTrailSegment(String sequenceId, ServerLevel world, Vec3 startPos, Vec3 endPos, Vector3f scale) {
        AnimationSequence seq = registry.get(sequenceId);
        if (seq == null) return;

        // Obliczamy środek odcinka, żeby tam postawić ItemDisplay
        Vec3 midPos = startPos.add(endPos).scale(0.5);

        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, world);
        display.setPos(midPos.x, midPos.y, midPos.z);
        display.setItemTransform(ItemDisplayContext.FIXED);
        display.setItemStack(new ItemStack(Items.PINK_STAINED_GLASS));

        // Wektor kierunku smugi
        Vec3 direction = endPos.subtract(startPos);
        double length = direction.length();

        // Jeśli smuga jest zbyt krótka, nie spawnuje się
        if (length < 0.01) return;

        // Kwaternion patrzący wzdłuż smugi
        Vector3f dir = new Vector3f((float)direction.x, (float)direction.y, (float)direction.z).normalize();
        Quaternionf rotation = new Quaternionf().lookAlong(dir, new Vector3f(0, 1, 0));

        // Skalowanie: X i Y to grubość, Z to długość smugi
        Vector3f finalScale = new Vector3f(scale.x, scale.y, (float)length);

        display.setTransformation(new Transformation(new Vector3f(), rotation, finalScale, new Quaternionf()));

        world.addFreshEntity(display);
        activeAnimations.add(new ActiveAnimation(display, seq));
    }
}