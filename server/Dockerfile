FROM node:20-alpine

WORKDIR /app

# Install build dependencies if needed
COPY package*.json ./
RUN npm install --production

COPY . .

EXPOSE 3000

ENV PORT=3000
ENV NODE_ENV=production

CMD ["node", "server.js"]
