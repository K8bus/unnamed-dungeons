package com.k8bus.visual_engine;

import com.mojang.math.Transformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public final class VisualEngine {

    private static final VisualEngine INSTANCE = new VisualEngine();
    public static VisualEngine get() { return INSTANCE; }

    private final Map<String, VfxSequence> registry = new HashMap<>();
    private final List<VfxInstance>        active   = new ArrayList<>();

    private VisualEngine() {}

    // -------------------------------------------------------------------------

    public void register(VfxSequence sequence) {
        registry.put(sequence.id(), sequence);
    }

    public void tick() {
        for (int i = active.size() - 1; i >= 0; i--) {
            VfxInstance inst = active.get(i);
            inst.tick();
            if (inst.isFinished()) active.remove(i);
        }
    }

    // -------------------------------------------------------------------------
    // Spawn 1: Burst na pozycji

    public VfxInstance play(String id, ServerLevel level, Vec3 pos) {
        VfxSequence seq = require(id);
        if (seq == null) return null;

        Display display = buildDisplay(seq, level, pos);
        VfxInstance inst = new VfxInstance(display, seq);

        level.addFreshEntity(display);
        active.add(inst);
        return inst;
    }

    // -------------------------------------------------------------------------
    // Spawn 2: Ring N równomiernie rozmieszczonych efektów wokół punktu
    //
    // Użycie: playCircle("poison_rise", level, pos, 8, 0.6f)
    //         → 8 pionowych smug w okręgu o promieniu 0.6

    public List<VfxInstance> playCircle(String id, ServerLevel level, Vec3 center,
                                        int count, float radius) {
        List<VfxInstance> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            Vec3 pos = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            VfxInstance inst = play(id, level, pos);
            if (inst != null) result.add(inst);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Spawn 3: Attached do encji

    public VfxInstance playAttached(String id, Entity anchor) {
        return playAttached(id, anchor, anchor.getBbHeight() + 0.3f);
    }

    public VfxInstance playAttached(String id, Entity anchor, float yOffset) {
        if (!anchor.isAlive()) return null;

        VfxSequence seq = require(id);
        if (seq == null) return null;

        ServerLevel level = (ServerLevel) anchor.level();

        Display display = buildDisplay(seq, level, anchor.position());
        display.setTransformation(new Transformation(
                new Vector3f(0f, yOffset, 0f), new Quaternionf(),
                new Vector3f(0f), new Quaternionf()
        ));

        VfxInstance inst = new VfxInstance(display, seq);
        level.addFreshEntity(display);

        // startRiding(anchor) — poprawna sygnatura w Twojej wersji
        display.startRiding(anchor);

        active.add(inst);
        return inst;
    }

    // -------------------------------------------------------------------------
    // Spawn 4: Kierunkowy

    public VfxInstance playDirectional(String id, ServerLevel level, Vec3 pos, Vec3 direction) {
        VfxSequence seq = require(id);
        if (seq == null) return null;

        Display display = buildDisplay(seq, level, pos);

        float yaw = (float) Math.atan2(direction.x, direction.z);
        double hd = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float pitch = (float) Math.atan2(-direction.y, hd);

        Quaternionf rot = new Quaternionf().rotationYXZ(yaw, pitch, (float) (Math.PI / 2));
        display.setTransformation(new Transformation(new Vector3f(), rot, new Vector3f(0f), new Quaternionf()));

        VfxInstance inst = new VfxInstance(display, seq);
        level.addFreshEntity(display);

        active.add(inst);
        return inst;
    }

    // -------------------------------------------------------------------------
    // Spawn 5: Segment smugi między dwoma punktami

    public VfxInstance playTrail(String id, ServerLevel level, Vec3 from, Vec3 to) {
        VfxSequence seq = require(id);
        if (seq == null) return null;

        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.01) return null;

        Vec3 mid = from.add(to).scale(0.5);

        Display display = buildDisplay(seq, level, mid);

        Vector3f dirF = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z).normalize();
        Quaternionf rot = new Quaternionf().lookAlong(dirF, new Vector3f(0, 1, 0));

        Vector3f scale = new Vector3f(seq.peakScale().x, seq.peakScale().y, (float) len);
        display.setTransformation(new Transformation(new Vector3f(), rot, scale, new Quaternionf()));

        VfxInstance inst = new VfxInstance(display, seq);
        level.addFreshEntity(display);

        active.add(inst);
        return inst;
    }

    // -------------------------------------------------------------------------

    public void stop(VfxInstance instance) {
        if (instance != null) instance.stop();
    }

    // -------------------------------------------------------------------------

    private Display buildDisplay(VfxSequence seq, ServerLevel level, Vec3 pos) {
        return switch (seq.type()) {
            case ITEM -> {
                var d = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
                d.setPos(pos.x, pos.y, pos.z);
                d.setItemTransform(ItemDisplayContext.FIXED);
                yield d;
            }
            case BLOCK -> {
                var d = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
                d.setPos(pos.x, pos.y, pos.z);
                d.setBlockState(Blocks.AIR.defaultBlockState());
                yield d;
            }
        };
    }

    private VfxSequence require(String id) {
        VfxSequence seq = registry.get(id);
        if (seq == null) System.err.println("[VisualEngine] Niezarejestrowany efekt: '" + id + "'");
        return seq;
    }
}