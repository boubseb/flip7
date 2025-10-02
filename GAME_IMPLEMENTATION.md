# 🎮 Implémentation du Jeu Flip7 - Backend

## ✅ Structure des Cartes

### Hiérarchie des Classes
```
Card (classe abstraite)
├── NumberCard (0-12)
├── OperatorCard (+2, +4, +6, +8, +10, ×2)
└── SpecialCard (Stop, Draw3, Life)
```

### Composition du Deck (94 cartes)
- **79 cartes numérotées** : 12×"12", 11×"11", ..., 2×"2", 1×"1", 1×"0"
- **6 cartes opérateurs** : 1×(×2), 1×(+2), 1×(+4), 1×(+6), 1×(+8), 1×(+10)
- **9 cartes spéciales** : 3×Stop, 3×Draw3, 3×Life

---

## ✅ Modèles du Jeu

### GamePlayer
- Main de cartes
- Statut (PLAYING, STOPPED, ELIMINATED, WAITING)
- Scores (round + total)
- Gestion des cartes Vie
- Détection des doubles
- Détection des 7 cartes différentes

### Game
- Gestion du deck et de la défausse
- Distribution initiale (1 carte par joueur)
- Tour par tour
- Actions : `drawCard()`, `stopDrawing()`, `playSpecialCard()`
- Conditions de fin de round :
  - Tous les joueurs stoppés/éliminés
  - Un joueur a 7 cartes numérotées différentes
- Condition de victoire : 200 points

---

## ✅ Historique des Parties (GameHistory)

### Entity `GameHistory`
Sauvegarde **TOUTES les parties** (terminées ou non) dans MongoDB :

**Informations principales** :
- ID de la room
- Liste des joueurs
- Dates de début/fin
- Statut (IN_PROGRESS, COMPLETED, ABANDONED)
- ID du gagnant
- Nombre total de rounds

**Détails par round** :
- Numéro du round
- Dates début/fin
- Données de chaque joueur :
  - Score du round
  - Éliminé ou non
  - A stoppé ou non
  - Victoire avec 7 cartes différentes
  - Nombre de cartes piochées
  - Carte Vie utilisée

**Scores finaux** :
- Score total de chaque joueur
- Rounds gagnés
- Rounds joués (non éliminé)

### Repository `GameHistoryRepository`
Requêtes disponibles :
- `findByRoomId()` - Historique d'une room
- `findByPlayerIdsContaining()` - Parties d'un joueur
- `findByWinnerId()` - Parties gagnées
- `findByStatus()` - Parties en cours/terminées
- `findByStartedAtBetween()` - Parties dans une période

---

## ✅ Services

### GameService
**Gestion du jeu** :
- `initializeGame()` - Crée une nouvelle partie
- `startNewRound()` - Démarre un round
- `drawCard()` - Un joueur pioche
- `stopDrawing()` - Un joueur s'arrête
- `playSpecialCard()` - Jouer une carte spéciale
- `getGame()` - État actuel de la partie

**Sauvegarde automatique** :
- `saveRoundStart()` - Début de chaque round
- `saveRoundEnd()` - Fin de chaque round (scores, stats)
- `saveGameEnd()` - Fin de partie (gagnant, scores finaux)

**Statistiques** :
- `getPlayerStats()` - Stats d'un joueur :
  - Parties jouées/gagnées
  - Score total/moyen
  - Taux de victoire
  - Rounds joués/gagnés

**Broadcasting WebSocket** :
- `/topic/rooms/{id}/game` - État du jeu
- `/topic/rooms/{id}/round-end` - Fin de round
- `/topic/rooms/{id}/game-over` - Fin de partie

---

## ✅ Contrôleurs REST

### GameController (`/api/game`)
- `POST /{roomId}/start-round` - Démarrer un round
- `POST /{roomId}/draw` - Piocher une carte
- `POST /{roomId}/stop` - Arrêter de piocher
- `POST /{roomId}/play-special` - Jouer une carte spéciale
- `GET /{roomId}/state` - État actuel du jeu

### GameHistoryController (`/api/history`)
- `GET /room/{roomId}` - Historique d'une room
- `GET /player/{playerId}` - Historique d'un joueur
- `GET /me` - Mon historique
- `GET /player/{playerId}/stats` - Stats d'un joueur
- `GET /me/stats` - Mes stats

---

## 🎯 Règles Implémentées

### Début de round
1. Distribution de 1 carte à chaque joueur
2. Délai de 5 secondes entre distributions (TODO frontend)

### Tour de jeu
1. Le joueur choisit : **PIOCHER** ou **STOP**
2. Si pioche :
   - Tire une carte
   - Vérification double → Éliminé (sauf si carte Vie)
   - Vérification 7 cartes différentes → Victoire du round

### Cartes spéciales
- **Stop** : Force un joueur à arrêter
- **+3** : Force à piocher 3 cartes (vérifie les doubles après chaque carte)
- **Vie** : Consommée automatiquement lors d'un double

### Calcul du score
1. Somme des cartes numérotées
2. Ajout des bonus (+2, +4, +6, +8, +10)
3. Application du ×2 à la fin (si présent)

### Fin de round
- Tous stoppés/éliminés
- Ou un joueur avec 7 cartes différentes

### Fin de partie
- Premier joueur à 200 points

---

## 📊 Avantages du Système d'Historique

✅ **Persistance totale** : Aucune partie n'est perdue
✅ **Statistiques riches** : Analyse détaillée des performances
✅ **Séparation room/game** : Une room peut héberger plusieurs parties
✅ **Parties incomplètes** : Sauvegardées automatiquement (status IN_PROGRESS)
✅ **Historique détaillé** : Round par round avec toutes les actions
✅ **Calculs automatiques** : Rounds gagnés, taux de victoire, etc.

---

## 🚀 Prochaines Étapes

### Backend
- [ ] Implémenter le délai de 5 secondes entre distributions
- [ ] Récupérer les vrais noms des joueurs depuis UserService
- [ ] Gérer l'abandon de partie (mettre status ABANDONED)
- [ ] Tests unitaires

### Frontend (Angular)
- [ ] Modèles TypeScript pour les cartes et le jeu
- [ ] Service GameService (HTTP + WebSocket)
- [ ] Composants d'affichage :
  - `game-board` - Plateau de jeu
  - `player-hand` - Main du joueur
  - `player-card` - Carte individuelle
  - `score-board` - Tableau des scores
- [ ] Animations de cartes
- [ ] Historique et statistiques
- [ ] Effets sonores

---

**Système complet de jeu Flip7 avec historique persistant ! 🎉**
