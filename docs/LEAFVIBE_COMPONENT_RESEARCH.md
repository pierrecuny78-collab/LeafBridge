# LeafVibe — recherche approfondie des composants techniques

Date de recherche : novembre 2026. Ce document distingue les API natives, les bibliothèques intégrables et les applications externes. Une référence ne signifie pas qu'elle est déjà intégrée au code.

## 1. Android : composants système à exploiter en priorité

| Besoin | API / composant | Usage LeafVibe | Précautions |
|---|---|---|---|
| Remplacer l'écran d'accueil | Activity HOME, Intent filters, PackageManager | LeafHome, tiroir, pages, dock | Ne pas modifier le système ; l'utilisateur choisit le launcher par défaut. |
| Widgets d'applications tierces | AppWidgetHost, AppWidgetHostView, AppWidgetManager | Vrais widgets de calendrier, musique, tâches, météo | Allocation/libération des IDs, liaison autorisée par l'utilisateur, écran de configuration, taille, cycle de vie et sauvegarde. |
| Widgets LeafVibe | Jetpack Glance ou RemoteViews | Tâches, routines, suivi, statut des modules | Glance est conçu pour les widgets, pas comme remplacement direct de toute l'UI Compose. |
| Raccourcis d'applications | LauncherApps, ShortcutManager, ShortcutInfo | Actions rapides par app, raccourcis profonds | Gestion des raccourcis dynamiques/épinglés et limites du launcher. |
| Ouverture et partage de fichiers | Storage Access Framework, ACTION_OPEN_DOCUMENT, ACTION_CREATE_DOCUMENT, ACTION_OPEN_DOCUMENT_TREE | Fichiers sans accès global au stockage | Conserver les URI grants persistants ; ne pas stocker de chemins supposés. |
| Fournisseur de documents | DocumentsProvider | Exposer un espace LeafVibe contrôlé aux autres applications | Permissions URI, règles d'accès, sécurité et tests multi-apps. |
| Fournisseur de données | ContentProvider, ContentResolver | API interne/partage sélectif tâches, notes et métadonnées | Contrôler exported, permissions et URI grants ; thread-safety. |
| Médias | MediaStore, Photo Picker | Bibliothèque médias sans demander un accès total | Respecter les permissions limitées et le sélecteur système. |
| Travail différé | WorkManager | Sauvegarde, indexation, synchronisation et tâches périodiques | Le système peut différer le travail ; pas de garantie temps réel. |
| Travail exact à heure fixe | AlarmManager | Rappels qui doivent sonner à une heure précise | Permissions/conditions particulières ; réserver aux besoins réellement exacts. |
| Travail continu visible | Foreground Service | Opération longue explicitement demandée par l'utilisateur | Notification persistante, types de service et restrictions de démarrage en arrière-plan. |
| Notifications | NotificationListenerService et NotificationManager | Centre de notifications, événements Shazam, rappels | Accès spécial accordé par l'utilisateur ; minimiser les données conservées. |
| Communication inter-apps | Intents, PendingIntent, BroadcastReceiver | Lancer actions, partager, recevoir événements | Valider les entrées ; PendingIntent immuables par défaut si possible. |
| Vie privée / travail | Android managed profiles, user profiles | Isolation professionnelle lorsque le système le permet | Un écran distinct n'est pas une isolation. Dépend de l'appareil et du profil géré. |
| Sécurité cryptographique | Android Keystore, BiometricPrompt, Credential Manager | Clés locales, déverrouillage, passkeys | Keystore protège les clés ; il ne chiffre pas automatiquement les fichiers ou la base. |
| Adaptation écrans | WindowManager, WindowMetrics, Jetpack WindowManager | Téléphone, paysage, tablette, pliable | Tester les dimensions réelles, clavier et multi-fenêtre. |
| Accessibilité | Accessibility APIs, TalkBack semantics | Navigation accessible | Ne pas demander le service Accessibility pour contourner les API ou espionner l'écran. |
| Bluetooth / USB | Bluetooth APIs, USB Host APIs | Appairage, périphériques et clés USB | Autorisations explicites, compatibilité matérielle, transfert local sécurisé. |

### Documentation Android primaire
- AppWidgetHost : https://developer.android.com/develop/ui/views/appwidgets/host
- App Widgets : https://developer.android.com/develop/ui/views/appwidgets
- Glance : https://developer.android.com/develop/ui/compose/glance
- Storage Access Framework : https://developer.android.com/guide/topics/providers/document-provider
- ContentProvider : https://developer.android.com/guide/topics/providers/content-provider-creating
- Room : https://developer.android.com/training/data-storage/room
- WorkManager : https://developer.android.com/develop/background-work/background-tasks/persistent
- Android Keystore : https://developer.android.com/privacy-and-security/keystore
- Credential Manager : https://developer.android.com/identity/credential-manager
- App architecture / sandbox : https://developer.android.com/guide/components/fundamentals

## 2. Persistance, données et recherche

| Technologie | Type | Intérêt | Décision de recherche |
|---|---|---|---|
| Room + SQLite | Relationnel Android | Tâches, projets, routines, relations et migrations | Candidat prioritaire Android ; conserver un schéma versionné. |
| SQLCipher | SQLite chiffré | Données réellement sensibles au repos | Étudier comme option de coffre ; gérer clés via Keystore, migrations et récupération. |
| DataStore | Préférences / petits objets | Paramètres, préférences par bureau, configuration | Pour préférences, pas pour remplacer une base relationnelle. |
| ObjectBox | Base embarquée | Données locales et synchronisation selon édition | Comparer licence, modèle et coût avant adoption. |
| Realm / alternatives | Base objet | Synchronisation et données structurées | Vérifier état actuel du produit, licence et dépendance fournisseur avant choix. |
| Lucene / SQLite FTS | Index texte | Recherche locale rapide | SQLite FTS pour première version ; index séparé et réindexable. |
| Tantivy | Moteur de recherche Rust | Index performant multi-plateforme | Étudier pour version desktop ou moteur natif, pas nécessaire dans l'APK initial. |
| Markdown + JSON | Formats ouverts | Notes exportables, projets et interchange | Utiliser comme formats d'échange ; valider schémas et versions. |
| ZIP + manifeste | Archive portable | Sauvegardes, export de workspace | Ajouter checksum, version, manifeste, validation et restauration atomique. |

## 3. Synchronisation et collaboration hors ligne

| Projet / protocole | Apport | Limite / décision |
|---|---|---|
| Syncthing | Synchronisation P2P de dossiers | Très bon outil compagnon ; pas une base de données applicative. |
| Automerge | CRDT orienté documents JSON | Candidat si édition multi-appareils concurrente devient nécessaire. |
| Yjs | CRDT collaboratif modulaire | Fort pour documents collaboratifs ; évaluer l'intégration Kotlin/Android. |
| CouchDB / PouchDB | Réplication et base locale/web | Modèle utile ; examiner conflits, stockage et maintenance. |
| ElectricSQL | Réplication SQLite | Étudier l'architecture et la maturité ; dépendance réseau/serveur selon scénario. |
| WebDAV | Protocole fichiers | Connecteur optionnel vers serveur personnel. |
| SFTP / SSH | Transfert sécurisé | Pour utilisateurs avancés et appareils contrôlés. |
| QR + TLS local | Appairage et transfert | Concevoir un protocole d'appairage authentifié, non un simple serveur ouvert sur le LAN. |

Règle : la base locale est la source de vérité pendant le mode hors ligne. La synchronisation est facultative, explicite et accompagnée d'une résolution de conflits lisible.

## 4. Interface, moteurs graphiques et multiplateforme

| Technologie / projet | Plateformes | Apport possible | Vigilance |
|---|---|---|---|
| Jetpack Compose + Material 3 | Android | UI déclarative moderne, composants adaptatifs | Candidat naturel pour refonte progressive de l'UI actuelle en Views. |
| Compose Multiplatform | Android, iOS, desktop | Partager logique et certaines interfaces | Ne pas supposer que toutes les API système sont communes. |
| Flutter | Android, iOS, desktop, web | UI cohérente et écosystème riche | Migration complète coûteuse ; plugins natifs nécessaires. |
| Kotlin Multiplatform | Android, iOS, desktop selon cibles | Partager logique métier et modèles | Garder UI et adaptateurs système spécifiques si plus fiables. |
| Qt / QML | Linux, Windows, macOS, embarqué | Interfaces riches et widgets desktop | Licence LGPL/commerciale à examiner, empreinte et intégration mobile. |
| GTK / libadwaita | Linux | Applications et composants GNOME | Référence desktop Linux, pas UI Android. |
| KDE Plasma / libplasma / QML | Linux, Plasma Mobile | Widgets, panneaux, activités, espaces | Très riche pour les concepts de bureau et extensions ; code à intégrer séparément. |
| WinUI / Windows App SDK | Windows | Fenêtres, notifications, intégration Windows | Connecteur desktop Windows, pas dans l'APK. |
| AppKit / SwiftUI | macOS / iOS | UI et intégration Apple natives | Adaptateur Apple dédié si LeafVibe s'étend à macOS. |
| Tauri | Desktop et mobile selon maturité | Shell léger web + Rust | Vérifier plugins mobiles et permissions avant de choisir. |
| Avalonia | Windows, Linux, macOS, mobile | UI .NET multiplateforme | Évaluer rendu, licences et composants mobiles. |

## 5. Automatisation et orchestration

| Projet | Type | Rôle potentiel |
|---|---|---|
| Node-RED | Flux événementiels | Automatisations visuelles locales et objets connectés. |
| Home Assistant | Domotique locale | Connecteur maison/appareils, pas noyau de LeafVibe. |
| Huginn | Agents et événements | Veille et automatisations auto-hébergées. |
| Kestra | Orchestration déclarative | Workflows serveur/desktop avancés. |
| n8n | Automatisation d'applications | Connecteur externe possible ; licence Sustainable Use, pas à traiter comme OSI open source. |
| Dagu | Orchestration de tâches | Pipelines locaux simples à comparer. |
| Temporal | Workflows durables | Trop lourd pour l'APK ; utile côté serveur si besoin réel. |
| WorkManager | Tâches Android | Choix prioritaire pour tâches persistantes Android. |
| Termux:Tasker / Termux API | Pont Android/terminal | Exécution locale facultative et contrôlée par l'utilisateur. |

Les automatisations doivent avoir un mode simulation, permissions minimales, journal d'exécution, arrêt d'urgence et confirmations pour actions sensibles.

## 6. Sécurité et confidentialité

- Keystore : conserver les clés cryptographiques, jamais les mots de passe en clair.
- BiometricPrompt : demander une authentification système pour déverrouiller certaines opérations.
- SQLCipher : chiffrer les bases sélectionnées ; concevoir soigneusement rotation et sauvegarde des clés.
- Credential Manager : intégrer passkeys et fournisseurs de credentials lorsque nécessaire ; ne pas créer un gestionnaire de mots de passe maison.
- KeePass : étudier le format ouvert et les pratiques d'applications comme KeePassDX pour les secrets.
- Android sandbox : exploiter l'isolation native des applications plutôt que simuler une sandbox par dossiers.
- Managed profiles : préférer un vrai profil professionnel à un simple bureau « Travail ».
- Audit : permissions, exported components, intents, sauvegardes, logs, données temporaires et URI grants.

Références :
- SQLCipher Android : https://github.com/sqlcipher/sqlcipher-android
- Android Keystore : https://developer.android.com/privacy-and-security/keystore
- Credential Manager : https://developer.android.com/identity/credential-manager
- KeePassDX : https://github.com/Kunzisoft/KeePassDX

## 7. Gestion fichiers, médias et périphériques

- SAF pour accès choisi par l'utilisateur et intégration des fournisseurs de documents.
- DocumentsProvider pour exposer un espace LeafVibe avec permissions.
- MediaStore / Photo Picker pour photos, vidéos et audio.
- Android USB Host pour lire les périphériques USB compatibles après autorisation.
- Bluetooth GATT pour périphériques BLE ; profil classique si besoin réel.
- Rclone comme référence pour connecteurs de stockage distants ; éviter d'exposer les identifiants aux modules.
- Tesseract OCR / ML Kit à évaluer pour indexation OCR, avec traitement local si possible.
- PDFium / PdfRenderer pour aperçu PDF ; vérifier licences et contraintes natives.
- ExifTool comme référence métadonnées desktop ; ne pas supposer disponible sur Android.

## 8. Stratégie d'intégration pour LeafBridge

### À intégrer en premier (Android natif)
1. AppWidgetHost et vraie gestion des widgets.
2. Room pour les données structurées de LeafVibe.
3. WorkManager pour sauvegardes, indexation et routines différées.
4. SAF et DocumentsProvider pour fichiers contrôlés.
5. Android Keystore + BiometricPrompt pour secrets et verrouillage.
6. ShortcutManager / LauncherApps pour raccourcis d'applications.
7. WindowManager pour tailles, paysage et écrans adaptatifs.

### À évaluer comme bibliothèques
- Jetpack Compose / Material 3 pour l'interface, en migration progressive.
- Glance pour widgets produits par LeafVibe.
- SQLCipher pour les données choisies comme sensibles.
- SQLite FTS pour recherche locale.
- Automerge ou Yjs uniquement si la collaboration multi-appareils concurrente est un vrai besoin.

### À garder comme services/connecteurs externes
- Syncthing, Nextcloud, Home Assistant, Node-RED, Huginn, Termux.
- Chaque connecteur doit pouvoir être désactivé sans casser LeafCore.
- Aucun service cloud n'est obligatoire.

## 9. Protocole d'audit avant réutilisation

Pour chaque dépôt externe, enregistrer :
- URL et commit/tag précis étudié.
- Licence du dépôt, des sous-modules et des dépendances.
- Dernière activité, releases, issues ouvertes, bus factor et politique de sécurité.
- Langage, minSdk/targetSdk, ABI, taille, performances et maintenance.
- Fonctionnalités réutilisables, API publique et effort d'intégration.
- Données collectées, permissions, télémétrie, réseau et mode hors ligne.
- Tests existants, couverture et reproductibilité du build.
- Décision : intégrer / adapter / connecter / s'inspirer / rejeter.

## 10. Sources primaires complémentaires

- Android AppWidgetHost : https://developer.android.com/develop/ui/views/appwidgets/host
- Android Room : https://developer.android.com/training/data-storage/room
- Android WorkManager : https://developer.android.com/develop/background-work/background-tasks/persistent
- Android ContentProvider : https://developer.android.com/guide/topics/providers/content-provider-creating
- Android SAF : https://developer.android.com/guide/topics/providers/document-provider
- Android Keystore : https://developer.android.com/privacy-and-security/keystore
- Android Credential Manager : https://developer.android.com/identity/credential-manager
- KDE Plasma desktop : https://github.com/KDE/plasma-desktop
- KDE Plasma developer guide : https://community.kde.org/Plasma/DeveloperGuide
- CRDT implementations : https://crdt.tech/implementations
- SQLCipher Android : https://github.com/sqlcipher/sqlcipher-android
- Material 3 : https://m3.material.io/
- Composables UI : https://composables.com/ui/docs/overview
- Node-RED : https://nodered.org/
- Huginn : https://github.com/huginn/huginn
- Syncthing : https://github.com/syncthing/syncthing
- Automerge : https://automerge.org/
- Yjs : https://yjs.dev/
