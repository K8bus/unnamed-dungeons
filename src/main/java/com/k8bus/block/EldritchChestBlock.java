package com.k8bus.block;

import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.handler.DungeonPhaseHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class EldritchChestBlock extends WoodenDungeonChestBlock {
    public EldritchChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onOpened(Level level, BlockPos pos, Player player, boolean dropLoot) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        DungeonState state = DungeonState.get(serverLevel);
        ActiveDungeon dungeon = state.getDungeonAt(pos);
        int playerCount = dungeon != null ? Math.max(1, dungeon.getPlayerSnapshotCount()) : 1;

        if (dropLoot) {
            super.onOpened(level, pos, player);

            boolean droppedCustomLoot = dungeon != null
                    && dungeon.getEldritchChestLootTable() != null
                    && dropFromLootTable(serverLevel, pos, player, dungeon.getEldritchChestLootTable(), playerCount);

            if (!droppedCustomLoot) {
                // Skalowanie podstawowego dropu finałowego dla każdego gracza w drużynie (fallback bez loot table)
                for (int i = 0; i < playerCount; i++) {
                    dropItem(level, pos, new ItemStack(Items.NETHERITE_INGOT, 1));
                    dropItem(level, pos, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
                    dropItem(level, pos, new ItemStack(Items.DIAMOND_BLOCK, 1));
                }
            }
        }

        if (dungeon != null) {
            int completionSeconds = dungeon.getCompletionSeconds();
            DungeonPhaseHandler.transitionPhaseWithTimer(serverLevel, dungeon, ActiveDungeon.DungeonPhase.COMPLETED, completionSeconds);

            for (UUID pId : dungeon.getPlayers()) {
                ServerPlayer p = serverLevel.getServer().getPlayerList().getPlayer(pId);
                if (p != null) {
                    p.connection.send(new ClientboundSetTitleTextPacket(
                            Component.literal("§6§lDUNGEON UKOŃCZONY!")
                    ));
                    p.connection.send(new ClientboundSetSubtitleTextPacket(
                            Component.literal("§aZgarnij łupy! Ewakuacja za " + completionSeconds + " sekund.")
                    ));
                    serverLevel.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }

            state.setDirty();
        }
    }

    @Override
    protected void onOpened(Level level, BlockPos pos, Player player) {
        onOpened(level, pos, player, true);
    }
}
