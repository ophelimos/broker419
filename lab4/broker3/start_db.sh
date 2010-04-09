#!/bin/bash
# server.sh
ECE419_HOME=/cad2/ece419s
JAVA_HOME=${ECE419_HOME}/java/jdk1.6.0
JOB_LIB_DIR=${ECE419_HOME}/corba/JOB-4.3/lib
JOB_LIB=${JOB_LIB_DIR}/OB.jar
JDBC_JAR=${ECE419_HOME}/jdbc/postgresql-8.2-504.jdbc2.jar

PROG=Server

echo "starting" ${PROG}

# Start up the Postgresql server

mkdir pg_data
${ECE419_HOME}/pgsql/bin/initdb -D pg_data
${ECE419_HOME}/pgsql/bin/pg_ctl -D pg_data -l logfile -o "-h localhost -p 6969" start

${ECE419_HOME}/pgsql/bin/dropdb -h localhost -p 6969 jay
#${ECE419_HOME}/pgsql/bin/createdb -h localhost -p 6969 jay

#${ECE419_HOME}/java/jdk1.6.0/bin/java -cp .:${ECE419_HOME}/jdbc/postgresql-8.2-504.jdbc2.jar jdbc/JDBCExample

# Shutdown after server shutdown
#${ECE419_HOME}/pgsql/bin/pg_ctl -D pg_data stop -m smart
