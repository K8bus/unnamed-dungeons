package com.k8bus.vfx;

import com.k8bus.network.DungeonsVfxPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DungeonsVfx {

    private DungeonsVfx() {}

    /**
     * Efekt zamachu bronią / cięcia (Slash Arc).
     * Wyświetla zakrzywioną, świecącą wstęgę cięcia w płaszczyźnie ciosu gracza.
     */
    public static void slashArc(ServerLevel level, LivingEntity attacker, VfxTheme theme) {
        Vec3 look = attacker.getLookAngle();
        Vec3 eyePos = attacker.getEyePosition();
        Vec3 spawnPos = eyePos.add(look.scale(0.8));

        send(level, spawnPos, new DungeonsVfxPayload(
                (byte) VfxType.SLASH_ARC.ordinal(),
                (byte) theme.ordinal(),
                (float) spawnPos.x, (float) spawnPos.y, (float) spawnPos.z,
                (float) look.x, (float) look.y, (float) look.z,
                0, 0, 0,
                1.5f,
                12,
                attacker.getId()
        ));
    }

    /**
     * Wybuch od punktu uderzenia (Impact Burst).
     * Płomienie, kryształy lub iskry eksplodujące w stożku od miejsca uderzenia.
     */
    public static void impactBurst(ServerLevel level, Vec3 hitPos, Vec3 hitNormal, VfxTheme theme) {
        send(level, hitPos, new DungeonsVfxPayload(
                (byte) VfxType.IMPACT_BURST.ordinal(),
                (byte) theme.ordinal(),
                (float) hitPos.x, (float) hitPos.y, (float) hitPos.z,
                (float) hitNormal.x, (float) hitNormal.y, (float) hitNormal.z,
                0, 0, 0,
                1.2f,
                15,
                0
        ));
    }

    /**
     * Płaski pierścień uderzenia na ziemi (Ground Ring / Shockwave).
     */
    public static void groundRing(ServerLevel level, Vec3 center, float radius, VfxTheme theme) {
        send(level, center, new DungeonsVfxPayload(
                (byte) VfxType.GROUND_RING.ordinal(),
                (byte) theme.ordinal(),
                (float) center.x, (float) center.y, (float) center.z,
                0, 1, 0,
                0, 0, 0,
                radius,
                18,
                0
        ));
    }

    /**
     * Błyskawica / promień łączący dwa punkty lub przeskakujący między celami (Lightning Chain / Beam).
     */
    public static void chain(ServerLevel level, Vec3 from, Vec3 to, VfxTheme theme) {
        send(level, from, new DungeonsVfxPayload(
                (byte) VfxType.LIGHTNING_CHAIN.ordinal(),
                (byte) theme.ordinal(),
                (float) from.x, (float) from.y, (float) from.z,
                0, 0, 0,
                (float) to.x, (float) to.y, (float) to.z,
                1.0f,
                12,
                0
        ));
    }

    /**
     * Smuga za lecącym pociskiem / strzałą (Projectile Ribbon Trail).
     */
    public static void projectileTrail(ServerLevel level, Entity projectile, VfxTheme theme) {
        Vec3 pos = projectile.position();
        send(level, pos, new DungeonsVfxPayload(
                (byte) VfxType.PROJECTILE_TRAIL.ordinal(),
                (byte) theme.ordinal(),
                (float) pos.x, (float) pos.y, (float) pos.z,
                0, 0, 0,
                0, 0, 0,
                1.0f,
                40,
                projectile.getId()
        ));
    }

    /**
     * Aura żywiołu wokół bytu.
     */
    public static void aura(ServerLevel level, Entity target, VfxTheme theme, int durationTicks) {
        Vec3 pos = target.position();
        send(level, pos, new DungeonsVfxPayload(
                (byte) VfxType.AURA.ordinal(),
                (byte) theme.ordinal(),
                (float) pos.x, (float) pos.y, (float) pos.z,
                0, 0, 0,
                0, 0, 0,
                1.0f,
                durationTicks,
                target.getId()
        ));
    }

    /**
     * Spętanie pnączami lub pajęczyną (Ensnare).
     */
    public static void ensnare(ServerLevel level, LivingEntity target, VfxTheme theme, int durationTicks) {
        Vec3 pos = target.position();
        send(level, pos, new DungeonsVfxPayload(
                (byte) VfxType.ENSNARE.ordinal(),
                (byte) theme.ordinal(),
                (float) pos.x, (float) pos.y, (float) pos.z,
                0, 0, 0,
                0, 0, 0,
                target.getBbWidth(),
                durationTicks,
                target.getId()
        ));
    }

    private static void send(ServerLevel level, Vec3 pos, DungeonsVfxPayload payload) {
        BlockPos bPos = BlockPos.containing(pos.x, pos.y, pos.z);
        for (ServerPlayer player : PlayerLookup.tracking(level, bPos)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
