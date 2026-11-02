# LeafBridge – application Android

LeafBridge écoute les notifications Android provenant de Shazam (`com.shazam.android`)
et transmet leur titre/texte à LeafMusic via HTTP local `127.0.0.1:8765`.

## Limites importantes
- Le service reçoit seulement les notifications effectivement émises par Shazam.
- Il ne lit pas l'historique complet de Shazam.
- Termux doit exécuter le serveur LeafMusic/LeafBridge sur le port 8765.
- La transmission ne fonctionne que lorsque le serveur Termux est joignable.
- Aucune donnée n'est envoyée vers un serveur distant.
- Le test depuis l'application envoie un objet JSON de test au récepteur local.

## Compilation APK sans Android Studio sur le téléphone
1. Créer un dépôt GitHub vide nommé `LeafBridge`.
2. Envoyer le contenu de ce dossier dans la branche `main`.
3. Ouvrir l'onglet **Actions** du dépôt.
4. Lancer **Build LeafBridge APK** (ou attendre la compilation après le push).
5. Ouvrir l'exécution terminée et télécharger l'artefact `LeafBridge-debug`.
6. Extraire l'archive téléchargée et installer `app-debug.apk` sur Android.
7. Ouvrir LeafBridge, appuyer sur « Autoriser les notifications Shazam ».
8. Dans les paramètres Android, autoriser LeafBridge à accéder aux notifications.

## Configuration Termux
Le récepteur doit écouter sur `127.0.0.1:8765` et accepter `POST /shazam`.
