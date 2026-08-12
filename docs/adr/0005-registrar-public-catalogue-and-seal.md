# Registrar is a public sealed catalogue on a face-connected shelf cluster

Assented Acts and Hansard volumes already land as written books on chiseled bookshelves, but players could empty the archive by hand and had no plugin way to browse it. We treat the **registrar** as the monarch’s anchor shelf plus every face-adjacent chiseled bookshelf reachable from it (no diagonals, hard cap 64). Anyone may open a catalogue GUI on that cluster; take, put, and break are sealed. Operators may break shelves, and may sneak-right-click to use vanilla take/put; hoppers cannot move books in or out. The catalogue reads shelf inventories — Acts then Hansard, author `Parliament` only — because Hansard records are cleared at prorogation and the bound volumes live on the wood. Shelving fills empty slots on that same live cluster before inventing a new face-adjacent shelf.

## Considered options

- **Members-only browse** — rejected; the archive is a public record.
- **Domain-only catalogue** — rejected; would need new persisted Hansard volumes after prorogation clears session records.
- **Territory-based ownership** — rejected; the registrar is defined by the set anchor and its physical cluster, not WorldGuard paint.
