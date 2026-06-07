package com.k8bus.block;

import com.k8bus.block.entity.DungeonGateBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class DungeonGateBlock extends BaseEntityBlock {

    public static final MapCodec<DungeonGateBlock> CODEC = simpleCodec(DungeonGateBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public DungeonGateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public DungeonGateBlock() {
        this(BlockBehaviour.Properties.of()
                .strength(-1.0f, 3600000.0f)
                .noOcclusion());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonGateBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}