Changelog
-----------

## [Unreleased]
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
- [#220] Sorts artist albums by release year.
- [#181] Adds playlist folder navigation support.
- [#229] Adds ban track support alongside love for Last.fm ratings.
- [#205] Adds cover count support to library stats.
- Adds support for Android 15 and 16 (API 35-36).
- Adds data extraction rules for backup and cloud migration.

### Changed
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
[#204]: https://github.com/musicbeeremote/mbrc/issues/204
[#205]: https://github.com/musicbeeremote/mbrc/issues/205
[#219]: https://github.com/musicbeeremote/mbrc/issues/219
[#220]: https://github.com/musicbeeremote/mbrc/issues/220
[#229]: https://github.com/musicbeeremote/mbrc/issues/229
[#241]: https://github.com/musicbeeremote/mbrc/issues/241
[#242]: https://github.com/musicbeeremote/mbrc/issues/242

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


[Unreleased]: https://github.com/musicbeeremote/mbrc/compare/v1.5.1...HEAD
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
