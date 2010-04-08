#!/bin/bash
# Run a host of test scripts against running DDS servers

NUM_DDS=4
START_PORT=5000

ITERATIONS=10000

CUR_PORT=$START_PORT

#$SED_SCRIPT="sed s/foo.txt/foo$i.txt/g"

for i in `seq 1 $ITERATIONS`
do
# Run a put
    cat cmd_put | sed "s/foo.txt/foo$i.txt/g" | nc localhost $CUR_PORT
# Try and get it from the next dds
    CUR_PORT=`expr $CUR_PORT + 1`
    cat cmd_get | nc localhost $CUR_PORT
# Try and list them
    CUR_PORT=`expr $CUR_PORT + 1`
    cat cmd_list | nc localhost $CUR_PORT
# Then delete them
    CUR_PORT=`expr $CUR_PORT + 1`
    cat cmd_del | nc localhost $CUR_PORT
# Restore CUR_PORT
    CUR_PORT=$START_PORT
done

exit 0
