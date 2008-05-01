#!/bin/sh
# @version $Revision: 1266 $ ($Author: skarzhevskyy@gmail.com $) $Date: 2007-11-28 02:39:05 -0500 (Wed, 28 Nov 2007) $
#
GENERATED_VERSION="${SCRIPTS_DIR}/generated-version.sh"
if [ ! -f ${GENERATED_VERSION} ]; then
    echo "${GENERATED_VERSION} Not Found, run maven first"
    exit 1;
fi
chmod +x ${GENERATED_VERSION}
. ${GENERATED_VERSION}
# echo BLUECOVE_VERSION=${BLUECOVE_VERSION}
