# BiDi repro — RTL paragraph context lost on Latin-initial Arabic text

Minimal reproduction for [issuetracker.google.com/issues/546131806](https://issuetracker.google.com/issues/546131806).

## What it shows

Arabic user-facing content that begins with a Latin strong character (a brand name
such as `DHL`, `Google`, `FedEx`) resolves to LTR paragraph behaviour, even when the
surrounding layout direction is already known to be RTL.

This is **not** a UAX #9 defect. First-strong resolution matches the Unicode
Bidirectional Algorithm (rules P2/P3) and is correct for directionally *unknown*
text. The issue is first-strong as the effective **default** for paragraph-level
content whose direction the framework already knows.

## Running it

```
./gradlew :app:installDebug
```

Requires JDK 17 and an Android SDK with API 36 installed.

**No locale change is needed.** The Activity sets `layoutDirection = RTL` on the
root view, which is exactly what an Arabic system locale does. Setting the system
language to Arabic instead produces the same result.

## How the test is controlled

Each pair on screen shares a **byte-identical tail** and differs only by the leading
Arabic word `شركة`:

| Variant | String |
|---|---|
| Arabic-first | `شركة DHL هي أقدم وأقوى شركة شحن سريع في الشرق الأوسط` |
| Latin-first | `DHL هي أقدم وأقوى شركة شحن سريع في الشرق الأوسط` |

The first strong character is the only variable. Everything else — widget, theme,
layout, layout direction, font, text size — is held constant.

> **The table above is itself an instance of the bug.** GitHub renders `<code>`
> spans with first-strong resolution, so the Arabic-first row places its leading
> word `شركة` at the right edge, where the first word of an Arabic sentence
> belongs — while the Latin-first row pushes `DHL` to the far left, even though
> `DHL` is likewise the first word. Same two strings, same renderer, opposite
> paragraph direction, one leading character apart.

## Sections

Each section renders three rows: Arabic-first (correct), Latin-first (defect), and
Latin-first with the documented mitigation applied.

| # | Surface | What to look at |
|---|---|---|
| 1 | `TextView` | Paragraph alignment; which edge `DHL` lands on |
| 2 | `TextView`, multi-clause | Line wrapping, position of the second Latin run `FedEx`, placement of the comma and of the sentence-final full stop |
| 3 | `EditText` | Alignment and caret disagree — tap at the end of the text and compare which side the caret sits on |
| 4 | Compose `Text` and `BasicTextField` | `TextStyle.textDirection` defaults to `TextDirection.Content`, which consults `LocalLayoutDirection` only when no strong character exists |
| 5 | `WebView` | `dir="auto"` vs `dir="rtl"` on identical markup |

## Defaults under test

| Layer | Default | Effect in an RTL context |
|---|---|---|
| Android Views | `TEXT_DIRECTION_FIRST_STRONG` | LTR paragraph on Latin-initial text |
| Jetpack Compose | `TextDirection.Content` | same |
| WebView / CSS | `dir="auto"` | same |

## Mitigations demonstrated

```xml
<!-- Views -->
android:textDirection="locale"
```
```kotlin
// Compose
style = TextStyle(textDirection = TextDirection.Rtl)
```
```html
<!-- WebView -->
<div dir="rtl">…</div>
```

These are per-call-site workarounds, not a resolution. See the tracker issue for the
platform-level request.
