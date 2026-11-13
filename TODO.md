# Unnamed Dungeons Engine & API - To Do List

> Content (enchanty, efekty, Wpłatomat/ekonomia oraz Jungle Outpost) został przeniesiony na osobny branch `content`, aby `main` pozostał czystym silnikiem i API dungeonów.

## Silnik i API (zrealizowane)
- [x] Tag bloków bram `#unnamed-dungeons:dungeon_gates` oraz obsługa customowych bram i wariantów `facing` / `open` w resource packach.
- [x] Konfigurowalne rozmiary barier bram (`width x height`) przez `DungeonGateBlock`, `DungeonGateBlockEntity`, komendę `/dungeon setgatesize`, NBT, JSON i `DungeonApi`.
- [x] Tryb powtarzalny (`repeatable`) oraz sterowane per-dungeon czasy (`timings` w JSON i `DungeonApi.configureTimings`).
- [x] Rozszerzone `DungeonEvents` (`CAN_ENTER_DUNGEON`, `DUNGEON_PHASE_CHANGE`, `WAVE_COMPLETE`, `CHEST_OPENED`).
- [x] Ochrona terenu (niszczenie, stawianie bloków, rozlewanie płynów z wiader, eksplozje) oraz przywracanie skrzyń przy `reset`, `resign` i ukończeniu `repeatable`.
- [x] Poprawka startu pokojów/fal (uruchamiane wyłącznie w fazie `ACTIVE` przez `DungeonRoomHandler`).

## Przed wydaniem / dalsze kroki
- [ ] Test na żywo w grze (pełne przejście `WAITING` -> `PREPARING` -> `ACTIVE` -> `COMPLETED` / `repeatable`, `/dungeon reset`, `/dungeon resign`, ochrona bloków/wiader).
- [ ] Podpięcie docelowych modeli 3D / tekstur dla `dungeon_gate` (`dungeon_gate.json` / `dungeon_gate_open.json`) oraz ewentualne zastąpienie tymczasowych grafik w HUD.
- [ ] Opcjonalne wydzielenie `DungeonsVfx` do osobnego moda / biblioteki.
