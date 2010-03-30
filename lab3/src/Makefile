#
# DDS
# ------------------------
# by Gokul Soundararajan
#
# Makefile
#

include config.mk

all: clean exe
exe: store/store.exe dds/dds.exe

store/store.exe:
	cd store; make
dds/dds.exe:
	cd dds; make

clean:
	rm -rf *.o *.exe *.a *.so *.*~ *~
	cd store; make clean
	cd dds; make clean
	cd include; make clean