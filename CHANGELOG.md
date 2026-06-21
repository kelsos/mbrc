Changelog
-----------

## [1.6.1]
### Added
- Shows a progress bar at the top of the now playing queue while it is syncing, so a long sync (opening the screen, pull-to-refresh, or a server-side "play all") gives visible feedback instead of appearing stalled. The bar reflects the actual page-load progress reported by the server.

### Changed
- Reworks the now playing track info into three rows, with the title and album spanning the full width. Long titles and album names now scroll in from the right edge of the screen instead of from the middle, with only the artist line sharing its row with the favorite/ban/lyrics buttons.

### Fixed
- Fixes out-of-memory crashes on very large now playing queues. Syncing the queue used to load the entire now playing list into memory at once; it now reconciles one page at a time, so memory stays flat regardless of queue size. As a safeguard, incoming network messages are also capped to a size relative to the available heap.
- Fixes the now playing queue sometimes loading only part of the list. Refreshes triggered close together (opening the screen, pull-to-refresh, or a server-side queue change) could overlap and overwrite each other's writes; a newer refresh now cancels and restarts the in-flight one, so the full queue is always loaded. A failed or superseded refresh no longer clears the existing queue.
- Fixes a spurious error being logged on every "play all" action. The play-all confirmation broadcast from the plugin is now handled instead of being treated as an unsupported message.
- Fixes a crash when scrolling the now playing queue during a refresh. Reconciling the queue could briefly leave two entries at the same position, which the list could then show twice and crash on; each position now updates in place, so an entry is never duplicated.

## [1.6.0] - 2026-06-01
### Added
- Restores the sync action on the library top app bar, so a sync can be triggered without going through the empty-state button or settings.
- Adds a visible delete button (next to edit) on each connection row in the connection manager, gated by a confirmation dialog. The existing swipe-to-delete gesture is preserved as a power-user shortcut.
- Network discovery now finds every MusicBee plugin on the LAN on the first scan instead of just one. The scan listens for a few seconds, re-broadcasts the discovery packet to combat multicast packet loss, and de-duplicates results by address.

### Changed
- Long track titles, artists, and albums now scroll horizontally (marquee) on the player and mini-control instead of being cut off with an ellipsis, so the full text is always readable. Library list rows still truncate with an ellipsis.

### Fixed
- Fixes accidental track removal while scrolling the now playing queue. Swipe-to-remove now triggers only on a deliberate right-to-left swipe, and Remove is also available from the row's overflow menu as a discoverable alternative. Reordering is now started from the drag handle on the right side of each row, so it no longer competes with scrolling or swiping.
- Fixes the left-edge swipe-to-open gesture for the navigation drawer not working on the now playing screen.
- Fixes the now playing queue capping at 100 items: only the first page ever loaded because `LazyPagingItems` was iterated indirectly. The queue now enables placeholders and iterates `LazyPagingItems` directly so Paging 3 keeps loading subsequent pages on scroll.
- Fixes pull-to-refresh not triggering when the now playing queue is empty by wrapping the empty/loading/error states in a vertically scrollable container.
- Fixes drag-and-drop reorder being snapped back to the server order when a new page lands mid-drag, and lets users reorder across page boundaries by guarding mid-drag refreshes with a settle window and resolving the source index against the displayed list.
- Fixes a crash when the server returns a bare empty array instead of an empty page object for paged library responses; pagination now terminates cleanly on that response shape.
- Fixes an out-of-memory crash on the socket message pipeline under burst load. The incoming-message flow now uses a bounded buffer that drops the oldest queued message when a downstream collector falls behind, instead of spawning unbounded coroutines per message.
- Fixes an `IllegalArgumentException` crash when navigating into an album, artist, or genre whose name contains a literal `%` (e.g. "100% Hits"). Route arguments are now encoded with `Uri.encode` and consumed directly from the navigation back stack, removing the duplicate `URLDecoder.decode` step that caused the crash.

## [1.6.0-rc.4] - 2026-04-22
### Fixed
- Fixes a crash on track change when the plugin omits the `year` field for tracks without a year tag.
- Fixes a home screen widget crash when tapping the widget to open the app, by launching the activity directly instead of routing through a Glance action callback.
- Fixes a crash when a broadcast action throws, by catching exceptions in the message handler and swallowing the no-default-connection error during now-playing refresh.
- Fixes the app License screen failing to load because the bundled `LICENSE.txt` asset was missing from the APK.

### Changed
- Updates `aboutlibraries` to 14.0.1 and bumps other minor/patch dependencies (Firebase BOM, Koin, androidx.annotation, Crashlytics plugin).
- Pins GitHub Actions to newer digests (upload-artifact, setup-android, codeql-action, gradle/actions).

## [1.6.0-rc.3] - 2026-04-19
### Fixed
- Fixes a crash when opening the output selection (or any other API call) before a default connection is configured.
- Fixes a crash when the plugin sends a cover protocol message with a null `cover` field.
- Fixes a startup crash for users upgrading from legacy versions caused by a type mismatch on the stored last-run version code.

## [1.6.0-rc.2] - 2026-04-18
### Fixed
- Fixes Google Services configuration for the Play release build.

## [1.6.0-rc.1] - 2026-04-15
### Added
- [#231] Adds play all and shuffle all library options to the library menu.
- [#230] Displays composer information in the lyrics screen header.
- Adds track details panel showing extended metadata (composer, genre, bitrate, file info, play statistics).
- Complete UI rewrite using Jetpack Compose.
- [#61] Adds light mode support alongside existing dark theme.
- Adds What's New screen to display changelog on updates.
- [#107] Moves library sync to WorkManager with detailed progress notifications.
- Adds enhanced connection error handling with automatic retry logic.
- Extends track search to include artist name matching.
- Adds improved mini player control with modern design.
- [#204] Adds support for resizable home screen widgets.
- Adds animated wave indicator for streaming content in player and mini control.
- Radio stations now load automatically when the screen opens.
- Displays queue size in the Queue screen header.
- [#155] Adds Go to Album and Go to Artist actions in player menu.
- [#155] Adds Go to Album and Go to Artist actions in queue.
- [#262] Adds sorting options to library tabs and drilldowns (genres, artists, albums, tracks).
- [#181] Adds playlist folder navigation support.
- [#229] Adds ban track support alongside love for Last.fm ratings.
- [#226] Adds albums by genre navigation for direct access to genre albums.
- [#205] Adds cover count support to library stats.
- Adds support for Android 15 and 16 (API 35-36).
- Adds data extraction rules for backup and cloud migration.
- [#264] Adds comprehensive star rating system with bomb rating, half-star support, and clear rating.
- [#264] Adds option to display track rating on the player screen.
- [#264] Adds setting to enable/disable half-star ratings.
- [#263] Adds album grid view with auto-detection for tablets and manual toggle.

### Changed
- [#184] Groups tracks without album info into a single "[Empty]" entry in album library.
- [#115] Fixes elapsed time not updating correctly for streaming content.
- [#130] Fixes widget not updating track name and album art correctly.
- Fixes widget track info position not updating dynamically.
- Fixes volume buttons incorrectly muting the desktop app.
- [#178] Fixes album artist view not showing compilation albums.
- Fixes position updater not stopping properly on disconnect.
- Fixes empty address handling in service discovery messages.
- [#241] Fixes artist matching when searching in album library.
- [#134] Fixes case-insensitive sorting for tracks, albums, genres, and artists.
- Fixes notification update logic for connection state changes.
- [#219] Fixes artist sorting to ignore "the" prefix.
- [#242] Updates layouts for improved RTL support.
- Migrates from RxJava to Kotlin Coroutines.
- Migrates from Toothpick to Koin for dependency injection.
- Migrates from DBFlow to Room for database.
- Migrates from Picasso to Coil for image loading.
- Migrates from Jackson to Moshi for JSON serialization.
- Migrates home screen widgets to Glance Compose.
- Migrates architecture from MVP to MVVM.
- Improves offline status interaction handling.
- Improves collection loss detection.
- Updates database schema with non-nullable fields and unique indices.

### Removed
- Removes legacy View-based UI components.
- Removes Butterknife.
- Removes RxJava.
- Removes ThreeTenBP (replaced with java.time).

[#230]: https://github.com/musicbeeremote/mbrc/issues/230
[#231]: https://github.com/musicbeeremote/mbrc/issues/231
[#61]: https://github.com/musicbeeremote/mbrc/issues/61
[#107]: https://github.com/musicbeeremote/mbrc/issues/107
[#115]: https://github.com/musicbeeremote/mbrc/issues/115
[#130]: https://github.com/musicbeeremote/mbrc/issues/130
[#134]: https://github.com/musicbeeremote/mbrc/issues/134
[#155]: https://github.com/musicbeeremote/mbrc/issues/155
[#178]: https://github.com/musicbeeremote/mbrc/issues/178
[#181]: https://github.com/musicbeeremote/mbrc/issues/181
[#184]: https://github.com/musicbeeremote/mbrc/issues/184
[#204]: https://github.com/musicbeeremote/mbrc/issues/204
[#205]: https://github.com/musicbeeremote/mbrc/issues/205
[#219]: https://github.com/musicbeeremote/mbrc/issues/219
[#262]: https://github.com/musicbeeremote/mbrc/issues/262
[#226]: https://github.com/musicbeeremote/mbrc/issues/226
[#229]: https://github.com/musicbeeremote/mbrc/issues/229
[#241]: https://github.com/musicbeeremote/mbrc/issues/241
[#242]: https://github.com/musicbeeremote/mbrc/issues/242
[#264]: https://github.com/musicbeeremote/mbrc/issues/264

## [1.5.1] - 2021-09-13
### Changed
- Fixes an issue where the last playing track information would not properly persist.
- [#228] Fixes an issue where pressing the up volume button once would decrease the volume.
- Fixes a crash when attempting to send debug logs via the Feedback screen.
- [#232] Fixes an issue that would cut the album title when in the album library tab.
- [#227] Fixes an issue that would fail to properly sync album covers after the first sync.

[#228]: https://github.com/musicbeeremote/android-app/issues/228 
[#232]: https://github.com/musicbeeremote/android-app/issues/232 
[#227]: https://github.com/musicbeeremote/android-app/issues/227

## [1.5.0] - 2021-04-03
### Added
- [#103], [#113] Adds radio station support.
- Adds support for album covers on the library albums, and tracks tab.
- Adds an auto-connect behavior when restoring the application to the foreground.

### Changed
- [#215] Fixes an issue where the volume up button would not work when volume was greater than 90%.
- [#221] Fixes an issue that would cause the library search to hang after navigating view details. e.g. After going to album tracks.
- [#218] Fixes an issue where the remote would continuously pause videos playing on the device.

[#221]: https://github.com/musicbeeremote/android-app/issues/221
[#218]: https://github.com/musicbeeremote/android-app/issues/218
[#215]: https://github.com/musicbeeremote/android-app/issues/215
[#113]: https://github.com/musicbeeremote/android-app/issues/113
[#103]: https://github.com/musicbeeremote/android-app/issues/103

## [1.4.0] - 2020-12-22
### Added
- [#209] Adds statistics dialog when you can check how many tracks, etc are currently synced.
- [#209] Improves visual feedback when performing a library metadata sync.
- [#214] Adds a feedback message when queueing tracks/albums/genres/artists.
- [#214] Adds the ability to queue the album or artist when browsing the track list.

### Changed
- [#213] Fixes a bug that would prevent users from queueing albums when browsing an artist's albums.

[#214]: https://github.com/musicbeeremote/android-app/issues/214
[#213]: https://github.com/musicbeeremote/android-app/issues/213
[#209]: https://github.com/musicbeeremote/android-app/issues/209

## [1.3.0] - 2020-12-12
### Added
- [#172] Introduces the ability to change audio outputs.

### Changed
- [#211] Fixes a bug with the feedback input being collapsed on smaller screen sizes.
- Fixes a crash while starting or stopping the background service.

[#211]: https://github.com/musicbeeremote/android-app/issues/211
[#172]: https://github.com/musicbeeremote/android-app/issues/172

## [1.2.1] - 2020-11-26
### Changed
- Fixes a background crash with the application service.
- Fixes a crash on cover retrieval.
- Fixes an issue with the service discovery.

## [1.2.0] - 2020-11-19
### Changed
- Fixed a crash on the service when running on the background

## [1.2.0-beta1] - 2020-11-07
### Changed
- Adds support for Android 11.
- Improves the library metadata sync performance.
- Updates to the latest Material Design library.
- Integrates the library search functionality to the library screen.
- Removes the ability to hide the notification due to background execution limitations introduced in Android.
- Improves media integration with Android.
    
### Removed
- Removes fast scrolling due to performance issues.

## [1.1.0] - 2017-01-25
### Added
- ([#100]) Adds the option to enqueue all tracks and play the selected on the library track tab.
- ([#92]) Adds the option to show only album artists in the library view.
- ([#99]) Adds fast scrolling to the library tabs.
- ([#112]) Adds Play Now (Queue All) option for albums.

### Changed
- ([#95]) Refreshes the playlists along with the library data.
- ([#96]) Sorting now starts only from the triple dash icon in the now playing.
- ([#91]) Fixes the library search collapse that did not work previously.
- ([#94]) Reworks the default action option to affect only the track view.
- ([#93]) Fixes the query for showing the albums of an artist.
- ([#90]) Fixes the tab colors in the Help screen.
- ([#105]) Automatically scrolls to the playing track when entering the now playing screen.
- ([#97]) Fixes an issue where the remote session volume slider wouldn't work.
- ([#102]) Fixes the ordering of the Genres to alphabetical.
- Pressing the track info on the main view should now open the Now Playing screen.

[#100]: https://github.com/musicbeeremote/android-app/issues/100
[#92]: https://github.com/musicbeeremote/android-app/issues/92
[#99]: https://github.com/musicbeeremote/android-app/issues/99
[#112]: https://github.com/musicbeeremote/android-app/issues/112
[#95]: https://github.com/musicbeeremote/android-app/issues/95
[#96]: https://github.com/musicbeeremote/android-app/issues/96
[#91]: https://github.com/musicbeeremote/android-app/issues/91
[#94]: https://github.com/musicbeeremote/android-app/issues/94
[#93]: https://github.com/musicbeeremote/android-app/issues/93
[#90]: https://github.com/musicbeeremote/android-app/issues/90
[#105]: https://github.com/musicbeeremote/android-app/issues/105
[#97]: https://github.com/musicbeeremote/android-app/issues/97
[#102]: https://github.com/musicbeeremote/android-app/issues/102

## [1.0.1] - 2016-12-22
### Changed
- Fixes a crash with a null extras payload when updating the widget.
- Fixes a crash with a null key event on the media intent handler.
- Fixes an issue where the artist albums would not display properly.

## [1.0.0] - 2016-12-15
### Added 
- Adds a message on connection to inform the user in case an incompatible version of the plugin is used.

### Changed
- Fixes an issue where the notification would not disappear on a connected wearable after closing MusicBee.
- Fixes an issue where the volume control in the wearable notification would not work.
- Fixes an issue where the temporary stop during track change would make the notification flicker.
- Fixes an issue where exiting the application through an other than MainActivity would navigate to the main activity.


## [1.0.0-beta.2] - 2016-12-03
### Changed
- Fixes a potential crash on the progress updater.

## [1.0.0-beta.1] - 2016-11-28
### Added
- Adds empty view for lists without data.
- Adds a library refresh actionbar action.

### Changed
- Full migration to kotlin language.
- Commands are now instantiated on registration.
- Changes the widget updateNotification mechanism to avoid previous issues where the widgets failed to updateNotification properly.
- Migrates cover loading mechanism to a newer API implementation.
- Migrates lyrics to an newer API implementation.
- Playlists now work through the no broadcast mechanism.

## [1.0.0-alpha.1] - 2016-09-15
### Added
- Adds basic playlist support. View and click two play.
- Adds extra actions on the Incoming call action (Stop and Pause along with Volume reduce).

### Changed
- Increases minimum supported Android version from 2.3 to 4.1
- Reworks part of the application UI
- Replaces Search with Library browser. Library browser has search functionality for the locally cached metadata.
- Merges Help and Feedback views.
- Fixes an issue where the application would start automatically on device boot.
- Fixes an issue with the widgets that stopped to updateNotification after some time.
- Increases the timeout of the automatic discovery function.
- Reworks the connection settings detection functionality. (If the network allows it the detection should happen automatically on the application startup)

### Removed
- Removes the dialogs that used to appear on each new setup.


[Unreleased]: https://github.com/musicbeeremote/mbrc/compare/v1.6.0...HEAD
[1.6.0]: https://github.com/musicbeeremote/mbrc/compare/v1.6.0-rc.4...v1.6.0
[1.6.0-rc.4]: https://github.com/musicbeeremote/mbrc/compare/v1.6.0-rc.3...v1.6.0-rc.4
[1.6.0-rc.3]: https://github.com/musicbeeremote/mbrc/compare/v1.6.0-rc.2...v1.6.0-rc.3
[1.6.0-rc.2]: https://github.com/musicbeeremote/mbrc/compare/v1.6.0-rc.1...v1.6.0-rc.2
[1.6.0-rc.1]: https://github.com/musicbeeremote/mbrc/compare/v1.5.1...v1.6.0-rc.1
[1.5.1]: https://github.com/musicbeeremote/mbrc/compare/v1.5.0...v1.5.1
[1.5.0]: https://github.com/musicbeeremote/mbrc/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/musicbeeremote/mbrc/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/musicbeeremote/mbrc/compare/v1.2.1...v1.3.0
[1.2.1]: https://github.com/musicbeeremote/mbrc/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/musicbeeremote/mbrc/compare/v1.2.0-beta1...v1.2.0
[1.2.0-beta1]: https://github.com/musicbeeremote/mbrc/compare/v1.1.0...v1.2.0-beta1
[1.1.0]: https://github.com/musicbeeremote/mbrc/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/musicbeeremote/mbrc/compare/v1.0.0...v1.0.1
[1.0.0-beta.2]: https://github.com/musicbeeremote/mbrc/compare/v1.0.0-beta.1...v1.0.0-beta.2
[1.0.0-beta.1]: https://github.com/musicbeeremote/mbrc/compare/v1.0.0-alpha.1...v1.0.0-beta.1
[1.0.0-alpha.1]: https://github.com/musicbeeremote/mbrc/compare/v0.11.2...v1.0.0-alpha.1
