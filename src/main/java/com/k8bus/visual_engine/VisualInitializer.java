package com.k8bus.visual_engine;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;

public class VisualInitializer implements ModInitializer {

    @Override
    public void onInitialize() {
        // Podpięcie tick loop silnika
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            VisualEngine.get().tick();
            WebRemovalTask.tick();  // ← to musi być
        });
        VfxItems.register();
        registerEffects();
    }

    // =========================================================================
    // Tutaj rejestrujesz wszystkie efekty enchantów.
    // Każdy to 1 linia — rejestracja przez VfxBuilder.
    // =========================================================================

    private static void registerEffects() {
        VisualEngine e = VisualEngine.get();

        // ----- FIRE -----
        e.register(VfxBuilder.item(8001)
                .scale(1.2f).fadeIn(4).hold(6).fadeOut(5)
                .build("fire_burst"));

        e.register(VfxBuilder.item(8001, 8002, 8003).ticks(2)
                .scale(0.6f).fadeIn(3).fadeOut(4).fixed()
                .build("fire_trail"));

        // ----- FROST / ICE -----
        e.register(VfxBuilder.item(8010)
                .scale(1.0f).fadeIn(5).hold(4).fadeOut(6)
                .build("frost_burst"));

        e.register(VfxBuilder.block(Blocks.BLUE_ICE)
                .scale(0.9f, 0.05f, 0.9f).fadeIn(3).hold(8).fadeOut(5).fixed()
                .build("frost_ring"));

        // ----- LIGHTNING -----
        e.register(VfxBuilder.item(8020)
                .scale(1.0f, 3.0f, 1.0f).fadeIn(2).hold(3).fadeOut(3)
                .build("lightning_bolt"));

        e.register(VfxBuilder.item(8021).ticks(1)
                .scale(0.4f).fadeIn(2).fadeOut(3).fixed()
                .build("lightning_arc_trail"));    // do playTrail()

        // ----- POISON -----
        e.register(VfxBuilder.item(8030)
                .scale(1.5f).fadeIn(6).hold(10).fadeOut(8)
                .build("poison_cloud"));

        e.register(VfxBuilder.item(8031, 8032, 8033).ticks(4)
                .scale(0.5f).loop().fadeIn(3).bright(true)
                .build("poison_aura"));             // ambient — zatrzymaj przez stop()

        // ----- SOUL -----
        e.register(VfxBuilder.item(8040, 8041).ticks(3)
                .scale(0.8f).fadeIn(4).hold(6).fadeOut(5)
                .build("soul_burst"));

        e.register(VfxBuilder.item(8042).ticks(2)
                .scale(0.5f).fadeIn(3).fadeOut(4).fixed()
                .build("soul_trail"));

        // ----- VOID -----
        e.register(VfxBuilder.item(8050)
                .scale(1.0f).fadeIn(5).hold(4).fadeOut(6)
                .build("void_burst"));

        e.register(VfxBuilder.block(Blocks.CRYING_OBSIDIAN)
                .scale(0.8f, 0.04f, 0.8f).fadeIn(4).hold(10).fadeOut(6).fixed()
                .build("void_ring"));

        // ----- HEAL / RADIANCE -----
        e.register(VfxBuilder.item(8060)
                .scale(1.8f).fadeIn(5).hold(4).fadeOut(6)
                .build("radiance_burst"));

        e.register(VfxBuilder.block(Blocks.SEA_LANTERN)
                .scale(0.6f, 0.04f, 0.6f).fadeIn(3).hold(6).fadeOut(5).fixed()
                .build("heal_ring"));

        // ----- IMPACT / SHOCKWAVE -----
        e.register(VfxBuilder.item(8070)
                .scale(2.5f, 0.04f, 2.5f).fadeIn(4).hold(2).fadeOut(5).fixed()
                .build("shockwave_ring"));

        e.register(VfxBuilder.item(8071)
                .scale(1.0f).fadeIn(3).hold(2).fadeOut(3)
                .build("crit_burst"));

        // ----- CHAIN / BIND -----
        e.register(VfxBuilder.item(8080).ticks(2)
                .scale(0.5f).fadeIn(2).fadeOut(3).fixed()
                .build("chain_arc_trail"));         // do playTrail() między wrogami

        e.register(VfxBuilder.item(8081, 8082).ticks(5)
                .scale(0.7f).loop().fadeIn(4)
                .build("chain_aura"));              // ambient status effect

        // ----- STATUS EFFECTS (ambient nad głową / na encji) -----
        e.register(VfxBuilder.item(9001, 9002, 9003).ticks(4)
                .scale(0.6f).loop().fadeIn(3).billboard(Display.BillboardConstraints.CENTER)
                .build("status_burning"));

        e.register(VfxBuilder.item(9010, 9011).ticks(5)
                .scale(0.7f).loop().fadeIn(4)
                .build("status_frozen"));

        e.register(VfxBuilder.item(9020, 9021).ticks(4)
                .scale(0.6f).loop().fadeIn(3)
                .build("status_poisoned"));

        e.register(VfxBuilder.item(9030).ticks(3)
                .scale(0.8f).loop().fadeIn(3)
                .build("status_shocked"));

        e.register(VfxBuilder.item(9040, 9041, 9042).ticks(3)
                .scale(0.9f).loop().fadeIn(4)
                .build("status_strength"));

        e.register(VfxBuilder.item(9050, 9051).ticks(5)
                .scale(0.7f).loop().fadeIn(4)
                .build("status_chained"));

        // ----- BRAKUJĄCE #1-3: Burst / Hit -----

        // Explosion ring — duży wybuch przy killu (Exploding enchant)
        e.register(VfxBuilder.item(8090)
                .scale(3.0f, 0.04f, 3.0f).fadeIn(3).hold(2).fadeOut(6).fixed()
                .build("explosion_ring"));

        // Chain impact — błysk w miejscu uderzenia zanim łańcuch poleci do kolejnego wroga
        e.register(VfxBuilder.item(8091, 8092).ticks(2)
                .scale(0.9f).fadeIn(2).hold(2).fadeOut(3)
                .build("chain_impact"));

        // ----- BRAKUJĄCE #4-6: Trail -----

        // Sonic ring trail — trail strzały (przeniesiony ze starego kodu)
        e.register(VfxBuilder.item(8100).ticks(1)
                .scale(0.5f).fadeIn(1).fadeOut(3).fixed()
                .build("sonic_ring_trail"));

        // Poison trail — zielony drip za zatrutym pociskiem
        e.register(VfxBuilder.item(8101, 8102).ticks(2)
                .scale(0.35f).fadeIn(1).fadeOut(5).fixed()
                .build("poison_trail"));

        // Void trail — ciemny ślad za Void Strike projectile
        e.register(VfxBuilder.item(8103).ticks(2)
                .scale(0.4f).fadeIn(1).fadeOut(4).fixed()
                .build("void_trail"));

        // ----- BRAKUJĄCE #7-9: Directional -----

        // Swirling blade ring — ring wirujących ostrzy dookoła gracza (Swirling enchant on kill)
        e.register(VfxBuilder.item(8110, 8111, 8112, 8113).ticks(2)
                .scale(1.8f, 0.06f, 1.8f).fadeIn(3).hold(4).fadeOut(5).fixed()
                .build("swirling_blade"));

        // Gravity pull — inward ring przy trafieniu (Gravity enchant)
        e.register(VfxBuilder.item(8120, 8121).ticks(3)
                .scale(2.0f, 0.04f, 2.0f).fadeIn(5).hold(2).fadeOut(5).fixed()
                .build("gravity_pull"));

        // Overcharge bolt — biały burst w momencie oddania strzału przy pełnym naciągu
        e.register(VfxBuilder.item(8130)
                .scale(1.1f).fadeIn(3).hold(2).fadeOut(4)
                .build("overcharge_bolt"));

        // ----- BRAKUJĄCE #10-11: Status effects -----

        // Weakness — szara aura (Weakened debuff)
        e.register(VfxBuilder.item(9060, 9061).ticks(6)
                .scale(0.65f).loop().fadeIn(4)
                .build("status_weakness"));

        // Levitation — unoszące się cząstki nad głową (Gravity enchant hit)
        e.register(VfxBuilder.item(9070, 9071, 9072).ticks(4)
                .scale(0.5f).loop().fadeIn(3).billboard(Display.BillboardConstraints.CENTER)
                .build("status_levitation"));

        // Rampaging — czerwona aura prędkości po killu (Rampaging enchant)
        e.register(VfxBuilder.item(9080, 9081).ticks(3)
                .scale(1.0f).loop().fadeIn(4).billboard(Display.BillboardConstraints.FIXED)
                .build("status_rampaging"));
    }
}

// ============================================================
// NOWE: Web / Poison rise / Deflect trail
// ============================================================
// Dołącz do registerEffects() lub wywołaj osobno.

/*
    // ----- WEB -----
    // Centralna masa pajęczyny (BlockDisplay z cobwebem — bez resource packa!)
    e.register(VfxBuilder.block(Blocks.COBWEB)
            .scale(1.1f).fadeIn(3).hold(55).fadeOut(5).fixed()
            .build("web_center"));

    // Pojedyncza nić — spawnowana w okręgu przez playCircle()
    e.register(VfxBuilder.block(Blocks.COBWEB)
            .scale(0.35f).fadeIn(2).hold(55).fadeOut(4).fixed()
            .build("web_strand"));

    // Mała sieć nad głową (attached)
    e.register(VfxBuilder.block(Blocks.COBWEB)
            .scale(0.6f, 0.08f, 0.6f).fadeIn(2).hold(55).fadeOut(4).fixed()
            .build("web_overhead"));

    // ----- POISON RISE -----
    // Pionowe zielone smugi wznoszące się wokół zatrutego gracza.
    // Model 8210 = cienki pionowy prostokąt (w Blockbench: ~0.1 x 2.0 x 0.1).
    // riseY(2f) animuje translację Y od 0 do 2 bloków podczas fade-in.
    e.register(VfxBuilder.item(8210, 8211).ticks(3)
            .scale(0.12f, 1.8f, 0.12f).riseY(1.8f)
            .fadeIn(12).fadeOut(8).fixed()
            .build("poison_rise"));

    // ----- DEFLECT TRAIL -----
    // Smuga za odbitą strzałą — segment spawnowany co tick przez DeflectTrailMixin.
    // Model 8220 = cienki poziomy prostokąt (trail segment, jak sonic_ring_trail).
    e.register(VfxBuilder.item(8220).ticks(1)
            .scale(0.3f, 0.3f, 1.0f).fadeIn(1).fadeOut(5).fixed()
            .build("deflect_trail"));
*/

