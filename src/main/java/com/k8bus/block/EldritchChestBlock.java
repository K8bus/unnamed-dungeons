package com.k8bus.block;

import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.DungeonState;
import com.k8bus.enchantment.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class EldritchChestBlock extends WoodenDungeonChestBlock {
    public EldritchChestBlock(Properties properties) {
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

        List<ResourceKey<Enchantment>> uniquePool = List.of(
                ModEnchantments.VINES, ModEnchantments.WEBBING
        );
        List<ResourceKey<Enchantment>> powerfulPool = List.of(
                ModEnchantments.JUNGLES_WRATH, ModEnchantments.LIFE_LEECH,
                ModEnchantments.NEUROTOXICITY, ModEnchantments.THUNDERING,
                ModEnchantments.VOID_STRIKE
        );

        // Skalowanie dropu finałowego dla każdego gracza w drużynie
        for (int i = 0; i < playerCount; i++) {
            if (random.nextFloat() < 0.20f) { // 20% Unique
                ResourceKey<Enchantment> rolled = uniquePool.get(random.nextInt(uniquePool.size()));
                dropEnchantedBook(serverLevel, pos, rolled, 1);
            } else { // Powerful
                ResourceKey<Enchantment> rolled = powerfulPool.get(random.nextInt(powerfulPool.size()));
                dropEnchantedBook(serverLevel, pos, rolled, random.nextInt(3) + 1);
            }

            dropItem(level, pos, new ItemStack(Items.NETHERITE_INGOT, 1));
            dropItem(level, pos, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
            dropItem(level, pos, new ItemStack(Items.DIAMOND_BLOCK, 1));
        }

        if (dungeon != null) {
            // Rozpocznij 15-sekundowy czas na zebranie łupów przed powrotem
            dungeon.setPhaseWithTimer(ActiveDungeon.DungeonPhase.COMPLETED, 15);

            for (UUID pId : dungeon.getPlayers()) {
                ServerPlayer p = serverLevel.getServer().getPlayerList().getPlayer(pId);
                if (p != null) {
                    p.connection.send(new ClientboundSetTitleTextPacket(
                            Component.literal("§6§lDUNGEON UKOŃCZONY!")
                    ));
                    p.connection.send(new ClientboundSetSubtitleTextPacket(
                            Component.literal("§aZgarnij łupy! Ewakuacja za 15 sekund.")
                    ));
                    serverLevel.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }

            state.setDirty();
        }
    }
}
