# Moteur LeafBridge pour Termux

## Installation initiale

Depuis Termux :

```sh
pkg update
pkg install python git -y
cd ~/LeafBridge-upload
chmod +x termux/leafbridge-start.sh termux/leafbridge-stop.sh
./termux/leafbridge-start.sh
curl http://127.0.0.1:8765/status
```

L'API écoute exclusivement sur `127.0.0.1:8765` : elle n'est pas exposée au réseau Wi-Fi.

## Commandes

- Démarrer : `~/LeafBridge-upload/termux/leafbridge-start.sh`
- Arrêter : `~/LeafBridge-upload/termux/leafbridge-stop.sh`
- État : `curl http://127.0.0.1:8765/status`
- Journaux : `curl http://127.0.0.1:8765/logs`

Les notifications Shazam reçues sont enregistrées dans `~/.leafbridge/shazam-inbox.jsonl`.

## État des intégrations

La réception locale Shazam est fonctionnelle. Les commandes Spotify, GitHub, classement musical et routine sont déclarées dans l'API, mais restent volontairement désactivées tant que leurs connecteurs et autorisations ne sont pas implémentés. L'API renvoie HTTP 501 pour éviter de prétendre qu'une action a été exécutée.

Le serveur est local et ne réalise aucune synchronisation cloud. Pour un démarrage après redémarrage Android, Termux:Boot pourra être configuré dans une étape séparée.
