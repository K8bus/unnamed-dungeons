# Unnamed Dungeons Engine & API

Silnik instancyjnych dungeonów (**NBT + JSON Data-Driven**), automatyczny detektor pokoi 3D (Flood-Fill), kompilator struktur `.nbt` oraz biblioteka efektów wizualnych (VFX) dla **Fabric**.

Zamiast tradycyjnych struktur ze spawnerami, **Unnamed Dungeons** dostarcza pełną maszynę stanów w stylu gier RPG / MMORPG: poczekalnie, odliczanie, skalowanie fal pod liczbę graczy, ochronę terenu (niszczenie, stawianie bloków i płynów, eksplozje), przywracanie skrzyń przy resecie, powtarzalny tryb dungeonu (`repeatable`), konfigurowalne rozmiary bram (`#unnamed-dungeons:dungeon_gates`), system anti-stuck podświetlający ostatnie moby oraz pełne API dla twórców modów i datapacków.

---

## Spis treści
1. [Główne funkcje silnika](#główne-funkcje-silnika)
2. [System bram i własne modele (`#unnamed-dungeons:dungeon_gates`)](#system-bram-i-własne-modele-unnamed-dungeonsdungeon_gates)
3. [Komendy w grze (In-Game Builder)](#komendy-w-grze-in-game-builder)
4. [System JSON Data-Driven (Datapacki i Eksport)](#system-json-data-driven-datapacki-i-eksport)
5. [Kompilator struktur NBT (Structure Block Metadata)](#kompilator-struktur-nbt-structure-block-metadata)
6. [Java API dla deweloperów](#java-api-dla-deweloperów)
   - [DungeonApi – zarządzanie instancjami](#1-dungeonapi--zarządzanie-instancjami)
   - [DungeonEvents – hooki i callbacki Fabric](#2-dungeonevents--hooki-i-callbacki-fabric)
   - [DungeonsVfx – biblioteka efektów wizualnych](#3-dungeonsvfx--biblioteka-efektów-wizualnych)
7. [Licencja](#licencja)

---

## Główne funkcje silnika

* **Maszyna stanów instancji (`ActiveDungeon`):**
  `IDLE` → `WAITING` (poczekalnia i odliczanie) → `PREPARING` (snapshot liczby graczy, zamknięcie bramy poczekalni) → `ACTIVE` (otwarcie bramy głównej, walka w pokojach) → `COMPLETED` / `RESETTING`.
* **Tryb powtarzalny (`repeatable`) i indywidualne czasy (`timings`):**
  Każdy dungeon może posiadać własne czasy (`waiting_seconds`, `preparing_seconds`, `completion_seconds`, `between_waves_min_seconds`, `between_waves_max_seconds`) oraz flagę `repeatable` (po ukończeniu bramy, moby i otwarte/zniszczone skrzynie wracają do stanu początkowego zamiast usuwać instancję).
* **Hybrydowy system NBT + JSON Data-Driven:**
  Geometria i pozycje bram pochodzą ze struktury `.nbt` (lub detektora Flood-Fill w grze), natomiast konfiguracja fal, przeliczników graczy, puli mobów, czasów, rozmiarów bram oraz tablic łupów (`LootTable`) jest ładowana z plików `.json` w datapackach (`data/<namespace>/dungeons/*.json`). Skonfigurowany w grze dungeon można jednym poleceniem wyeksportować do gotowego pliku `.json`.
* **Automatyczna detekcja pokoi 3D (`RoomSpaceDetector`):**
  Algorytm BFS 3D automatycznie wykrywa ściany i bramy z tagu `#unnamed-dungeons:dungeon_gates`, wyliczając `BoundingBox` pokoju od pozycji gracza.
* **Skalowanie fal i customowe pule mobów (`WaveDefinition`):**
  Każda fala przelicza liczbę mobów według wzoru: `baseCount + (players - 1) * extraPerPlayer`. Możesz przypisać dowolne moby z vanilli lub innych modów (np. `minecraft:blaze`, `my_mod:boss`).
* **Ochrona instancji i cykl życia gracza:**
  Automatyczne przełączanie graczy w tryb `ADVENTURE`, zapis i przywracanie spawnpointu, blokada niszczenia, stawiania bloków, rozlewania płynów z wiader oraz eksplozji wewnątrz granic dungeonu (z wyjątkiem trybu Creative i operatorów `GAMEMASTERS`), przywracanie skrzyń przy resecie/rezygnacji oraz synchronizacja HUD-u klienta.

---

## System bram i własne modele (`#unnamed-dungeons:dungeon_gates`)

* Silnik rozpoznaje bramy na podstawie tagu bloków `data/unnamed-dungeons/tags/block/dungeon_gates.json` (domyślnie zawiera `unnamed-dungeons:dungeon_gate`), jak również dowolnych podklas `DungeonGateBlock`.
* Zewnętrzne mody lub datapacki mogą dodać do tego tagu własne bloki bram — silnik automatycznie obsługuje opcjonalne właściwości `facing` (`HORIZONTAL_FACING`) oraz `open` (`OPEN`) i stawia/usuwa niewidzialne bariery wokół bramy.
* Własne modele 3D / tekstury można podmienić przez resource pack w `assets/unnamed-dungeons/blockstates/dungeon_gate.json` (warianty `facing` + `open=false`/`open=true`) oraz `models/block/dungeon_gate.json` / `dungeon_gate_open.json`.
* Rozmiar bariery każdej bramy (`width x height`) jest konfigurowalny indywidualnie przez:
  * konstruktor `DungeonGateBlock(properties, width, height)`,
  * blok-encja `DungeonGateBlockEntity` / komendę `/dungeon setgatesize <width> <height>`,
  * tag metadanych w `.nbt` (np. `waiting_gate:5x4`),
  * definicję JSON (`waiting_gate_size`, `dungeon_gate_size`, `reward_gate_size`),
  * metodę API `DungeonApi.setGateBarrierSize(...)`.

---

## Komendy w grze (In-Game Builder)

Wymagane uprawnienia: poziom `GAMEMASTERS` (OP).

| Komenda | Opis |
| :--- | :--- |
| `/dungeon setbox <size> <killsRequired>` | Tworzy nowy obszar dungeonu wokół gracza o promieniu `<size>`. |
| `/dungeon setwaitingspawn` | Ustawia punkt spawnu poczekalni w miejscu, w którym stoisz. |
| `/dungeon setwaitinggate` | Ustawia bramę poczekalni na blok bramy (`#unnamed-dungeons:dungeon_gates`), na który aktualnie patrzysz. |
| `/dungeon setdungeongate` | Ustawia główną bramę wejściową do dungeonu na blok bramy, na który patrzysz. |
| `/dungeon addroom <wave1> <wave2> <wave3>` | Automatycznie skanuje pokój (Flood-Fill 3D od pozycji gracza) i dodaje go z 3 falami o podanej bazowej liczbie mobów. |
| `/dungeon setroomgate <index>` | Przypisuje bramę wyjściową (otwieraną po ukończeniu wszystkich fal) dla pokoju `<index>` (0, 1, 2...) na blok bramy, na który patrzysz. |
| `/dungeon setgatesize <width> <height>` | Ustawia indywidualny rozmiar bariery (`width x height`) dla bramy, na którą aktualnie patrzysz. |
| `/dungeon setrepeatable <true\|false>` | Włącza lub wyłącza automatyczny reset dungeonu po jego ukończeniu (`repeatable`). |
| `/dungeon setherochest <index>` | Ustawia pozycję skrzyni nagrody (`Hero Chest`) dla pokoju `<index>` na blok, na który patrzysz. |
| `/dungeon setroommobs <index> "<mobsCsv>"` | Ustawia pulę mobów dla pokoju `<index>`, np. `/dungeon setroommobs 0 "minecraft:blaze,minecraft:wither_skeleton"`. |
| `/dungeon export "<id>"` | Eksportuje pokoje, fale, czasy, powtarzalność i rozmiary bram aktualnego dungeonu do pliku JSON w `<world>/dungeons/<namespace>/<path>.json`. |
| `/dungeon applyjson "<id>"` | Wczytuje plik JSON (z datapacka lub folderu świata) i nakłada jego konfigurację na aktualny dungeon. |
| `/dungeon load "<templateOrJsonId>" [kills] [rotation]` | Wczytuje strukturę `.nbt` i automatycznie nakłada na nią definicję `.json` o tym samym ID (`rotation`: `none`, `90`, `180`, `270`). |
| `/dungeon status` | Wyświetla stan wszystkich aktywnych dungeonów i pokoi na świecie. |
| `/dungeon reset` / `/dungeon resign` | Resetuje dungeon, w którym stoisz, lub wycofuje z niego drużynę. |
| `/dungeon forcereset` / `/dungeon clear` | Awaryjnie resetuje lub usuwa wszystkie instancje na świecie. |

---

## System JSON Data-Driven (Datapacki i Eksport)

Silnik automatycznie szuka definicji JSON w dwóch miejscach:
1. W folderze świata: `<world>/dungeons/<namespace>/<path>.json` (tam zapisywane są pliki po użyciu `/dungeon export`).
2. W datapackach / zasobach modów: `data/<namespace>/dungeons/<path>.json`.

### Przykład pliku `data/unnamed-dungeons/dungeons/example_crypt.json`:
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

## Kompilator struktur NBT (Structure Block Metadata)

Podczas budowania struktury do eksportu `.nbt`, możesz postawić **Structure Blocki w trybie `DATA`** i wpisać poniższe znaczniki w polu metadanych. Podczas wywołania `/dungeon load` (lub `DungeonApi.spawnFromTemplate`) silnik sam odczyta ich pozycje z uwzględnieniem obrotu (`Rotation`), nałoży konfigurację JSON i zastąpi Structure Blocki powietrzem:

### Znaczniki globalne dungeonu:
* `dungeon_box_start` oraz `dungeon_box_end` – rogi całego dungeonu (opcjonalne; brak oznacza użycie wymiarów całego szablonu `.nbt`).
* `waiting_spawn` – punkt teleportacji w poczekalni.
* `waiting_gate` lub `waiting_gate:<width>x<height>` – pozycja (i opcjonalny rozmiar bariery) bramy poczekalni.
* `dungeon_gate` lub `dungeon_gate:<width>x<height>` – pozycja (i opcjonalny rozmiar bariery) głównej bramy wejściowej do lochu.
* `eldritch_chest` lub `eldritch_chest:<namespace>:<loot_table>` – główna skrzynia końcowa.

### Znaczniki pokoi (`<id>` to numer pokoju, np. `1`, `2`, `3`):
* `room_<id>_box_start` oraz `room_<id>_box_end` – rogi pokoju `<id>`.
* `room_<id>_gate` lub `room_<id>_gate:<width>x<height>` – brama otwierana po oczyszczeniu pokoju `<id>`.
* `room_<id>_chest` lub `room_<id>_chest:<namespace>:<loot_table>` – pozycja `Hero Chest` po ukończeniu pokoju.
* `room_<id>_spawn` – dedykowany punkt spawnu mobów w pokoju (można postawić wiele bloków z tym samym tagiem).
* `room_<id>_waves_<w1>_<w2>_<w3>` – szybka definicja fal bez JSON-a (np. `room_1_waves_6_10_14`).
* `room_<id>_mobs:<id1>,<id2>` – szybka pula mobów bez JSON-a (np. `room_1_mobs:minecraft:breeze,minecraft:stray`).

---

## Java API dla deweloperów

Wszystkie główne klasy API znajdują się w pakietach `com.k8bus.api` oraz `com.k8bus.vfx`.

### 1. `DungeonApi` – zarządzanie instancjami

```java
import com.k8bus.api.DungeonApi;
import com.k8bus.dungeon.ActiveDungeon;
import com.k8bus.dungeon.WaveDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

// 1. Spawnowanie gotowego dungeonu z szablonu NBT + JSON:
ActiveDungeon spawned = DungeonApi.spawnFromTemplate(
    serverLevel,
    originPos,
    Identifier.fromNamespaceAndPath("unnamed-dungeons", "example_crypt"),
    30,
    Rotation.CLOCKWISE_90
);

// 2. Konfiguracja powtarzalności, czasów (w sekundach) oraz rozmiaru bramy:
DungeonApi.setRepeatable(serverLevel, spawned, true);
DungeonApi.configureTimings(serverLevel, spawned, 20, 5, 10, 2, 4);
DungeonApi.setGateBarrierSize(serverLevel, spawned.getDungeonGatePos(), 5, 4);

// 3. Programowe wykrycie pokoju (Flood-Fill 3D) i dodanie własnych fal:
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

### 2. `DungeonEvents` – hooki i callbacki Fabric

Możesz zarejestrować własną logikę w `onInitialize()` swojego moda:

```java
import com.k8bus.api.DungeonEvents;
import com.k8bus.vfx.DungeonsVfx;
import com.k8bus.vfx.VfxTheme;
import net.minecraft.world.entity.monster.Monster;

public void registerDungeonHooks() {
    // Warunek wejścia do dungeonu (np. wymagany klucz lub limit graczy):
    DungeonEvents.CAN_ENTER_DUNGEON.register((level, dungeon, player) -> !player.isSleeping());

    // Modyfikacja każdego moba spawnowanego przez falę (np. customowe eq, statystyki, aura VFX):
    DungeonEvents.MOB_SPAWNED.register((level, room, waveIndex, entity) -> {
        if (waveIndex >= 2 && entity instanceof Monster monster) {
            monster.setCustomName(net.minecraft.network.chat.Component.literal("§4Elitarny Strażnik"));
            DungeonsVfx.aura(level, monster, VfxTheme.SOUL, 200);
        }
    });

    // Reakcja na oczyszczenie pokoju:
    DungeonEvents.ROOM_CLEAR.register((level, dungeon, room) -> {
        if (!room.getHeroChestPos().equals(net.minecraft.core.BlockPos.ZERO)) {
            DungeonsVfx.groundRing(level, room.getHeroChestPos().getCenter(), 3.5f, VfxTheme.RADIANCE);
        }
    });

    // Przejęcie dropu ze skrzyni (zwrócenie false blokuje domyślny drop silnika):
    DungeonEvents.CHEST_OPENED.register((level, dungeon, pos, player, chestState) -> true);
}
```

Dostępne eventy w [`DungeonEvents`](src/main/java/com/k8bus/api/DungeonEvents.java):
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

### 3. `DungeonsVfx` – biblioteka efektów wizualnych

Statyczne API wysyłające zoptymalizowane pakiety efektów do graczy śledzących dany chunk (`PlayerLookup.tracking`). Każdy efekt przyjmuje motyw kolorystyczny `VfxTheme` (`FIRE`, `FROST`, `LIGHTNING`, `POISON`, `VOID`, `RADIANCE`, `SOUL`, `DEFLECT`, `VINES`, `WEBBING`):

```java
import com.k8bus.vfx.DungeonsVfx;
import com.k8bus.vfx.VfxTheme;

// Łuk cięcia mieczem przed graczem/mobem:
DungeonsVfx.slashArc(serverLevel, attackerEntity, VfxTheme.VOID);

// Stożkowy rozbłysk w punkcie trafienia:
DungeonsVfx.impactBurst(serverLevel, hitPosVec3, hitNormalVec3, VfxTheme.FIRE);

// Fala uderzeniowa (pierścień) na ziemi:
DungeonsVfx.groundRing(serverLevel, centerVec3, 4.0f, VfxTheme.LIGHTNING);

// Promień / błyskawica łącząca dwa punkty (np. chain lightning):
DungeonsVfx.chain(serverLevel, fromVec3, toVec3, VfxTheme.LIGHTNING);

// Smuga za lecącym pociskiem / strzałą:
DungeonsVfx.projectileTrail(serverLevel, arrowEntity, VfxTheme.POISON);

// Aura żywiołu wokół bytu (czas w tickach):
DungeonsVfx.aura(serverLevel, targetEntity, VfxTheme.FROST, 100);

// Efekt spętania (pnącza/pajęczyna) wokół celu:
DungeonsVfx.ensnare(serverLevel, targetLivingEntity, VfxTheme.VINES, 60);
```

---

## Licencja

**Copyright (c) 2026 k8bus. All Rights Reserved (ARR).**

* **Dozwolone:** Wykorzystywanie oficjalnych wydań tego moda jako zależności (API / Library dependency) we własnych modach oraz dołączanie go do paczek modów (Modpacks) na Modrinth / CurseForge.
* **Zabronione:** Kopiowanie kodu źródłowego lub zasobów (assets) do własnych projektów, modyfikowanie oraz re-uploadowanie plików `.jar` bez zgody autora. Szczegóły w pliku [LICENSE](LICENSE).
