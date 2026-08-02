# Nintendo Game Boy cartridge catalogue data

`nintendo-game-boy-cartridge-catalog.tsv` is a transformed snapshot of the
[Game Boy hardware database](https://gbhwdb.gekkio.fi/) by Gekkio and its
[contributors](https://github.com/Gekkio/gb-hardware-db#contributors).

The source export was retrieved from
`https://gbhwdb.gekkio.fi/static/export/cartridges.csv` on 2026-08-02. Its
SHA-256 digest was
`007ea8d8ac02448764b75eda7057314c952f19b7d0654d473cb3877c8e54e18f`.
The snapshot selects the printed cartridge label code, ROM ID, and game title.
It also normalizes the title's No-Intro release-region flag to `RegionCode`
values and preserves explicitly listed ISO 639-1 game languages. A `World`
release becomes the three major markets `JP`, `US`, and `EUR`. On compilation
cartridges, `+` separates the language sets of the individual games and is
retained structurally. An empty language column means that the source title did
not state languages; no language is inferred from the release region.

Duplicate physical observations are removed and the remaining associations are
sorted. No spelling corrections were made, because unusual printed codes are
evidence. The region and language interpretation follows the
[No-Intro naming convention](https://wiki.no-intro.org/index.php?title=Naming_Convention)
and its documented
[multi-game language separator](https://wiki.no-intro.org/index.php?title=General_dat_notes).

The source data and this transformed TSV are licensed under the
[Creative Commons Attribution-ShareAlike 4.0 International License](https://creativecommons.org/licenses/by-sa/4.0/).
The RetroCrawler Java source code remains covered by the repository's MIT
license; the CC BY-SA notice applies specifically to the catalogue data.

This is a dated collection of observed physical cartridges, not a claim that
every manufactured label code is known. Consumers must treat a missing code as
unknown rather than invalid.
