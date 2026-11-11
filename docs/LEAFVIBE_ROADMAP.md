# LeafBridge — feuille de route ambitieuse

## Identité
- Icône Android dédiée, palette LeafVibe Oasis #206B4D et Noyer #6B4935 avec accent bleu de connectivité.

## Évolutions proposées
- Hub des appareils : état Bluetooth, Wi-Fi local et USB lorsque les API Android l'autorisent.
- Transfert local avec progression, reprise et vérification SHA-256.
- Historique local des transferts et profils d'appareils.
- Centre de partage pour documents, notes et médias.
- Intégration Termux facultative, isolée et contrôlée par permissions.
- Sauvegarde/restauration du workspace et export portable.

## Sécurité
- Refuser les connexions entrantes par défaut.
- Authentification et confirmation sur les transferts sensibles.
- Permissions minimales, révocables et expliquées.
- Ne pas exposer de serveur réseau sans activation explicite.
- Évaluer la suppression de cleartext HTTP dans un changement séparé après audit des usages.

## Hors ligne et tests
- Priorité aux connexions locales et aux fonctions utilisables sans compte LeafBridge.
- Tests : découverte, transfert interrompu, intégrité, révocation, sauvegarde et permissions.
- Les fonctions proposées restent à implémenter et valider.
