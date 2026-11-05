# LeafBridge — application Android + lanceur LeafVibe

Cette branche ajoute une refonte de l’interface et une automatisation locale Shazam → Spotify.

## Fonctionnalités ajoutées

- LeafBridge reste une application Android classique et peut aussi être choisi comme lanceur d’accueil Android (HOME).
- Thème Leaf : bleu nuit, vert et texte clair.
- Espaces Personnel et Professionnel accessibles par balayage horizontal sur l’accueil.
- Recherche web intégrée, onglets, bibliothèque persistante, catégories personnalisées, favoris, suppression et choix du navigateur.
- Catalogue des applications lançables, fiche détaillée avec bouton « Ouvrir l’application » en haut, widgets LeafBridge, widgets Android déclarés et liens associés.
- Dock d’automatisations avec schéma de flux Shazam → traitement → recherche Spotify → playlist.
- Capture des notifications Shazam en arrière-plan : aucun lancement de Shazam ni ouverture de la notification.
- OAuth Spotify avec PKCE ; jetons transmis au moteur local Termux. Le moteur recherche le titre, l’ajoute à la playlist et évite de réajouter une URI déjà ajoutée.

## Installer le moteur Termux

Le moteur se trouve dans `termux/leafbridge_engine.py`. Il écoute uniquement sur `127.0.0.1:8765`.

Dans Termux, arrêter d’abord l’ancien moteur qui utilise éventuellement ce port, puis sauvegarder son fichier et installer celui de cette branche :

```bash
mkdir -p ~/LeafBridge/automations ~/LeafBridge/data
cp ~/LeafBridge/leafbridge_engine.py ~/LeafBridge/leafbridge_engine.py.bak 2>/dev/null || true
curl -L https://raw.githubusercontent.com/pierrecuny78-collab/LeafBridge/fix/leafbridge-stability/termux/leafbridge_engine.py -o ~/LeafBridge/leafbridge_engine.py
python -m py_compile ~/LeafBridge/leafbridge_engine.py
python ~/LeafBridge/leafbridge_engine.py
```

Ne lance pas deux moteurs simultanément sur le port 8765. Le moteur doit rester actif pour traiter les notifications et transmettre les titres à Spotify.

## Configurer Spotify

1. Créer une application dans le tableau de bord Spotify Developer.
2. Dans ses paramètres, ajouter exactement l’URI de redirection : `leafbridge://spotify-callback`.
3. Dans LeafBridge → Comptes → Spotify, saisir le Client ID et l’identifiant de la playlist, puis appuyer sur « Connecter Spotify ».
4. Accepter les autorisations de modification de playlist demandées par Spotify.
5. Laisser le moteur Termux actif. Les titres Shazam reçus avant la connexion restent dans la file locale et sont retraités lors de la configuration Spotify.

La playlist préremplie dans l’interface est « Mes titres Shazam » (ID `3cPjGCqbeDaIH95Vvofjwm`) ; elle peut être remplacée. LeafBridge ne demande jamais le mot de passe Spotify.

## Activer Shazam en arrière-plan

1. Ouvrir LeafBridge → Automatisations.
2. Activer « Shazam vers Spotify ».
3. Appuyer sur « Autoriser l’accès Shazam » et activer l’accès aux notifications pour LeafBridge dans Android.
4. Vérifier que le moteur Termux répond dans le tableau de bord.

Le service lit uniquement les notifications émises par Shazam après son activation. Il ne récupère pas l’historique complet de Shazam.

## Choisir LeafBridge comme écran d’accueil

Ouvrir Réglages → « Choisir LeafBridge comme lanceur par défaut », puis sélectionner LeafBridge dans la liste Android. L’icône d’application classique reste disponible dans le tiroir d’applications.

## Limites connues de cette version

- Les espaces Personnel et Professionnel sont des espaces d’organisation avec raccourcis distincts ; ce ne sont pas des coffres chiffrés ni des profils Android séparés.
- Les « widgets LeafBridge » de cette version sont des raccourcis internes aux fiches et espaces LeafBridge. Les widgets Android natifs sont simplement recensés par application.
- GitHub, Instagram et TikTok sont présentés dans Comptes, mais leurs intégrations OAuth et leurs automatisations complètes restent à développer.
- La forme exacte des notifications Shazam dépend de la version de Shazam et de la langue Android. Une notification inhabituelle peut être conservée dans la file locale sans correspondance Spotify automatique.
- Les modules GitHub Sync, Classement musical et Routine restent des points d’entrée : ils nécessitent leurs scripts autorisés dans `~/LeafBridge/automations/`.

## Compilation APK

Ouvrir Actions → Build LeafBridge APK, puis télécharger l’artefact `LeafBridge-debug` de la dernière exécution réussie. Cette branche est une version de test ; elle n’est pas encore fusionnée dans `main`.


## Téléchargement direct du paquet Preview

Après une compilation CI réussie, le fichier est publié dans [`downloads/LeafBridge-preview.apk`](https://github.com/pierrecuny78-collab/LeafBridge/blob/fix/leafbridge-stability/downloads/LeafBridge-preview.apk).
