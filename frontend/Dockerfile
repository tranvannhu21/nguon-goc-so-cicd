# syntax=docker/dockerfile:1

# ----------------------------------------------------------------------------
# Stage 1: Build - Node 22 LTS (Vite 8 requires Node >= 20.19 / 22.12)
# ----------------------------------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /workspace

# Build-time env vars read by Vite during `npm run build`.
# Only PUBLIC data may be passed here - never secrets, JWT, or DB credentials,
# because VITE_* values are embedded into the shipped JS bundle.
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_ASSET_BASE_URL=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_ASSET_BASE_URL=$VITE_ASSET_BASE_URL

# Copy manifests first, then install dependencies for layer caching.
# npm ci requires package-lock.json to be in sync with package.json.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

# Copy the rest of the source and build.
COPY . .
RUN npm run build

# ----------------------------------------------------------------------------
# Stage 2: Runtime - Nginx (official non-root image, listens on 8080)
# ----------------------------------------------------------------------------
FROM nginxinc/nginx-unprivileged:1.28-alpine

# Only the built static bundle is copied - no source, no node_modules,
# no .env, no package-manager cache.
COPY --from=build --chown=nginx:nginx /workspace/dist /usr/share/nginx/html

# Custom config: SPA fallback, /api and /files reverse proxy, caching, gzip.
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080

# wget (busybox) ships with the alpine-based image.
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
