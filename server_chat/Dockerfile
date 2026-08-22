FROM node:22-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --production
COPY . .
RUN mkdir -p /app/logs/chat-server
EXPOSE 3001
CMD ["node", "index.js"]
