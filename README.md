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
