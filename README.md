# Unnamed Dungeons Engine & API

A data-driven **Instanced Dungeon Engine** (**NBT + JSON**), **3D Flood-Fill Room Detector**, **NBT Structure Compiler**, and an experimental **(ULTRA WIP) Combat VFX Library** for **Fabric** (Minecraft `26.2`, Java `25`).

Instead of relying on static vanilla spawners, **Unnamed Dungeons** provides a full RPG/MMORPG-style dungeon state machine: waiting lobbies, ready-up countdowns, player-scaled combat waves, full area protection (breaking, block placement, fluid placement, explosions), chest restoration on reset, repeatable dungeon runs, tag-based gates with configurable barrier dimensions, anti-stuck mob highlighting, and a clean Java API + Fabric Events for modders, datapack authors, and mapmakers.

---

## Table of Contents
1. [Core Engine Features](#core-engine-features)
2. [How Dungeon Lifecycle Works](#how-dungeon-lifecycle-works)
3. [Step-by-Step Guide: Building & Running a Dungeon In-Game](#step-by-step-guide-building--running-a-dungeon-in-game)
4. [Command Reference](#command-reference)
5. [Gates, Custom Blocks & Resource Packs (`#unnamed-dungeons:dungeon_gates`)](#gates-custom-blocks--resource-packs-unnamed-dungeonsdungeon_gates)
6. [JSON Data-Driven System (Datapacks & Export)](#json-data-driven-system-datapacks--export)
7. [NBT Structure Compiler (Structure Block Metadata)](#nbt-structure-compiler-structure-block-metadata)
8. [Java API for Mod Developers](#java-api-for-mod-developers)
   - [DungeonApi – Instance Management](#1-dungeonapi--instance-management)
   - [DungeonEvents – Fabric Callbacks & Hooks](#2-dungeonevents--fabric-callbacks--hooks)
   - [DungeonsVfx – Visual Effects API (ULTRA WIP)](#3-dungeonsvfx--visual-effects-api-ultra-wip)
9. [License](#license)

---

## Core Engine Features

* **Instance State Machine (`ActiveDungeon`):**
  `IDLE` → `WAITING` (lobby countdown + ready-up) → `PREPARING` (player count snapshot, waiting gate locks) → `ACTIVE` (main dungeon gate opens, room waves trigger on entry) → `COMPLETED` / `RESETTING`.
* **Repeatable Runs & Per-Dungeon Timings:**
  Every dungeon instance has its own configurable `repeatable` flag and `timings` (`waiting_seconds`, `preparing_seconds`, `completion_seconds`, `between_waves_min_seconds`, `between_waves_max_seconds`) — no global mod config overrides individual dungeons.
* **Hybrid NBT + JSON Data-Driven Architecture:**
  Dungeon geometry, boxes, gates, and spawn markers can come from `.nbt` structures or in-game Flood-Fill scanning, while wave counts, player scaling, custom mob pools, loot tables, timings, and gate barrier sizes can be configured in `.json` files (`data/<namespace>/dungeons/*.json`). Any dungeon built in-game can be exported straight to JSON.
* **Automatic 3D Flood-Fill Room Detection (`RoomSpaceDetector`):**
  Stand inside an enclosed room and run `/dungeon addroom` — a 3D BFS scan automatically detects walls and `#unnamed-dungeons:dungeon_gates` gates to compute the exact room `BoundingBox`.
* **Player-Scaled Waves & Custom Mob Pools (`WaveDefinition`):**
  Each wave scales its mob count using: `baseCount + (players - 1) * extraPerPlayer`. Assign any vanilla or modded entity IDs (e.g., `minecraft:blaze`, `my_mod:boss`).
* **Anti-Stuck Tracking & Protection:**
  When `<= 2` mobs remain in a room wave, they automatically receive `GLOWING`. Inside dungeon bounds, Survival/Adventure players cannot break blocks, place blocks, pour buckets, or blow up terrain with explosions (Creative builders and `GAMEMASTERS` operators bypass block/bucket protection).
* **Loot & Chest Restoration:**
  Built-in crates/chests (`wooden_dungeon_chest`, `hero_chest`, `eldritch_chest`) support vanilla/custom `LootTable`s and are automatically restored to their original state when a dungeon resets, resigns, or finishes in `repeatable` mode.

---

## How Dungeon Lifecycle Works

1. **`IDLE` (Ready for players):**
   * Players in **Creative** or **Spectator** mode are treated as builders and do not trigger the dungeon.
   * When a **Survival** (or Adventure) player steps inside the dungeon `BoundingBox`, `DungeonEvents.CAN_ENTER_DUNGEON` is checked.
   * Allowed players are saved (original game mode & spawnpoint), switched to **Adventure** mode, and have their temporary respawn point set near the lobby. The dungeon moves to `WAITING`.
2. **`WAITING` (Lobby countdown):**
   * Counts down `waiting_seconds` (default `30s`) and syncs the timer to the client HUD.
   * **Ready-Up:** Right-clicking a dungeon gate during `WAITING` shortens the remaining timer to `3` seconds (if more than 3s remain).
   * If all players leave during `WAITING`, the dungeon returns to `IDLE`.
3. **`PREPARING` (Lock-in):**
   * Snapshots the current player count (`playerSnapshotCount`) for wave scaling.
   * Locks `waitingGate` (closes gate + places invisible `BARRIER` blocks) and counts down `preparing_seconds` (default `10s`).
4. **`ACTIVE` (Combat):**
   * Unlocks `dungeonGate` so players can enter the dungeon rooms.
   * Rooms remain `IDLE` until a dungeon player physically enters a room's `roomBox`.
   * Entering an `IDLE` room locks its `rewardGatePos` and spawns Wave 1. Clearing all mobs starts a between-waves countdown (`between_waves_min_seconds`..`between_waves_max_seconds`).
   * Clearing all waves unlocks `rewardGatePos` and spawns `HeroChest` at `heroChestPos` (if configured).
5. **`COMPLETED` / `RESETTING`:**
   * Opening `EldritchChest` starts the evacuation countdown (`completion_seconds`, default `15s`).
   * **If `repeatable == false`:** players are restored & teleported to `waitingSpawnPos`, barriers/mobs are cleared, waiting & dungeon gates are removed, and the dungeon instance is removed from `DungeonState`.
   * **If `repeatable == true`:** players are restored & teleported to `waitingSpawnPos`, all opened/spawned chests & tracked blocks are restored, mobs and barriers are cleared, `waitingGate` is unlocked, `dungeonGate` and room `rewardGate`s are locked, and the dungeon returns to `RESETTING` (until players exit the bounds) / `IDLE`.

---

## Step-by-Step Guide: Building & Running a Dungeon In-Game

All builder commands require `GAMEMASTERS` (OP) permissions.

### 1. Build the layout (in Creative mode)
* Build an entrance/waiting lobby and at least one **fully enclosed** combat room (floor, walls, ceiling — no gaps to the outside so 3D Flood-Fill can detect the room walls).
* Place `unnamed-dungeons:dungeon_gate` blocks in your doorways:
  1. **Waiting Gate** (entrance to the waiting lobby)
  2. **Dungeon Gate** (between the waiting lobby and the dungeon)
  3. **Room Reward Gate(s)** (exit of each combat room)
* Optionally place `unnamed-dungeons:wooden_dungeon_chest` crates around the dungeon and an `unnamed-dungeons:eldritch_chest` in the final treasure room.

### 2. Register and configure the dungeon
1. Stand near the center of your dungeon build and create the outer bounding box:
   ```mcfunction
   /dungeon setbox 30 30
   ```
   *(Creates a box with radius `30` around you and sets fallback kill goal `30` if no rooms are added.)*
2. Stand inside the waiting lobby and set the spawn/teleport point:
   ```mcfunction
   /dungeon setwaitingspawn
   ```
3. Look directly at the waiting lobby gate and bind it:
   ```mcfunction
   /dungeon setwaitinggate
   ```
4. Look directly at the main dungeon entrance gate and bind it:
   ```mcfunction
   /dungeon setdungeongate
   ```
5. *(Optional)* Make the dungeon replayable and resize any gate's barrier area (e.g. 5 wide × 4 tall):
   ```mcfunction
   /dungeon setrepeatable true
   /dungeon setgatesize 5 4
   ```
6. Stand inside your first enclosed room and add it with 3 wave base mob counts:
   ```mcfunction
   /dungeon addroom 5 8 12
   ```
   * Room indices start at `0`. Look at the room's exit gate, an empty floor spot for the reward chest, and customize its mob pool:
   ```mcfunction
   /dungeon setroomgate 0
   /dungeon setherochest 0
   /dungeon setroommobs 0 "minecraft:zombie,minecraft:skeleton,minecraft:blaze"
   ```
7. *(Optional)* Export everything (rooms, waves, mob pools, timings, gate sizes, `repeatable`) to JSON so you can tweak timings or loot tables in a text editor and reload them anytime:
   ```mcfunction
   /dungeon export "my_crypt"
   /dungeon applyjson "my_crypt"
   ```

### 3. Playtest the dungeon
* Leave the dungeon bounding box and switch to Survival: `/gamemode survival`.
* Walk into the waiting lobby — you will be switched to Adventure mode and the countdown will begin.
* Right-click a gate to skip the wait countdown to `3` seconds, fight through the rooms, open the chests, and test `/dungeon resign`, `/dungeon reset`, or `repeatable` completion.
* Check `/dungeon status` at any time to inspect phase, kill count, and room states.

---

## Command Reference

| Command | Description |
| :--- | :--- |
| `/dungeon setbox <size> <killsRequired>` | Creates a dungeon bounding box of radius `<size>` (1–100) around the player. |
| `/dungeon setwaitingspawn` | Sets the lobby spawn/evacuation position to your current position. |
| `/dungeon setwaitinggate` | Binds the waiting lobby gate to the `#unnamed-dungeons:dungeon_gates` block you are looking at. |
| `/dungeon setdungeongate` | Binds the main dungeon entrance gate to the gate block you are looking at. |
| `/dungeon addroom <wave1> <wave2> <wave3>` | Runs 3D Flood-Fill from your position and adds a room with 3 wave base counts. |
| `/dungeon setroomgate <index>` | Binds the exit reward gate for room `<index>` (0, 1, 2...) to the gate block you are looking at. |
| `/dungeon setgatesize <width> <height>` | Sets custom barrier dimensions (`1..32`) on the gate block you are looking at. |
| `/dungeon setrepeatable <true\|false>` | Enables or disables automatic reset instead of removal on dungeon completion. |
| `/dungeon setherochest <index>` | Sets the `HeroChest` spawn position for room `<index>` to the block you are looking at. |
| `/dungeon setroommobs <index> "<mobsCsv>"` | Sets the custom mob pool for room `<index>` (e.g. `"minecraft:blaze,minecraft:wither_skeleton"`). |
| `/dungeon export "<id>"` | Exports rooms, waves, timings, `repeatable`, and gate sizes to `<world>/dungeons/<namespace>/<path>.json`. |
| `/dungeon applyjson "<id>"` | Loads a JSON definition (from world folder or datapack) and applies it to the current dungeon. |
| `/dungeon load "<templateOrJsonId>" [kills] [rotation]` | Spawns an `.nbt` structure at your position and auto-applies matching `.json` (`rotation`: `none`, `90`, `180`, `270`). |
| `/dungeon status` | Prints status of all active dungeons and rooms in the current dimension. |
| `/dungeon resign` | Abandons the current run, teleports players to lobby spawn, restores chests, and resets state. |
| `/dungeon reset` | Resets the dungeon you are standing in back to `IDLE`. |
| `/dungeon forcereset` / `/dungeon clear` | Emergency-resets all dungeons or removes all active dungeon instances and barriers. |

---

## Gates, Custom Blocks & Resource Packs (`#unnamed-dungeons:dungeon_gates`)

* **Block Tag:** `data/unnamed-dungeons/tags/block/dungeon_gates.json` defines `#unnamed-dungeons:dungeon_gates` (includes `unnamed-dungeons:dungeon_gate` by default).
* **Custom Gate Blocks from Other Mods / Datapacks:**
  * Add any block to `#unnamed-dungeons:dungeon_gates` (or extend `DungeonGateBlock`).
  * If the block exposes `BlockStateProperties.HORIZONTAL_FACING` (`facing`) and/or `BlockStateProperties.OPEN` (`open`), the engine automatically reads `facing` to orient barriers and updates `open=true`/`open=false` on lock/unlock.
* **Resource Pack Support for `unnamed-dungeons:dungeon_gate`:**
  * [blockstates/dungeon_gate.json](src/main/resources/assets/unnamed-dungeons/blockstates/dungeon_gate.json) provides all `facing` (`north`, `south`, `east`, `west`) and `open` (`false`, `true`) variants.
  * Override `assets/unnamed-dungeons/models/block/dungeon_gate.json` (closed) and `assets/unnamed-dungeons/models/block/dungeon_gate_open.json` (open) with your own models and textures.
* **Barrier Size Resolution Order:**
  1. Custom `width x height` saved on `DungeonGateBlockEntity` (set via `/dungeon setgatesize`, JSON, NBT metadata, or `DungeonApi.setGateBarrierSize`).
  2. Default `width x height` passed to the `DungeonGateBlock` constructor.
  3. Built-in fallback: `2x2` for `ROOM` gates, `3x3` for `WAITING` and `DUNGEON` gates.

---

## JSON Data-Driven System (Datapacks & Export)

Definitions are resolved in the following priority order:
1. **World folder override:** `<world>/dungeons/<namespace>/<path>.json` (also where `/dungeon export` writes files).
2. **Datapack / mod resources:** `data/<namespace>/dungeons/<path>.json`.

### Example `data/unnamed-dungeons/dungeons/example_crypt.json`:
```json
{
  "template": "unnamed-dungeons:example_crypt",
  "kills_required": 30,
  "repeatable": true,
  "timings": {
    "waiting_seconds": 30,
    "preparing_seconds": 10,
    "completion_seconds": 15,
    "between_waves_min_seconds": 3,
    "between_waves_max_seconds": 5
  },
  "waiting_gate_size": { "width": 3, "height": 3 },
  "dungeon_gate_size": { "width": 5, "height": 4 },
  "eldritch_chest_loot_table": "minecraft:chests/ancient_city",
  "rooms": {
    "1": {
      "reward_loot_table": "minecraft:chests/simple_dungeon",
      "reward_gate_size": { "width": 2, "height": 3 },
      "waves": [
        {
          "base_count": 5,
          "extra_per_player": 2,
          "mob_pool": [
            "minecraft:zombie",
            "minecraft:husk"
          ]
        },
        {
          "base_count": 8,
          "extra_per_player": 3,
          "mob_pool": [
            "minecraft:skeleton",
            "minecraft:stray"
          ]
        }
      ]
    },
    "2": {
      "reward_loot_table": "minecraft:chests/bastion_treasure",
      "waves": [
        {
          "base_count": 6,
          "extra_per_player": 3,
          "mob_pool": [
            "minecraft:wither_skeleton",
            "minecraft:blaze"
          ]
        }
      ]
    }
  }
}
```

---

## NBT Structure Compiler (Structure Block Metadata)

When saving a structure to `.nbt`, place **Structure Blocks in `DATA` mode** with the metadata tags below. Calling `/dungeon load` (or `DungeonApi.spawnFromTemplate`) automatically parses all positions with rotation applied, applies matching JSON if present, and replaces the Structure Blocks with air.

### Global Dungeon Markers
* `dungeon_box_start` & `dungeon_box_end` – corners of the dungeon bounding box (optional; defaults to the `.nbt` template bounds).
* `waiting_spawn` – lobby teleport/evacuation position.
* `waiting_gate` or `waiting_gate:<width>x<height>` – waiting lobby gate position (and optional barrier size).
* `dungeon_gate` or `dungeon_gate:<width>x<height>` – main dungeon entrance gate position (and optional barrier size).
* `eldritch_chest` or `eldritch_chest:<namespace>:<loot_table>` – final chest position and optional loot table.

### Room Markers (`<id>` is the 1-based room number, e.g. `1`, `2`, `3`)
* `room_<id>_box_start` & `room_<id>_box_end` – corners of room `<id>`.
* `room_<id>_gate` or `room_<id>_gate:<width>x<height>` – reward gate unlocked after clearing room `<id>`.
* `room_<id>_chest` or `room_<id>_chest:<namespace>:<loot_table>` – `HeroChest` position (and optional loot table).
* `room_<id>_spawn` – dedicated mob spawn point inside the room (you can place multiple).
* `room_<id>_waves_<w1>_<w2>_<w3>` – inline wave counts without JSON (e.g. `room_1_waves_6_10_14`).
* `room_<id>_mobs:<id1>,<id2>` – inline mob pool without JSON (e.g. `room_1_mobs:minecraft:breeze,minecraft:stray`).

---

## Java API for Mod Developers

Main API entry points live in `com.k8bus.api` (and experimental VFX in `com.k8bus.vfx`).

### 1. `DungeonApi` – Instance Management

```java
import com.k8bus.api.DungeonApi;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.WaveDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

// 1. Spawn a dungeon from an NBT template + matching JSON definition:
ActiveDungeon spawned = DungeonApi.spawnFromTemplate(
    serverLevel,
    originPos,
    Identifier.fromNamespaceAndPath("unnamed-dungeons", "example_crypt"),
    30,
    Rotation.CLOCKWISE_90
);

// 2. Configure repeatability, per-dungeon timings (seconds), and gate barrier size:
DungeonApi.setRepeatable(serverLevel, spawned, true);
DungeonApi.configureTimings(serverLevel, spawned, 20, 5, 10, 2, 4);
DungeonApi.setGateBarrierSize(serverLevel, spawned.getDungeonGatePos(), 5, 4);

// 3. Programmatically detect a room via 3D Flood-Fill and attach waves:
DungeonApi.getDungeonAt(serverLevel, player.blockPosition()).ifPresent(dungeon -> {
    List<WaveDefinition> waves = List.of(
        new WaveDefinition(5, 2, List.of(Identifier.withDefaultNamespace("husk"))),
        new WaveDefinition(8, 3, List.of(
            Identifier.withDefaultNamespace("wither_skeleton"),
            Identifier.withDefaultNamespace("blaze")
        ))
    );
    DungeonApi.detectAndAddRoom(serverLevel, dungeon, player.blockPosition(), waves);
});
```

### 2. `DungeonEvents` – Fabric Callbacks & Hooks

Register listeners during mod initialization (`onInitialize()`):

```java
import com.k8bus.api.DungeonEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.Monster;

public void registerDungeonHooks() {
    // Control who can enter a dungeon (e.g. key check, party limit, level requirement):
    DungeonEvents.CAN_ENTER_DUNGEON.register((level, dungeon, player) -> !player.isSleeping());

    // Customize spawned wave mobs (gear, stats, bossbars):
    DungeonEvents.MOB_SPAWNED.register((level, room, waveIndex, entity) -> {
        if (waveIndex >= 2 && entity instanceof Monster monster) {
            monster.setCustomName(Component.literal("§4Elite Guardian"));
        }
    });

    // React when a room is cleared:
    DungeonEvents.ROOM_CLEAR.register((level, dungeon, room) -> {
        // Custom effects, sounds, advancement triggers, etc.
    });

    // Override chest loot (return false to cancel the engine's built-in / loot-table drops):
    DungeonEvents.CHEST_OPENED.register((level, dungeon, pos, player, chestState) -> true);
}
```

Available events in [`DungeonEvents`](src/main/java/com/k8bus/api/DungeonEvents.java):
* `DungeonEvents.CAN_ENTER_DUNGEON` – `(level, dungeon, player)` → `boolean`
* `DungeonEvents.DUNGEON_PHASE_CHANGE` – `(level, dungeon, oldPhase, newPhase)`
* `DungeonEvents.DUNGEON_START` – `(level, dungeon)`
* `DungeonEvents.DUNGEON_COMPLETE` – `(level, dungeon)`
* `DungeonEvents.DUNGEON_RESET` – `(level, dungeon, resigned)`
* `DungeonEvents.PLAYER_CHANGE` – `(level, dungeon, player, entered)`
* `DungeonEvents.WAVE_START` – `(level, room, waveIndex, waveDefinition)`
* `DungeonEvents.WAVE_COMPLETE` – `(level, dungeon, room, completedWaveIndex)`
* `DungeonEvents.MOB_SPAWNED` – `(level, room, waveIndex, entity)`
* `DungeonEvents.ROOM_CLEAR` – `(level, dungeon, room)`
* `DungeonEvents.CHEST_OPENED` – `(level, dungeon, pos, player, chestState)` → `boolean`

### 3. `DungeonsVfx` – Visual Effects API (ULTRA WIP)

> [!WARNING]
> **ULTRA WIP / EXPERIMENTAL:** `DungeonsVfx` (`com.k8bus.vfx.*`) is currently **heavy work-in-progress** and **will be extracted into a separate standalone mod** in the future. Method signatures, payload formats, visuals, and models may break or be removed from this mod without backwards compatibility. Rely on `com.k8bus.api.*` (`DungeonApi` / `DungeonEvents`) for production integrations.

Static helper sending VFX packets to tracking players (`PlayerLookup.tracking`). Available themes in [`VfxTheme`](src/main/java/com/k8bus/vfx/VfxTheme.java): `FIRE`, `FROST`, `LIGHTNING`, `POISON`, `VOID`, `RADIANCE`, `SOUL`, `DEFLECT`, `VINES`, `WEBBING`:

```java
import com.k8bus.vfx.DungeonsVfx;
import com.k8bus.vfx.VfxTheme;

DungeonsVfx.slashArc(serverLevel, attackerEntity, VfxTheme.VOID);
DungeonsVfx.impactBurst(serverLevel, hitPosVec3, hitNormalVec3, VfxTheme.FIRE);
DungeonsVfx.groundRing(serverLevel, centerVec3, 4.0f, VfxTheme.LIGHTNING);
DungeonsVfx.chain(serverLevel, fromVec3, toVec3, VfxTheme.LIGHTNING);
DungeonsVfx.projectileTrail(serverLevel, arrowEntity, VfxTheme.POISON);
DungeonsVfx.aura(serverLevel, targetEntity, VfxTheme.FROST, 100);
DungeonsVfx.ensnare(serverLevel, targetLivingEntity, VfxTheme.VINES, 60);
```

---

## License

**Copyright (c) 2026 k8bus. All Rights Reserved (ARR).**

* **Allowed:** Using official releases of this mod as a library/API dependency in your own mods and including it in modpacks on Modrinth / CurseForge.
* **Prohibited:** Copying source code or assets into your own projects, modifying, or re-uploading `.jar` files without explicit permission from the author. See [LICENSE](LICENSE) for details.
