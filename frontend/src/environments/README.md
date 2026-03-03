# Configuration des environnements

Ce projet utilise différents fichiers d'environnement selon le contexte d'utilisation.

## Fichiers disponibles

### 1. `environment.local.ts` 
**Usage :** Développement sur la même machine
- Frontend et Backend sur le même PC
- Accès uniquement depuis `http://localhost:4200`
- API : `http://localhost:3200`

**Commande :** `ng serve` (utilise environment.ts par défaut, renommez ce fichier si besoin)

### 2. `environment.development.ts`
**Usage :** Tests sur le réseau local (WiFi)
- Frontend accessible depuis d'autres appareils du même WiFi
- API : `http://192.168.1.36:3200` (IP locale)
- Accès : `http://192.168.1.36:4200`

**Commande :** `ng serve --configuration development`

### 3. `environment.ts` (production/internet)
**Usage :** Accès depuis Internet
- Frontend accessible depuis n'importe où
- API : `http://82.67.194.54:3200` (IP publique)
- Nécessite ouverture des ports 3200 et 4200 sur la box

**Commande :** `ng serve` ou `ng build`

## Configuration du backend

Dans `backend/src/main/resources/application.properties` :

```properties
server.port=3200
server.address=0.0.0.0  # Écoute sur toutes les interfaces
```

## Pare-feu Windows (obligatoire pour accès réseau/internet)

Exécuter en tant qu'administrateur :

```powershell
# Autoriser le port 3200 (backend)
netsh advfirewall firewall add rule name="Flip7 Backend" dir=in action=allow protocol=TCP localport=3200

# Autoriser le port 4200 (frontend)
netsh advfirewall firewall add rule name="Angular Dev Server" dir=in action=allow protocol=TCP localport=4200
```

## Redirection de ports sur la box (pour accès Internet)

Configurez votre box/routeur pour rediriger :
- Port externe 3200 → Port interne 3200 (192.168.1.36)
- Port externe 4200 → Port interne 4200 (192.168.1.36)

## Note importante

⚠️ **IP publique :** Votre IP publique peut changer si vous redémarrez votre box. Pensez à mettre à jour `environment.ts` si nécessaire.

⚠️ **Sécurité :** L'accès via Internet utilise HTTP (non sécurisé). Pour la production, utilisez HTTPS.
