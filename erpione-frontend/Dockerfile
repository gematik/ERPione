FROM nginx:alpine

COPY default.conf /etc/nginx/conf.d/default.conf
#COPY nginx.conf /etc/nginx/nginx.conf  # TODO: does not work
COPY dist/ /usr/share/nginx/html/
COPY entrypoint.sh /entrypoint.sh


RUN addgroup -S nginxuser && adduser -S nginxuser -G nginxuser && \
    chown -R nginxuser:nginxuser /etc/nginx /usr/share/nginx/html /var/cache/nginx && \
    chmod +x /entrypoint.sh

#USER nginxuser

ENTRYPOINT ["/entrypoint.sh"]
