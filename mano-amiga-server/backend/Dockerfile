FROM node:22-alpine
ENV NODE_ENV=production
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev && npm cache clean --force
COPY . .
# No corremos como root dentro del contenedor.
USER node
EXPOSE 3000
CMD ["node", "server.js"]
