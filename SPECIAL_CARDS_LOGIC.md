# Logique Complète des Cartes Spéciales

## Principe de base

Les cartes spéciales (+3 et STOP) utilisent une **queue/liste FIFO** pour gérer l'ordre de traitement.

## Structure `PendingSpecialCard`

```java
class PendingSpecialCard {
    SpecialCard card;              // La carte spéciale
    String sourcePlayerId;         // Qui a pioché la carte
    String targetPlayerId;         // À qui elle est assignée (null tant que non assignée)
    int remainingForcedDraws;      // Cartes ENCORE À PIOCHER avant cette carte
}
```

**Note importante** : Pour savoir à qui passer le tour après résolution complète, on regarde le `sourcePlayerId` de la **première carte** qui a déclenché la chaîne (celle qu'on traite actuellement ou qu'on vient de finir).

## Scénario complet (exemple)

### Configuration
- 3 joueurs : **A**, **B**, **C**
- Tour initial : **A**

### Déroulement

1. **A pioche +3**
   - Ajout queue : `[+3(source=A, target=null, remaining=0)]`
   - Tour reste à **A** (doit assigner)
   - Modal s'affiche pour A

2. **A assigne +3 à C**
   - `+3(source=A, target=C, remaining=0)`
   - **C commence à piocher 3 cartes**
   - Tour passe à **C** temporairement

3. **C pioche carte 1/3** : Nombre
   - Continue

4. **C pioche carte 2/3** : Nombre
   - Continue

5. **C pioche carte 3/3** : **+3** ⚠️
   - **Suspension** : On était à 3/3, maintenant 3 cartes restantes = 0
   - Ajout queue : `[+3(source=C, target=null, remaining=0)]`
   - Tour reste à **C** (doit assigner ce nouveau +3)
   - Modal s'affiche pour C

6. **C assigne son +3 à B**
   - `+3(source=C, target=B, remaining=0)`
   - **B commence à piocher 3 cartes**
   - Tour passe à **B** temporairement

7. **B pioche 3 cartes** : 12, 11, 9
   - Aucune carte spéciale
   - **Fin du +3 de C**
   - Queue maintenant vide
   - **Tour retourne à... qui ?**

### ✅ Règle : Qui joue après ?

Après que B ait fini de piocher :
- **Tour revient au joueur APRÈS celui qui a pioché la PREMIÈRE carte spéciale**
- Le premier +3 a été pioché par **A**
- Donc le tour revient à **B** (joueur après A)

**Logique** :
- **A** pioche +3 → tour sauvegardé = A
- Suite au traitement complet de cette carte (même avec imbrications)
- Tour passe au joueur suivant A → **B**

## Algorithme correct

### Quand on pioche une carte normale
```
1. Ajouter la carte à la main
2. Calculer le score
3. Si score > 77 : éliminer
4. Fin du tour
```

### Quand on pioche une carte spéciale (+3 ou STOP)
```
1. Ajouter la carte à la main
2. Créer PendingSpecialCard :
   - source = joueur actuel
   - target = null
   - remainingForcedDraws = 0 (initialement)
3. Ajouter à la queue
4. Arrêter la pioche (return)
5. Attendre que le joueur assigne la carte
```

### Quand on assigne un +3
```
1. Sauvegarder firstSourcePlayerId = source de cette carte (pour retour de tour)
2. Retirer la carte de la queue
3. Définir target = joueur ciblé
4. currentPlayer = target (changer temporairement le tour)
5. Boucle : piocher 3 cartes
   Pour i de 1 à 3:
     - Piocher une carte
     - Si carte normale : continuer
     - Si carte spéciale :
       * SUSPENDRE : cartes restantes = 3 - i
       * Créer nouveau PendingSpecialCard(remaining = 3 - i)
       * Ajouter à la queue
       * ARRÊTER la boucle (return)
       * Le joueur doit assigner cette nouvelle carte
6. Si boucle complète (3 cartes piochées) :
   - Vérifier queue : y a-t-il une carte avec remaining > 0 ?
   - Si oui : reprendre cette pioche (voir ci-dessous)
   - Si non : passer au joueur suivant firstSourcePlayerId
```

### Reprendre une pioche suspendue
```
1. Trouver dans queue : carte avec remaining > 0 ET même sourcePlayer
2. Retirer de la queue
3. currentPlayer = sourcePlayer
4. Boucle : piocher 'remaining' cartes
   (même logique que ci-dessus)
```

### Quand on assigne un STOP
```
1. Retirer la carte de la queue
2. Définir target = joueur ciblé
3. target.status = FORCED_STOP
4. Vérifier queue : y a-t-il une carte avec remaining > 0 ?
   - Si oui : reprendre cette pioche
   - Si non : passer au joueur suivant
```

## Cas particuliers

### +3 pioché à la 3ème carte d'un +3
- C pioche pour finir le +3 de A
- Carte 3/3 = +3
- remaining = 0 (car c'était la dernière)
- C assigne ce +3 à B
- B pioche 3 cartes
- **Retour au tour normal** (pas de remaining)

### +3 pioché à la 1ère carte d'un +3
- C commence à piocher pour le +3 de A
- Carte 1/3 = +3
- remaining = 2 (il reste 2 cartes)
- C assigne ce +3 à B
- B pioche 3 cartes
- **Retour à C pour piocher les 2 cartes restantes**

### STOP pioché pendant un +3
- C pioche carte 2/3 : STOP
- remaining = 1
- C assigne STOP à B
- B.status = FORCED_STOP
- **Retour à C pour piocher 1 carte restante**

## Gestion du tour

### Règle d'or
Le tour change **uniquement** :
1. Quand un joueur STOP (volontairement)
2. Quand un joueur est éliminé (> 77)
3. Quand un joueur fait FLIP7
4. Quand TOUTES les cartes spéciales sont traitées ET aucune pioche en suspens

### Tour temporaire vs Tour réel
- **Tour réel** : A → B → C → A ...
- **Tour temporaire** : Change pendant le traitement d'un +3
- Après traitement : retour au tour réel suivant

## Implémentation

### Variables nécessaires dans Game
```java
Queue<PendingSpecialCard> pendingSpecialCards;  // Queue FIFO
int currentPlayerIndex;                          // Index du joueur actuel
String firstSourcePlayerId;                      // Le joueur qui a pioché la 1ère carte de la chaîne
```

**Note** : `firstSourcePlayerId` est sauvegardé quand on commence à traiter une carte, et réutilisé pour passer le tour à la fin.

### Pas besoin de
- ❌ `player.remainingForcedDraws` (stocké dans la queue)
- ❌ `card.pending` (la présence dans la queue suffit)
- ❌ Champ `originalSourcePlayerId` dans PendingSpecialCard (on utilise une variable temporaire)

## Questions à valider

1. **Après un +3 assigné, qui joue ?**
   - Le joueur qui a SUBI le +3, ou le suivant ?
   - Réponse : Le suivant dans l'ordre normal

2. **Si A assigne +3 à C, et C pioche un +3 qu'il s'assigne à lui-même ?**
   - C pioche 3 cartes supplémentaires
   - Puis retourne finir son premier +3
   - Puis tour passe au suivant

3. **Le tour change-t-il quand on assigne une carte ?**
   - Oui, temporairement, le temps du traitement
   - Puis retour au flux normal

---

**Valides-tu cette logique ?** Si oui, je commence l'implémentation.
