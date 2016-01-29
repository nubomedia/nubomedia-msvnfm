#!/bin/bash
if [ "$STUN_SERVER_ACTIVATE" = "true" ]; then
    sed -i -e "s/stunServerAddress=74.125.136.127/stunServerAddress=$STUN_SERVER_ADDRESS/g" /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
    sed -i -e "s/stunServerPort=19305/stunServerPort=$STUN_SERVER_PORT/g" /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
fi