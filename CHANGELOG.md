# Changelog

**English** · [Русский](CHANGELOG.ru.md)

Built `.jar` + `.jad` pairs for every version live in `dist/versions/`. To install one,
copy **both** files of that version into a single folder on the phone and open the `.jad`.

The version is set in `build.bat` (`set VERSION=`) before any build that goes onto a
device: the phone compares `MIDlet-Version`, and without a bump it may not notice the
update. Numbering is simple — the third digit is fixes and small things, the second is
noticeable new capability.

---

## 0.2.0 — 2026-10-12

**The interface is now available in English.**

- Every user-visible string moved into a `Strings` class, one file per language in
  `i18n/ru/` and `i18n/en/`. The language is chosen at build time, so each JAR carries
  only its own language and nothing is wasted on the device.
- `build.bat` now builds both languages: `build.bat release` produces a Russian and an
  English release and archives both. A single language can be built with
  `build.bat release en`.
- Build artefacts are renamed accordingly: `MDReader-ru.jar` / `MDReader-en.jar`, and in
  the archive `MDReader-<version>-<lang>.jar` with a matching `.jad`.
- Documentation (README, CHANGELOG, PLAN) is now bilingual, English first.

Both builds share the same MIDlet identity, so installing one replaces the other and the
saved reading positions carry over. Nothing else about the reader changed in this version.

## 0.1.2 — 2026-09-22

- The About screen now links to the source: `github.com/Hinduc0der/mdreader-j2me`.
- Project published on GitHub under MIT.

Verified on a Nokia E90: the browser lists one entry per volume, the auto-scroll marker
draws as a triangle, and the `•` list bullet renders correctly. Both findings from 0.1.1
are confirmed fixed on real hardware.

## 0.1.1 — 2026-09-21

First round of fixes from testing on a real Nokia E90.

**Fixed**

- **The browser listed every drive twice.** Symbian's `listRoots()` returns two entries
  per volume — a localised name and a drive letter («Память тлф/» and «C:/», «Карта
  памяти/» and «E:/»). The pair is now merged into a single entry: the human-readable
  name as the label, the drive letter as the path. Logic lives in `Roots.merge` and is
  covered by `test/RootsTest.java`.
- **An empty box in the status bar.** The auto-scroll marker was the character `▶`
  (U+25B6), which does not exist in S60 and S40 fonts. It is now drawn with
  `fillTriangle` instead.
- Undecodable bytes are no longer replaced with `�` (U+FFFD) for the same reason; `?` is
  used instead.

**Added**

- An "All drives" command in the browser — show the roots unmerged, in case the merging
  gets it wrong on some device. The choice is stored in RMS.
- The About screen now shows authorship, the version from the manifest, and the source link.

**Internals**

- The RMS settings record format moved to version 2. New fields are appended at the end
  and version 1 records still read correctly, so settings and reading positions survive
  the upgrade from 0.1.0.

## 0.1.0 — 2026-09-20

First working build. Memory-card browsing over JSR-75, reading without loading the file
into memory (an 8 KB window), Markdown rendering, table of contents from headings, jump
to a percentage, three themes, three font sizes, auto-scroll, hand-written UTF-8 and
Windows-1251 decoders, and per-file reading positions. Tested in the WTK emulator at
240x320 and 176x220.
