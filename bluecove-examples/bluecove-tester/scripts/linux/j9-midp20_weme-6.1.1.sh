#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
SCRIPTS_DIR=`dirname ${0}`/..
. ${SCRIPTS_DIR}/environment.sh

export JAVAHOME=/opt/IBM/WEME/runtimes/61/lnx-x86-midp20
export PATH=${JAVAHOME}/bin/:$PATH
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:${JAVAHOME}/bin

# igone j9 bugs *** glibc detected *** free(): invalid pointer:...
export MALLOC_CHECK_=0

JVM_ARGS="-jcl:midp20"
JVM_ARGS="${JVM_ARGS} -Xbootclasspath:${JAVAHOME}/lib/jclMidp20/jclMidp20.jxe"
JVM_ARGS="${JVM_ARGS} -Dmicroedition.connection.pkgs=com.intel.bluetooth"
#JVM_ARGS="${JVM_ARGS} -Dbluecove.debug=1"

JCLMIDP20_EXT_DIR="${JAVAHOME}/lib/jclMidp20/ext"
if [ ! -d ${GENERATED_VERSION} ]; then
    mkdir ${JCLMIDP20_EXT_DIR}
fi

cp ${BLUECOVE_JAR} "${JCLMIDP20_EXT_DIR}/bluecove.jar"
cp ${BLUECOVE_GPL_JAR} "${JCLMIDP20_EXT_DIR}/bluecove-gpl.jar"
cp ${BLUECOVE_GPL_PROJECT_HOME}/src/main/resources/*.so "${JAVA_HOME}/bin"


${JAVAHOME}/bin/j9 ${JVM_ARGS} -cp ${BLUECOVE_TESTER_HOME}/target/bctest.jar javax.microedition.lcdui.AppManager ${BLUECOVE_TESTER_HOME}/target/bctest.jad
rc=$?
if [ ! "${rc}" = "0" ]; then
    echo Error calling java
    echo Prsess enter to exit
    read
fi
