#
# DDS
# ------------------------
# by Gokul Soundararajan
#
# Makefile
#

CC=gcc
CCC=g++
CFLAGS=-g -O0
CLIBS=-lpthread -lhermes -labacus /cad2/ece419s/db/lib/libdb-4.7.a
#CLIBS=-lpthread -lhermes -labacus -ldb

CODE_BASE=${HOME}/ece419/lab3

LIB_BASE=${HOME}/ece419/lab3

ABACUS_DIR=${LIB_BASE}/lib/abacus_c
HERMES_DIR=${LIB_BASE}/lib/hermes_c
BDB_DIR=${LIB_BASE}/lib/db
INCLUDES=-I${CODE_BASE}/src/include -I${ABACUS_DIR}/src/include -I${HERMES_DIR}/src/include -I${BDB_DIR}/include
LIBDIRS=-L${HERMES_DIR}/src/ -L${BDB_DIR}/lib/ -L${ABACUS_DIR}/src/

