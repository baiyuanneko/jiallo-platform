#!/bin/sh
set -e

if [ -n "$API_BASE_URL" ]; then
    sed -i "s|/jiallo-backend|$API_BASE_URL|g" /usr/share/nginx/html/config/appConfig.js
fi

exec "$@"
