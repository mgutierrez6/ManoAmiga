FROM node:22-alpine
ENV NODE_ENV=production
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev && npm cache clean --force
COPY . .
# Necesita escuchar en 443/80: en Linux eso requiere root o la capacidad NET_BIND_SERVICE.
# Es lo único expuesto y no tiene datos ni secretos de la base.
EXPOSE 443 80
CMD ["node", "index.js"]
