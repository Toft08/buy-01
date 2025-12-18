# -------- BUILD STAGE --------
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# -------- RUNTIME STAGE --------
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx content
RUN rm -rf ./*

# Copy built Angular app (Angular 18+ outputs to dist/[project-name]/browser)
COPY --from=build /app/dist/buy-01/browser ./

# Copy SSL certificates
COPY ssl/localhost-cert.pem /etc/nginx/ssl/localhost-cert.pem
COPY ssl/localhost-key.pem /etc/nginx/ssl/localhost-key.pem

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 443

CMD ["nginx", "-g", "daemon off;"]
