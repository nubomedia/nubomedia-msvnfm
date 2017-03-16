#!/bin/bash
mkdir -p /opt/
echo export NUBOMEDIASTUNSERVERADDRESS=NUBOMEDIA_STUN_SERVER_ADDRESS > /opt/envvars
echo export NUBOMEDIASTUNSERVERPORT=3478 >> /opt/envvars
echo export NUBOMEDIATURNSERVERADDRESS=NUBOMEDIA_TURN_SERVER_ADDRESS >> /opt/envvars
echo export NUBOMEDIATURNSERVERPORT=3478 >> /opt/envvars
echo export NUBOMEDIAMONITORINGIP=NUBOMEDIA_MONITORING_IP >> /opt/envvars

cd /root/deploy && git pull origin master
mv /root/deploy/WebRtcEndpoint.conf.ini /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
mv /root/deploy/fix.sh /usr/local/bin/
mv /root/deploy/logstash-forwarder.conf /etc/logstash-forwarder.conf
mv /root/deploy/collectd.conf /etc/collectd/collectd.conf

# Update the configurations
bash /usr/local/bin/fix.sh
sed -i -e "s/HOSTNAMEMONITORING/$HOSTNAME/g" /etc/collectd/collectd.conf 
service collectd restart
