#!/usr/bin/env bash
set -euo pipefail

if [[ ${SSH_ORIGINAL_COMMAND:-} != deploy || $# != 2 ]]; then
    exit 1
fi

cd "$1"
registry_image=$2
IFS= read -r reference
digest=${reference#"$registry_image"@sha256:}
if [[ "$reference" != "$registry_image@sha256:$digest" || ! "$digest" =~ ^[a-f0-9]{64}$ ]]; then
    echo 'Invalid release image reference.' >&2
    exit 1
fi

exec 9>.production-deploy.lock
flock -w 120 9
export PRODUCTION_IMAGE="$reference"
compose=(docker compose -f compose.production.yml)
"${compose[@]}" pull app
"${compose[@]}" up -d --no-deps --no-build --wait --wait-timeout 180 app
