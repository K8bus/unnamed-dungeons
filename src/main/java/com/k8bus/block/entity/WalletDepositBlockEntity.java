package com.k8bus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.k8bus.menu.WalletDepositMenu;
import org.jetbrains.annotations.Nullable;

public class WalletDepositBlockEntity extends BlockEntity implements MenuProvider {
    public WalletDepositBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WALLET_DEPOSIT, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Wpłatomat");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new WalletDepositMenu(syncId, playerInventory);
    }
}
