**BlueCove** is a JSR-82 J2SE implementation that currently interfaces with the Mac OS X, WIDCOMM, BlueSoleil and Microsoft Bluetooth stack found in Windows XP SP2 and newer. Originally developed by Intel Research and currently maintained by volunteers.

BlueCove runs on any JVM starting from version 1.1 or newer on Windows Mobile, Windows XP and Windows Vista, Mac OS X. [details](stacks.md)

Since version 2.1 BlueCove distributed under the Apache Software License, Version 2.0

Linux BlueZ support added in BlueCove version 2.0.3 as additional GPL licensed module.

BlueCove provides Java API for Bluetooth JSR 82. See [Documentation](Documentation.md) and [FAQ](FAQ.md) to get started.

![http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/stack-diagram.png](http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/stack-diagram.png)

_Legal_ _For various technical and legal reasons, BlueCove library is not legally referred to as an implementation of [JSR-82](http://jcp.org/en/jsr/detail?id=82). In order to legally refer to a piece of software as a JSR, it must pass the [JSR-82 TCK](https://opensource.motorola.com/sf/sfmain/do/viewProject/projects.jsr82) or [Sun TCK](http://java.sun.com/scholarship/)._ Current BlueCove JSR-82 TCK compatibility status can be found [here](http://www.bluecove.org/tck/) or [here](http://snapshot.bluecove.org/tck/). Mainly there are some missing API in native stacks to implement full TCK compatibility.

Also we are using our own tests [bluecove-tester](http://www.bluecove.org/bluecove-examples/bluecove-tester/) to ensure quality of the product and ease of development.

See [Changelog](Changelog.md) and [Nightly Build](http://snapshot.bluecove.org/) for the latest product information.

If you have a questions or require supports use Goolge Groups [bluecove-users](http://groups.google.com/group/bluecove-users/)

BlueCove v2.0.X and before had been licensed under [GNU Lesser General Public License](http://www.gnu.org/licenses/lgpl.html).

BlueCove-gpl BlueZ module is licensed under [GNU General Public License](http://www.gnu.org/licenses/gpl.html).

BlueCove v2.1.0 is licensed under [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

BlueCove-BlueZ module is licensed under [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).