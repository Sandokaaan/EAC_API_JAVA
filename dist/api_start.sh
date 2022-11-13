#!/bin/bash

FIFO="/tmp/apififo"

# delete old FIFO
rm -f $FIFO
# create new FIFO
mkfifo $FIFO

# wait a moment
WAIT=$( earthcoin-cli -getinfo | grep protocolversion )
while [[ -z "$WAIT" ]]
do
  sleep 3s
  WAIT=$( earthcoin-cli -getinfo | grep protocolversion )
  echo waiting...
done
echo ready

# go to installed path
cd /usr/local/share/apijava/

# run code
java -cp API.jar:lib/*:. main.Api
