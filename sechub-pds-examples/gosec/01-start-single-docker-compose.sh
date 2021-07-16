#!/usr/bin/env bash
# SPDX-License-Identifier: MIT

SCRIPT_DIR=`dirname $0`
REPLICAS="$1"

ENVIRONMENT_FILE=".env"

if [[ ! -f  "$ENVIRONMENT_FILE" ]]
then
    echo "Environment file does not exist."
    echo "Creating default environment file $ENVIRONMENT_FILE for you."

    cp "$SCRIPT_DIR/env-initial" "$SCRIPT_DIR/.env"
else
    echo "Using existing environment file: $ENVIRONMENT_FILE."
fi

if [[ -z "$REPLICAS" ]]
then
    
    REPLICAS=1
else
    echo "Starting cluster of $REPLICAS containers."
fi

echo "Starting single container."
docker-compose --file docker-compose_pds_gosec_ubuntu.yaml up --build