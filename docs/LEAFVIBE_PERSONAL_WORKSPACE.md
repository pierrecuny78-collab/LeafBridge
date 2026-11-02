# LeafVibe — environnement de travail personnel

## Navigation par balayage
Le launcher comporte quatre espaces horizontaux persistants dans la navigation :
1. **Organisation** (gauche) : tâches/suivi via LeafBridge, création de notes et documents, calendrier et raccourcis de classement.
2. **Bureau principal** : point d'entrée, applications, réglages et dock.
3. **Vie privée** (droite) : fichiers personnels, agenda, communication et médias.
4. **Vie professionnelle** (extrême droite) : projets LeafVibe, documents de travail, outils et communication professionnelle.

Le balayage horizontal change d'espace. Les raccourcis d'applications sont proposés depuis les applications réellement installées. Un appui long sur une application l'épingle au dock commun.

## Cloisonnement des espaces
Cette version sépare les espaces au niveau de l'interface et des raccourcis. Elle ne prétend pas créer deux comptes Android isolés : les applications, leurs comptes et leurs données restent gérés par Android. Une séparation forte des données nécessite des profils Android distincts (profil professionnel Android Enterprise ou utilisateur secondaire) et une gestion explicite des autorisations.

## Actions actuellement prévues dans l'interface
- Ouvrir LeafBridge et ses modules.
- Lancer le sélecteur de fichiers Android pour ouvrir ou créer un document.
- Ouvrir le calendrier disponible sur l'appareil.
- Filtrer le tiroir d'applications par catégories.
- Personnaliser le thème et le fond d'écran.
- Épingler des applications au dock.

## Limites Android et étapes suivantes
- Les widgets tiers ne sont pas encore intégrés dans les panneaux. Pour cela, LeafVibe doit implémenter un véritable `AppWidgetHost`, l'allocation et la persistance des IDs, le flux d'autorisation utilisateur, la configuration des widgets et leur suppression.
- Les raccourcis profonds d'applications dépendent des raccourcis publiés par chaque application.
- Les fenêtres libres ne sont pas garanties sur tous les appareils Samsung/Android. Le multitâche système reste contrôlé par Android/One UI.
- Le cloisonnement privé/pro devra s'appuyer sur les profils Android si une isolation réelle est requise.
