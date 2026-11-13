package com.k8bus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DungeonGateBlockEntity extends BlockEntity {

    public enum GateState { LOCKED, UNLOCKED }

    private GateState gateState = GateState.UNLOCKED;
    private int barrierWidth = 0;
    private int barrierHeight = 0;

    public DungeonGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DUNGEON_GATE, pos, state);
    }

    public void lock() {
        if (this.gateState == GateState.LOCKED) {
            return;
        }

        this.gateState = GateState.LOCKED;
        setChanged();
    }

    public void unlock() {
        if (this.gateState == GateState.UNLOCKED) {
            return;
        }

        this.gateState = GateState.UNLOCKED;
        setChanged();
    }

    public boolean isLocked() {
        return gateState == GateState.LOCKED;
    }

    public int getBarrierWidth() {
        return barrierWidth;
    }

    public int getBarrierHeight() {
        return barrierHeight;
    }

    public boolean hasCustomBarrierSize() {
        return barrierWidth > 0 && barrierHeight > 0;
    }

    public void setBarrierSize(int width, int height) {
        this.barrierWidth = Math.max(0, width);
        this.barrierHeight = Math.max(0, height);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putString("gate_state", gateState.name());
        output.putInt("barrier_width", barrierWidth);
        output.putInt("barrier_height", barrierHeight);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        String state = input.getStringOr("gate_state", "UNLOCKED");
        try {
            gateState = GateState.valueOf(state);
        } catch (IllegalArgumentException ignored) {
            gateState = GateState.UNLOCKED;
        }
        barrierWidth = Math.max(0, input.getIntOr("barrier_width", 0));
        barrierHeight = Math.max(0, input.getIntOr("barrier_height", 0));
    }
}
