# MD Reader — a Markdown reader for feature phones

**English** · [Русский](README.ru.md)

> Idea, requirements and all testing on real hardware — [@Hinduc0der](https://github.com/Hinduc0der).
> Code written by Claude (Anthropic) under his direction.

A MIDlet for MIDP 2.0 / CLDC 1.1 that reads `.md` (and `.txt`) straight off the memory
card and renders the markup itself. Targets the Nokia 5300 (240x320), Sony Ericsson K750
(176x220) and anything else with Java and JSR-75. Layout is derived from the screen size,
so there are no per-resolution builds.

Ready-to-install build: `dist/MDReader.jar` + `dist/MDReader.jad`.

## Installing on a phone

1. Copy **both** files, the `.jar` and the `.jad`, into the same folder on the memory card.
2. On the phone, open the `.jad` (on S40 — via *Gallery* / *Applications*) and accept the install.
3. The first time you open a file the phone will ask for permission to access data —
   that is JSR-75. To stop it asking every time, set the application permission
   *Data access: ask once* (on S40 — *Applications → Options → Data access*).

The MIDlet is unsigned, so the phone treats it as untrusted. That is expected; it is
simply being careful about reading the card.

## Controls

| Key | Action |
|---|---|
| `2` / `8`, up / down | one line up / down |
| `7` / `9` | page up / down |
| `4` / `6` | previous / next heading |
| `5`, joystick centre | menu |
| `0` | auto-scroll on / off |
| `1` / `3` | slower / faster |
| `*` | theme: day → night → sepia |
| `#` | font size |
| right soft key | close the book, back to the browser |

On touch devices: tap the top of the screen for the previous page, the bottom for the
next one, the middle for the menu.

The menu has: table of contents (the file's headings), jump to a percentage, open another
file, settings (font, theme, encoding, status bar, paragraph joining) and auto-scroll.

Reading position and settings are stored in RMS; the last 20 books are remembered by path.
On startup the last book reopens exactly where you stopped.

## Markdown it understands

Headings `#`…`######`, `**bold**`, `*italic*`, `` `code` ``, fenced ``` blocks and
4-space-indented blocks, `>` blockquotes, `-`/`*`/`+` and numbered lists, `---` rules,
links `[text](url)` and images — the caption is shown. Tables and HTML are rendered as
plain text.

Encoding is detected automatically (UTF-8 versus Windows-1251) and can be forced in the
settings. The decoder is hand-written: the system one on S40 cannot be trusted.

## Building

You need JDK 8, WTK 2.5.2 and, for a release build, ProGuard. Paths are set at the top
of `build.bat`.

```
build.bat            plain build, classes as they are
build.bat release    + ProGuard: shrinking and obfuscation (roughly half the JAR size)
```

The output is `dist/MDReader.jar` and `.jad` — always the latest build, overwritten every
time. On top of that, every **release** build is archived:

```
dist/versions/MDReader-0.1.0.jar + .jad
dist/versions/MDReader-0.1.1.jar + .jad
dist/versions/MDReader-0.1.2.jar + .jad
```

In an archived pair the `.jad` points at its own versioned `.jar`, so any old version can
be installed by copying that pair into one folder. Debug builds are not archived — the
archive only holds what actually went onto a phone.

Bump the version in `build.bat` (`set VERSION=`) before any build you put on a phone:
otherwise the phone may not notice the update, and the previous archive entry gets
overwritten (the build warns about this). What changed in each version is in
[CHANGELOG.md](CHANGELOG.md).

### Running in the emulator

```
"C:\WTK2.5.2_01\bin\emulator.exe" -Xdevice:DefaultColorPhone -Xdomain:maximum ^
  -classpath build\pre mdreader.MDReaderMIDlet
```

`-Xdevice:DefaultGrayPhone` gives a 176x220 screen, like the K750. The emulator reads
files from `%USERPROFILE%\j2mewtk\2.5.2\appdb\temp.<device>N\filesystem\root1\` (launched
via `-classpath` that directory is temporary and recreated each run). For a more honest
picture, KEmulator nnmod works too.

Be warned: the emulator uses desktop Windows fonts and lies about memory. Two bugs in
0.1.1 were invisible there and only showed up on a real Nokia.

## How the code is laid out

| File | Responsibility |
|---|---|
| `MDReaderMIDlet` | startup, menu, settings, table of contents |
| `FileBrowser` | browsing the memory card over JSR-75 |
| `Roots` | merging the duplicate filesystem roots Symbian reports |
| `Book` | the file: an 8 KB reading window, logical lines by byte offset |
| `Text` | hand-written UTF-8 and Windows-1251 decoders, auto-detection |
| `MdParser` / `MdBlock` | a Markdown line → a block with per-character styles |
| `ReaderCanvas` | wrapping to screen width, drawing, keys, auto-scroll |
| `Settings` | RMS: settings and reading positions |

The file is never read into memory as a whole: a page is identified by a pair of
«line offset + visual line number inside it», and `Book` keeps an 8 KB window. On a K750
with its one megabyte of heap that is not optional.

`test/RootsTest.java` runs on desktop Java and covers the root-merging logic:

```
javac -d build\test src\mdreader\Roots.java test\RootsTest.java
java -cp build\test RootsTest
```

## Licence

MIT, see [LICENSE](LICENSE).

## How this was made

The whole thing was built through conversation: a human sets the task, defines the
constraints (target devices, memory limits, the old ReadManiac as the reference point)
and tests the builds on real phones; Claude writes the code, drives the build and works
out what the findings mean. Some things would never have surfaced otherwise — Symbian
listed every drive twice in the file browser, and the auto-scroll marker rendered as an
empty box. The emulator reproduces neither; both are only visible on an actual Nokia.
The log of those findings is in [CHANGELOG.md](CHANGELOG.md).
