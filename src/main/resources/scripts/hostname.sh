#!/bin/bash -v
HOST_NAME=$(cat /etc/hostname)
sudo sed -i "s/\([0-9]*\.[0-9]*\.[0-9]*\.[0-9]*[ \t]*localhost\)/\1 $HOST_NAME/" /etc/hosts
