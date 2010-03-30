#!/bin/bash
# server.sh
ECE419_HOME=/cad2/ece419s
JAVA_HOME=${ECE419_HOME}/java/jdk1.6.0
JOB_LIB_DIR=${ECE419_HOME}/corba/JOB-4.3/lib
JOB_LIB=${JOB_LIB_DIR}/OB.jar
JDBC_JAR=${ECE419_HOME}/jdbc/postgresql-8.2-504.jdbc2.jar

PROG=broker/Server

echo "starting" ${PROG}
${JAVA_HOME}/bin/java -Djava.endorsed.dirs=${JOB_LIB_DIR} -classpath ${JDBC_JAR}:${JOB_LIB}:classes:. ${PROG} $*
