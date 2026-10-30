FROM node:24-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx/nginx-unprivileged:stable-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/rota/browser/ /usr/share/nginx/html/
USER 101
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=4s --start-period=10s --retries=6 \
    CMD wget --quiet --spider http://127.0.0.1:8080/healthz || exit 1

# Widget hosting is independent of the hotel sites and CRM, with its own public routes.
FROM runtime AS widget
COPY nginx.widget.conf /etc/nginx/conf.d/default.conf
