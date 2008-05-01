#! /bin/sh
#  @version $Revision$ ($Author$) $Date$
#

. ./scripts/version.sh

#java -cp ./target/bluecove-tester-${VERSION}-app.jar net.sf.bluecove.awt.Main

JVM_ARGS=
ERRORS=0
SUCCESS=0
echo Starting > run-continuous.log

break_tests=0

while [ "${break_tests}" = "0" ]
do

echo "-----------------------------"
echo "--- SUCCESS=${SUCCESS} ERRORS=${ERRORS} ---"
echo "-----------------------------"

java ${JVM_ARGS} -cp ./target/bluecove-tester-${VERSION}-app.jar net.sf.bluecove.awt.Main --runonce  >>  run-continuous.log
rc=$?
#echo "rc=[${rc}]"
if [ "${rc}" = "2" ]; then
    echo Error calling java
    let "ERRORS += 1"
elif [ "${rc}" = "1" ]; then
    let "SUCCESS += 1"
elif [ "${rc}" = "3" ]; then
    echo "No tests executed"
else
    echo "End of tests rc=[${rc}]"
    break_tests=1
fi

done

echo "--------- Done --------------"
echo "--- SUCCESS=${SUCCESS} ERRORS=${ERRORS} ---"
echo "-----------------------------"
