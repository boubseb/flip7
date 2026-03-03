# ----- Stage 1: Build -----
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies first (cache layer)
COPY package.json package-lock.json* ./
RUN npm ci

# Copy source and build for production
COPY . .
RUN npm run build

# ----- Stage 2: Runtime (Node SSR) -----
FROM node:22-alpine
WORKDIR /app

# Copy built output
COPY --from=build /app/dist/flip7-frontend ./dist/flip7-frontend

# Install only production dependencies for the SSR server
COPY --from=build /app/package.json ./
RUN npm ci --omit=dev 2>/dev/null || true

EXPOSE 4000

ENV PORT=4000
ENV NODE_ENV=production

CMD ["node", "dist/flip7-frontend/server/server.mjs"]
