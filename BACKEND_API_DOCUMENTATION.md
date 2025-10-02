# 🎮 API Backend Flip7 - Documentation Complète

## 📋 Table des Matières
1. [Endpoints de Jeu](#endpoints-de-jeu)
2. [Endpoints d'Historique](#endpoints-dhistorique)
3. [WebSocket Topics](#websocket-topics)
4. [Modèles de Données](#modèles-de-données)
5. [Règles du Jeu](#règles-du-jeu)

---

## 🎯 Endpoints de Jeu

### Base URL: `/api/game`

#### 1. Démarrer un nouveau round
```http
POST /api/game/{roomId}/start-round
Authorization: Bearer {userId}
```
**Réponse:**
```json
{
  "message": "Round démarré"
}
```
**Comportement:**
- Distribue 1 carte à chaque joueur
- Délai de 5 secondes entre chaque distribution (effet visuel)
- Broadcast WebSocket de chaque distribution
- Sauvegarde le round dans l'historique

---

#### 2. Piocher une carte
```http
POST /api/game/{roomId}/draw
Authorization: Bearer {userId}
```
**Réponse:**
```json
{
  "success": true,
  "message": "Carte piochée",
  "card": {
    "id": "uuid",
    "type": "NUMBER",
    "displayName": "7",
    "value": 7
  },
  "roundEnded": false,
  "lifeUsed": false,
  "eliminated": false
}
```
**Cas particuliers:**
- **Double détecté:** `eliminated: true` (ou `lifeUsed: true` si carte Vie)
- **7 cartes différentes:** `roundEnded: true` + message "7 cartes différentes !"
- **Pas votre tour:** `success: false`

---

#### 3. Arrêter de piocher
```http
POST /api/game/{roomId}/stop
Authorization: Bearer {userId}
```
**Réponse:**
```json
{
  "success": true,
  "message": "Vous avez décidé de vous arrêter"
}
```

---

#### 4. Jouer une carte spéciale
```http
POST /api/game/{roomId}/play-special
Authorization: Bearer {userId}
Content-Type: application/json

{
  "cardId": "uuid-de-la-carte",
  "targetPlayerId": "uuid-du-joueur-cible"
}
```
**Réponse:**
```json
{
  "success": true,
  "message": "John Doe a été forcé de s'arrêter"
}
```
**Cartes spéciales:**
- **Stop:** `targetPlayerId` arrête de piocher
- **+3:** `targetPlayerId` pioche 3 cartes
- **Vie:** Utilisée automatiquement lors d'un double

---

#### 5. Récupérer l'état du jeu
```http
GET /api/game/{roomId}/state
Authorization: Bearer {userId}
```
**Réponse:**
```json
{
  "gameState": "PLAYING",
  "roundNumber": 2,
  "currentPlayerIndex": 1,
  "remainingCards": 78,
  "players": [
    {
      "userId": "uuid",
      "username": "John Doe",
      "status": "PLAYING",
      "totalScore": 45,
      "roundScore": 12,
      "handSize": 3,
      "hand": [/* cartes si c'est vous */]
    }
  ]
}
```
**Note:** Les cartes complètes (`hand`) ne sont envoyées que pour le joueur connecté.

---

#### 6. Abandonner la partie
```http
POST /api/game/{roomId}/abandon
Authorization: Bearer {userId}
Content-Type: application/json

{
  "reason": "Raison de l'abandon (optionnel)"
}
```
**Réponse:**
```json
{
  "message": "Partie abandonnée"
}
```
**Comportement:**
- Marque la partie comme `ABANDONED` dans l'historique
- Sauvegarde les scores actuels
- Broadcast WebSocket `/topic/rooms/{id}/game-abandoned`

---

#### 7. Quitter la partie
```http
POST /api/game/{roomId}/leave
Authorization: Bearer {userId}
```
**Réponse:**
```json
{
  "message": "Vous avez quitté la partie"
}
```
**Comportement:**
- Si tous les joueurs quittent → Abandon automatique
- Si 1 seul joueur reste → Abandon automatique

---

## 📊 Endpoints d'Historique

### Base URL: `/api/history`

#### 1. Historique d'une room
```http
GET /api/history/room/{roomId}
```
**Réponse:** Liste de `GameHistory`

---

#### 2. Historique d'un joueur
```http
GET /api/history/player/{playerId}
```
**Réponse:** Liste de `GameHistory`

---

#### 3. Mon historique
```http
GET /api/history/me
Authorization: Bearer {userId}
```
**Réponse:** Liste de `GameHistory`

---

#### 4. Statistiques d'un joueur
```http
GET /api/history/player/{playerId}/stats
```
**Réponse:**
```json
{
  "playerId": "uuid",
  "totalGamesPlayed": 15,
  "totalWins": 7,
  "totalScore": 2450,
  "averageScore": 163,
  "winRate": 46.67,
  "totalRoundsPlayed": 45,
  "totalRoundsWon": 12
}
```

---

#### 5. Mes statistiques
```http
GET /api/history/me/stats
Authorization: Bearer {userId}
```
**Réponse:** Même format que ci-dessus

---

## 🔔 WebSocket Topics

### Connexion
```javascript
const socket = new SockJS('http://localhost:3200/ws');
const stompClient = Stomp.over(socket);
```

### Topics à écouter

#### 1. Distribution des cartes (progressive)
```
/topic/rooms/{roomId}/card-distributed
```
**Payload:**
```json
{
  "playerIndex": 0,
  "playerId": "uuid",
  "playerName": "John Doe"
}
```
**Fréquence:** 1 événement toutes les 5 secondes pendant la distribution

---

#### 2. Round prêt
```
/topic/rooms/{roomId}/round-ready
```
**Payload:**
```json
{
  "message": "Le round commence !"
}
```

---

#### 3. État du jeu mis à jour
```
/topic/rooms/{roomId}/game
```
**Payload:** `GameStateDTO` (même format que GET `/state`)

---

#### 4. Fin de round
```
/topic/rooms/{roomId}/round-end
```
**Payload:**
```json
{
  "roundNumber": 3,
  "playerScores": [
    {
      "userId": "uuid",
      "username": "John Doe",
      "roundScore": 35,
      "totalScore": 145,
      "eliminated": false
    }
  ]
}
```

---

#### 5. Fin de partie
```
/topic/rooms/{roomId}/game-over
```
**Payload:**
```json
{
  "winnerId": "uuid",
  "winnerName": "John Doe",
  "winningScore": 210,
  "finalScores": [
    {
      "userId": "uuid",
      "username": "John Doe",
      "totalScore": 210
    }
  ]
}
```

---

#### 6. Partie abandonnée
```
/topic/rooms/{roomId}/game-abandoned
```
**Payload:**
```json
{
  "message": "Tous les joueurs ont quitté la partie"
}
```

---

## 📦 Modèles de Données

### GameHistory
```json
{
  "id": "uuid",
  "roomId": "uuid",
  "playerIds": ["uuid1", "uuid2"],
  "startedAt": "2025-10-02T14:30:00",
  "endedAt": "2025-10-02T15:15:00",
  "status": "COMPLETED",
  "winnerId": "uuid1",
  "totalRounds": 5,
  "rounds": [
    {
      "roundNumber": 1,
      "startedAt": "2025-10-02T14:30:00",
      "endedAt": "2025-10-02T14:35:00",
      "roundWinnerId": "uuid1",
      "playerData": [
        {
          "playerId": "uuid1",
          "username": "John Doe",
          "roundScore": 42,
          "eliminated": false,
          "stopped": true,
          "hasSevenDifferent": false,
          "cardsDrawn": 5,
          "usedLife": false
        }
      ]
    }
  ],
  "finalScores": [
    {
      "playerId": "uuid1",
      "username": "John Doe",
      "totalScore": 210,
      "roundsWon": 3,
      "roundsPlayed": 5
    }
  ]
}
```

### Card (Union Type)
```typescript
// NumberCard
{
  "id": "uuid",
  "type": "NUMBER",
  "displayName": "7",
  "value": 7
}

// OperatorCard
{
  "id": "uuid",
  "type": "OPERATOR",
  "displayName": "×2",
  "operatorType": "MULTIPLY_2"
}

// SpecialCard
{
  "id": "uuid",
  "type": "SPECIAL",
  "displayName": "STOP",
  "specialType": "STOP"
}
```

---

## 🎲 Règles du Jeu

### Objectif
Atteindre **200 points** en plusieurs rounds

### Déroulement d'un round
1. Distribution de 1 carte à chaque joueur (5s entre chaque)
2. Tour par tour :
   - **PIOCHER** ou **STOP**
   - Si pioche → Vérifier double/7 différentes
3. Fin quand tous stoppés/éliminés ou 7 cartes différentes

### Élimination
- 2 cartes numérotées identiques = Éliminé (score du round = 0)
- **Exception:** Carte Vie = Protection (consommée)

### Calcul du score
```
Score = (Somme cartes numérotées + Bonus) × Multiplicateur
```
- **Bonus:** +2, +4, +6, +8, +10
- **Multiplicateur:** ×2 si présent

### Cartes spéciales
- **Stop:** Force un joueur à arrêter
- **+3:** Force à piocher 3 cartes (risque de double)
- **Vie:** Protection automatique contre 1 double

### Fin de round
1. Tous les joueurs stoppés/éliminés
2. OU un joueur a 7 cartes numérotées **différentes**

### Fin de partie
Premier joueur à **200 points**

---

## 🔐 Authentification

Toutes les requêtes nécessitent un header :
```http
Authorization: Bearer {userId}
```

Le `userId` est récupéré après login via `/api/users/login`

---

## ✅ Statuts du Jeu

### GameState
- `WAITING` - En attente de joueurs
- `DISTRIBUTING` - Distribution en cours
- `PLAYING` - Round en cours
- `ROUND_ENDED` - Round terminé
- `GAME_OVER` - Partie terminée

### PlayerStatus
- `PLAYING` - En train de jouer
- `STOPPED` - A décidé de s'arrêter
- `ELIMINATED` - Éliminé (double)
- `WAITING` - En attente

### GameHistory.GameStatus
- `IN_PROGRESS` - Partie en cours
- `COMPLETED` - Partie terminée
- `ABANDONED` - Partie abandonnée

---

**Backend Flip7 100% fonctionnel ! 🎉**
