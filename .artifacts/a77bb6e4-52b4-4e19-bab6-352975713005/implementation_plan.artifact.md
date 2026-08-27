# Fix Syntax Highlighting Errors in Lua and JS Shell Screens

The current implementation of syntax highlighting in `LuaShellScreen.kt` and `QjsShellScreen.kt` uses incorrect or internal APIs from the `dev.hossain:compose-highlight` library (version 0.33.0). This plan updates both screens to use the correct public APIs for obtaining the highlighting engine and performing highlighting with the active theme.

## Proposed Changes

### [UI Components]

#### [MODIFY] [LuaShellScreen.kt](file:///D:/MiBand/vela2/interconnect_2/actual/MiBand-DevBox-Android/app/src/main/java/com/sucharek/miband_interconnect_test/ui/screens/activities/luashell/LuaShellScreen.kt)
- Update imports to use `dev.hossain.highlight.ui.rememberHighlightEngine` and `dev.hossain.highlight.ui.LocalHighlightTheme`.
- Replace `HighlightEngine.current` with `rememberHighlightEngine()`.
- Update `highlightEngine.highlight` to include the required `HighlightTheme` parameter (obtained via `LocalHighlightTheme.current`).

#### [MODIFY] [QjsShellScreen.kt](file:///D:/MiBand/vela2/interconnect_2/actual/MiBand-DevBox-Android/app/src/main/java/com/sucharek/miband_interconnect_test/ui/screens/activities/qjsshell/QjsShellScreen.kt)
- Fix incorrect imports: change `dev.hossain.highlight.LocalHighlightEngine` and `dev.hossain.highlight.SyntaxHighlightedCode` to their correct locations in `dev.hossain.highlight.ui`.
- Replace the broken `LocalHighlightEngine.current` with `rememberHighlightEngine()`.
- Update `highlightEngine.highlight` to include the required `HighlightTheme` parameter.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify that the compilation errors are resolved.

### Manual Verification
- Deploy the app and navigate to the **Lua Shell** and **VelaJS Shell** screens.
- Type code in the input fields and verify that syntax highlighting is applied after a short delay.
- Verify that the console history also displays syntax-highlighted code.
