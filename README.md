# GSAP Perspective Module

A production-ready Ignition Perspective module that brings [GSAP (GreenSock Animation Platform)](https://gsap.com) into industrial dashboards. Designers can build high-performance, hardware-accelerated animations that respond directly to live Ignition tag values — no JavaScript knowledge required.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Dual Version Support (8.1 & 8.3)](#dual-version-support-81--83)
- [Build Instructions](#build-instructions)
  - [Prerequisites](#prerequisites)
  - [Build for Ignition 8.3 (default)](#build-for-ignition-83-default)
  - [Build for Ignition 8.1](#build-for-ignition-81)
  - [Signing the Module](#signing-the-module)
- [Installation](#installation)
- [Component Reference: GSAP Container](#component-reference-gsap-container)
  - [Properties](#properties)
  - [Animation Object Schema](#animation-object-schema)
  - [Timeline Steps](#timeline-steps)
  - [Component Events](#component-events)
  - [Script Functions](#script-functions)
- [Usage Examples](#usage-examples)
  - [Example 1 — Fade in on Alarm Active](#example-1--fade-in-on-alarm-active)
  - [Example 2 — Slide Indicator from Tag Binding](#example-2--slide-indicator-from-tag-binding)
  - [Example 3 — Multi-Step Timeline from Python Script](#example-3--multi-step-timeline-from-python-script)
- [GSAP Easing Reference](#gsap-easing-reference)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

Ignition Perspective renders dashboards in the browser, which means any animation technology available on the web is available to Perspective components. GSAP is the industry standard for high-performance web animation: it runs on a `requestAnimationFrame` loop, uses GPU-composited properties (`transform`, `opacity`) by default, and supports complex sequenced timelines.

This module exposes a single **GSAP Container** component. A designer:

1. Writes any HTML markup into the **Content HTML** property (the "stage").
2. Configures an **Animation** object that targets CSS selectors within that stage.
3. Binds the **Trigger** property to a tag or expression.

Whenever the tag value changes, the animation replays. The same animation can also be fired imperatively from Python scripts via the `play()` and `setTrigger()` script functions.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Ignition Gateway (JVM)                                     │
│  ┌───────────────┐   ┌──────────────────────────────────┐  │
│  │ GatewayHook   │   │ GsapContainerModelDelegate       │  │
│  │ registers     │   │ · play()         → fireEvent → WS│  │
│  │ component +   │   │ · setTrigger()   → writes prop   │  │
│  │ delegate      │   │ · updateAnimation() → prop + play│  │
│  └───────────────┘   │ · handleEvent()  ← WS            │  │
│                      └──────────────────────────────────┘  │
│  DesignerHook registers component on palette               │
└─────────────────────────────────────────────────────────────┘
            WebSocket (Perspective session)
┌─────────────────────────────────────────────────────────────┐
│  Browser (React + GSAP)                                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ GsapContainer (PComponent)                           │  │
│  │                                                      │  │
│  │  props.trigger ──▶ componentDidUpdate                │  │
│  │                         │                           │  │
│  │                         ▼                           │  │
│  │               buildTimeline()                       │  │
│  │               querySelectorAll(target)              │  │
│  │               gsap.timeline({ paused: true })       │  │
│  │                         │                           │  │
│  │                   tl.restart()                      │  │
│  │                         │                           │  │
│  │  onComplete ──▶ delegate.fireEvent                  │  │
│  │                  (animationCompleteHandler)          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**Key design decision — `dangerouslySetInnerHTML`:**
GSAP targets real DOM nodes. The `content` prop is injected as raw HTML so that `querySelectorAll(target)` can find any element in the stage after mount. React-rendered children would require per-element refs and defeat the flexible selector pattern. The `content` property is authored in the trusted Designer environment, not by end-users.

---

## Dual Version Support (8.1 & 8.3)

The build is controlled by three Gradle project properties defined in `gradle.properties`:

| Property | 8.3 default | 8.1 override |
|---|---|---|
| `ignitionSdkVersion` | `8.3.0` | `8.1.43` |
| `ignitionMinVersion` | `8.3.0` | `8.1.0` |
| `javaTargetVersion` | `17` | `11` |

**What changes between versions:**

| Concern | Ignition 8.1 | Ignition 8.3 |
|---|---|---|
| Java toolchain | 11 | 17 |
| Ignition SDK Maven artifacts | `8.1.x` on IA Nexus | `8.3.x` on IA Nexus |
| `requiredIgnitionVersion` in `.modl` | `8.1.0` | `8.3.0` |
| `PerspectiveContext.get()` | Available | Available |
| `ComponentDescriptorImpl.ComponentBuilder` | Available | Available |
| `ComponentModelDelegate` API | Stable | Stable |
| React / TypeScript component | **Identical** — compiled once, runs on both | — |

The Java component registration API is stable across both versions. The only material differences are at the Gradle build level (SDK artifact version + Java toolchain target). No Java source code changes are needed between builds.

---

## Build Instructions

### Prerequisites

| Tool | Version |
|---|---|
| JDK 17 (or JDK 11 for 8.1 builds) | OpenJDK / Temurin recommended |
| Gradle | 8.x (wrapper included, run `./gradlew`) |
| Node.js | 22.x (downloaded automatically by the Gradle Node plugin) |
| Yarn | 1.22.x (downloaded automatically) |

### Build for Ignition 8.3 (default)

```bash
# Clone the repository
git clone <repo-url>
cd gsap-perspective

# Full build: compiles Java, bundles TypeScript via Webpack, packages .modl
./gradlew buildModule
```

The unsigned `.modl` file is written to `build/`.

### Build for Ignition 8.1

Pass the three override properties on the command line. Ensure JDK 11 is available (Gradle will provision it automatically if [Foojay Toolchains](https://github.com/gradle/foojay-toolchains) is configured):

```bash
./gradlew buildModule \
  -PignitionSdkVersion=8.1.43 \
  -PignitionMinVersion=8.1.0  \
  -PjavaTargetVersion=11
```

To permanently set the 8.1 profile, copy `gradle.properties` overrides:

```bash
# One-time setup for an 8.1 dev environment
cat >> gradle.properties << 'EOF'
ignitionSdkVersion=8.1.43
ignitionMinVersion=8.1.0
javaTargetVersion=11
EOF
```

### Build variants in CI (GitHub Actions example)

```yaml
strategy:
  matrix:
    profile:
      - { sdk: "8.3.0",  min: "8.3.0", java: "17", label: "83" }
      - { sdk: "8.1.43", min: "8.1.0", java: "11", label: "81" }
steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-java@v4
    with:
      distribution: temurin
      java-version: ${{ matrix.profile.java }}
  - name: Build (${{ matrix.profile.label }})
    run: |
      ./gradlew buildModule \
        -PignitionSdkVersion=${{ matrix.profile.sdk }} \
        -PignitionMinVersion=${{ matrix.profile.min }} \
        -PjavaTargetVersion=${{ matrix.profile.java }}
```

### Signing the Module

Ignition requires a signed module for production gateways. Use the provided signing script:

```bash
cd modulesigner
./sign-module.sh \
  --keystore  /path/to/keystore.jks \
  --alias     your-key-alias         \
  --module    ../build/gsap-perspective-<version>.unsigned.modl \
  --out       ../build/gsap-perspective-<version>.modl
```

---

## Installation

1. In the Ignition Gateway web UI, navigate to **Config → Modules**.
2. Click **Install or Upgrade a Module**.
3. Upload the signed `.modl` file.
4. Accept the certificate prompt.
5. Reload connected Designer sessions.

The **GSAP Container** component will appear under the **Kyvis Labs** category in the Perspective component palette.

---

## Component Reference: GSAP Container

**Component Type ID:** `kyvislabs.display.gsapcontainer`

### Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `trigger` | boolean / number / string | `false` | Bind to a tag or expression. The animation replays every time this value changes. |
| `animation` | object | see below | Full GSAP animation configuration. |
| `content` | string | `<div class="gsap-target">…</div>` | HTML markup rendered in the animation stage. Give animatable elements the CSS class or ID matching `animation.target`. |
| `autoplay` | boolean | `false` | Play the animation immediately on component mount. |
| `style` | style object | `{}` | Standard Perspective style property (classes, background, border, etc.). |

### Animation Object Schema

| Field | Type | Default | Description |
|---|---|---|---|
| `type` | `"to"` / `"from"` / `"fromTo"` / `"timeline"` | `"to"` | The GSAP tween type. |
| `target` | string | `".gsap-target"` | CSS selector for the element(s) to animate. |
| `duration` | number | `1.0` | Duration in seconds. |
| `ease` | string | `"power2.out"` | GSAP easing string. See [Easing Reference](#gsap-easing-reference). |
| `delay` | number | `0` | Seconds before the animation starts. |
| `repeat` | number | `0` | Extra repeats after the first play. `-1` = infinite. |
| `yoyo` | boolean | `false` | Reverse on alternating repeats (ping-pong). |
| `vars` | object | `{}` | Destination state: `opacity`, `x`, `y`, `scale`, `rotation`, `backgroundColor`, etc. |
| `fromVars` | object | `{}` | Starting state — for `"fromTo"` type only. |
| `steps` | array | `[]` | Timeline steps — for `"timeline"` type only. |

### Timeline Steps

Each entry in `animation.steps`:

| Field | Type | Default | Description |
|---|---|---|---|
| `target` | string | parent `animation.target` | CSS selector for this step. |
| `type` | `"to"` / `"from"` / `"fromTo"` | `"to"` | Tween type for this step. |
| `duration` | number | `0.5` | Step duration in seconds. |
| `ease` | string | `"power2.out"` | Easing for this step. |
| `delay` | number | `0` | Per-step delay. |
| `vars` | object | required | Destination state. |
| `fromVars` | object | `{}` | Starting state for `"fromTo"` steps. |
| `position` | string / number | sequential | GSAP timeline position. `"<"` = with previous, `"<0.2"` = 0.2s after previous starts, `"+=0.1"` = 0.1s after previous ends, numeric = absolute time. |

### Component Events

Configure these in the **Events** tab of the component in the Designer:

| Event | Payload fields | Description |
|---|---|---|
| `animationCompleteHandler` | `timestamp`, `triggerValue` | Fires when the animation (including all repeats) finishes. |
| `animationStartHandler` | — | Fires when the animation begins. |

### Script Functions

Available via the Perspective component scripting API:

```python
# Replay the animation immediately (does not change the trigger value)
self.getSibling("GsapContainer1").play()

# Set a new trigger value — React detects the prop change and replays
self.getSibling("GsapContainer1").setTrigger(newTriggerValue)

# Replace the animation config and immediately play the new animation
self.getSibling("GsapContainer1").updateAnimation({
    "type":     "fromTo",
    "target":   ".my-element",
    "duration": 0.4,
    "ease":     "back.out(1.7)",
    "fromVars": {"scale": 0.5, "opacity": 0},
    "vars":     {"scale": 1.0, "opacity": 1}
})
```

---

## Usage Examples

### Example 1 — Fade in on Alarm Active

**Goal:** Flash a red warning banner every time an alarm tag transitions to active.

**Step 1 — Drop the component**

Drag a **GSAP Container** from the Kyvis Labs palette onto your view. Set its size to approximately 240 × 70 px.

**Step 2 — Content HTML property**

```html
<div class="alarm-indicator"
     style="width:100%; height:100%; background:#c62828;
            border-radius:6px; display:flex; align-items:center;
            justify-content:center; color:#fff; font-size:16px;
            font-family:sans-serif; opacity:0;">
  ⚠ ALARM ACTIVE
</div>
```

The element starts at `opacity: 0` so it is invisible until the animation plays.

**Step 3 — Animation property** (set in the Designer property panel)

```json
{
  "type":     "fromTo",
  "target":   ".alarm-indicator",
  "duration": 0.45,
  "ease":     "power2.out",
  "repeat":   2,
  "yoyo":     true,
  "fromVars": { "opacity": 0, "scale": 0.92 },
  "vars":     { "opacity": 1, "scale": 1.0 }
}
```

`repeat: 2` with `yoyo: true` causes the banner to pulse in → out → in before settling.

**Step 4 — Trigger binding**

In the property panel, click the binding icon on **Trigger** and set:

```
Tag path: [default]Devices/Pump1/AlarmActive
```

Every time `AlarmActive` changes state, the animation fires.

**Step 5 — (Optional) animationCompleteHandler script**

```python
# Runs in the Perspective component event script after the animation finishes
if event.triggerValue == True:
    system.perspective.print("Alarm animation complete for trigger value: True")
```

---

### Example 2 — Slide Indicator from Tag Binding

**Goal:** A KPI badge slides in from the left whenever a production count OPC tag updates.

**Content HTML:**

```html
<div class="kpi-badge"
     style="display:inline-flex; align-items:center; gap:10px;
            background:#1565c0; color:#fff; padding:10px 20px;
            border-radius:24px; font-family:sans-serif; opacity:0;">
  <span class="kpi-value"
        style="font-size:24px; font-weight:700;">
    {view.params.currentValue}
  </span>
  <span style="font-size:14px;">units / hr</span>
</div>
```

> **Note:** Perspective expression bindings in `content` HTML are not evaluated — `content` is treated as a raw HTML string. To display live tag values inside the animation stage, pass them into the HTML string using a **Script Transform** on the `content` binding:
> ```python
> return '<div class="kpi-badge" ...><span class="kpi-value">' + str(value) + '</span></div>'
> ```

**Animation:**

```json
{
  "type":     "fromTo",
  "target":   ".kpi-badge",
  "duration": 0.45,
  "ease":     "power3.out",
  "fromVars": { "opacity": 0, "x": -50 },
  "vars":     { "opacity": 1, "x": 0 }
}
```

**Trigger binding:**

Bind **Trigger** to `[default]Production/Line1/UnitsPerHour`. Every scan cycle that delivers a new value triggers a fresh slide-in, giving operators an immediate visual cue that the data is live.

---

### Example 3 — Multi-Step Timeline from Python Script

**Goal:** On a button click, stagger three status lights in sequence.

**Content HTML:**

```html
<div style="display:flex; gap:14px; padding:16px;">
  <div class="light light-a"
       style="width:42px; height:42px; background:#e53935;
              border-radius:50%; opacity:0;"></div>
  <div class="light light-b"
       style="width:42px; height:42px; background:#fb8c00;
              border-radius:50%; opacity:0;"></div>
  <div class="light light-c"
       style="width:42px; height:42px; background:#43a047;
              border-radius:50%; opacity:0;"></div>
</div>
```

**Animation property:**

```json
{
  "type": "timeline",
  "steps": [
    {
      "target":   ".light-a",
      "type":     "from",
      "duration": 0.4,
      "ease":     "back.out(1.7)",
      "vars":     { "opacity": 0, "scale": 0.3 }
    },
    {
      "target":   ".light-b",
      "type":     "from",
      "duration": 0.4,
      "ease":     "back.out(1.7)",
      "position": "<0.15",
      "vars":     { "opacity": 0, "scale": 0.3 }
    },
    {
      "target":   ".light-c",
      "type":     "from",
      "duration": 0.4,
      "ease":     "back.out(1.7)",
      "position": "<0.15",
      "vars":     { "opacity": 0, "scale": 0.3 }
    }
  ]
}
```

`"position": "<0.15"` means each light starts 0.15 s after the previous one begins, creating a staggered pop.

**Button `onActionPerformed` script:**

```python
self.getSibling("GsapContainer1").play()
```

---

## GSAP Easing Reference

| Category | Example strings |
|---|---|
| Linear | `"none"`, `"linear"` |
| Power (default) | `"power1.out"`, `"power2.inOut"`, `"power3.in"`, `"power4.out"` |
| Bounce | `"bounce.out"`, `"bounce.in"`, `"bounce.inOut"` |
| Elastic | `"elastic.out(1, 0.3)"`, `"elastic.in(1, 0.5)"` |
| Back (overshoot) | `"back.out(1.7)"`, `"back.in(2)"`, `"back.inOut(1.7)"` |
| Circ | `"circ.out"`, `"circ.in"`, `"circ.inOut"` |
| Expo | `"expo.out"`, `"expo.in"`, `"expo.inOut"` |
| Sine | `"sine.out"`, `"sine.in"`, `"sine.inOut"` |

Numbers in parentheses for `elastic` and `back` are configurable amplitude/period parameters.

---

## Troubleshooting

**Component does not appear in the palette**
- Verify the module installed without errors: Gateway → Config → Modules.
- Reload the Designer: File → Reload Designer Resources.
- Search the gateway log for `GSAP Perspective` messages.

**Animation does not play when a tag changes**
- Confirm the **Trigger** property has an active binding (not a static value).
- Open the component inspector and check that `trigger` actually changes in the browser when the tag updates.
- Ensure the `animation.target` CSS selector matches at least one element in the `content` HTML. Check the browser developer console for `matched no elements` warnings from the `kyvislabs.display.gsapcontainer` logger.

**`dangerouslySetInnerHTML` note in browser console**
This is expected and intentional. The GSAP component must own a real DOM subtree for selector-based targeting. See [Architecture](#architecture).

**Module fails to load on Ignition 8.1**
Ensure the `.modl` file was built with the 8.1 profile flags. The 8.3 build sets `requiredIgnitionVersion=8.3.0` in the module manifest, which causes an 8.1 gateway to refuse it.

**`ClassNotFoundException: com.kyvislabs.gsap.*`**
The GSAP module hooks are wired to the `com.kyvislabs.gsap.*` package. If the old ApexCharts source files cause confusion, delete the `common/src/main/java/com/kyvislabs/apexcharts/` directory tree (they are the fork baseline and are not loaded by the GSAP module hooks).

---

## Contributing

1. Fork the repository and create a feature branch off `master`.
2. Follow existing code style (Java: Google Java Format; TypeScript: 4-space indent).
3. Ensure `./gradlew buildModule` succeeds for both the 8.3 default and the 8.1 profile.
4. Open a pull request with a clear description of the change.

---

## License

See [license.html](license.html) for the full license text.

GSAP core (`gsap` npm package) is free and MIT licensed. GSAP premium plugins (MorphSVG, DrawSVG, MotionPath, SplitText, etc.) require a separate [GreenSock Club](https://gsap.com/pricing/) license for commercial use. This module ships only GSAP core.
