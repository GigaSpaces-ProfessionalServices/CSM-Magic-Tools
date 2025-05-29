#!/usr/bin/env bash
# Usage: ./fetch-tenant-token.sh <TENANT_HOST> <APP_ID> <EMAIL>
set -euo pipefail
TENANT_HOST=$1
APP_ID=$2
EMAIL=$3
read -srp "Password: " PASSWORD; echo
COOKIE_JAR=$(mktemp)
trap 'rm -f "$COOKIE_JAR"' EXIT
LOGIN_JSON=$(curl -s -c "$COOKIE_JAR" \
  -H 'Content-Type: application/json' \
  -H "frontegg-requested-application-id: $APP_ID" \
  -d '{"email":"'"$EMAIL"'","password":"'"$PASSWORD"'"}' \
  "https://$TENANT_HOST/api/frontegg/identity/resources/auth/v1/user")
ACCESS_TOKEN=$(jq -r '.accessToken' <<<"$LOGIN_JSON")
if [[ -z "$ACCESS_TOKEN" || "$ACCESS_TOKEN" == "null" ]]; then
  echo "Login failed:"; echo "$LOGIN_JSON" | jq .; exit 1
fi
# ensure session cookie exists (needed by /api/get-token)
SESSION_NAME="fe_session-${APP_ID//-/}"
if ! grep -q "$SESSION_NAME" "$COOKIE_JAR"; then
  printf ".\tTRUE\t/\tFALSE\t0\t%s\t%s\n" "$SESSION_NAME" "$ACCESS_TOKEN" >> "$COOKIE_JAR"
fi
BACKEND_JSON=$(curl -s -b "$COOKIE_JAR" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "frontegg-tenant-host: $TENANT_HOST" \
  -H "frontegg-requested-application-id: $APP_ID" \
  "https://$TENANT_HOST/api/get-token")
BACKEND_TOKEN=$(jq -r '.token // empty' <<<"$BACKEND_JSON")
if [[ -z "$BACKEND_TOKEN" ]]; then
  echo "get-token failed:"; echo "$BACKEND_JSON" | jq .; exit 1
fi
echo "$BACKEND_TOKEN"
