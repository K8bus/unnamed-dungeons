package com.k8bus.mixin;

import com.k8bus.dungeon.DungeonState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class BlockMiningMixin {

    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void unnamedDungeons$preventProtectedBlockBreaking(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Dungeon istnieje w DungeonState dla faz: IDLE, WAITING, PREPARING, ACTIVE, RESETTING.
        // Po ukończeniu (COMPLETED) dungeon jest usuwany z DungeonState przez completeDungeon(),
        // więc getDungeonAt() zwróci null i ochrona automatycznie znika.
        if (DungeonState.get(level).getDungeonAt(pos) == null) {
            return;
        }

        // Gracze w trybie Creative mogą niszczyć (np. budowniczy/admin)
        if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return;
        }

        // Operatorzy (gamemaster+) mogą niszczyć
        if (player.createCommandSourceStack().permissions().hasPermission(
                new net.minecraft.server.permissions.Permission.HasCommandLevel(
                        net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS
                )
        )) {
            return;
        }

        cir.setReturnValue(false);
    }
}