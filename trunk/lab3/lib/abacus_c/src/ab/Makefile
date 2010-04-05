#
# ABACUS
# ------------------------
# by Gokul Soundararajan
#
# Makefile
#

include ../config.mk

all: clean libs
libs: abacus.o

abacus.o: abacus.c
	${CC} -c ${CFLAGS} ${INCLUDES} abacus.c -o abacus.o

clean:
	rm -rf *.o *.exe *.a *.so *.*~ *~

# Used for emacs' flymake syntax checker
check-syntax:
	$(CC) $(CFLAGS) ${INCLUDES} -DCACHING_ON -Wall -Wextra -fsyntax-only $(CHK_SOURCES)

.PHONY: clean check-syntax
