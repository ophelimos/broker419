#!/bin/bash
#
# Start up 4 DDS's so I don't have to do it manually

NUM_DDS=4
START_PORT=5000

DDS_PROG=./dds.exe

CUR_PORT=$START_PORT

STORES_FILE=../store/store.nodes

DB_PREFIX=/tmp/db

GOSSIP_PERIOD=10

HOSTNAME=`hostname`

i=1
rm -R $DB_PREFIX$i
mkdir $DB_PREFIX$i
echo "$DDS_PROG $CUR_PORT "$STORES_FILE" $DB_PREFIX$i > $DB_PREFIX$i/output &"
$DDS_PROG $CUR_PORT "$STORES_FILE" $DB_PREFIX$i > $DB_PREFIX$i/output &
CUR_PORT=`expr $CUR_PORT + 1`

for i in `seq 2 $NUM_DDS`
do
    # Remove the previous directory to get rid of old database files
    rm -R $DB_PREFIX$i
    mkdir $DB_PREFIX$i
    echo  "$DDS_PROG $CUR_PORT "$STORES_FILE" $DB_PREFIX$i $GOSSIP_PERIOD $HOSTNAME `expr $CUR_PORT - 1` > $DB_PREFIX$i/output &"
    $DDS_PROG $CUR_PORT "$STORES_FILE" $DB_PREFIX$i $GOSSIP_PERIOD $HOSTNAME `expr $CUR_PORT - 1` > $DB_PREFIX$i/output &
    CUR_PORT=`expr $CUR_PORT + 1`
done

exit 0
