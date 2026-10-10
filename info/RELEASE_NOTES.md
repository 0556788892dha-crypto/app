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


## v1.1 (navigation, search and content expansion)
- Restored the complete v1.1 implementation instead of carrying forward only the v1.2 visual changes.
- Added 331 distinct offline entries in `data_v1_1.json`, bringing the combined library to 2,331 entries across the existing knowledge domains.
- Improved Hebrew search normalization and suggestions, including matching article bodies; matching terms are highlighted in results.
- Restored fixed bottom navigation, umbrella grouping on the home screen, and Back navigation to the originating subcategory.
- The build validates base and added content together, rejecting duplicate titles/bodies, missing fields and filler titles.

## v1.2 (v1.1 + visual refresh)
- Version code: 12; version name: 1.2.
- Preserved all v1.1 features and all 331 added entries.
- Replaced the app icon with a globe-and-open-book knowledge symbol.
- Centered category and subcategory text/counts and made tiles more compact while retaining their original icon sizes.
- Preserved three-column grids, umbrella topic grouping, bottom navigation, search suggestions/highlighting, favorites, history, reading settings and the dedicated changelog screen.
- Automated build labels the output `DA_INFO_v1.2.apk`; release requires a successful database validation and build.
