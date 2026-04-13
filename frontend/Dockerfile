# ----- Stage 1: Build -----
FROM node:22-alpine AS build
WORKDIR /app

# Copy source first
COPY . .

# Remove any node_modules that may have been copied from the host (e.g.
# Windows-built binaries that don't work on Alpine Linux), then install fresh.
# --legacy-peer-deps handles Angular Material/CDK version skew vs Angular Core.
RUN rm -rf node_modules && npm install --legacy-peer-deps

RUN npm run build

# ----- Stage 2: Runtime (Node SSR) -----
FROM node:22-alpine
WORKDIR /app

# Run as a non-root user
RUN addgroup --system app && adduser --system --ingroup app app

# Copy built output
COPY --from=build /app/dist/flip7-frontend ./dist/flip7-frontend

# Install only production dependencies for the SSR server
COPY --from=build /app/package.json /app/package-lock.json ./
RUN npm install --omit=dev --legacy-peer-deps

USER app

EXPOSE 4000

ENV PORT=4000
ENV NODE_ENV=production

CMD ["node", "dist/flip7-frontend/server/server.mjs"]
