package com.k8bus.mixin;

import com.k8bus.dungeon.DungeonState;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.level.ServerExplosion.class)
public class ExplosionMixin {

    @Shadow
    @Final
    private Level level;

    @Shadow
    @Final
    private java.util.List<BlockPos> toBlow;

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void unnamedDungeons$protectDungeonBlocks(boolean spawnParticles, CallbackInfo ci) {
        if (!level.isClientSide()) {
            DungeonState state = DungeonState.get((ServerLevel) level);
            this.toBlow.removeIf(pos -> state.getDungeonAt(pos) != null);
        }
    }
}
