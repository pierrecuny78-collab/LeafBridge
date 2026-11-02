# LeafBridge portable workspace

## What 1.3.0 provides

- A ZIP backup created through Android's system document picker.
- Restore from a LeafBridge ZIP backup.
- Backup contents: Shazam notification inbox, favorites, automation states, and engine log.
- The backup can be saved to local storage, a USB drive exposed by Android, or a cloud document provider.
- A resizable Android home-screen widget that opens the LeafBridge control center.
- A link to the GitHub source repository for editing the actual application project on a computer.

## Important boundaries

- The local PIN profile is intentionally not exported. Each device keeps its own unlock PIN.
- This archive is a runtime-data backup, not a copy of the Android source repository.
- The application source remains in GitHub and can be cloned/downloaded from the repository.
- Restore overwrites the corresponding engine data files. Keep a copy of the destination device's data before restoring.
- The widget is a launcher shortcut panel; it does not yet show live engine statistics or expose individual automation controls.
- A full in-app project manager (create/import/export arbitrary project folders, file editor, templates, version history, and cross-platform project package format) remains a next development milestone.

## Portable project format planned

The next project-management milestone should introduce a .leafproject ZIP with:
- leafproject.json manifest (name, version, language, entry point, dependencies)
- src/ source files
- assets/ resources
- data/ optional project data
- README.md and LICENSE
- optional checksums and an export report

Project import must reject absolute paths, parent-directory traversal, oversized files, and executable content outside the selected project folder.
