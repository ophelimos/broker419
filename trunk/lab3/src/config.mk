#
# DDS
# ------------------------
# by Gokul Soundararajan
#
# Makefile
#

CC=gcc
CCC=g++
CFLAGS=-g -O4
CLIBS=-lpthread -lhermes -labacus -ldb

CODE_BASE=CHANGE_THIS_TO_POINT_TO_YOUR_DIR

LIB_BASE=CHANGE_THIS_TO_POINT_TO_YOUR_DIR

ABACUS_DIR=${LIB_BASE}/lib/abacus_c
HERMES_DIR=${LIB_BASE}/lib/hermes_c
BDB_DIR=${LIB_BASE}/lib/db
INCLUDES=-I${CODE_BASE}/src/include -I${ABACUS_DIR}/src/include -I${HERMES_DIR}/src/include -I${BDB_DIR}/include
LIBDIRS=-L${HERMES_DIR}/src/ -L${BDB_DIR}/lib/ -L${ABACUS_DIR}/src/

