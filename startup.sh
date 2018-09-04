#!/usr/bin/env bash

set -e

if test -e local.env; then
    set -a
    source local.env
    set +a
else
    printf "$(tput setaf 1)No local environment found. Use verify-local-startup or openssl to generate a local.env file\n$(tput sgr0)"
    exit
fi

source ../ida-hub-acceptance-tests/scripts/services.sh
source ../ida-hub-acceptance-tests/scripts/env.sh

if test ! "$1" == "skip-build"; then
    ./gradlew clean build installDist
fi

if ! docker ps | grep stub-idp-postgres-db
then
    printf "$(tput setaf 3)Postgres not running... Attempting to start postgres using docker...\n$(tput sgr0)"
    docker run --rm -d -p 5432:5432 --name stub-idp-postgres-db postgres
fi

mkdir -p logs
start_service stub-idp stub-idp configuration/stub-idp.yml $STUB_IDP_PORT
wait
