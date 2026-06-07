package com.k8bus.visual_engine;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.util.List;

public class VisualInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            VisualEngine.getInstance().tick();
        });

        registerDefaultSequences();
    }

    private void registerDefaultSequences() {
        VisualEngine.getInstance().registerPattern(new AnimationSequence(
                "sonic_ring",
                List.of(
                        new AnimationFrame(8001, 1),
                        new AnimationFrame(8002, 2)
                ),
                false
        ));
    }
}
