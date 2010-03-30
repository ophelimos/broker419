#!/bin/bash

i=0
cmd=$1
n=$2
while [ $i -lt $n ]
do
  $cmd
  i=`expr $i + 1`
done

