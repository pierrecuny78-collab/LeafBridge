# LeafVibe / LeafBridge — Architecture local-first

## Décision produit

LeafBridge est une application Android locale-first. Les fonctions essentielles doivent rester utilisables sans compte distant et sans connexion Internet. La synchronisation multi-appareils est facultative et ne doit jamais conditionner l'accès aux données locales.

## Identité et accès

- Un profil local LeafVibe est créé sur l'appareil.
- L'utilisateur peut définir un code d'accès local; le code brut ne doit jamais être enregistré.
- Le stockage des secrets doit utiliser Android Keystore lorsque du chiffrement réversible est nécessaire.
- Prévoir le déverrouillage biométrique via AndroidX Biometric / BiometricPrompt, avec repli sur le verrouillage de l'appareil ou le code local.
- Prévoir verrouillage après inactivité, temporisation après échecs répétés, déconnexion/verrouillage manuel et suppression du profil avec confirmation.
- Ne pas présenter ce profil local comme un compte cloud : il n'est pas récupérable sur un autre appareil sans mécanisme de sauvegarde/synchronisation configuré.

## Données locales

- La base locale est la source de vérité pour le profil, les préférences, la bibliothèque, les tâches et l'historique.
- Les opérations d'automatisation sont inscrites dans une file persistante avec états : en attente, en cours, terminée, à réessayer, erreur.
- Chaque opération doit être idempotente afin d'éviter les doublons lors des reprises.
- Les journaux doivent être consultables et ne doivent pas exposer de jetons OAuth, mots de passe ou données sensibles.
- Les exports et sauvegardes locales doivent être explicites et contrôlés par l'utilisateur.

## Connexions externes

- Spotify : OAuth 2.0 Authorization Code avec PKCE pour l'application mobile; aucun mot de passe Spotify dans LeafBridge. Les jetons sont stockés chiffrés et révocables.
- GitHub : autorisation officielle avec permissions minimales, activée seulement lorsque le module est réellement développé.
- Shazam : réception locale des notifications Android, avec file de reprise si le moteur local est momentanément indisponible.
- Les connexions externes sont indépendantes du profil local. Leur absence ne bloque pas la bibliothèque locale.

## Synchronisation facultative

- La synchronisation est désactivée par défaut.
- Le protocole devra chiffrer les données en transit et au repos côté service, gérer les conflits et permettre export/suppression.
- Aucun serveur, fournisseur payant ou abonnement ne doit être obligatoire pour le mode local.
- Ne jamais synchroniser automatiquement les fichiers explicitement marqués privés.

## Modules et automatisations

Chaque module expose un état réel (non configuré, prêt, actif, en pause, erreur) et des capacités déclarées. L'interface ne doit pas afficher un module comme actif tant que son exécution réelle n'a pas été vérifiée.

Pipeline musical cible :
1. Réception d'une notification Shazam.
2. Normalisation du titre et de l'artiste.
3. Déduplication et sauvegarde locale.
4. Classement (métadonnées disponibles, règles locales et corrections manuelles).
5. Ajout Spotify uniquement si l'utilisateur a autorisé le compte et les permissions nécessaires.
6. Journalisation du résultat et reprise contrôlée en cas d'échec.

## Plan d'implémentation

1. Profil local, écran de création/connexion, verrouillage et paramètres de sécurité.
2. Base locale structurée et migrations; remplacer progressivement les fichiers JSON ad hoc.
3. File d'automatisation persistante, reprise et journal d'exécution.
4. Connexion Spotify PKCE et premier flux Shazam → bibliothèque → Spotify.
5. Classement musical, playlists, recherche et règles configurables.
6. Synchronisation multi-appareils facultative, seulement après stabilisation du mode hors ligne.

## Critères d'acceptation

- Démarrer et consulter les données locales en mode avion.
- Verrouiller puis déverrouiller l'application sans réseau.
- Ne jamais enregistrer un PIN ou un mot de passe en clair.
- Redémarrer l'application sans perdre les tâches en attente.
- Distinguer visuellement les fonctions opérationnelles des fonctions non implémentées.
- Une panne Spotify/GitHub ne doit pas bloquer la réception ni la consultation locale des titres.
