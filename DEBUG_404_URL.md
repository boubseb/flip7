# 🔍 Debug - URL incorrecte (404)

## ❌ Erreur constatée

```
POST http://localhost:3200/game/391dac12.../draw
404 Not Found
```

**Problème** : L'URL manque `/api`

**Attendu** : `http://localhost:3200/api/game/.../draw`

---

## ✅ Vérifications faites

### 1. Code dans game.service.ts est CORRECT ✅

```typescript
export class GameService {
  private apiUrl = `${environment.apiUrl}/api/game`;  // ✅ Correct
```

### 2. Environment.ts est CORRECT ✅

```typescript
export const environment = {
  apiUrl: 'http://localhost:3200',  // ✅ Correct
```

### 3. Calcul attendu

```
environment.apiUrl = 'http://localhost:3200'
+ '/api/game'
= 'http://localhost:3200/api/game'
```

---

## 🐛 Cause probable

**Le frontend n'a pas rechargé le code modifié !**

Quand tu as modifié `game.service.ts`, Angular n'a peut-être pas détecté le changement ou le cache est corrompu.

---

## 🔧 Solutions

### Solution 1 : Redémarrer ng serve (RECOMMANDÉ)

```powershell
# Dans le terminal où tourne ng serve
# 1. Arrête avec Ctrl+C
# 2. Relance
ng serve
```

### Solution 2 : Vider le cache Angular

```powershell
# Arrête ng serve
# Supprime le dossier cache
rm -r .angular/cache

# Supprime node_modules/.cache (si existe)
rm -r node_modules/.cache

# Relance
ng serve
```

### Solution 3 : Hard reload dans le navigateur

1. Ouvre DevTools (F12)
2. Clique droit sur le bouton refresh
3. Sélectionne "Vider le cache et actualiser"

Ou :
- **Chrome/Edge** : Ctrl + Shift + R
- **Firefox** : Ctrl + F5

### Solution 4 : Vérification directe dans le navigateur

Ouvre la console du navigateur et tape :

```javascript
// Vérifier l'URL utilisée
import { environment } from './environments/environment';
console.log(environment.apiUrl + '/api/game');
```

---

## 🧪 Test après redémarrage

Une fois `ng serve` redémarré :

1. **Recharge la page** (F5)
2. **Clique sur "Hit"**
3. **Vérifie la console**

Tu devrais voir :
```
🌐 DrawCard HTTP Request:
   URL: http://localhost:3200/api/game/391dac12.../draw  ✅
```

**Pas** :
```
URL: http://localhost:3200/game/391dac12.../draw  ❌
```

---

## 📋 Checklist complète

- [ ] Arrêter ng serve (Ctrl+C)
- [ ] Optionnel : Vider cache `.angular/cache`
- [ ] Relancer ng serve
- [ ] Attendre "✔ Compiled successfully"
- [ ] Recharger page navigateur (Ctrl+Shift+R)
- [ ] Vérifier logs console : URL doit contenir `/api/game`
- [ ] Cliquer "Hit"
- [ ] Vérifier requête Network : doit être `/api/game/...`

---

## 🎯 URL attendue vs reçue

### ❌ URL actuelle (INCORRECTE)
```
http://localhost:3200/game/391dac12-1135-4062-aa35-0d9979bd293f/draw
                      ^^^^^
                      Manque /api
```

### ✅ URL correcte (ATTENDUE)
```
http://localhost:3200/api/game/391dac12-1135-4062-aa35-0d9979bd293f/draw
                      ^^^^^^^^
                      Avec /api
```

---

## 🔍 Vérification Network Tab

1. Ouvre DevTools (F12)
2. Onglet **Network**
3. Clique "Hit"
4. Cherche la requête `draw`
5. Vérifie l'URL complète

**Si ça montre toujours** `/game/...` **sans** `/api` :
→ Le frontend n'a pas rechargé le nouveau code !

---

## 💡 Note importante

Angular CLI (`ng serve`) devrait normalement recharger automatiquement les changements, mais parfois :

- Les services injectés au niveau root peuvent ne pas être rechargés
- Le cache peut être corrompu
- Les imports TypeScript peuvent être mis en cache

**La solution la plus fiable** : Arrêter et redémarrer `ng serve` !

---

## 🚀 Après correction

Une fois l'URL correcte, le backend devrait répondre :

```json
{
  "success": true,
  "message": "Carte piochée avec succès",
  "card": {
    "id": "card_123",
    "type": "NUMBER",
    "value": 5
  }
}
```

Et tu verras dans la console :
```
✅ DrawCard response: { success: true, ... }
🃏 Card drawn: { success: true, card: {...} }
```

🎉 C'est bon signe !
