FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --legacy-peer-deps
COPY . .
RUN npm run build

FROM nginx:alpine

# Copy main config (overrides defaults like access.log)
COPY nginx.main.conf /etc/nginx/nginx.conf

# Copy site-specific configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy build files
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["/bin/sh", "-c", "envsubst '$SERVER_URL $CHAT_SERVER_URL' < /etc/nginx/conf.d/default.conf > /tmp/nginx-default.conf && cp /tmp/nginx-default.conf /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]
