#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
SCRIPTS_DIR=`dirname ${0}`/..
. ${SCRIPTS_DIR}/environment.sh

JAVA_HOME=/opt/IBM/WEME/runtimes/61/lnx-x86-ppro10

JVM_ARGS="-jcl:ppro10"
JVM_ARGS="${JVM_ARGS} -Dmicroedition.connection.pkgs=com.intel.bluetooth"
#JVM_ARGS="${JVM_ARGS} -Dbluecove.debug=1"

${JAVA_HOME}/bin/j9 ${JVM_ARGS} -cp ${BLUECOVE_TESTER_APP_JAR} net.sf.bluecove.awt.Main
rc=$?
if [ ! "${rc}" = "0" ]; then
    echo Error calling java
    echo Prsess enter to exit
    read
fi
