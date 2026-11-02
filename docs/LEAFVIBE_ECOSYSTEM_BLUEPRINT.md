# LeafVibe — catalogue de briques, architecture cible et plan d'intégration

> Objectif : assembler des composants éprouvés et des idées issues de projets libres, sans recopier aveuglément des dépôts ni multiplier les applications. LeafVibe reste le produit cohérent ; les projets externes sont des références, dépendances possibles ou inspirations, après audit.

## Principes non négociables

- Local-first et offline-first : les fonctions essentielles restent utilisables sans compte distant ni réseau.
- Interopérabilité : formats ouverts, import/export, sauvegardes lisibles et API documentées.
- Sécurité : moindre privilège, permissions demandées au moment de l'usage, secrets séparés des données ordinaires.
- Modularité : modules activables/désactivables et contrats d'API versionnés.
- Réversibilité : aucune dépendance à un fork non maintenu sans stratégie de sortie.
- Licence : aucun code externe intégré avant vérification de la licence, des dépendances transitives, des notices et des obligations de distribution.
- Compatibilité : Android/One UI d'abord pour le téléphone actuel ; Linux, Windows et macOS par modules et interfaces adaptées.
- Accessibilité : grandes zones tactiles, contraste, taille de texte, navigation clavier et lecteur d'écran.

## Références externes à auditer

Les dépôts ci-dessous sont des candidats d'étude, pas des dépendances déjà intégrées.

| Projet / source | Apports à étudier | Décision provisoire / vigilance |
|---|---|---|
| [Lawnchair](https://github.com/LawnchairLauncher/lawnchair) | Launcher3, grille, widgets, dossiers, thèmes Material You, icônes, recherche | Référence forte pour le moteur de launcher. Branche 16 en développement ; évaluer la branche stable et sa licence avant toute réutilisation. |
| [Kvaesitso](https://github.com/MM2-0/Kvaesitso) | Recherche globale, résultats contextuels, widgets, outils, plugins | Très bonne référence de recherche et de plugins. Code principal GPL-3.0 ; SDK de plugins et core/shared Apache-2.0 selon le dépôt. Ne pas mélanger les licences sans analyse. |
| [KISS Launcher](https://github.com/Neamar/KISS) | Recherche rapide, historique, tags, gestes et raccourcis | Étudier l'UX de recherche et les gestes ; vérifier licence, maintenance et compatibilité actuelles. |
| [Fossify](https://github.com/FossifyOrg) | Applications Android simples, locales, cohérence UI | Références pour fichiers, galerie, calendrier et confidentialité ; auditer chaque application séparément. |
| [KDE Plasma Activities](https://docs.kde.org/stable_kf6/en/plasma-desktop/plasma-desktop/activities-interface.html) | Contextes de travail, bureaux virtuels, widgets par activité | Modèle conceptuel pour distinguer contexte (privé/pro) et bureau (tâche courante), pas code Android directement réutilisable. |
| [Nextcloud](https://github.com/nextcloud) | Fichiers, calendrier, contacts, tâches et synchronisation auto-hébergeable | Connecteur facultatif ; l'expérience locale ne doit pas dépendre d'un serveur Nextcloud. |
| [Joplin](https://github.com/laurent22/joplin) | Notes, carnets, pièces jointes, synchronisation et export | Étudier formats, import/export et organisation documentaire. Vérifier licence et dépendances. |
| [Standard Notes](https://github.com/standardnotes) | Notes chiffrées, modèle de synchronisation et sécurité | Étudier les modèles cryptographiques et séparation des secrets ; ne pas réimplémenter la cryptographie sans expertise. |
| [Syncthing](https://github.com/syncthing/syncthing) | Synchronisation pair-à-pair et fonctionnement sans cloud central | Référence de synchronisation entre appareils ; intégration ultérieure, jamais obligatoire pour l'usage local. |
| [ActivityWatch](https://github.com/ActivityWatch/activitywatch) | Suivi d'activité et temps d'écran local | Référence pour le module Tracking ; collecte transparente, contrôles et pause faciles. |
| [OpenTracks](https://github.com/OpenTracksApp/OpenTracks) | Suivi local de parcours et export GPX | Référence pour activité/mobilité, à isoler et activer explicitement. |
| [Home Assistant](https://github.com/home-assistant/core) | Automatisations locales, intégrations matérielles et tableaux de bord | Référence d'architecture d'automatisation ; ne pas embarquer le serveur complet dans l'application Android. |
| [Termux](https://github.com/termux/termux-app) | Terminal, environnement Linux utilisateur et scripts Android | Intégration facultative pour développeurs ; respecter les limites Android et les permissions. |
| [AppWidgetHost Android](https://developer.android.com/develop/ui/views/appwidgets/host) | Hébergement officiel de widgets tiers | API native à utiliser pour de vrais widgets dans LeafVibe ; cycle de vie, autorisation et IDs à gérer. |
| [Android work profiles](https://developer.android.com/work/managed-profiles) | Isolation des données professionnelles | Voie officielle à privilégier pour une séparation forte privé/pro ; disponibilité selon gestionnaire de profil et appareil. |

## Modèle d'expérience utilisateur

### Navigation spatiale
- Panneau gauche : Organisation — tâches, projets, routines, notes, fichiers, planning, suivi.
- Centre : Bureau — favoris, recherche universelle, dock, notifications LeafVibe et raccourcis.
- Panneau droit : deux espaces distincts, Vie privée et Vie professionnelle.
- Chaque espace possède sa disposition, ses raccourcis, son fond, son thème et ses widgets.
- Un bureau est un contexte visuel ; il ne constitue pas une frontière de sécurité Android.

### Vie privée
- Photos, médias, documents personnels, notes, contacts et calendrier personnel.
- Raccourcis configurables ; aucune lecture des données privées sans autorisation explicite.
- Verrouillage d'accès local et masquage rapide de l'espace.
- Exports chiffrés facultatifs à concevoir ; pas de chiffrement maison.

### Vie professionnelle
- Projets, dépôts Git, documents, calendrier, communication, outils de développement.
- Profil Android professionnel si disponible ; sinon séparation logique clairement indiquée.
- Politique de partage/import/export distincte.
- Raccourcis par projet et espace de travail.

### Organisation
- Boîte de réception universelle des éléments à traiter.
- Tâches avec échéances, priorités, récurrence et états.
- Projets avec notes, fichiers, liens, dépôts et journaux.
- Routines et checklists.
- Planning et chronologie.
- Suivi du temps local avec pause/effacement/export.
- Recherche par nom, type, date, étiquette et contenu indexé localement.
- Liens entre tâches, fichiers, projets et automatisations.

## Architecture logicielle proposée

### Couches
1. **LeafHome** : launcher Android, navigation, dock, panneaux, widgets et thèmes.
2. **LeafCore** : identité locale, permissions internes, événements, tâches planifiées et registre de modules.
3. **LeafData** : stockage local versionné, base structurée, fichiers et index de recherche.
4. **LeafBridge** : passerelle locale vers Termux et les automatisations existantes.
5. **LeafSync** : synchronisation facultative, pair-à-pair ou serveur personnel, conflits explicites.
6. **LeafConnectors** : intégrations isolées (calendrier, fichiers, Git, Nextcloud, Syncthing, Home Assistant).
7. **LeafSecurity** : coffre de secrets via Android Keystore, verrouillage, chiffrement éprouvé, journaux d'accès.
8. **LeafImportExport** : formats ouverts, sauvegardes, migrations et restauration.

### Contrats de modules
Chaque module doit déclarer : identifiant stable, version, permissions demandées, données produites, événements émis/écoutés, dépendances, politique offline, options de sauvegarde, licence et état de santé.

Événements internes envisagés : `task.created`, `task.completed`, `project.updated`, `file.imported`, `routine.due`, `automation.started`, `automation.failed`, `sync.conflict`.

Ne jamais permettre à un module de lire toutes les données par défaut. Les abonnements aux événements doivent être explicitement autorisés.

## Modules fonctionnels à prévoir

### Bureau et personnalisation
- Widgets Android tiers via AppWidgetHost.
- Widgets LeafVibe natifs : tâches du jour, prochain rendez-vous, statut des routines, suivi de temps, raccourcis projets.
- Dossiers, glisser-déposer, épinglage/désépinglage, menus contextuels et raccourcis d'applications.
- Dispositions par espace, sauvegardées localement.
- Thèmes clair/sombre, palettes, couleurs dynamiques, fonds d'écran et packs d'icônes.
- Barre de recherche universelle et commandes rapides.
- Accessibilité, mode une main et prise en charge paysage/tablette.

### Fichiers et connaissance
- Navigateur de fichiers via Storage Access Framework.
- Favoris, étiquettes, collections et historique récent.
- Aperçu PDF/images/texte ; ouverture par application externe pour les autres formats.
- Notes Markdown, liens internes et pièces jointes.
- Import/export ZIP versionné avec manifeste et contrôle d'intégrité.
- Index local réindexable ; exclusion de dossiers privés possible.

### Productivité
- Tâches, projets, routines, calendrier et time tracking.
- Tableaux Kanban et vue chronologique.
- Modèles de projets, checklists et tableaux de bord.
- Capture rapide depuis notification ou raccourci.
- Rappels locaux et actions différées.

### Automatisation
- Déclencheurs locaux : heure, événement LeafCore, fichier importé, connexion USB lorsque Android l'autorise, action utilisateur.
- Actions : notification, création de tâche, lancement d'application, script Termux après consentement.
- Mode simulation, journal, arrêt d'urgence et limites d'exécution.
- Aucune automatisation silencieuse qui exfiltre des fichiers ou déclenche des actions sensibles.

### Connexion et appareils
- QR code pour appairage local.
- Transfert manuel et transfert chiffré facultatif.
- Synchronisation différée avec résolution de conflits.
- Connecteurs USB/Bluetooth uniquement selon capacités et permissions Android.
- Appareils anciens utilisés comme nœuds auxiliaires seulement après audit sécurité et compatibilité.

### Développement
- Gestionnaire de projets multi-langages.
- Modèles de dépôts et arborescences.
- Intégration Git via fournisseur ou binaire adapté.
- Terminal Termux facultatif.
- Exécution dans environnements isolés ; ne pas exécuter du code arbitraire dans le processus principal.
- Documentation, tests et suivi des dépendances.

## Roadmap priorisée

### P0 — Fiabiliser le socle
- Compiler et tester le launcher existant sur le Samsung cible.
- Corriger navigation, persistance et gestion des erreurs.
- Vérifier les autorisations et le manifeste.
- Ajouter tests unitaires du stockage et tests instrumentés essentiels.
- Ne pas rendre le launcher HOME par défaut automatiquement.

### P1 — Réutiliser les API Android éprouvées
- AppWidgetHost + configuration, suppression et restauration des widgets.
- Gestion des raccourcis Android publiés par les applications.
- Disposition persistante par espace : icônes, dossiers, dock, ordre et taille.
- Recherche universelle locale des applications, raccourcis et éléments LeafVibe.
- Intégration du profil professionnel Android lorsque disponible.

### P2 — Productivité intégrée
- Modèle de données tâches/projets/notes/routines.
- Calendrier et documents par sélecteurs Android.
- Widgets LeafVibe et tableau de bord.
- Sauvegarde/restauration versionnée et migration.

### P3 — Connecteurs et synchronisation
- Connecteurs Nextcloud, Syncthing, Git et Home Assistant.
- Synchronisation opt-in, appairage, conflits et chiffrement.
- Mode multi-appareils.

### P4 — Environnement avancé
- Gestionnaire de fenêtres/panneaux compatible avec les API Android disponibles.
- Bureau tablette/paysage.
- Automatisations avancées et scripts isolés.
- Extension vers Linux, Windows et macOS.

## Conditions de validation avant livraison
- Build CI vert sur la branche exacte.
- Installation/mise à jour testée sans perte des données.
- Test hors ligne pour chaque fonction annoncée offline.
- Vérification de permissions et des chemins de fichiers.
- Tests de sauvegarde et restauration sur données de test.
- Audit des licences et attribution.
- Aucun faux bouton : chaque action doit fonctionner ou être explicitement marquée « à venir ».

## Décisions techniques à ne pas prendre prématurément
- Ne pas forker entièrement Lawnchair avant d'avoir comparé son intégration à l'architecture actuelle.
- Ne pas intégrer le code GPL dans une application distribuée sous une licence incompatible.
- Ne pas promettre l'isolation privé/pro par simple séparation d'écrans.
- Ne pas embarquer plusieurs serveurs complets dans l'APK.
- Ne pas imposer de cloud, de compte externe ou de connexion permanente.

## Sources primaires
- Android App Widgets host: https://developer.android.com/develop/ui/views/appwidgets/host
- Android managed profiles: https://developer.android.com/work/managed-profiles
- Lawnchair: https://github.com/LawnchairLauncher/lawnchair
- Kvaesitso: https://github.com/MM2-0/Kvaesitso
- KISS: https://github.com/Neamar/KISS
- KDE Activities: https://docs.kde.org/stable_kf6/en/plasma-desktop/plasma-desktop/activities-interface.html
- Nextcloud: https://github.com/nextcloud
- Joplin: https://github.com/laurent22/joplin
- Syncthing: https://github.com/syncthing/syncthing
- ActivityWatch: https://github.com/ActivityWatch/activitywatch
- OpenTracks: https://github.com/OpenTracksApp/OpenTracks
- Home Assistant: https://github.com/home-assistant/core
- Termux: https://github.com/termux/termux-app
