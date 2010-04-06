#!/bin/bash
#
# Start up 4 DDS's so I don't have to do it manually

NUM_DDS=4
START_PORT=5000

DDS_PROG=./dds.exe

CUR_PORT=$START_PORT

STORES_FILE=../store/store.nodes

# Peering not included yet

for i in `seq $NUM_STORES`
do
    # Remove the previous directory to get rid of old database files
    rm -R keymap$i
    mkdir keymap$i
    $DDS_PROG $CUR_PORT "$STORES_FILE" keymap$i &
    CUR_PORT=`expr $CUR_PORT + 1`
done

exit 0
