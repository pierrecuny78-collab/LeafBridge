# LeafColour — identité visuelle LeafBridge

LeafBridge adopte la palette LeafColour commune aux applications LeafVibe. Les teintes doivent rester cohérentes sur téléphone, tablette et ordinateur.

## Palette validée

| Jeton | Nom | HEX | Usage |
|---|---|---|---|
| Oasis | Vert Oasis | #206B4D | En-tête, navigation, couleur principale |
| Noyer | Brun Noyer | #6B4935 | Texte principal, boutons et contraste |
| Or naturel | Or naturel | #C5A46D | Accent, éléments mis en évidence |
| Crème foncée | Crème foncée | #DCCBB1 | Fond principal et surfaces claires |
| Brun sable | Brun sable | #B69A7B | Bordures et surfaces secondaires |

## Règles

- Ne pas réintroduire les couleurs explicitement écartées : chocolat noir, vert sauge et ivoire.
- Conserver l'identité LeafVibe, le nom et l'icône de l'application.
- Utiliser les couleurs via `LeafColour.kt`, sans disperser des codes HEX dans les écrans.
- Maintenir un contraste lisible entre texte et arrière-plan.
- Les pages Web affichées dans le navigateur gardent les couleurs du site consulté ; LeafColour habille l'interface native LeafBridge.
- Toute nouvelle page ou tout nouveau bouton doit utiliser les jetons de cette palette.

## État d'intégration

La palette est centralisée dans `app/src/main/java/org/leafvibe/leafbridge/LeafColour.kt` et appliquée à l'enveloppe native de `MainActivity`.

L'intégration des composants présents uniquement dans les APK compilées reste une étape distincte : les noms de classes repérés ne constituent pas leur code source.
