package com.k8bus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DungeonGateBlockEntity extends BlockEntity {

    public enum GateState { LOCKED, UNLOCKED }

    private GateState gateState = GateState.UNLOCKED;

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

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putString("gate_state", gateState.name());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        String state = input.getStringOr("gate_state", "UNLOCKED");
        gateState = GateState.valueOf(state);
    }
}