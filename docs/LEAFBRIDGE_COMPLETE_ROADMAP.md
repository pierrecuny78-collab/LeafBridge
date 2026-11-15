# LeafBridge Complete — suggestions et feuille de route

> Document de conception. Ce fichier ne signifie pas que les fonctionnalités listées sont déjà implémentées dans l'application.

## Configuration validée
- Fonctionnement : hybride (fonctions locales hors ligne + fonctions Internet facultatives).
- Interface : avancée.
- IA : hybride (locale et fournisseur externe facultatif).
- Accès distant : avancé, uniquement avec authentification, chiffrement et consentement explicite.
- Extensions demandées : Clipboard, Remote, Screen, Vault, Discovery, Web, Scheduler et Recovery.

## Objectif
Faire de LeafBridge l'application Android centrale de communication et de gestion des appareils de l'écosystème LeafVibe, tout en conservant l'indépendance des autres projets Leaf.

## Modules à intégrer

| Module | Fonction attendue | Contraintes principales |
|---|---|---|
| LeafHome | Tableau de bord, état des appareils, raccourcis, activités | Tous les contrôles doivent avoir une action réelle |
| LeafConnect | Bluetooth, BLE, Wi-Fi/réseau local, USB OTG | Respect des API et autorisations Android |
| LeafDiscovery | Découverte locale et appareils connus | Appairage explicite, pas de scan furtif |
| LeafTransfer | Envoi/réception, dossiers, file d'attente, reprise, SHA-256 | Gestion des interruptions et conflits |
| LeafApps | Catalogue des APK et applications Leaf | Installation via le mécanisme Android, validation de signature, consentement |
| LeafClipboard | Partage de texte entre appareils autorisés | Exclure les secrets et prévoir expiration/effacement |
| LeafRemote | Commandes à distance et sessions | Authentification forte, chiffrement, révocation et journal |
| LeafScreen | Partage/capture d'écran | Consentement MediaProjection et indicateur de session |
| LeafVault | Coffre-fort local chiffré | Clés protégées, verrouillage, récupération documentée |
| LeafWeb | Tableau de bord web local | Serveur désactivable, authentification, TLS si exposé |
| LeafScheduler | Tâches planifiées | Respect des restrictions arrière-plan Android |
| LeafRecovery | Diagnostic, sauvegarde et restauration | Sauvegarde avant réparation, aucune suppression silencieuse |
| LeafAutomation | Scénarios et déclencheurs | Permissions minimales et validation des actions sensibles |
| LeafAI | Assistant local + services externes facultatifs | Pas de privilèges automatiques, données minimisées |
| LeafSecurity | Permissions par module, appareils de confiance, journaux | Refus par défaut et révocation simple |

## Gestionnaire des applications Leaf
Ajouter un bouton principal « Applications Leaf » donnant accès à un catalogue intégré :
- applications disponibles, installées et mises à jour ;
- recherche, catégories, version et compatibilité ;
- vérification des signatures/empreintes et origine du paquet ;
- installation et mise à jour avec confirmation Android ;
- état de téléchargement et historique ;
- conservation des données et avertissement en cas de signature incompatible.

Le catalogue ne doit pas contourner les protections Android ni installer silencieusement des APK.

## Architecture recommandée
- Kotlin et composants Android natifs ; Jetpack Compose si compatible avec la base existante.
- Architecture par modules/features, avec interfaces stables.
- Couche de communication indépendante des écrans.
- Stockage local (Room/DataStore selon les données).
- Tâches planifiées via WorkManager lorsque adapté.
- API locale versionnée pour les autres projets Leaf.
- Compagnon Windows/Linux ultérieur pour les fonctions nécessitant un agent sur ordinateur.

Les choix technologiques définitifs doivent être confirmés après audit du code source et des versions Gradle existantes.

## Plan de réalisation
1. Audit des deux APK et des sources : manifestes, package, activités, services, receivers, ressources, permissions, signatures, versions et dépendances.
2. Cartographie des fonctions et détection des doublons/conflits.
3. Fusion dans un projet source unique : préserver les fonctionnalités utiles, résoudre les collisions de classes/ressources et adapter le manifeste.
4. Stabilisation du noyau : navigation, paramètres, stockage, journaux et gestion des erreurs.
5. Communications et découverte.
6. Transferts et synchronisation.
7. Catalogue LeafApps.
8. Sécurité et Vault.
9. Clipboard, Remote, Screen et Web.
10. Scheduler, Automation, AI et Recovery.
11. Tests de régression, sécurité, compatibilité et consommation.
12. Build APK debug, validation sur appareils, puis build release signé.

## Critères de validation
- Installation et démarrage sans crash.
- Toutes les activités/services déclarés sont accessibles et cohérents.
- Chaque bouton déclenche une action réelle ou est clairement désactivé avec une explication.
- Bluetooth, réseau et USB testés sur matériel compatible.
- Transfert réel vérifié par SHA-256 et reprise après interruption.
- Permissions refusées proprement et expliquées.
- Remote et Screen nécessitent consentement et peuvent être arrêtés immédiatement.
- Vault chiffré, verrouillage et restauration testés.
- Mise à jour sans perte de données ; signature cohérente.
- Fonctionnement hors ligne des fonctions locales.
- Rapport de tests et liste des limitations publiés avec chaque APK.

## Audit et fusion des deux APK actuellement fournies
Les premières inspections ont repéré dans les deux paquets des classes communes, notamment `MainActivity` et `ShazamNotificationListener`. La deuxième APK contient également des classes repérées telles que `LeafHomeActivity`, `LeafBrowserActivity`, `ExtensionLibraryActivity`, `LeafBridgeWidget`, `WorkspaceBackup` et `LocalProfileStore`.

Ces noms de classes sont des indices d'inventaire, pas une preuve que les fonctions sont opérationnelles. Il faut les comparer au niveau du code source, des ressources, du manifeste et des dépendances avant de décider quoi fusionner. Il ne faut pas assembler les APK en concaténant leurs fichiers DEX : la fusion doit se faire au niveau du projet Android source puis être recompilée et signée.

## Éléments de livraison
- Projet source fusionné.
- APK de test debug.
- APK release signée avec clé conservée séparément.
- Rapport d'audit des deux APK.
- Rapport de tests et limitations connues.
- Guide d'installation, mise à jour et sauvegarde.

## État
**À faire :** audit complet des sources, fusion réelle, implémentation des modules, compilation et tests.  
Aucune fonctionnalité décrite ici ne doit être présentée comme déjà livrée tant que les critères correspondants ne sont pas validés.
