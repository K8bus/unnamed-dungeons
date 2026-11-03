package com.k8bus.dungeon.handler;

import com.k8bus.UnnamedDungeons;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class DungeonCombatHandler {

    public static void onEntityDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource damageSource) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!(damageSource.getEntity() instanceof ServerPlayer player)) return;

        DungeonState state = DungeonState.get(level);
        ActiveDungeon dungeon = state.getDungeonAt(entity.blockPosition());

        if (dungeon == null) return;
        if (dungeon.getPhase() != ActiveDungeon.DungeonPhase.ACTIVE) return;
        if (!dungeon.hasPlayer(player.getUUID())) return;

        if (!dungeon.getRooms().isEmpty()) {
            return;
        }

        dungeon.incrementKillCount();

        UnnamedDungeons.LOGGER.info(
                "{} zabił moba w dungeonie: {}/{}",
                player.getName().getString(),
                dungeon.getKillCount(),
                dungeon.getKillRequired()
        );

        if (dungeon.isCompleted()) {
            DungeonPhaseHandler.completeDungeon(level, dungeon, state);
            return;
        }

        state.setDirty();
    }
}
