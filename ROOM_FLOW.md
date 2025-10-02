# 🎮 Flow des Rooms - Flip7

## Architecture des pages

### `/room` - Lobby Principal
**Responsabilité**: Créer ou rejoindre une room

**Actions disponibles**:
- ✅ Créer une nouvelle room (password + max players)
- ✅ Rejoindre une room existante (ID + password)
- ✅ Voir la liste des rooms disponibles (status WAITING uniquement)

**Après action**:
- → Redirige vers `/game/:roomId`

---

### `/game/:roomId` - Salle de jeu
**Responsabilité**: Attente des joueurs + Partie en cours

#### 🔒 Sécurité d'accès
**Vérifications automatiques**:
1. ✅ L'utilisateur doit être connecté (sinon → `/login`)
2. ✅ La room doit exister (sinon → `/room` après 2s)
3. ✅ **L'utilisateur doit être dans `room.players`** (sinon → `/room` après 2s)

→ Empêche les spectateurs non autorisés

---

## Flow utilisateur

### 👑 Admin crée une room

```
1. Admin sur /room
   └─> Remplit formulaire (password + max players)
   └─> Clique "Créer la room"

2. Backend crée la room
   └─> Admin est automatiquement ajouté à room.players[]
   └─> Status = WAITING

3. Redirection vers /game/:roomId
   └─> Vue "Salle d'attente"
   └─> Admin voit:
       ✅ Son badge "👑 Admin"
       ✅ Liste des joueurs (lui seul pour l'instant)
       ✅ ID de la room avec bouton "Copier"
       ✅ Bouton "🎮 Lancer la partie" (dès que >= 2 joueurs)
```

### 👤 Joueur rejoint une room

```
1. Joueur sur /room
   └─> Entre l'ID (ou sélectionne dans la liste)
   └─> Entre le password
   └─> Clique "Rejoindre"

2. Backend vérifie:
   ✅ Password correct ?
   ✅ Room en WAITING ?
   ✅ Pas déjà full ?
   └─> Ajoute le joueur à room.players[]
   └─> Broadcast WebSocket aux autres joueurs

3. Redirection vers /game/:roomId
   └─> Vue "Salle d'attente"
   └─> Joueur voit:
       ✅ Liste des joueurs (dont lui avec badge "Vous")
       ✅ Badge "👑 Admin" sur l'admin
       ✅ ID de la room avec bouton "Copier"
       ❌ PAS de bouton "Lancer la partie"
       ℹ️ Message: "En attente que l'admin lance la partie..."
```

### 🚀 Admin lance la partie

```
1. Admin clique "🎮 Lancer la partie"
   └─> Backend vérifie:
       ✅ >= 2 joueurs ?
       ✅ Requesteur = admin ?
   └─> Change status → IN_GAME
   └─> turnIndex = 0
   └─> gameState initialisé
   └─> Broadcast WebSocket "/topic/rooms/:id/start"

2. TOUS les joueurs (admin + autres)
   └─> Reçoivent l'événement WebSocket
   └─> currentView passe de "room" à "game"
   └─> Affichage du plateau de jeu
   └─> Indicateur de tour
   └─> Barre des joueurs
```

---

## Règles de jointure

### ✅ Peut rejoindre une room si:
1. **Room en WAITING** (avant que la partie démarre)
   - Via formulaire dans `/room`
   - Avec password correct
   - Si pas déjà full

2. **Room en IN_GAME** ET **déjà dans room.players**
   - Cas: reconnexion d'un joueur déconnecté
   - Accès direct via URL `/game/:roomId`
   - Vérifié par le frontend (ligne de vérification dans loadRoom())

### ❌ NE PEUT PAS rejoindre si:
1. **Room en IN_GAME** ET **PAS dans room.players**
   - Backend refuse avec "Game already started"
   - Frontend bloque avec "Vous ne faites pas partie de cette room"

2. **Room FINISHED**
   - Backend refuse
   - Plus possible de jouer

---

## WebSocket Topics

### 🔔 `/topic/rooms/:roomId`
**Événement**: Mise à jour de la room
**Trigger**: 
- Nouveau joueur rejoint
- Joueur quitte (si implémenté)

**Action frontend**:
```typescript
roomUpdates$.subscribe(room => {
  this.currentRoom = room;
  // Met à jour la liste des joueurs
  // Refresh le compteur "X/Y joueurs"
});
```

### 🎮 `/topic/rooms/:roomId/start`
**Événement**: Partie démarre
**Trigger**: Admin clique "Lancer la partie"

**Action frontend**:
```typescript
gameStarts$.subscribe(room => {
  this.currentRoom = room;
  this.currentView = 'game';
  // Affiche le plateau
});
```

### 🎯 `/topic/rooms/:roomId/turn`
**Événement**: Tour joué
**Trigger**: Joueur actif joue une carte

**Action frontend**:
```typescript
turnUpdates$.subscribe(update => {
  this.currentRoom = update.room;
  // Met à jour turnIndex
  // Refresh l'indicateur de tour
  // Applique le coup (update.moveData)
});
```

---

## Affichage conditionnel

### 🏠 Vue "Salle d'attente" (WAITING)

```html
<!-- Toujours affiché -->
✅ ID de la room (avec copier)
✅ Liste des joueurs
✅ Compteur "X/Y joueurs"

<!-- Conditionnel ADMIN ONLY -->
<button *ngIf="isAdmin && currentRoom.players.length >= 2">
  🎮 Lancer la partie
</button>

<!-- Conditionnel NON-ADMIN -->
<p *ngIf="!isAdmin && currentRoom.players.length >= 2">
  En attente que l'admin lance la partie...
</p>

<!-- Si < 2 joueurs -->
<div *ngIf="currentRoom.players.length < 2">
  ⏳ En attente d'autres joueurs...
  Partagez l'ID avec vos amis !
</div>
```

### 🎮 Vue "Partie en cours" (IN_GAME)

```html
<!-- Indicateur de tour -->
<div [class.my-turn]="isMyTurn">
  {{ isMyTurn ? '🎯 C\'est votre tour !' : '⏳ Tour du joueur X' }}
</div>

<!-- Barre des joueurs -->
<div class="players-bar">
  <!-- Joueur actif = turnIndex -->
  <div [class.active]="i === currentRoom.turnIndex">
    ...
  </div>
</div>

<!-- Bouton jouer -->
<button *ngIf="isMyTurn" (click)="playTurn('move')">
  Jouer une carte
</button>
```

---

## Variables d'état importantes

```typescript
// Dans GamePageComponent
currentView: 'room' | 'game'     // Détermine quelle vue afficher
currentRoom: Room | null          // Données de la room
currentUserId: string             // ID du joueur connecté
isAdmin: boolean                  // = currentRoom.adminId === currentUserId
isMyTurn: boolean                 // = currentRoom.currentPlayerId === currentUserId
wsConnected: boolean              // État de la connexion WebSocket
```

---

## Résumé du comportement

✅ **Admin crée** → Va sur `/game/:roomId` → Voit "Lancer la partie"  
✅ **Joueur rejoint** → Va sur `/game/:roomId` → Voit "En attente de l'admin"  
✅ **Admin lance** → Tout le monde passe en mode jeu via WebSocket  
✅ **Reconnexion** → Si déjà dans room.players, accès direct  
❌ **Spectateur** → Si pas dans room.players, bloqué et redirigé  
❌ **Join tard** → Si game started, refus backend "Game already started"  

---

**Système 100% fonctionnel pour la gestion multi-room ! 🎉**
