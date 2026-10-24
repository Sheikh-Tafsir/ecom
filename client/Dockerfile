FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --legacy-peer-deps
COPY . .

# Accept build args from docker-compose and expose as env vars for Vite
ARG VITE_API_PATH
ARG VITE_GOOGLE_LOGIN_CLIENT_ID
ARG VITE_SESSION_HINT_KEY
ARG VITE_LOCAL_STORAGE_CART_KEY
ARG VITE_WEB_SOCKET_ON
ENV VITE_API_PATH=$VITE_API_PATH
ENV VITE_GOOGLE_LOGIN_CLIENT_ID=$VITE_GOOGLE_LOGIN_CLIENT_ID
ENV VITE_SESSION_HINT_KEY=$VITE_SESSION_HINT_KEY
ENV VITE_LOCAL_STORAGE_CART_KEY=$VITE_LOCAL_STORAGE_CART_KEY
ENV VITE_WEB_SOCKET_ON=$VITE_WEB_SOCKET_ON

RUN npm run build

FROM nginx:alpine

# Copy main config (overrides defaults like access.log)
COPY nginx.main.conf /etc/nginx/nginx.conf

# Copy site-specific configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy build files
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
