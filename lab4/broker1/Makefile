# **********************************************************************
#
# Copyright (c) 2000
# Object Oriented Concepts, Inc.
# Billerica, MA, USA
#
# All Rights Reserved
#
# **********************************************************************
# update per project
IDL_file = broker1.idl
PACKAGE = broker

LD_LIBRARY_PATH=/cad2/ece419s/corba/OB-4.3/lib

ECE419_HOME=/cad2/ece419s
JAVA_HOME=$(ECE419_HOME)/java/jdk1.6.0
CORBA_DIR=${ECE419_HOME}/corba/
JOB_DIR=${CORBA_DIR}/OB-4.3
OB_DIR=${CORBA_DIR}/OB-4.3
JOB_LIB_DIR=${JOB_DIR}/lib
JIDL_EXE=${OB_DIR}/bin/jidl

all:
	echo "JIDL" ${JIDL_EXE}
	echo "CORBA_DIR" ${CORBA_DIR}
	mkdir -p generated/broker
	mkdir -p classes/broker

	$(JIDL_EXE) --package $(PACKAGE) --output-dir generated $(IDL_file)
	CLASSPATH=.:generated:$(JOB_LIB_DIR):$$CLASSPATH \
	${JAVA_HOME}/bin/javac $(JAVACFLAGS) -d classes \
	*.java \
	generated/$(PACKAGE)/*.java


clean::
	rm -rf classes generated 
