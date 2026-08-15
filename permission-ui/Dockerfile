# =============================================
# 前端 Dockerfile — 直接运行预构建的静态资源
# 构建产物已在本地打好：permission-ui/dist/
# =============================================
FROM nginx:1.27-alpine

# 自定义 Nginx 配置（SPA 路由 + API 代理）
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制预构建的静态资源
COPY dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
