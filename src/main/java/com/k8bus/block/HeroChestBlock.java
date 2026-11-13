package com.k8bus.block;

import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.dungeon.RoomEncounter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Random;

public class HeroChestBlock extends WoodenDungeonChestBlock {
    public HeroChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onOpened(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        super.onOpened(level, pos, player);
        Random random = new Random();

        DungeonState state = DungeonState.get(serverLevel);
        ActiveDungeon dungeon = state.getDungeonAt(pos);
        int playerCount = dungeon != null ? Math.max(1, dungeon.getPlayerSnapshotCount()) : 1;

        RoomEncounter room = null;
        if (dungeon != null) {
            room = dungeon.getRoomAt(pos);
            if (room == null) {
                for (RoomEncounter candidate : dungeon.getRooms()) {
                    if (pos.equals(candidate.getHeroChestPos())) {
                        room = candidate;
                        break;
                    }
                }
            }
        }

        if (room != null && room.getRewardLootTable() != null
                && dropFromLootTable(serverLevel, pos, player, room.getRewardLootTable(), playerCount)) {
            return;
        }

        // Skalowanie podstawowego dropu dla każdego członka drużyny (fallback bez loot table)
        for (int i = 0; i < playerCount; i++) {
            dropItem(level, pos, new ItemStack(Items.DIAMOND, random.nextInt(2) + 1));
            dropItem(level, pos, new ItemStack(Items.GOLDEN_APPLE, 1));
            dropItem(level, pos, new ItemStack(Items.EXPERIENCE_BOTTLE, random.nextInt(4) + 3));
        }
    }
}
