package com.k8bus.mixin;

import com.k8bus.dungeon.DungeonState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockPlacementMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void unnamedDungeons$preventProtectedBlockPlacement(BlockPlaceContext placeContext, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(placeContext.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (DungeonState.get(serverLevel).getDungeonAt(placeContext.getClickedPos()) == null) {
            return;
        }

        Player player = placeContext.getPlayer();
        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                return;
            }
            if (serverPlayer.createCommandSourceStack().permissions().hasPermission(
                    new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)
            )) {
                return;
            }
        }

        cir.setReturnValue(InteractionResult.FAIL);
    }
}
