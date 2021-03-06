# Rule-sup-002 -  Interdiction de construire dans une zone protégée par les SUP


## Définition

> Les boîtes ne doivent pas intersecter  les zones de protection définies par les SUP

## Paramètres

La manière de prendre en compte cette zone peut être différenciée en fonction de la valeur de **{{TYPEPSC}}** définie dans le fichier (prescXXX.shp)

## Explications

La classe ForbiddenZoneGenerator  va générer des zones en fonction de l'emprise des prescriptions. Pour les objets ponctuels et linéaires, un buffer est généré pour simuler une surface. La valeur est définie dans la classe (ForbiddenZoneGenerator) et  dépend du type de SUP. Les boîtes générées ne devront pas intersecter ces zones là. Pour l'instant, les cas suivants sont considérés :


| ELEMENT_PAYSAGE     | Type                                                                                                          | Valeur {{TYPEPSC}} | Buffer si nécessaires                                                                    | Implémenté                                                                                      |
|:--------------------|:--------------------------------------------------------------------------------------------------------------|:-------------------|:-----------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------|
| protectedWood       | Espace boisé classé (R123-11 a)                                                                               | 1                  | DISTANCERECOILEVEGETATION = 3                                                            | ![#005500](https://placehold.it/15/005500/000000?text=+)<span style="color:green"> Fait </span> |
| riskAll    | Secteur avec limitation de la constructibilité ou de l’occupation pour des raisons de nuisances ou de risques | 2                  | DISTANCERENUISANCERISQUE= 1                                                              | ![#005500](https://placehold.it/15/005500/000000?text=+)<span style="color:green"> Fait </span> |
| EMPLACEMENT_RESERVE | Emplacement réservé                                                                                           | 5                  | DISTANCERECOILERESERVEDEMPLACEMENT = 3                                                   | ![#005500](https://placehold.it/15/005500/000000?text=+)<span style="color:green"> Fait </span> |
| ELEMENT_PAYSAGE     | Elément de paysage (bâti et espaces), à mettre en valeur                                                      | 7                  | DISTANCERECOILPAYSAGE= 3                                                                 | ![#005500](https://placehold.it/15/005500/000000?text=+)<span style="color:green"> Fait</span>  |
| alignment              | Limitations particulières d'implantation des constructions                                                    | 11                 | Dépend de l'attribut **PrescriptionReader.ATT_RECOIL** (la valeur de base est : "Recul") | ![#005500](https://placehold.it/15/005500/000000?text=+)<span style="color:green"> Fait </span> |
| FACADE_ALIGNMENT    | Alignement de façade                                                                                          | 12                 |                                                                                          | Sans objet                                                                                      |
| TVB                 | Limitations particulières d'implantation des constructions                                                    | 25                 | DISTANCETVB = 3                                                                          | ![#005500](https://placehold.it/15/005500/000000?text=+)<span style="color:green"> Fait </span> |
