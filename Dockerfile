# Multi-stage Dockerfile for PerfCraft Web
# Stage 1: Build TypeScript application with Vite
FROM node:22-alpine AS builder
WORKDIR /app

# Install dependencies
COPY web/package*.json ./
RUN npm ci

# Copy web source and public assets
COPY web/ ./
RUN npm run build

# Stage 2: Serve with lightweight, secure Nginx Alpine
FROM nginx:alpine-slim

# Copy custom Nginx configuration with gzip and SPA routing
RUN rm -rf /etc/nginx/conf.d/*
COPY web/nginx.conf /etc/nginx/conf.d/default.conf

# Copy production artifacts from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
