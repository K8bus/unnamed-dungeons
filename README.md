# Unnamed Dungeons Engine & API

Silnik instancyjnych dungeonów (**NBT + JSON Data-Driven**), automatyczny detektor pokoi 3D (Flood-Fill), kompilator struktur `.nbt` oraz biblioteka efektów wizualnych (VFX) dla **Fabric**.

Zamiast tradycyjnych struktur ze spawnerami, **Unnamed Dungeons** dostarcza pełną maszynę stanów w stylu gier RPG / MMORPG: poczekalnie, odliczanie, skalowanie fal pod liczbę graczy, blokadę niszczenia terenu, automatyczne otwieranie/zamykanie bram (`DungeonGateBlock`), system anti-stuck podświetlający ostatnie moby oraz pełne API dla twórców modów i datapacków.

---

## Spis treści
1. [Główne funkcje silnika](#główne-funkcje-silnika)
2. [Komendy w grze (In-Game Builder)](#komendy-w-grze-in-game-builder)
3. [System JSON Data-Driven (Datapacki i Eksport)](#system-json-data-driven-datapacki-i-eksport)
4. [Kompilator struktur NBT (Structure Block Metadata)](#kompilator-struktur-nbt-structure-block-metadata)
5. [Java API dla deweloperów](#java-api-dla-deweloperów)
   - [DungeonApi – zarządzanie instancjami](#1-dungeonapi--zarządzanie-instancjami)
   - [DungeonEvents – hooki i callbacki Fabric](#2-dungeonevents--hooki-i-callbacki-fabric)
   - [DungeonsVfx – biblioteka efektów wizualnych](#3-dungeonsvfx--biblioteka-efektów-wizualnych)
6. [Licencja](#licencja)

---

## Główne funkcje silnika

* **Maszyna stanów instancji (`ActiveDungeon`):**
  `IDLE` → `WAITING` (poczekalnia i odliczanie) → `PREPARING` (snapshot liczby graczy, zamknięcie bramy poczekalni) → `ACTIVE` (otwarcie bramy głównej, walka w pokojach) → `COMPLETED` / `RESETTING`.
* **Hybrydowy system NBT + JSON Data-Driven:**
  Geometria i pozycje bram pochodzą ze struktury `.nbt` (lub detektora Flood-Fill w grze), natomiast konfiguracja fal, przeliczników graczy, puli mobów i tablic łupów (`LootTable`) jest ładowana z plików `.json` w datapackach (`data/<namespace>/dungeons/*.json`). Skonfigurowany w grze dungeon można jednym poleceniem wyeksportować do gotowego pliku `.json`!
* **Automatyczna detekcja pokoi 3D (`RoomSpaceDetector`):**
  Nie musisz ręcznie wpisywać koordynatów `x1 y1 z1 x2 y2 z2` dla każdego pokoju. Stajesz wewnątrz zbudowanego pomieszczenia, a algorytm BFS 3D sam wykrywa ściany i bramy `dungeon_gate`, automatycznie wyliczając `BoundingBox` pokoju.
* **Skalowanie fal i customowe pule mobów (`WaveDefinition`):**
  Każda fala przelicza liczbę mobów według wzoru: `baseCount + (players - 1) * extraPerPlayer`. Możesz przypisać dowolne moby z vanilli lub innych modów (np. `minecraft:blaze`, `my_mod:boss`).
* **Ochrona instancji i cykl życia gracza:**
  Automatyczne przełączanie graczy w tryb `ADVENTURE`, tymczasowy zapis i przywracanie spawnpointu (nawet po wyjściu z serwera w trakcie gry), blokada kopania i eksplozji wewnątrz granic dungeonu oraz synchronizacja HUD-u klienta.

---

## Komendy w grze (In-Game Builder)

Wymagane uprawnienia: poziom `GAMEMASTERS` (OP).

| Komenda | Opis |
| :--- | :--- |
| `/dungeon setbox <size> <killsRequired>` | Tworzy nowy obszar dungeonu wokół gracza o promieniu `<size>`. |
| `/dungeon setwaitingspawn` | Ustawia punkt spawnu poczekalni w miejscu, w którym stoisz. |
| `/dungeon setwaitinggate` | Ustawia bramę poczekalni na blok `Dungeon Gate`, na który aktualnie patrzysz. |
| `/dungeon setdungeongate` | Ustawia główną bramę wejściową do dungeonu na blok `Dungeon Gate`, na który patrzysz. |
| `/dungeon addroom <wave1> <wave2> <wave3>` | Automatycznie skanuje pokój (Flood-Fill 3D od pozycji gracza) i dodaje go z 3 falami o podanej bazowej liczbie mobów. |
| `/dungeon setroomgate <index>` | Przypisuje bramę wyjściową (otwieraną po ukończeniu wszystkich fal) dla pokoju `<index>` (0, 1, 2...) na blok `Dungeon Gate`, na który patrzysz. |
| `/dungeon setherochest <index>` | Ustawia pozycję skrzyni nagrody (`Hero Chest`) dla pokoju `<index>` na blok, na który patrzysz. |
| `/dungeon setroommobs <index> "<mobsCsv>"` | Ustawia pulę mobów dla pokoju `<index>`, np. `/dungeon setroommobs 0 "minecraft:blaze,minecraft:wither_skeleton"`. |
| `/dungeon export "<id>"` | Eksportuje pokoje, fale i pule mobów aktualnego dungeonu do pliku JSON w `<world>/dungeons/<namespace>/<path>.json`. |
| `/dungeon applyjson "<id>"` | Wczytuje plik JSON (z datapacka lub folderu świata) i nakłada jego konfigurację fal oraz mobów na aktualny dungeon. |
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
  "eldritch_chest_loot_table": "minecraft:chests/ancient_city",
  "rooms": {
    "1": {
      "reward_loot_table": "minecraft:chests/simple_dungeon",
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
* `waiting_gate` – pozycja bramy poczekalni.
* `dungeon_gate` – pozycja głównej bramy wejściowej do lochu.
* `eldritch_chest` lub `eldritch_chest:<namespace>:<loot_table>` – główna skrzynia końcowa.

### Znaczniki pokoi (`<id>` to numer pokoju, np. `1`, `2`, `3`):
* `room_<id>_box_start` oraz `room_<id>_box_end` – rogi pokoju `<id>`.
* `room_<id>_gate` – brama otwierana po oczyszczeniu pokoju `<id>`.
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
DungeonApi.spawnFromTemplate(
    serverLevel,
    originPos,
    Identifier.fromNamespaceAndPath("unnamed-dungeons", "example_crypt"),
    30,
    Rotation.CLOCKWISE_90
);

// 2. Programowe wykrycie pokoju (Flood-Fill 3D) i dodanie własnych fal:
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
    // Modyfikacja każdego moba spawnowanego przez falę (np. customowe eq, statystyki, aura VFX):
    DungeonEvents.MOB_SPAWNED.register((level, room, waveIndex, entity) -> {
        if (waveIndex >= 2 && entity instanceof Monster monster) {
            monster.setCustomName(net.minecraft.network.chat.Component.literal("§4Elitarny Strażnik"));
            DungeonsVfx.aura(level, monster, VfxTheme.BLOOD, 200);
        }
    });

    // Reakcja na oczyszczenie pokoju:
    DungeonEvents.ROOM_CLEAR.register((level, dungeon, room) -> {
        if (!room.getHeroChestPos().equals(net.minecraft.core.BlockPos.ZERO)) {
            DungeonsVfx.groundRing(level, room.getHeroChestPos().getCenter(), 3.5f, VfxTheme.HOLY);
        }
    });

    // Reakcja na ukończenie całego dungeonu:
    DungeonEvents.DUNGEON_COMPLETE.register((level, dungeon) -> {
        // Własne nagrody, osiągnięcia lub statystyki
    });
}
```

Dostępne eventy w [`DungeonEvents`](src/main/java/com/k8bus/api/DungeonEvents.java):
* `DungeonEvents.DUNGEON_START` – `(level, dungeon)`
* `DungeonEvents.DUNGEON_COMPLETE` – `(level, dungeon)`
* `DungeonEvents.DUNGEON_RESET` – `(level, dungeon, resigned)`
* `DungeonEvents.PLAYER_CHANGE` – `(level, dungeon, player, entered)`
* `DungeonEvents.WAVE_START` – `(level, room, waveIndex, waveDefinition)`
* `DungeonEvents.MOB_SPAWNED` – `(level, room, waveIndex, entity)`
* `DungeonEvents.ROOM_CLEAR` – `(level, dungeon, room)`

### 3. `DungeonsVfx` – biblioteka efektów wizualnych

Statyczne API wysyłające zoptymalizowane pakiety efektów do graczy śledzących dany chunk (`PlayerLookup.tracking`). Każdy efekt przyjmuje motyw kolorystyczny `VfxTheme` (`FIRE`, `ICE`, `POISON`, `VOID`, `LIGHTNING`, `BLOOD`, `HOLY`, `SHADOW`):

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
DungeonsVfx.aura(serverLevel, targetEntity, VfxTheme.ICE, 100);

// Efekt spętania (pnącza/pajęczyna) wokół celu:
DungeonsVfx.ensnare(serverLevel, targetLivingEntity, VfxTheme.POISON, 60);
```

---

## Licencja

**Copyright (c) 2026 k8bus. All Rights Reserved (ARR).**

* **Dozwolone:** Wykorzystywanie oficjalnych wydań tego moda jako zależności (API / Library dependency) we własnych modach oraz dołączanie go do paczek modów (Modpacks) na Modrinth / CurseForge.
* **Zabronione:** Kopiowanie kodu źródłowego lub zasobów (assets) do własnych projektów, modyfikowanie oraz re-uploadowanie plików `.jar` bez zgody autora. Szczegóły w pliku [LICENSE](LICENSE).
