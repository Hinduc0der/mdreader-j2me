# MD Reader — a Markdown reader for J2ME (MIDP 2.0 / CLDC 1.1)

**English** · [Русский](PLAN.ru.md)

Goal: read `.md` files on feature phones (Nokia 5300 — 240x320, SE K750 — 176x220,
the Chinese "NOKLA" clones and other MIDP 2.0 devices). Look and controls in the spirit
of the old ReadManiac.

## The constraints everything follows from

- CLDC 1.1: no generics, no `StringBuilder` (only `StringBuffer`), no collections beyond
  `Vector`/`Hashtable`, no `String.split`, no `assert`, no `enum`. We compile with
  `javac -source 1.3 -target 1.3`, so the compiler itself rules out anything newer.
- Heap is about 1 MB on a K750 and about 2 MB on a 5300. The file is never loaded whole.
- Old devices cap the JAR size (hundreds of KB on a K750). ProGuard is mandatory.
- On some S40/SE firmware UTF-8 is unsupported or simply wrong, hence a hand-written decoder.
- Files are read over JSR-75 (FileConnection). The phone will ask for permission; that is
  normal and is settled by signing the MIDlet or by the application's permission settings.

## Architecture

> The table below is the original plan, written before any code existed. Some classes
> ended up named differently or merged into their neighbours. For how it is actually
> built, see the "How the code is laid out" section in [README.md](README.md).

| Class | Role |
|---|---|
| `MDReaderMIDlet` | startup, restoring the last book, lifecycle |
| `FileBrowser` | memory-card browsing (JSR-75), `.md/.markdown/.txt` filter, favourites |
| `BookIndex` | single-pass file scan: paragraph offsets in an `int[]`, cached in RMS |
| `TextSource` | reading a chunk of the file at an offset + the UTF-8/CP1251 decoder |
| `MdParser` | a line → a markup block (h1-h6, list, quote, code, rule, text) |
| `Layout` | splitting a block into lines by screen width, styles via `Font` |
| `ReaderCanvas` | full-screen rendering, key handling, auto-scroll |
| `Settings` | RMS: font, inversion, encoding, auto-scroll speed |
| `Library` | RMS: reading position and bookmarks per file |

## Markup (a Markdown subset)

Supported: `#`..`######`, `**bold**`, `*italic*`, `` `code` ``, fenced code blocks,
`> quotes`, `-`/`*`/`1.` lists, `---` rules, `[text](link)` → the text is shown.
Not supported: tables (shown as-is), images (caption only), HTML (as plain text).

## Controls (ReadManiac-style)

- `2`/`8`, up/down — line / page
- `4`/`6` — chapter (next heading)
- `5`/Fire — menu
- `0` — auto-scroll on/off, `1`/`3` — speed
- `*` — invert colours, `#` — font size
- left soft key — menu, right — back / exit

## Build

`build.bat` → javac (bootclasspath = CLDC+MIDP) → ProGuard `-microedition` (shrinking +
preverification) → jar + jad. Output in `dist/MDReader.jar`.

## Status as of 2026-09-20

Version 0.1.0 builds and was verified in the WTK emulator at 240x320 and 176x220:
card browsing, reading, markup, table of contents, jump to a percentage, themes, font
size, auto-scroll, position saving. `dist/MDReader.jar` is 18.8 KB after ProGuard
(27 KB without it). Installing from the `.jad` was checked separately.

## Testing on a Nokia E90 (2026-09-21)

First run on real hardware. Working: install from the `.jad`, card access, reading,
markup, Cyrillic in UTF-8. Two things turned up that the emulator does not show:

1. **Duplicate drives in the browser.** Symbian's `listRoots()` returns TWO entries per
   volume — a localised name and a drive letter: «Память тлф/», «C:/», «Карта памяти/»,
   «E:/». Fixed in `Roots.merge`: the name+letter pair is merged into one entry, taking
   the human-readable name as the label and the letter as the path. There is an escape
   hatch — the "All drives" command in the browser menu.
2. **An empty box in the status bar** — the auto-scroll marker was the character `▶`
   (U+25B6), and no such glyph exists in S60/S40 fonts. It is now drawn with
   `fillTriangle`, independent of the font. For the same reason `Text` no longer
   substitutes `�` for undecodable bytes and uses `?` instead.

Testing 0.1.2 on the E90 (2026-09-22): one browser entry per volume, the auto-scroll
triangle draws, and the `•` list bullet (U+2022) **does** exist in the device font — it
does not need changing.

The lesson for the future: **keep everything drawn as text within ASCII + Cyrillic**, and
draw symbols with graphics. `•` is a verified exception. The WTK emulator uses desktop
Windows fonts and cannot catch this class of bug at all.

## Still to do

1. Testing on more real hardware (5300, NOKLA) — the emulator lies about memory and fonts.
   The E90 is fully covered, see above.
2. Bookmarks (only the last position per file is remembered so far).
3. Full-text search.
4. Markdown tables — currently rendered as plain text.
5. Code blocks after seeking: the ``` state is only tracked within a page, so jumping into
   the middle of a long code block shows it as ordinary text.
6. Possibly the Nokia UI API: keep the backlight on while reading (Nokia only, via
   reflection, so the build does not break on other devices).
