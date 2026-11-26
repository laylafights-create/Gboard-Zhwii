# Custom Text Color integration status

**Not built, not tested, not tried on a device. Do not describe this feature as working.**
The Gboard 18.5.4 APK was not available when this status was last updated, so none of the
smali / view-tree assumptions below have been checked against the real app.

## Present in the source

- A `Custom Text Color` page in the Keyboard Layout & Symbols group, with four independent HEX
  values (key labels, suggestions, toolbar labels, popup labels) in the existing Patches
  `SharedPreferences` store. HEX validation accepts `#RRGGBB` and `#AARRGGBB`. The page has a
  text-only preview and Reset all colors.
- `GboardTextColorRuntime.resolveForView(View, int)`: classifies the view, then returns the stored
  color for that surface or the stock color when nothing is set or storage fails.
- `GboardTextColorBytecodePatch`: hooks `AutoCenterScaleTextView.onDraw(Canvas)`. After the single
  `getCurrentTextColor()` + `move-result vX` it inserts `invoke-static {p0, vX}` + `move-result vX`,
  reusing the same register, so the method needs no extra register. The patch fails at patch time
  (it does not silently skip) if any of these assumptions does not hold:
  exactly one `getCurrentTextColor()` call, consumed by `move-result`, and then passed unchanged to
  `Paint.setColor(int)` (declared on `TextPaint` or `Paint`) with no overwrite in between.
- Only text color is touched. No background, drawable, theme, blur, transparency or key-surface
  property is read or written.

## Checked statically in this review (no Gradle run)

- `gboard-port-product-catalog.json`: `custom_text_color` was out of `feature_id` order (the contract
  test requires sorted order). Moved it, changed its `target_family_ids` entry to `textcolor` (the
  schema pattern forbids underscores), and regenerated `gboard-port-product-catalog.sha256` with the
  same normalization the contract test uses.
- `patches-list.json`: the entry was in the wrong place. The generator sorts by patch name, so
  `Custom Text Color` belongs between `Custom Symbols` and `Custom Theme`. Reordered by hand; the
  entry content matches what the registry would generate (name, description, `use`, empty
  dependencies, 18.5.4 compatibility, no options). `generatePatchesList` itself has not been run.
- The `RuntimeCallId.TEXT_COLOR_RUNTIME_RESOLVE_FOR_VIEW` descriptor matches the Java method
  (`public static int resolveForView(View, int)`), and the patch emits it through
  `RuntimeCallEmitter`.
- The contract/registry expectations in `GboardPortProductCatalogContractTest`,
  `GboardCustomTextColorPatchRegistryTest` and the contribution-wiring checks were read against the
  catalog and registry; the migration-scope map, registration count (47) and marker string agree.
  They were not executed.
- `GboardTextColorTargetClassifierTest` and `GboardTextColorCacheTest` were compiled and run with
  JUnit against hand-written Android stubs (10 tests, all passing). This exercises the classifier,
  cache, settings and runtime sources, but not on Android.

## Changes made to the hook/classifier in this review

- The patch now accepts `Paint.setColor(int)` as well as `TextPaint.setColor(int)`, because R8 can
  rebind the call to the declaring class. This is a robustness change, not a verified fact about
  Gboard.
- `GboardTextColorTargetClassifier` additionally recognizes class names the top-row-swipe feature
  already matches on 18.5.4: `ProactiveSuggestionsHolderView`, `*CandidatesHolderView`
  (suggestions) and `AccessPointsBar`, `AccessPointsBarItemSoftKeyView` (toolbar). Existing id rules
  (`key_pos_proactive_suggestions`, `softkey_holder_fixed_candidates` via `candidate`,
  `key_pos_header_*`) already match names used elsewhere in this repo.

## Not verified (needs the APK, a Gradle build, and a device)

1. `./gradlew test` has not been run: the build needs Morphe GitHub Packages credentials and the
   Gradle 9.6.1 distribution, neither of which was available. Kotlin compilation of the patch,
   the Kotlin tests, `RuntimeAbi` verification against built `.rve` artifacts and
   `generatePatchesList` are all unconfirmed.
2. That `AutoCenterScaleTextView` exists at
   `com.google.android.libraries.inputmethod.widgets.text.AutoCenterScaleTextView` in 18.5.4, has its
   own `onDraw(Canvas)`, and that this method has the assumed smali shape.
3. That `AutoCenterScaleTextView` actually draws key labels, suggestions, toolbar labels and popup
   labels. Any surface drawn by another view class is not covered and would need its own hook.
4. That the classifier's class/id rules (especially the `PopupView` rule for popups) match the real
   view tree. The names listed above come from other code in this repo, not from inspecting the APK.
5. Behavior on a device: visible color change, performance of the draw-path lookup, behavior while
   settings change, and interaction with themes and Frosted Glass.

## Rainbow key text (added)

- New `Rainbow key text` On/Off button on the Custom Text Color page (`pref_text_color_rainbow`).
  When on, KEY-target labels get a per-label palette color (`GboardTextColorRainbow`), so each key
  has its own hue; icons (empty text) keep the stock color. Other targets are unchanged.
- Not compiled or run: no `javac`/Gradle in this environment. `GboardTextColorRainbowTest` is untested.
- Icon keys: `GboardTextColorIconTinter` color-filters every visible ImageView under the root view
  (shift, delete, globe, emoji, enter, toolbar icons) while rainbow is on, posted off the draw
  path and throttled to 500 ms; the filter is cleared when rainbow is turned off. Unverified: that
  Gboard draws these icons as ImageViews (if it draws them as drawables inside SoftKeyView, a
  separate hook is needed). Space bar, comma, period and ?123 are text and use the text hook.
- Giao diện trang Custom Text Color đã Việt hoá (tên mục, mô tả, nhãn, nút, thông báo lỗi). Tên patch trong registry/JSON giữ nguyên tiếng Anh.

## Rainbow icon fix
- Only ImageViews up to 64dp whose drawable renders as a single-color glyph are tinted (photos, clipboard images, screenshot thumbnails are skipped).
- Icons are colored in reading order (up to 12 distinct); ?123 is pink, the space bar label is blue.
- Recycled views lose the tint before the next frame; throttled syncs are delayed, not dropped.
- Compiled against minimal Android stubs; pure rules checked with synthetic pixels. No Gradle build or device run.
- Thin or elongated views (dividers, the red line on the keyboard top edge) are never tinted.
- Icons are rechecked before every frame, so switching to ?123 does not leave the toolbar untinted.
- Keyboard bounce animation (scale from bottom, 600 ms) hooked at Lphr;->onWindowShown; Kotlin not compiled, not tried on a device.
- On/Off button "Hiệu ứng mở bàn phím" (default on). Settings page change not compiled.
