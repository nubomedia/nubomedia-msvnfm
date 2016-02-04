#!/bin/bash
if [ "$TURN_SERVER_ACTIVATE" = "true" ]; then
    sed -i -e "s/turnURL=nubomedia:nub0m3d1a:80.96.122.61:3478/turnURL=$TURN_SERVER_USERNAME:$TURN_SERVER_PASSWORD:$TURN_SERVER_URL/g" /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
fi