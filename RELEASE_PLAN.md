# Release Plan

This plan tracks the work needed to move CricketWatch from a working Wear OS app
to a release candidate. Work through these one item at a time and mark each item
done only after implementation and verification.

## Release blockers

- [x] Stable debug signing documented and wired through optional local properties.
- [x] Signed release artifact, preferably an Android App Bundle, with documented signing and versioning.
- [x] Release minification and resource shrinking enabled, with APK/AAB size compared and a smoke-tested release build.
- [x] Foreground refresh cadence checked so the watch is not doing unnecessary network work.
- [x] Focused tests for release-critical filtering, display and refresh policy behavior.
- [x] Repeatable small round Wear OS profile release gate documented.
- [ ] Small round Wear OS screenshot or physical-device evidence captured for the release candidate.

## Functionality hardening

- [ ] Confirm missing API key, API-limit and network-failure states are clear on the watch.
- [ ] Confirm cached or stale score behaviour is intentionally absent or implemented before release.
- [ ] Confirm England and Surrey filtering excludes youth and irrelevant matches without dropping senior fixtures.

## Test coverage

- [ ] Repository tests for current-match, score-feed, enrichment, API-limit and fallback paths.
- [ ] Display-model tests for score rows, declarations, two-innings matches and title cleanup.
- [x] Refresh policy tests for automatic cadence and manual refresh availability.
- [ ] Instrumented or screenshot tests on small and large Wear OS profiles, including larger system font.

## Polish and listing

- [ ] Wear-native match list checked on Pixel Watch 2 and a small round profile.
- [ ] Play listing copy and screenshots that explain England and Surrey focus.
- [ ] Release-quality app icon and launcher appearance checked at watch sizes.
