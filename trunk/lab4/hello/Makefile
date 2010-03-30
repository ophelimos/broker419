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
IDL_file = Hello.idl
PACKAGE = hello

ECE419_HOME=/cad2/ece419s
JAVA_HOME=$(ECE419_HOME)/java/jdk1.6.0
CORBA_DIR=${ECE419_HOME}/corba/
#need change
JOB_DIR=${CORBA_DIR}/JOB-4.3.3
OB_DIR=${CORBA_DIR}/OB-4.3
JOB_LIB_DIR=${JOB_DIR}/lib
JIDL_EXE=${OB_DIR}/bin/jidl

all:
	echo "JIDL" ${JIDL_EXE}
	echo "CORBA_DIR" ${CORBA_DIR}
	mkdir -p generated/hello
	mkdir -p classes/hello
	
	${JIDL_EXE} --package $(PACKAGE) --output-dir generated $(IDL_file)
	CLASSPATH=.:generated:/cad2/ece419s/corba/JOB-4.3/lib \
	${JAVA_HOME}/bin/javac $(JAVACFLAGS) -d classes \
	*.java \
	generated/$(PACKAGE)/*.java

clean:
	rm -rf classes generated *.*~ *~


















