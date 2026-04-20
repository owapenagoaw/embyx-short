# Changelog

All notable changes to the Android app are documented in this file.

## [0.1.1] - 2026-04-20

### Added

- Added Debug Overlay switch in Profile page settings.
- Added global floating debug panel with:
  - CPU usage (sampled from /proc/stat)
  - GPU decoder info (active video decoder names)
  - App heap memory usage
  - Local media cache size
- Added Random/Sequential playback history persistence and rendering in Profile page.
- Added playlist list rendering in Profile page.
- Added decoder registry for Home and Favorites players.

### Changed

- Improved random playback strategy:
  - Use Emby TotalRecordCount for weighted library selection when source is ALL.
  - Use RandomSeed for random fetch requests.
- Split random and sequential playback sessions in HomeViewModel.
- Sequential mode now persists and restores state by library scope:
  - video list
  - current page
  - position
  - play/pause state
- Unified player runtime config for Home/Favorites:
  - shared cache-backed media source factory
  - conservative load control

### Data Model / API

- ItemsResponse now includes TotalRecordCount.
- EmbyMediaApi.getVideos now supports:
  - EnableTotalRecordCount
  - RandomSeed

### Build

- Android app versionName bumped to 0.1.1.
