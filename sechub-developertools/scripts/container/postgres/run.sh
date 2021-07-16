#!/usr/bin/env sh
# SPDX-License-Identifier: MIT

debug () {
    while true
    do
	    echo "Press [CTRL+C] to stop.."
	    sleep 5s
    done
}

server () {
    # start PostgreSQL server
    pg_ctl start

    psql --command="CREATE USER $POSTGRES_DB_USER PASSWORD '$POSTGRES_DB_PASSWORD';"
    psql --command="CREATE DATABASE $POSTGRES_DB_NAME OWNER $POSTGRES_DB_USER;"

    # check PostgreSQL server status,
    # for the container to stay alive
    while pg_ctl status
    do
        sleep 5m
    done
}

if [ "$DATABASE_START_MODE" = "server" ]
then
    server
else
    debug
fi
