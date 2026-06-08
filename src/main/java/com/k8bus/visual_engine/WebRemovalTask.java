package com.k8bus.visual_engine;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class WebRemovalTask {

    private record Task(Display display, int removeAt, int fadeOut) {}

    private static final List<Task> pending = new ArrayList<>();
    private static int currentTick = 0;

    private WebRemovalTask() {}

    public static void schedule(Display display, int holdTicks, int fadeOutTicks) {
        pending.add(new Task(display, currentTick + holdTicks, fadeOutTicks));
    }

    public static void tick() {
        currentTick++;
        for (int i = pending.size() - 1; i >= 0; i--) {
            Task t = pending.get(i);
            if (!t.display().isAlive()) { pending.remove(i); continue; }

            if (currentTick == t.removeAt()) {
                t.display().setTransformationInterpolationDelay(0);
                t.display().setTransformationInterpolationDuration(t.fadeOut());
                t.display().setTransformation(new Transformation(
                        new Vector3f(0.0f, 0.0f, 0.0f), // translation
                        new Quaternionf(),              // leftRotation (identity)
                        new Vector3f(0.0f, 0.0f, 0.0f), // scale (zerujemy)
                        new Quaternionf()               // rightRotation (identity)
                ));
            }

            if (currentTick >= t.removeAt() + t.fadeOut()) {
                t.display().discard();
                pending.remove(i);
            }
        }
    }
}