# LeafVibe Extension Library — architecture and initial catalogue

Status: architecture proposal and research inventory. Nothing in this catalogue is automatically installed, enabled, or trusted merely because it is listed.

## Product vision

LeafVibe includes an **Extension Library**: a built-in catalogue where the user can discover tools, inspect their details, and choose whether to include them in the LeafVibe environment. The library separates:

1. **Catalogue** — available entries, including tools not yet downloaded.
2. **Installed** — components present locally.
3. **Enabled** — installed components explicitly activated by the user.
4. **Running** — enabled components currently executing.
5. **Disabled / paused** — installed but not active.
6. **Removed** — uninstalled from LeafVibe; user data is handled according to the extension's uninstall policy.

An item may be visible in the catalogue without being installable on the current device. Compatibility and permissions must be displayed before installation.

## User-facing library

Each extension card should show:

- Name, icon, category, short purpose and screenshots where available.
- Publisher / source repository and a link to the original project.
- Version, last release/update date, maintenance status and supported platforms.
- Licence and obligations (including copyleft / attribution / source disclosure).
- Download size, estimated installed size, offline capability and required network access.
- Android/API or desktop compatibility.
- Dependencies and required LeafVibe capabilities.
- Permissions requested, why each is needed, and whether optional.
- Security review status, signature/checksum and provenance.
- Data handled, storage location, export format and uninstall/data-retention behaviour.
- Actions appropriate to its type: **Install / Download**, **Enable**, **Disable**, **Update**, **Uninstall**, **Open**, **Configure**, **View details**.

Search, categories, filters (offline, free/open source, platform, licence, size, privacy, maturity), favourites, update notices and a locally cached catalogue should be supported. The catalogue must remain browsable offline using its last downloaded index.

## Lifecycle and state model

Use explicit states and transitions:

- Available → Downloading → Verified → Installed → Enabled → Running.
- Installed → Disabled (keeps files and settings).
- Enabled → Disabled (stops work first, then prevents restart).
- Installed / Disabled → Uninstalling → Removed.
- Any install/update failure → Error, with a readable reason and retry option.

Never silently activate a downloaded extension. Installation and activation are separate user decisions. Ask before granting sensitive permissions, starting background services, enabling notification access, accessibility, VPN, file-wide access, or network listeners. Provide a per-extension **Stop now** control and a global **Disable all optional extensions** switch.

## Extension types — do not treat all tools as the same thing

1. **Built-in feature**: compiled into the main APK; can be enabled/disabled by LeafVibe settings, but cannot be independently downloaded/uninstalled.
2. **Dynamic feature module**: code packaged in the Android App Bundle and delivered on demand by Google Play Feature Delivery. Useful for optional LeafVibe features, but depends on Play delivery and is not the universal offline/plugin solution.
3. **External Android app**: installed/updated/uninstalled by Android's package installer and opened through supported intents/deep links. LeafVibe cannot silently install or freely uninstall arbitrary apps; system confirmation and Android privileges apply.
4. **LeafVibe local extension**: declarative manifest + assets/configuration and a constrained capability interface. This is the preferred future extension format for portable/offline modules. Initially it should not execute arbitrary downloaded DEX/JAR/native code.
5. **Desktop adapter**: platform-specific integration for Windows/Linux/macOS, isolated behind a common LeafVibe capability API.
6. **Connector/service**: optional integration with a local or remote service (for example WebDAV, Syncthing, Home Assistant). Must clearly declare network use and remain disabled until configured.
7. **Reference / research entry**: a project worth studying, not an installable extension. This status is essential for third-party projects whose code/licence/security have not yet been audited.

## Technical architecture

Suggested components:

- `ExtensionCatalogRepository`: loads a signed, versioned catalogue; supports local cache and repository sources.
- `ExtensionManifest`: stable ID, name, version, type, platforms, licence SPDX identifier, source URL, checksum/signature, dependencies, capabilities, permissions, entry point, data policy and compatibility.
- `ExtensionInstaller`: type-specific installation strategy; never run downloaded code merely because its archive unpacked.
- `ExtensionRegistry`: local SQLite/Room records for installed versions, enabled state, source, hashes, settings and lifecycle.
- `ExtensionPermissionManager`: capability grants, revocation and audit history.
- `ExtensionRuntime`: lifecycle controller; starts/stops approved modules and monitors failures.
- `ExtensionUpdateService`: checks updates when permitted; supports offline import/export and rollback where safe.
- `ExtensionAuditLog`: records install, enable, disable, permission, update and removal events.
- UI: Library, Installed, Running, Updates, Details, Permissions and Activity log.

Use Room for registry metadata, DataStore for small user preferences, WorkManager for deferrable checks/download verification, Android Keystore for signing/trust material, and Storage Access Framework for user-selected offline extension bundles. Do not require a cloud account.

### Manifest example (illustrative schema, not executable code)

```json
{
  "schemaVersion": 1,
  "id": "org.leafvibe.widget.host",
  "name": "Widget Host",
  "version": "0.1.0",
  "type": "builtin_feature",
  "platforms": ["android"],
  "licence": "Apache-2.0",
  "source": "https://developer.android.com/develop/ui/views/appwidgets/host",
  "capabilities": ["widgets.host"],
  "permissions": [],
  "offline": true,
  "status": "research"
}
```

The catalogue's status field must distinguish `research`, `auditing`, `approved`, `available`, `deprecated` and `blocked`. Do not label a project installable until its source, licence, maintenance, compatibility, dependencies and security have been checked.

## Android constraints and implementation choices

- Android AppWidgetHost is a native route for hosting widgets from installed apps; it requires widget binding and user approval where applicable.
- Jetpack Glance is a Compose-based way to build widgets for LeafVibe itself; it is not a general-purpose plugin loader.
- Play Feature Delivery supports on-demand delivery of feature modules packaged in an app bundle. It is not suitable as the only distribution route because it relies on Google Play and does not provide a fully offline independent plugin marketplace.
- External APK installation must use Android's supported package-install flow and user confirmation. Uninstall rights are restricted; do not promise silent install/uninstall.
- Do not load arbitrary downloaded executable code into the main app process. Start with declarative extensions and precompiled, reviewed modules. If third-party executable plugins are ever supported, isolate them in a separate process with a narrow IPC capability API, signatures, strict permission grants, resource limits and a revocation mechanism.
- Android app sandboxing remains the baseline. A visual “Private” or “Professional” space is not a security boundary by itself.

## Initial catalogue seed

All entries below are **research references**, not pre-approved downloadable LeafVibe plugins. Their current versions, licence details, maintenance and platform compatibility must be rechecked before any integration.

| Entry | Category | Intended role in LeafVibe | Initial type | Status |
|---|---|---|---|---|
| Android AppWidgetHost | Android UI | Host widgets from installed Android apps on LeafVibe desktops | Built-in feature | research |
| Jetpack Glance | Android UI | Build LeafVibe-owned widgets | Built-in feature | research |
| LauncherApps / ShortcutManager | Android integration | Discover launcher activities and app shortcuts | Built-in feature | research |
| Storage Access Framework | Files | User-controlled file and folder access | Built-in feature | research |
| DocumentsProvider | Files | Expose LeafVibe storage as a document provider | Built-in feature | research |
| Room / SQLite | Data | Central local database and migrations | Built-in feature | research |
| SQLCipher | Data security | Encrypted SQLite storage where justified | Built-in feature | research |
| WorkManager | Automation | Reliable deferred background work | Built-in feature | research |
| Android Keystore / BiometricPrompt | Security | Protect keys and gate sensitive actions | Built-in feature | research |
| Syncthing | Synchronisation | Optional peer-to-peer file synchronisation | External app / connector | research |
| Automerge | Sync / CRDT | Conflict-aware local-first shared data | Library / desktop adapter | research |
| Yjs | Sync / CRDT | Collaborative document state | Library / desktop adapter | research |
| Node-RED | Automation | Visual automation and local integrations | External service / connector | research |
| Home Assistant | Automation / IoT | Local device and home automation integration | Connector | research |
| Huginn | Automation | Self-hosted event and agent workflows | External service / connector | research |
| KDE Plasma widgets / activities | Desktop concepts | Inspiration for activities, panels and desktop widgets | Reference only | research |
| Kvaesitso | Launcher | Search and plugin UX inspiration; audit GPL obligations | Reference only | research |
| Lawnchair | Launcher | Launcher architecture and Android integration reference | Reference only | research |
| Neo Launcher | Launcher | Folders, gestures, categories and launcher patterns | Reference only | research |
| OhmLauncher | Launcher | Desktop-like launcher and widget patterns | Reference only | research |
| OnlyFlows | Productivity | Offline-first task, project and tracking patterns | Reference only | research |
| OpenTodo | Productivity | Local-first task data and backup patterns | Reference only | research |
| Joplin | Notes | Notes, notebooks, attachments and export integration study | External app / connector | research |
| Nextcloud | Files / collaboration | Optional self-hosted files, calendar and contacts connector | Connector | research |

## Recommended build sequence

1. **Catalogue UI + registry**: show entries, search/filter, details, local status and favourites. Use sample/local catalogue data first.
2. **Safe built-in toggles**: enable/disable LeafVibe-owned features without downloading executable code.
3. **External app links**: detect installed apps where Android permits, open their settings/store pages and clearly hand off installation to Android.
4. **Offline extension bundles**: import a signed declarative package from a user-selected file; validate schema, signature, hash, compatibility and permissions.
5. **Reviewed feature modules**: modularise selected LeafVibe features; use Play Feature Delivery only where its Play dependency is acceptable.
6. **Desktop extension runtime**: share manifests and capability concepts across platforms, while using native adapters per OS.
7. **Third-party executable extensions**: only after a threat model, sandbox design, signing infrastructure, audit process and rollback strategy exist.

## Acceptance criteria

- User can browse the catalogue without installing anything.
- Every item clearly indicates whether it is a reference, built-in, external app, connector or installable LeafVibe extension.
- Install never implies enable; enable always requires an explicit user action.
- Disable stops scheduled/background work and revokes capabilities without deleting user data.
- Uninstall explains what will be removed and what user data will remain/export.
- No extension can access private/professional files or other modules without explicit capability grants.
- Offline catalogue browsing, installed extension management and import of approved offline packages work without an account.
- Invalid signatures, hashes, manifests, incompatible versions and revoked publishers are rejected.
- Every lifecycle and permission change is auditable.
