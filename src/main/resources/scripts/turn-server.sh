#!/bin/bash
sed -i -e "s/turnURL=nubomedia:nub0m3d1a:80.96.122.61/turnURL=$TURN_SERVER_USERNAME:$TURN_SERVER_PASSWORD:$TURN_SERVER_URL/g" /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
