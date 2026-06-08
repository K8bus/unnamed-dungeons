package com.k8bus.visual_engine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Smuga za odbitą strzałą.
 *
 * === Jak użyć w enchancie ===
 *
 * Gdy Twój enchant odbija strzałę (np. Deflect), oznacz ją:
 *   DeflectTrail.mark(arrow);
 *
 * Potem co tick (mixin na AbstractArrow.tick()) sprawdza się automatycznie
 * przez DeflectTrailMixin. Czyść ręcznie jeśli strzała zostaje usunięta:
 *   DeflectTrail.unmark(arrow);
 */
public final class DeflectTrail {

    // ID encji → ostatnia pozycja (do liczenia segmentów smugi)
    private static final Map<Integer, Vec3> trackedArrows = new HashMap<>();

    private DeflectTrail() {}

    /** Oznacza strzałę jako odbitą — smuga zacznie się spawnować. */
    public static void mark(AbstractArrow arrow) {
        trackedArrows.put(arrow.getId(), arrow.position());
    }

    /** Zdejmuje oznaczenie (np. gdy strzała trafia w cel). */
    public static void unmark(AbstractArrow arrow) {
        trackedArrows.remove(arrow.getId());
    }

    /** Sprawdza czy strzała jest oznaczona. */
    public static boolean isMarked(AbstractArrow arrow) {
        return trackedArrows.containsKey(arrow.getId());
    }

    /**
     * Wywoływane przez DeflectTrailMixin co tick dla każdej przelatującej strzały.
     * Spawnuje segment smugi między poprzednią a obecną pozycją.
     */
    public static void tick(AbstractArrow arrow) {
        if (!(arrow.level() instanceof ServerLevel level)) return;

        Vec3 current = arrow.position();
        Vec3 last    = trackedArrows.get(arrow.getId());

        if (last == null) {
            trackedArrows.put(arrow.getId(), current);
            return;
        }

        // Tylko jeśli strzała się porusza
        if (arrow.getDeltaMovement().lengthSqr() > 0.005) {
            VisualEngine.get().playTrail("deflect_trail", level, last, current);
        }

        trackedArrows.put(arrow.getId(), current);

        // Strzała zatrzymana lub wbita — przestań śledzić
        if (arrow.onGround() || !arrow.isAlive()) {
            trackedArrows.remove(arrow.getId());
        }
    }
}