#!/bin/sh
echo "window.ERPIONE_ENV = '$(echo "${ERPIONE_ENV:-LOCAL}" | tr '[:lower:]' '[:upper:]')';" > /usr/share/nginx/html/env.js
exec nginx -g 'daemon off;'
