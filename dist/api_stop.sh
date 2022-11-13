#!/bin/bash

FIFO="/tmp/apififo"

# send the FIFO signal
echo exit > $FIFO
