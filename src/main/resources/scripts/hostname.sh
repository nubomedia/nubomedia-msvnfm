#!/bin/bash -v
echo $HOSTNAME > /etc/hostname
HOSTNAME=$(cat /etc/hostname)
#sudo sed -i "s/\([0-9]*\.[0-9]*\.[0-9]*\.[0-9]*[ \t]*localhost\)/\1 $HOSTNAME/" /etc/hosts

INSTANCE_NAME=$(curl http://169.254.169.254/latest/meta-data/hostname)
IFS='.' read -ra array <<< "$INSTANCE_NAME"
instance_name_simple=${array[0]}

sed -i " 1 s/.*/& $instance_name_simple/" /etc/hosts  