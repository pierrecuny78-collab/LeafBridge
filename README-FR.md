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


## Version 1.1 – interface et moteur Termux

L'application comporte maintenant cinq onglets : Accueil, Automatisations, Web, Connexions et Réglages. Le navigateur intégré utilise des onglets WebView. Les boutons d'automatisation appellent le moteur local via `GET /status` et `POST /automation`.

### Installer le moteur local dans Termux

Le moteur fourni se trouve dans `termux/leafbridge_engine.py`. Il écoute uniquement sur `127.0.0.1:8765`, enregistre les notifications dans `~/LeafBridge/data/shazam-inbox.jsonl` et ne lance que des scripts explicitement autorisés placés dans `~/LeafBridge/automations/`.

Dans Termux :

```bash
pkg install python -y
mkdir -p ~/LeafBridge/automations ~/LeafBridge/data
curl -L https://raw.githubusercontent.com/pierrecuny78-collab/LeafBridge/main/termux/leafbridge_engine.py -o ~/LeafBridge/leafbridge_engine.py
python ~/LeafBridge/leafbridge_engine.py
```

Garde cette session Termux active pour le test, puis ouvre LeafBridge et utilise « Tester la connexion ».

**Important :** si ton ancien récepteur Shazam utilise déjà le port 8765, ne lance pas les deux serveurs en même temps. Il faudra fusionner son traitement existant avec ce moteur avant de remplacer l'ancien processus. Les quatre modules affichés dans l'application sont des points d'entrée : tant que leurs scripts `.sh` ne sont pas installés, le moteur répond « Module non configuré » et ne prétend pas avoir lancé une automatisation.

### Démarrage au redémarrage du téléphone

Après avoir validé le moteur manuellement, tu peux installer Termux:Boot depuis la même source que Termux, puis créer un script dans `~/.termux/boot/` qui démarre le moteur. Il faut d'abord vérifier qu'aucun ancien serveur n'occupe déjà le port 8765.
