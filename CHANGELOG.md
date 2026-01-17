### Added:
- Add lootr compatibility (config file entry, default to true)
- Add dungeon completion messages (congrats and leave link, translatable entries)
- Add Full dungeon regeneration (IT CLEARS EVERYTHING, config file entry, default to false)
- Add an area clear before first dungeon generation and wider dungeon clear when regenerating
- Add a check nearby dungeonPortal on OP screen to avoid closeby dungeon structure to be cleared/overwritten
### Fixed:
- Temple nbt files: leaves now have persistent state to avoid them decaying
- FireBlockMixin: prevent fire to be placed, to avoid it burning blocks like vines
- Dungeon Spawner: use Identifier instead of ID to avoid corruption causing spawners showing wrong entities and being useless most of the time
- Dungeon Spawner: fix spawn counting so they properly breaks when reaching maxSpawnCount
- Dungeon Gate: multiple fixes (nbt, detection, refresh, sync)
### Changed:
- Dark dungeon connector room use gates now as example
- Updated en-us and fr_fr lang files
- Updated readme about gate and spawner blocks