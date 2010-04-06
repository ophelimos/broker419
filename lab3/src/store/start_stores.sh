#!/bin/bash
#
# Start up a whole bunch of storage servers so I don't have to do it manually

NUM_STORES=8
START_PORT=4000

NODES_FILE="store.nodes"
STORE_PROG=./store.exe

CUR_PORT=$START_PORT

# Truncate the store.nodes file
echo "" > "$NODES_FILE"

for i in `seq $NUM_STORES`
do
    # Remove the previous directory to get rid of old database files
    rm -R tmpdir$i
    mkdir tmpdir$i
    echo "localhost $CUR_PORT" >> "$NODES_FILE"
    $STORE_PROG $CUR_PORT tmpdir$i &
    CUR_PORT=`expr $CUR_PORT + 1`
done

exit 0
