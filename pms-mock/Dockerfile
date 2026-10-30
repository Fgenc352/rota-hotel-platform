FROM node:24-alpine AS validate
WORKDIR /app
COPY *.mjs ./
COPY public ./public
RUN node --check server.mjs && node --check public/app.mjs

FROM node:24-alpine AS runtime
ENV NODE_ENV=production PMS_HOST=0.0.0.0 PMS_PORT=8090 PMS_CRM_URL=http://localhost:4200/crm/pms
WORKDIR /app
COPY --from=validate --chown=node:node /app/ /app/
RUN mkdir -p /app/data && chown -R node:node /app
USER node
EXPOSE 8090
HEALTHCHECK --interval=10s --timeout=4s --start-period=5s --retries=6 \
    CMD ["node", "-e", "fetch('http://127.0.0.1:8090/health', {signal: AbortSignal.timeout(2500)}).then(r => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))"]
CMD ["node", "server.mjs"]
