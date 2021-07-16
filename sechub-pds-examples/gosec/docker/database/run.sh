#!/usr/bin/env sh
# SPDX-License-Identifier: MIT

debug () {
    while true
    do
	    echo "Press [CTRL+C] to stop.."
	    sleep 120
    done
}

server () {
    # start PostgreSQL server
    pg_ctl start

    psql --command="CREATE USER gosec;"
    psql --command="CREATE DATABASE gosec OWNER gosec;"

    # check PostgreSQL server status,
    # for the container to stay alive
    while pg_ctl status
    do
        sleep 300
    done
}

if [ "$DATABASE_START_MODE" = "server" ]
then
    server
else
    debug
fi