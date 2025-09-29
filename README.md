# Flip7 - Jeu de société web

Architecture:
- frontend: Angular SPA (mobile-first)
- backend: Spring Boot (REST) + MongoDB

Démarrer le backend:
1. Configurez MongoDB localement ou utilisez un service cloud. Modifiez `backend/src/main/resources/application.properties` si besoin.
2. Dans `backend` lancez `mvn spring-boot:run`.

Démarrer le frontend:
1. Dans `frontend` installez les dépendances: `npm install`.
2. Lancez: `npm run start`.

Prochaine étape: implémenter le moteur de jeu (logique du tour par tour), websocket pour synchronisation en temps réel, authentification JWT et pages de statistiques/compte.

Démarrage avec Docker (si vous n'avez pas Maven ou MongoDB installés) :

1. Installez Docker et Docker Compose.
2. Depuis la racine du projet lancez :

```powershell
docker compose up --build
```

Cela lance un conteneur MongoDB et un conteneur qui construit/execute le backend via Maven.

Note: pour le frontend Angular utilisez une version LTS de Node.js (20.x recommandé). Les versions impaires (ex: 23.x) peuvent causer des erreurs avec la CLI Angular.
