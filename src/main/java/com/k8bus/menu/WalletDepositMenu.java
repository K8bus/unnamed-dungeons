package com.k8bus.menu;

import com.k8bus.dungeon.DungeonState;
import com.k8bus.network.WalletSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;

public class WalletDepositMenu extends AbstractContainerMenu {
    private final Container inventory;

    public WalletDepositMenu(int syncId, Inventory playerInventory) {
        super(ModMenuTypes.WALLET_DEPOSIT, syncId);
        this.inventory = new SimpleContainer(1);
        
        inventory.startOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.EMERALD) || stack.is(Items.EMERALD_BLOCK);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                processDeposit(playerInventory.player);
            }
        });

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    private void processDeposit(Player player) {
        if (player.level().isClientSide()) return;
        
        ItemStack stack = inventory.getItem(0);
        if (!stack.isEmpty() && (stack.is(Items.EMERALD) || stack.is(Items.EMERALD_BLOCK))) {
            int amount = stack.getCount() * (stack.is(Items.EMERALD_BLOCK) ? 9 : 1);
            stack.setCount(0);
            
            DungeonState state = DungeonState.get((ServerLevel) player.level());
            state.addWalletBalance(player.getUUID(), amount);
            
            if (player instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(serverPlayer, new WalletSyncPayload(state.getWalletBalance(player.getUUID())));
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(itemStack2, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (itemStack2.is(Items.EMERALD) || itemStack2.is(Items.EMERALD_BLOCK)) {
                if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index < 28) {
                if (!this.moveItemStackTo(itemStack2, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 28 && index < 37 && !this.moveItemStackTo(itemStack2, 1, 28, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
        }

        return itemStack;
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
        if (!player.level().isClientSide()) {
            ItemStack stack = this.inventory.getItem(0);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }
}
