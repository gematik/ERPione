#!/bin/sh

set -o xtrace

echo "Starting ERPione with active profiles: $ACTIVE_SPRING_PROFILES"
echo " -> using header forward strategy: $SERVER_FORWARD_HEADERS_STRATEGY"

if [ -n "$PROXY_HOST" ]; then
  export http_proxy="http://$PROXY_HOST:$PROXY_PORT"
  export HTTP_PROXY="http://$PROXY_HOST:$PROXY_PORT"
  export https_proxy="http://$PROXY_HOST:$PROXY_PORT"
  export HTTPS_PROXY="http://$PROXY_HOST:$PROXY_PORT"
  export no_proxy="$NON_PROXY_HOST"
  export NO_PROXY="$NON_PROXY_HOST"

  echo "Proxy settings for: $HTTPS_PROXY"
  java -Dhttps.proxyHost="$PROXY_HOST" \
      -Dhttps.proxyPort="$PROXY_PORT" \
      -Dhttp.proxyHost="$PROXY_HOST" \
      -Dhttp.proxyPort="$PROXY_PORT" \
      -Dhttp.nonProxyHosts="$NON_PROXY_HOST" \
      -jar app.jar --spring.profiles.active="$ACTIVE_SPRING_PROFILES"
else
  java -jar app.jar --spring.profiles.active="$ACTIVE_SPRING_PROFILES"
fi
