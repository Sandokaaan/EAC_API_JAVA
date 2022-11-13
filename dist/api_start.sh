#!/bin/bash

# create new FIFO
rm -f /tmp/apififo
mkfifo /tmp/apififo

# go to installed path
cd /usr/local/share/apijava/

# run code
java -cp API.jar:lib/*:. main.Api
