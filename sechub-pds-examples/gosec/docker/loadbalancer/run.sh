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
    echo "Start Nginx"
    nginx -g 'daemon off;'
}

if [ "$LOADBALANCER_START_MODE" = "server" ]
then
    server
else
    debug
fi