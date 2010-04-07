#!/bin/bash
#
# Start up 4 DDS's so I don't have to do it manually

NUM_DDS=2
START_PORT=5000

DDS_PROG=./dds.exe

CUR_PORT=$START_PORT

STORES_FILE=../store/store.nodes

DB_PREFIX=db

GOSSIP_PERIOD=1

i=1
rm -R $DB_PREFIX$i
mkdir $DB_PREFIX$i
echo $DDS_PROG $CUR_PORT "$STORES_FILE" $DB_PREFIX$i &
$DDS_PROG $CUR_PORT "$STORES_FILE" $DB_PREFIX$i &
CUR_PORT=`expr $CUR_PORT + 1`

for i in `seq 2 $NUM_DDS`
do
    # Remove the previous directory to get rid of old database files
    rm -R $DB_PREFIX$i
    mkdir $DB_PREFIX$i
    echo    $DDS_PROG $CUR_PORT "$STORES_FILE" $DB_PREFIX$i $GOSSIP_PERIOD localhost `expr $CUR_PORT - 1`&
    $DDS_PROG $CUR_PORT "$STORES_FILE" $DB_PREFIX$i $GOSSIP_PERIOD localhost `expr $CUR_PORT - 1`&
    CUR_PORT=`expr $CUR_PORT + 1`
done

exit 0
