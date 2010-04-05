#
# DDS - STORE
# ------------------------
# by Gokul Soundararajan
#
# Makefile
#

include ../config.mk

all: clean store.exe

store.exe: store.c
	${CC} ${CFLAGS} ${INCLUDES} store.c -o store.exe ${LIBDIRS} ${CLIBS}

clean:
	rm -rf *.o *.exe *.a *.so *.*~ *~

# Used for emacs' flymake syntax checker
check-syntax:
	$(CC) $(CFLAGS) ${INCLUDES} -DCACHING_ON -Wall -Wextra -fsyntax-only $(CHK_SOURCES)

.PHONY: clean check-syntax
