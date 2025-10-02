# 🔧 Corrections - Backend ne reçoit pas les requêtes

## 🐛 Problèmes identifiés

### 1. ❌ Mauvais nom de token
**GameService** utilisait `'authToken'` alors que tous les autres services utilisent `'access_token'`

```typescript
// ❌ AVANT
const token = localStorage.getItem('authToken') || '';

// ✅ APRÈS
const token = localStorage.getItem('access_token') || '';
```

### 2. ❌ URL de l'API incorrecte
**GameService** manquait `/api` dans le chemin

```typescript
// ❌ AVANT
private apiUrl = `${environment.apiUrl}/game`;  
// → http://localhost:3200/game/{roomId}/draw

// ✅ APRÈS
private apiUrl = `${environment.apiUrl}/api/game`;
// → http://localhost:3200/api/game/{roomId}/draw
```

### 3. ⚠️ Vérification isMyTurn trop stricte
Le code bloquait l'action si `isMyTurn` était false, empêchant le débogage

```typescript
// Temporairement relaxé pour debug
if (!this.isMyTurn) {
  console.warn('⚠️ Not your turn, but allowing action for debugging');
}
```

---

## ✅ Corrections appliquées

### 1. `game.service.ts`

**Token corrigé:**
```typescript
private getHeaders(): HttpHeaders {
  const token = localStorage.getItem('access_token') || ''; // ✅
  console.log('🔑 GameService token:', token ? 'Present' : 'Missing');
  return new HttpHeaders({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  });
}
```

**URL corrigée:**
```typescript
private apiUrl = `${environment.apiUrl}/api/game`; // ✅ Ajout de /api
private historyUrl = `${environment.apiUrl}/api/history`; // ✅ Ajout de /api
```

**Logs ajoutés dans drawCard:**
```typescript
drawCard(roomId: string): Observable<DrawResult> {
  const url = `${this.apiUrl}/${roomId}/draw`;
  console.log('🌐 DrawCard HTTP Request:');
  console.log('   URL:', url);
  console.log('   Headers:', this.getHeaders());
  
  return this.http.post<DrawResult>(url, {}, { headers: this.getHeaders() })
    .pipe(
      tap(result => console.log('✅ DrawCard response:', result)),
      tap(null, error => console.error('❌ DrawCard error:', error))
    );
}
```

---

### 2. `game-page.component.ts`

**Logs améliorés:**
```typescript
onPlayerAction(action: any): void {
  console.log('🎯 Player action received:', action);
  console.log('   - roomId:', this.roomId);
  console.log('   - isMyTurn:', this.isMyTurn);
  
  if (!this.roomId) {
    console.error('❌ No roomId!');
    return;
  }
  
  // Temporairement: permettre l'action même si ce n'est pas notre tour
  if (!this.isMyTurn) {
    console.warn('⚠️ Not your turn, but allowing action for debugging');
  }
  
  if (action.action === 'REVEAL') {
    console.log('🎲 Calling drawCard API...');
    this.gameService.drawCard(this.roomId).subscribe({
      // ...
    });
  }
}
```

---

## 🔍 Logs de débogage

Maintenant, quand tu cliques sur "Hit", tu devrais voir dans la console :

```
🎯 Player action received: { action: 'REVEAL', card: ... }
   - roomId: 67891234abcd...
   - isMyTurn: false
⚠️ Not your turn, but allowing action for debugging
🎲 Calling drawCard API...
🔑 GameService token: Present
🌐 DrawCard HTTP Request:
   URL: http://localhost:3200/api/game/67891234abcd.../draw
   Headers: HttpHeaders {...}
```

**Si succès:**
```
✅ DrawCard response: { success: true, card: {...}, ... }
🃏 Card drawn: { success: true, ... }
```

**Si erreur:**
```
❌ DrawCard error: { status: 401, message: 'Unauthorized', ... }
```

---

## 🧪 Tests à faire

### 1. Vérifier le token
```javascript
// Dans la console du navigateur
localStorage.getItem('access_token')
```
**Attendu:** Une chaîne JWT (commence par "eyJ...")

### 2. Vérifier le backend
- Le backend est-il démarré sur le port 3200 ?
- Vérifier les logs backend pour voir si la requête arrive

### 3. Tester l'endpoint directement
```bash
# Dans PowerShell ou cmd
curl -X POST http://localhost:3200/api/game/YOUR_ROOM_ID/draw ^
  -H "Authorization: Bearer YOUR_TOKEN" ^
  -H "Content-Type: application/json"
```

---

## 📡 Requête HTTP attendue

### Request
```
POST http://localhost:3200/api/game/{roomId}/draw
Headers:
  Content-Type: application/json
  Authorization: Bearer eyJ...
Body: {}
```

### Response attendue
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

---

## ✅ Checklist de vérification

- [x] Token corrigé: `'access_token'` au lieu de `'authToken'`
- [x] URL corrigée: `/api/game` au lieu de `/game`
- [x] Logs ajoutés dans GameService
- [x] Logs ajoutés dans GamePageComponent
- [x] Vérification isMyTurn temporairement relaxée

---

## 🎯 Prochaine étape

1. **Recharger le frontend** (`ng serve` ou F5)
2. **Cliquer sur "Hit"**
3. **Vérifier la console** :
   - Tu devrais voir tous les logs
   - L'URL doit être `http://localhost:3200/api/game/.../draw`
   - Le token doit être "Present"
   - La réponse doit arriver du backend

Si tu vois toujours rien, vérifie :
- Le backend est bien démarré ?
- Le backend écoute sur le port 3200 ?
- Les logs backend montrent la requête ?
