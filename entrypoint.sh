#!/bin/sh
set -e
if [ -n "$GOOGLE_CREDENTIALS_JSON" ]; then
  echo "$GOOGLE_CREDENTIALS_JSON" > /tmp/gcp-key.json
  export GOOGLE_APPLICATION_CREDENTIALS=/tmp/gcp-key.json
fi
exec java -Djava.net.preferIPv4Stack=true -jar app.jar