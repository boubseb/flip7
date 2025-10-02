# ✅ Étape 1: Lier le bouton "Hit" pour tirer une carte

## 🎯 Objectif
Connecter le bouton "Hit" du player-board pour appeler l'API Flip7 et tirer une carte, **sans toucher au design**.

## 📝 Modifications effectuées

### 1. `game-page.component.ts`

**Import ajouté:**
```typescript
import { GameService } from '../../services/game/game.service';
```

**Injection dans le constructeur:**
```typescript
constructor(
  private gameService: GameService,  // ✅ Ajouté
  // ... autres services
) {}
```

**Nouvelle méthode `onPlayerAction`:**
```typescript
onPlayerAction(action: any): void {
  if (!this.roomId || !this.isMyTurn) return;
  
  if (action.action === 'REVEAL') {
    // Joueur clique sur "Hit" - Tirer une carte
    this.gameService.drawCard(this.roomId).subscribe({
      next: (result) => {
        if (result.eliminated) {
          this.showError('Double ! Vous êtes éliminé !');
        } else if (result.lifeUsed) {
          console.log('⚡ Carte Vie utilisée automatiquement');
        } else if (result.roundEnded) {
          console.log('🏆 7 cartes différentes ! Round gagné !');
        }
        // L'état sera mis à jour via WebSocket
      },
      error: (error) => {
        this.showError('Erreur lors de la pioche');
      }
    });
  } else if (action.action === 'STOP') {
    // Joueur clique sur "Stop"
    this.gameService.stopDrawing(this.roomId).subscribe({
      next: () => {
        console.log('✋ Stopped drawing');
      },
      error: (error) => {
        this.showError('Erreur lors de l\'arrêt');
      }
    });
  }
}
```

**Suppression de `playTurn` (obsolète):**
```typescript
// ❌ SUPPRIMÉ - utilisait l'ancien système room
playTurn(move: string): void {
  this.roomService.playTurn(this.currentRoom.id, move).subscribe(...)
}
```

---

### 2. `game-page.component.html`

**Changement de binding:**
```html
<!-- AVANT -->
(onPlayCard)="playTurn($event)"

<!-- APRÈS -->
(onPlayCard)="onPlayerAction($event)"
```

---

## 🔄 Flux d'exécution

### Quand l'utilisateur clique sur "Hit"

```
1. PlayerBoardComponent.continueRevealing()
   ↓
2. Animation flip (600ms)
   ↓
3. emit { action: 'REVEAL', card: ... }
   ↓
4. GamePageComponent.onPlayerAction({ action: 'REVEAL' })
   ↓
5. gameService.drawCard(roomId)
   ↓
6. HTTP POST /api/game/{roomId}/draw
   ↓
7. Backend traite la pioche:
   - Ajoute carte à la main
   - Vérifie doubles
   - Vérifie 7 cartes différentes
   - Calcule score
   ↓
8. Backend retourne DrawResult:
   {
     success: true,
     message: "...",
     card: {...},
     roundEnded: false,
     lifeUsed: false,
     eliminated: false
   }
   ↓
9. Frontend affiche notification si nécessaire
   ↓
10. Backend broadcast via WebSocket:
    /topic/rooms/{roomId}/game-state
    ↓
11. Tous les joueurs reçoivent le nouvel état
```

---

## 🧪 Comment tester

### 1. Démarrer backend + frontend
```powershell
# Terminal 1 - Backend
cd backend
.\mvnw spring-boot:run

# Terminal 2 - Frontend
cd frontend
ng serve
```

### 2. Créer une room et lancer un round

### 3. Cliquer sur "Hit"
- ✅ Animation de flip se lance
- ✅ Appel HTTP vers `/api/game/{roomId}/draw`
- ✅ Backend traite la pioche
- ✅ Si double: message "Double ! Vous êtes éliminé !"
- ✅ Si 7 cartes: message console "Round gagné !"

### 4. Vérifier la console
```
🎯 Player action: { action: 'REVEAL', card: ... }
🃏 Card drawn: { success: true, card: {...} }
```

---

## 📡 API utilisée

### POST `/api/game/{roomId}/draw`
**Headers:**
```
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "Carte piochée avec succès",
  "card": {
    "id": "card_123",
    "type": "NUMBER",
    "value": 5,
    "displayName": "5"
  },
  "roundEnded": false,
  "lifeUsed": false,
  "eliminated": false
}
```

**Cas spéciaux:**
- **Double sans Vie:** `eliminated: true`
- **Double avec Vie:** `lifeUsed: true`
- **7 cartes différentes:** `roundEnded: true`

---

## ✅ Résultat

Le bouton "Hit" est maintenant **fonctionnel** ! 

- ✅ Appelle l'API Flip7
- ✅ Tire une carte
- ✅ Détecte les doubles
- ✅ Détecte les victoires
- ✅ Affiche les erreurs

**Design:** ❌ Aucun changement  
**Logique:** ✅ Connectée à l'API

---

## 🔜 Prochaines étapes

1. [ ] Mettre à jour l'affichage des cartes après la pioche
2. [ ] Mettre à jour le score
3. [ ] Gérer le WebSocket pour voir les changements en temps réel
4. [ ] Bouton "Stop" fonctionnel
5. [ ] Afficher l'état du jeu (tour du joueur, etc.)
