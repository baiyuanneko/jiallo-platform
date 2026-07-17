#!/bin/bash
# Development entrypoint for the backend container.
#
# 1. Starts the app via `mvn spring-boot:run` (devtools hot-restart is
#    enabled through the SPRING_DEVTOOLS_RESTART_ENABLED env var).
# 2. Watches src/ for changes and recompiles into target/classes, which
#    triggers a devtools restart of the running application.

mvn spring-boot:run &
APP_PID=$!
trap 'kill "$APP_PID" 2>/dev/null; exit 0' TERM INT

MARKER=/tmp/.dev-last-compile
touch "$MARKER"

while true; do
  sleep 2
  if [ -n "$(find src -type f -newer "$MARKER" -print -quit 2>/dev/null)" ]; then
    touch "$MARKER"
    echo "[dev] source change detected, recompiling..."
    mvn -q compile || true
  fi
done
