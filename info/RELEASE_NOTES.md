# DA INFO — Release notes

## v0.8 (interim development build)
- Version code: 8; version name: 0.8.
- Refined the home screen with clearer hierarchy, more breathing room, and a live entries/topics summary.
- Upgraded the app mark with a polished book-and-knowledge visual and a cohesive teal/violet palette.
- Improved category tiles, typography, and touch-friendly visual separation.
- Added 17 new distinct knowledge entries across science, technology, security, learning, economics, engineering, and environment.
- Preserved offline access, local search, search suggestions, highlighted matches, bookmarks, history, and light/dark settings.
- Build validation continues to reject missing fields, short entries, and duplicate titles/bodies.

## Content quality rule
Missing information is preferable to fabricated facts. The new entries are concise educational summaries; entry counts are not a substitute for future fact-checking and editorial review.


## v0.9 (content expansion)
- Version code: 9; version name: 0.9.
- Expanded the offline Hebrew knowledge base with 246 additional distinct educational entries since the prior 1,351-entry checkpoint across science, computing, psychology and learning, history, geography, finance, transport, environment, mathematics, design, business, and practical technology.
- Entries are added only when titles are unique and bodies meet a minimum length check; no filler records are generated to inflate the count.
- The database remains bundled locally for offline use.
- This is an incremental step toward the 2,000-entry target; the verified total is recorded in the repository after this batch.


- UI improvements in this release candidate: subcategory tiles that open their full entry lists, compact circular settings control at the top, increased metric-card height to prevent clipping, and a refreshed app mark.
- Automated APK build is configured to produce the v0.9 artifact; the artifact is not considered ready until the GitHub Actions run succeeds.

- Current verified database count: 1,597 entries across 45 categories; 403 entries remain toward the requested 2,000-entry target.

## v1.0 (2,000-entry milestone)
- Version code: 10; version name: 1.0.
- Expanded the offline knowledge database to exactly 2,000 entries across 45 categories.
- Added 399 entries during this content expansion; titles and article bodies were checked for duplicates.
- Kept category and subcategory metadata required for in-app grouping.
- The Android build workflow now requires at least 2,000 entries and labels the output APK as DA_INFO_v1.0.apk.
- This release updates the knowledge database and version metadata; the APK must still pass the automated build before it is considered ready to install.


## v1.2 (layout and release history)
- Version code: 12; version name: 1.2.
- Changed knowledge-domain tiles to a three-column grid, with three tiles per row in subcategory selection as well.
- Added a version change-log button to the About section, with a concise history of releases 1.0–1.2.
- Preserved the offline database and existing search, suggestions, bookmarks, history, dark mode, and interface-language setting.
- Automated build now labels the APK and artifact as v1.2; build validation is required before release.
