package com.k8bus.mixin;

import com.k8bus.dungeon.DungeonState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketPlacementMixin {

    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
    private void unnamedDungeons$preventProtectedFluidPlacement(
            @Nullable LivingEntity user,
            Level level,
            BlockPos pos,
            @Nullable BlockHitResult hitResult,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (DungeonState.get(serverLevel).getDungeonAt(pos) == null) {
            return;
        }

        if (user instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                return;
            }
            if (serverPlayer.createCommandSourceStack().permissions().hasPermission(
                    new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)
            )) {
                return;
            }
        }

        cir.setReturnValue(false);
    }
}
