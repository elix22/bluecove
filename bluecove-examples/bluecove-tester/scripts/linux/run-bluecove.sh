#! /bin/sh
#  @version $Revision$ ($Author$) $Date$
#

. ./scripts/version.sh

java -cp ./target/bluecove-tester-${VERSION}-app.jar net.sf.bluecove.awt.Main
