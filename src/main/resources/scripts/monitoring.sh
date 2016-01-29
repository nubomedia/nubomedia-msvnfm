#!/bin/bash
#HOSTNAME=$(cat /etc/hostname)
#sed -i -e "s/HOSTNAMEMONITORING/$HOSTNAME/g" /etc/collectd/collectd.conf
#MONITORING_URL='172.22.28.70'
#sed -i -e "s/172.22.2.70/$MONITORING_URL/g" /etc/collectd/collectd.conf
#/etc/init.d/collectd restart
#HOSTNAME_MONITORING="HOSTNAMEMONITORING"
sed -i -e "s/HOSTNAMEMONITORING/$HOSTNAME/g" /etc/collectd/collectd.conf 
service collectd restart