# Wrist RSVP Reader

Wrist RSVP Reader is a native Wear OS reader focused on **RSVP** (Rapid Serial Visual Presentation): the watch shows one word at a time and keeps the visual anchor letter fixed so the reader's eyes stay in one stable position.

The project is intentionally scoped to a simple, local workflow:

```text
Computer browser -> local Wi-Fi -> Watch receive screen -> Room database -> offline reading
```

No companion phone app is required. No cloud service is used.

## What It Does

- Runs as a native Wear OS app.
- Receives books directly from a computer browser over the local network.
- Stores books locally on the watch with Room.
- Reads offline after a book is saved.
- Supports RSVP mode with highlighted anchor letter.
- Supports Page Scroll mode.
- Saves and restores reading progress.
- Supports basic WPM control, font size, light/dark theme and rotary input.
- Imports text formats through the watch receive server:
  - `.txt`
  - `.md`
  - `.markdown`
  - `.html`
  - `.htm`
  - `.xhtml`
  - `.epub`

## What It Does Not Use

- No firmware.
- No ESP32.
- No PlatformIO.
- No root.
- No Samsung private APIs.
- No BLE file transfer.
- No external server.
- No phone companion as the main flow.

## Current Status

This repository currently contains the Watch-first MVP:

- `apps:watch` Wear OS app.
- Local library on the watch.
- Demo text fallback.
- PC-to-Watch upload page served by the watch.
- RSVP reader.
- Page Scroll reader.
- Chapters screen.
- Book details screen.
- Local progress persistence.
- Parser modules for TXT, Markdown, HTML and initial EPUB support.
- Unit tests for reader, parser, data and Watch ViewModels.

## Main Flow

1. Connect the computer and the watch to the same Wi-Fi network.
2. Open the app on the watch.
3. Tap **Receber do computador**.
4. The watch shows a local URL, for example:

   ```text
   http://192.168.0.34:8790
   ```

5. Open that URL on the computer browser.
6. Enter the 6-digit code shown on the watch.
7. Choose a book file and upload it.
8. Wait for the watch confirmation.
9. Open the book and read offline.

The receive server only runs while the receive screen is open.

## Reader Controls

In RSVP mode:

- Tap center: play/pause.
- Swipe left/right: next/previous word.
- Swipe up/down: increase/decrease WPM.
- Rotary input while paused: move through words.
- Rotary input while playing: adjust WPM.
- `PS`: switch to Page Scroll.
- `CAP`: open chapters.

In Page Scroll mode:

- `RS`: return to RSVP.
- Scroll manually or use the available watch input.
- Progress is shared with RSVP mode.

## Requirements

- Android Studio.
- JDK 21.
- Android SDK.
- Wear OS emulator or Wear OS watch.
- Computer and watch on the same local network for uploads.

## Open In Android Studio

1. Clone the repository.
2. Open the project root in Android Studio.
3. Let Gradle sync.
4. Select the `apps.watch` run configuration.
5. Run on a Wear OS emulator or watch.

## Build And Test

On Windows PowerShell:

```powershell
.\gradlew.bat :core:reader:test
.\gradlew.bat :core:parser:test
.\gradlew.bat :core:data:testDebugUnitTest
.\gradlew.bat :apps:watch:testDebugUnitTest
.\gradlew.bat :apps:watch:assembleDebug
```

Install on a connected Wear OS target:

```powershell
.\gradlew.bat :apps:watch:installDebug
```

## Project Structure

```text
apps/watch
  Wear OS UI, local receive server, library, reader and settings

core/domain
  Domain models for books, chapters, tokens, progress and settings

core/reader
  RSVP tokenization, anchor calculation, anchor centering and pacing

core/parser
  TXT, Markdown, HTML and EPUB parsing

core/data
  Room database, DAOs, entities and repositories

core/designsystem
  Shared Compose UI components

core/testing
  Shared test helpers and fakes
```

## Architecture Notes

- The watch is the product entry point.
- The computer only needs a browser.
- The watch starts a temporary local HTTP server on ports `8790-8793`.
- Uploads require the pairing code shown on the watch.
- The reader does not depend on the upload screen after saving the book.
- The UI reads tokens by index/window instead of loading the whole book into the reader screen.
- The RSVP layout centers the **anchor letter**, not the whole word.

## Privacy And Local Network Model

Book content stays local:

- The browser sends the selected file directly to the watch IP.
- The app does not upload books to the internet.
- The app does not require a backend account.
- Uploaded books are stored in the watch local database.

Troubleshooting:

- If the watch URL does not open, check if the computer and watch are on the same Wi-Fi network.
- Some routers block direct device-to-device traffic.
- Keep the receive screen open while uploading.
- If an EPUB fails, test TXT or Markdown first.

## Known Limitations

- Complex EPUB files can lose visual structure because the parser focuses on readable text.
- Large books can take longer to process on the watch.
- Local upload depends on the network allowing device-to-device access.
- Page Scroll token mapping is still approximate.
- Not every Wear OS device has rotary input.

## Roadmap

- Improve local discovery so the computer can find the watch more easily.
- Improve Page Scroll token/offset mapping.
- Add broader Compose UI tests around full flows.
- Polish EPUB handling for more real-world books.
- Add release packaging instructions.

## Publishing Notes

User-provided books and local screenshots are intentionally ignored by Git. Do not commit copyrighted reading files into `apps/watch/src/main/assets/books`.
