# ---------- 阶段 1：构建 ----------
FROM node:22-alpine AS builder

WORKDIR /build

# 先拷贝 package.json + lock 文件，利用 Docker 层缓存
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- 阶段 2：运行 ----------
FROM nginx:alpine

COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
