# Unnamed Dungeons Engine & API - To Do List

> Mod content (enchantments, effects, Wallet/economy, and Jungle Outpost) was moved to the separate `content` branch so `main` remains a clean Dungeon Engine & API.

## Engine & API (Completed)
- [x] Gate block tag `#unnamed-dungeons:dungeon_gates` and custom gate / `facing` + `open` resource-pack variants.
- [x] Configurable gate barrier sizes (`width x height`) via `DungeonGateBlock`, `DungeonGateBlockEntity`, `/dungeon setgatesize`, NBT metadata, JSON, and `DungeonApi`.
- [x] Repeatable dungeon mode (`repeatable`) and per-dungeon controllable `timings` (JSON + `DungeonApi.configureTimings`).
- [x] Expanded `DungeonEvents` (`CAN_ENTER_DUNGEON`, `DUNGEON_PHASE_CHANGE`, `WAVE_COMPLETE`, `CHEST_OPENED`).
- [x] Full area protection (breaking, block placement, bucket fluid placement, explosions) and chest/block restoration on `reset`, `resign`, and `repeatable` completion.
- [x] Fixed premature room/wave startup (rooms now start strictly during `ACTIVE` phase via `DungeonRoomHandler`).
- [x] Live in-game verification with structures converted from external mods.

## Future Work
- [ ] **DungeonsVfx (ULTRA WIP):** Rework and split `com.k8bus.vfx.*` out into a separate standalone VFX library mod.
- [ ] Placeholder/final blockstate & item models for `wooden_dungeon_chest`, `hero_chest`, `eldritch_chest` and `dungeon_gate` item/block variants, plus optional custom HUD icons.
