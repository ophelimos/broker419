#
# DDS - CORE
# ------------------------
# by Gokul Soundararajan
#
# Makefile
#

include ../config.mk

all: clean dds.exe

dds.exe: dds.o route.o keymap.o
	${CC} ${CFLAGS} ${INCLUDES} dds.o route.o keymap.o -o dds.exe ${LIBDIRS} ${CLIBS}

dds.o: dds.c
	${CC} -c ${CFLAGS} ${INCLUDES} dds.c -o dds.o 
keymap.o: keymap.c
	${CC} -c ${CFLAGS} ${INCLUDES} keymap.c -o keymap.o 
route.o: route.c
	${CC} -c ${CFLAGS} ${INCLUDES} route.c -o route.o 
clean:
	rm -rf *.o *.exe *.a *.so *.*~ *~

