# Unnamed Dungeons Engine & API - To Do List

> Mod content (enchantments, effects, Wallet/economy, and Jungle Outpost) was moved to the separate `content` branch so `main` remains a clean Dungeon Engine & API.

## Engine & API (Completed)
- [x] Gate block tag `#unnamed-dungeons:dungeon_gates` and custom gate / `facing` + `open` resource-pack variants.
- [x] Configurable gate barrier sizes (`width x height`) via `DungeonGateBlock`, `DungeonGateBlockEntity`, `/dungeon setgatesize`, NBT metadata, JSON, and `DungeonApi`.
- [x] Repeatable dungeon mode (`repeatable`) and per-dungeon controllable `timings` (JSON + `DungeonApi.configureTimings`).
- [x] Expanded `DungeonEvents` (`CAN_ENTER_DUNGEON`, `DUNGEON_PHASE_CHANGE`, `WAVE_COMPLETE`, `CHEST_OPENED`).
- [x] Full area protection (breaking, block placement, bucket fluid placement, explosions) and chest/block restoration on `reset`, `resign`, and `repeatable` completion.
- [x] Fixed premature room/wave startup (rooms now start strictly during `ACTIVE` phase via `DungeonRoomHandler`).

## Before Release / Future Work
- [ ] Live in-game playtest (full run `WAITING` -> `PREPARING` -> `ACTIVE` -> `COMPLETED` / `repeatable`, `/dungeon reset`, `/dungeon resign`, non-OP block/bucket protection).
- [ ] Final 3D models/textures for `dungeon_gate` (`dungeon_gate.json` / `dungeon_gate_open.json`) and optional custom HUD icons.
- [ ] Optional split of `DungeonsVfx` into a standalone library mod.
