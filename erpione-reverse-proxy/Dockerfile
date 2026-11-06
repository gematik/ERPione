FROM nginx:alpine

COPY nginx.conf /etc/nginx/nginx.conf
COPY 404.html /usr/share/nginx/html

RUN addgroup -S nginxuser && adduser -S nginxuser -G nginxuser && \
    chown -R nginxuser:nginxuser /etc/nginx /usr/share/nginx/html /var/log/nginx
USER nginxuser
