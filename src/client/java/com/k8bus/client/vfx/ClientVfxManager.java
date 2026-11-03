package com.k8bus.client.vfx;

import com.k8bus.network.DungeonsVfxPayload;
import com.k8bus.vfx.VfxTheme;
import com.k8bus.vfx.VfxType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClientVfxManager {

    private static final List<ActiveTrail> activeTrails = new ArrayList<>();
    private static final List<ActiveAura> activeAuras = new ArrayList<>();
    private static final List<ActiveGroundRing> activeRings = new ArrayList<>();

    public static void spawnVfx(DungeonsVfxPayload payload) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) return;

        VfxType type = payload.getVfxType();
        VfxTheme theme = payload.getVfxTheme();
        Vec3 pos = payload.getPos();
        Vec3 dir = payload.getDirection();

        switch (type) {
            case SLASH_ARC -> spawnSlashArc(level, pos, dir, theme, payload.scale());
            case IMPACT_BURST -> spawnImpactBurst(level, pos, dir, theme, payload.scale());
            case GROUND_RING -> activeRings.add(new ActiveGroundRing(pos, payload.scale(), payload.durationTicks(), theme));
            case LIGHTNING_CHAIN -> spawnLightningChain(level, pos, payload.getSecondaryPos(), theme);
            case PROJECTILE_TRAIL -> {
                Entity target = level.getEntity(payload.targetEntityId());
                if (target != null) {
                    activeTrails.add(new ActiveTrail(target, theme, payload.durationTicks()));
                }
            }
            case AURA -> {
                Entity target = level.getEntity(payload.targetEntityId());
                if (target != null) {
                    activeAuras.add(new ActiveAura(target, theme, payload.durationTicks()));
                }
            }
            case ENSNARE -> spawnEnsnare(level, pos, payload.scale(), theme);
        }
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) return;

        // 1. Ticki smug pocisków
        Iterator<ActiveTrail> trailIt = activeTrails.iterator();
        while (trailIt.hasNext()) {
            ActiveTrail trail = trailIt.next();
            if (!trail.tick(level)) {
                trailIt.remove();
            }
        }

        // 2. Ticki aur
        Iterator<ActiveAura> auraIt = activeAuras.iterator();
        while (auraIt.hasNext()) {
            ActiveAura aura = auraIt.next();
            if (!aura.tick(level)) {
                auraIt.remove();
            }
        }

        // 3. Ticki pierścieni naziemnych
        Iterator<ActiveGroundRing> ringIt = activeRings.iterator();
        while (ringIt.hasNext()) {
            ActiveGroundRing ring = ringIt.next();
            if (!ring.tick(level)) {
                ringIt.remove();
            }
        }
    }

    // =========================================================================
    // IMPLEMENTACJE EFEKTÓW W STYLU MINECRAFT DUNGEONS
    // =========================================================================

    private static void spawnSlashArc(ClientLevel level, Vec3 pos, Vec3 dir, VfxTheme theme, float scale) {
        double yaw = Math.atan2(dir.z, dir.x);
        int steps = 18;
        double arcAngle = Math.PI * 0.7; // ~126 stopni łuku
        ParticleOptions primary = getPrimaryParticle(theme);
        ParticleOptions secondary = getSecondaryParticle(theme);

        for (int i = 0; i < steps; i++) {
            double progress = (double) i / (steps - 1);
            double currentAngle = yaw - (arcAngle / 2.0) + (progress * arcAngle);
            double dist = (0.9 + Math.sin(progress * Math.PI) * 0.5) * scale;

            double px = pos.x + Math.cos(currentAngle) * dist;
            double py = pos.y + (progress - 0.5) * 0.4;
            double pz = pos.z + Math.sin(currentAngle) * dist;

            double vx = Math.cos(currentAngle) * 0.15;
            double vy = 0.02;
            double vz = Math.sin(currentAngle) * 0.15;

            level.addParticle(primary, px, py, pz, vx, vy, vz);
            if (i % 2 == 0 && secondary != null) {
                level.addParticle(secondary, px, py, pz, vx * 0.5, vy, vz * 0.5);
            }
        }
    }

    private static void spawnImpactBurst(ClientLevel level, Vec3 pos, Vec3 normal, VfxTheme theme, float scale) {
        int count = 20;
        ParticleOptions primary = getPrimaryParticle(theme);
        ParticleOptions secondary = getSecondaryParticle(theme);

        Vec3 n = normal.lengthSqr() > 0.001 ? normal.normalize() : new Vec3(0, 1, 0);

        for (int i = 0; i < count; i++) {
            double rx = (level.getRandom().nextDouble() - 0.5) * 1.4;
            double ry = (level.getRandom().nextDouble() - 0.5) * 1.4;
            double rz = (level.getRandom().nextDouble() - 0.5) * 1.4;

            Vec3 vel = n.scale(0.3 + level.getRandom().nextDouble() * 0.4 * scale)
                    .add(rx * 0.25, ry * 0.25, rz * 0.25);

            level.addParticle(primary, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
            if (secondary != null && i % 3 == 0) {
                level.addParticle(secondary, pos.x, pos.y, pos.z, vel.x * 0.5, vel.y * 0.5, vel.z * 0.5);
            }
        }
    }

    private static void spawnLightningChain(ClientLevel level, Vec3 from, Vec3 to, VfxTheme theme) {
        Vec3 diff = to.subtract(from);
        double dist = diff.length();
        int segments = Math.max(4, (int) (dist * 4));
        Vec3 dir = diff.normalize();
        ParticleOptions particle = getPrimaryParticle(theme);

        Vec3 current = from;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Vec3 targetPoint = from.add(diff.scale(t));

            // Losowe przesunięcie prostopadłe dla efektu zygzaka błyskawicy
            double jaggedness = 0.2;
            if (i < segments) {
                targetPoint = targetPoint.add(
                        (level.getRandom().nextDouble() - 0.5) * jaggedness,
                        (level.getRandom().nextDouble() - 0.5) * jaggedness,
                        (level.getRandom().nextDouble() - 0.5) * jaggedness
                );
            }

            // Gęste cząsteczki wzdłuż każdego małego segmentu
            int subSteps = 3;
            for (int s = 0; s < subSteps; s++) {
                Vec3 p = current.lerp(targetPoint, (double) s / subSteps);
                level.addParticle(particle, p.x, p.y, p.z, 0, 0, 0);
            }
            current = targetPoint;
        }
    }

    private static void spawnEnsnare(ClientLevel level, Vec3 center, float radius, VfxTheme theme) {
        int ringParticles = 24;
        ParticleOptions p = getPrimaryParticle(theme);

        for (int i = 0; i < ringParticles; i++) {
            double angle = (2.0 * Math.PI * i) / ringParticles;
            double x = center.x + Math.cos(angle) * (radius + 0.3);
            double z = center.z + Math.sin(angle) * (radius + 0.3);
            level.addParticle(p, x, center.y + 0.05, z, 0, 0.08, 0);
            level.addParticle(p, x, center.y + 0.5, z, 0, 0.04, 0);
        }
    }

    // =========================================================================
    // MAPOWANIE MOTYWÓW NA CZĄSTECZKI VANILLI (ZOPTYMALIZOWANE DLA EFEKTÓW DUNGEONS)
    // =========================================================================

    private static ParticleOptions getPrimaryParticle(VfxTheme theme) {
        return switch (theme) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case POISON -> ParticleTypes.HAPPY_VILLAGER;
            case VOID -> ParticleTypes.WITCH;
            case RADIANCE -> ParticleTypes.END_ROD;
            case SOUL -> ParticleTypes.SOUL_FIRE_FLAME;
            case DEFLECT -> ParticleTypes.CRIT;
            case VINES -> ParticleTypes.COMPOSTER;
            case WEBBING -> ParticleTypes.WHITE_ASH;
        };
    }

    private static ParticleOptions getSecondaryParticle(VfxTheme theme) {
        return switch (theme) {
            case FIRE -> ParticleTypes.LAVA;
            case FROST -> ParticleTypes.ENCHANTED_HIT;
            case LIGHTNING -> ParticleTypes.CRIT;
            case POISON -> ParticleTypes.ITEM_SLIME;
            case VOID -> ParticleTypes.PORTAL;
            case RADIANCE -> ParticleTypes.ENCHANT;
            case SOUL -> ParticleTypes.SOUL;
            case DEFLECT -> ParticleTypes.ENCHANTED_HIT;
            case VINES -> ParticleTypes.HAPPY_VILLAGER;
            case WEBBING -> ParticleTypes.SNOWFLAKE;
        };
    }

    // =========================================================================
    // KLASY POMOCNICZE CZASOWYCH EFEKTÓW
    // =========================================================================

    private static class ActiveTrail {
        private final Entity entity;
        private final VfxTheme theme;
        private int ticksRemaining;

        public ActiveTrail(Entity entity, VfxTheme theme, int durationTicks) {
            this.entity = entity;
            this.theme = theme;
            this.ticksRemaining = durationTicks;
        }

        public boolean tick(ClientLevel level) {
            if (!entity.isAlive() || entity.onGround() || ticksRemaining-- <= 0) {
                return false;
            }

            Vec3 pos = entity.position();
            Vec3 vel = entity.getDeltaMovement();
            ParticleOptions p = getPrimaryParticle(theme);
            ParticleOptions sec = getSecondaryParticle(theme);

            // Gęsta smuga za lecącym pociskiem
            for (int i = 0; i < 3; i++) {
                double t = (double) i / 3.0;
                double px = pos.x - vel.x * t;
                double py = pos.y - vel.y * t + entity.getBbHeight() * 0.5;
                double pz = pos.z - vel.z * t;
                level.addParticle(p, px, py, pz, 0, 0.01, 0);
                if (sec != null && i == 0) {
                    level.addParticle(sec, px, py, pz, 0, 0, 0);
                }
            }
            return true;
        }
    }

    private static class ActiveAura {
        private final Entity entity;
        private final VfxTheme theme;
        private int ticksRemaining;
        private int age = 0;

        public ActiveAura(Entity entity, VfxTheme theme, int durationTicks) {
            this.entity = entity;
            this.theme = theme;
            this.ticksRemaining = durationTicks;
        }

        public boolean tick(ClientLevel level) {
            if (!entity.isAlive() || ticksRemaining-- <= 0) {
                return false;
            }
            age++;
            double radius = entity.getBbWidth() * 0.8 + 0.2;
            double angle = age * 0.4;
            double x = entity.getX() + Math.cos(angle) * radius;
            double y = entity.getY() + (age % 20) * (entity.getBbHeight() / 20.0);
            double z = entity.getZ() + Math.sin(angle) * radius;

            ParticleOptions p = getPrimaryParticle(theme);
            level.addParticle(p, x, y, z, 0, 0.05, 0);
            return true;
        }
    }

    private static class ActiveGroundRing {
        private final Vec3 center;
        private final float maxRadius;
        private final int totalTicks;
        private final VfxTheme theme;
        private int age = 0;

        public ActiveGroundRing(Vec3 center, float maxRadius, int totalTicks, VfxTheme theme) {
            this.center = center;
            this.maxRadius = maxRadius;
            this.totalTicks = Math.max(1, totalTicks);
            this.theme = theme;
        }

        public boolean tick(ClientLevel level) {
            if (++age > totalTicks) return false;

            // Easing: szybki start, powolne wygaszanie
            float progress = (float) age / totalTicks;
            float currentRadius = maxRadius * (float) Math.sin(progress * Math.PI * 0.5);

            int points = (int) (currentRadius * 16) + 12;
            ParticleOptions p = getPrimaryParticle(theme);
            ParticleOptions sec = getSecondaryParticle(theme);

            for (int i = 0; i < points; i++) {
                double angle = (2.0 * Math.PI * i) / points;
                double px = center.x + Math.cos(angle) * currentRadius;
                double py = center.y + 0.05;
                double pz = center.z + Math.sin(angle) * currentRadius;

                double vx = Math.cos(angle) * 0.05;
                double vy = 0.02;
                double vz = Math.sin(angle) * 0.05;

                level.addParticle(p, px, py, pz, vx, vy, vz);
                if (sec != null && i % 4 == 0) {
                    level.addParticle(sec, px, py, pz, vx * 0.5, vy, vz * 0.5);
                }
            }
            return true;
        }
    }
}
