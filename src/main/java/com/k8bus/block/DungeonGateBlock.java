package com.k8bus.block;

import com.k8bus.block.entity.DungeonGateBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class DungeonGateBlock extends BaseEntityBlock {

    public static final MapCodec<DungeonGateBlock> CODEC = simpleCodec(DungeonGateBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public DungeonGateBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    public DungeonGateBlock() {
        this(BlockBehaviour.Properties.of()
                .strength(-1.0f, 3600000.0f)
                .noOcclusion());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPEN)) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonGateBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.k8bus.dungeon.DungeonState dungeonState = com.k8bus.dungeon.DungeonState.get(serverLevel);
            com.k8bus.dungeon.ActiveDungeon dungeon = dungeonState.getDungeonAt(pos);
            if (dungeon != null && dungeon.getPhase() == com.k8bus.dungeon.ActiveDungeon.DungeonPhase.WAITING) {
                if (dungeon.getPhaseTimer() > 60) { // jeśli zostało więcej niż 3 sekundy
                    dungeon.phaseTimer = 60; // skróć do 3s
                    dungeonState.setDirty();

                    for (java.util.UUID pId : dungeon.getPlayers()) {
                        net.minecraft.server.level.ServerPlayer p = serverLevel.getServer().getPlayerList().getPlayer(pId);
                        if (p != null) {
                            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                                    net.minecraft.network.chat.Component.literal("§6§lGOTOWOŚĆ! START ZA 3s")
                            ));
                            serverLevel.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                        }
                    }
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}