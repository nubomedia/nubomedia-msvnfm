#!/bin/bash
if [ "$STUN_SERVER_ACTIVATE" = "true" ]; then
    sed -i -e "s/stunServerAddress=80.96.122.61/stunServerAddress=$STUN_SERVER_ADDRESS/g" /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
    sed -i -e "s/stunServerPort=3478/stunServerPort=$STUN_SERVER_PORT/g" /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
fi