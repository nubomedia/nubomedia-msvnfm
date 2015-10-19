#!/bin/bash
HOSTNAME=$(cat /etc/hostname)
sed -i -e "s/HOSTNAMEMONITORING/$HOSTNAME/g" /etc/collectd/collectd.conf
MONITORING_DEV='172.22.28.70'
sed -i -e "s/172.22.2.66/$MONITORING_DEV/g" /etc/collectd/collectd.conf
/etc/init.d/collectd restart

