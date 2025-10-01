# 🎮 Système Multi-Room - Flip7 Game

## ✅ Backend (Spring Boot) - Créé

### 1. Entities
- **`Room.java`** - Entité principale des rooms
  - `id` (UUID généré automatiquement)
  - `password` (hashé avec BCrypt)
  - `adminId` (créateur de la room)
  - `players` (List<String> des IDs joueurs)
  - `status` (WAITING, IN_GAME, FINISHED)
  - `turnIndex` (index du joueur actuel)
  - `gameState` (JSON string pour sauvegarder l'état)
  - `maxPlayers` (4 par défaut)
  - `createdAt` (timestamp)

### 2. DTOs
- **`RoomCreateRequest.java`** - Pour créer une room
  - password, maxPlayers
- **`RoomJoinRequest.java`** - Pour rejoindre une room
  - roomId, password
- **`RoomResponse.java`** - Réponse API (sans mot de passe)
  - Calcule automatiquement `currentPlayerId` depuis turnIndex

### 3. Repository
- **`RoomRepository.java`** (JPA)
  - `findByStatus(RoomStatus)` - Trouver rooms par statut
  - `findByAdminId(String)` - Rooms d'un admin
  - `findByIdAndPassword(String, String)` - Vérifier accès

### 4. Service
- **`RoomService.java`** - Logique métier complète
  - ✅ `createRoom()` - Crée room avec password hashé (BCrypt)
  - ✅ `joinRoom()` - Vérifie password, ajoute joueur, check room full
  - ✅ `startGame()` - Admin lance la partie (min 2 joueurs)
  - ✅ `playTurn()` - Gère les tours (vérifie que c'est le bon joueur)
  - ✅ `getRoom()` - Récupère info room
  - ✅ `getAvailableRooms()` - Liste des rooms en attente
  - 🔔 Broadcasting WebSocket automatique sur tous les événements

### 5. Controller
- **`RoomController.java`** - REST API
  - `POST /api/rooms/create` - Créer une room
  - `POST /api/rooms/join` - Rejoindre avec ID + password
  - `GET /api/rooms/{roomId}` - Détails d'une room
  - `GET /api/rooms/available` - Liste des rooms disponibles
  - `POST /api/rooms/{roomId}/start` - Admin démarre la partie
  - `POST /api/rooms/{roomId}/play` - Jouer son tour

### 6. Configuration
- **`WebSocketConfig.java`** - WebSocket + STOMP
  - Endpoint: `/ws` (avec SockJS fallback)
  - Broker prefix: `/topic` (pour recevoir)
  - App prefix: `/app` (pour envoyer)
  - CORS autorisé: `http://localhost:4200`

### 7. Dépendances ajoutées (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

---

## ✅ Frontend (Angular) - Créé

### 1. Models
- **`room.model.ts`** - Interfaces TypeScript
  - `Room` interface complète
  - `RoomStatus` enum
  - `RoomCreateRequest`, `RoomJoinRequest`
  - `RoomCreateResponse`, `TurnUpdate`

### 2. Services
- **`room.service.ts`** - HTTP Client
  - `createRoom()`
  - `joinRoom()`
  - `getRoom()`
  - `getAvailableRooms()`
  - `startGame()`
  - `playTurn()`
  - Headers automatiques avec Bearer token

- **`websocket.service.ts`** - WebSocket Client (STOMP)
  - ✅ `connect()` / `disconnect()`
  - ✅ `subscribeToRoom(roomId)` - Écoute 3 topics:
    - `/topic/rooms/{id}` - Mises à jour room (joueur rejoint)
    - `/topic/rooms/{id}/start` - Partie démarre
    - `/topic/rooms/{id}/turn` - Tour joué
  - ✅ `unsubscribeFromRoom(roomId)`
  - 📡 Observables RxJS pour chaque type d'événement
  - 🔄 Reconnexion automatique toutes les 5s

### 3. Page de jeu
- **`game-page.component.ts`** - Logique complète
  - 3 vues : `lobby`, `room`, `game`
  - Gestion connexion WebSocket
  - CRUD rooms (create, join, leave)
  - Détection admin / tour actuel
  - Subscriptions RxJS pour temps réel
  
- **`game-page.component.html`** - Interface UI complète
  - 🏠 **Vue Lobby** :
    - Formulaire création room (password + max players)
    - Formulaire rejoindre room (ID + password)
    - Liste des rooms disponibles avec refresh
  - 🏠 **Vue Room (Waiting)** :
    - Liste des joueurs connectés
    - Badge admin / badge "Vous"
    - Bouton "Lancer la partie" (admin only, min 2 joueurs)
  - 🎮 **Vue Game** :
    - Indicateur de tour (avec animation glow)
    - Barre des joueurs avec avatar
    - Plateau de jeu (placeholder pour l'instant)
    - Bouton "Jouer" actif uniquement pendant son tour

- **`game-page.component.scss`** - Styling moderne
  - Gradient buttons (violet/bleu)
  - Status connection indicator (top-right)
  - Cards avec hover effects
  - Animations (pulse, glow)
  - Responsive layout

### 4. Routes
- **`app.routes.ts`** - Route ajoutée
  - `/game` → `GamePageComponent`

### 5. Dépendances installées (package.json)
```bash
npm install sockjs-client @stomp/stompjs
npm install --save-dev @types/sockjs-client
```

---

## 🔔 WebSocket Topics

### Backend → Frontend (Broadcasting)

1. **`/topic/rooms/{roomId}`** - Mises à jour room
   - ✅ Nouveau joueur rejoint
   - ✅ Joueur quitte
   - ✅ État room change

2. **`/topic/rooms/{roomId}/start`** - Partie démarre
   - ✅ Admin a lancé la partie
   - ✅ Status passe de WAITING → IN_GAME

3. **`/topic/rooms/{roomId}/turn`** - Tour joué
   - ✅ Un joueur a joué
   - ✅ turnIndex mis à jour
   - ✅ gameState mis à jour
   - ✅ Payload: `{ room: Room, moveData: string }`

---

## 🚀 Comment tester

### 1. Backend
```bash
cd backend
.\mvnw spring-boot:run
```
✅ Serveur sur `http://localhost:3200`

### 2. Frontend
```bash
cd frontend
ng serve
```
✅ App sur `http://localhost:4200`

### 3. Flow de test

1. **Login** → Obtenir un token (userID)
2. **Aller sur `/game`**
3. **Créer une room** → Entrer un password
4. **Dans un autre navigateur** :
   - Login avec un autre user
   - Rejoindre la room avec l'ID + password
5. **Admin lance la partie**
6. **Jouer tour par tour** (bouton "Jouer" actif pour le joueur actif)

---

## 📝 À implémenter (logique de jeu)

### Backend
- [ ] Logique des cartes dans `RoomService.playTurn()`
- [ ] Validation des moves selon les règles Flip7
- [ ] Gestion du `gameState` JSON (deck, mains, cartes jouées)
- [ ] Détection fin de partie → status FINISHED
- [ ] Calcul du gagnant

### Frontend
- [ ] Composant `game-board` avec affichage cartes
- [ ] Composant `player-hand` pour la main du joueur
- [ ] Animation des cartes jouées
- [ ] Règles Flip7 (popup ou panneau)
- [ ] Historique des coups
- [ ] Effets sonores

---

## 🔒 Sécurité

### Actuel (Simplifié)
- ✅ Passwords hashés avec BCrypt
- ✅ Bearer token (userID simple pour test)
- ✅ Vérification admin pour start game
- ✅ Vérification tour pour play

### À améliorer
- [ ] JWT tokens au lieu de userID simple
- [ ] Validation token côté backend
- [ ] Rate limiting sur API
- [ ] HTTPS en production
- [ ] Validation input (XSS, injection)

---

## ✨ Fonctionnalités implémentées

✅ Multi-room system  
✅ Password-protected rooms  
✅ Admin role (créateur de room)  
✅ Max players configurable  
✅ Tour par tour avec turnIndex  
✅ WebSocket temps réel (STOMP)  
✅ UI complète (lobby, waiting, game)  
✅ Reconnexion automatique WebSocket  
✅ Status indicators (connecté/déconnecté)  
✅ Responsive design moderne  

---

**Système prêt pour la logique de jeu Flip7 ! 🎴**
