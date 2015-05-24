**How to determine which Bluetooth stack is installed on my Windows?**

> [see this page](http://www.bluecove.org/bluecove-examples/bluetooth-stack.html)

**Where can I get help or support?**

> [Discussions questions and support here @ Goolge Groups: bluecove-users](http://groups.google.com/group/bluecove-users/)

> [Defects (bugs) should be submitted here @ Issues](http://code.google.com/p/bluecove/issues/list)

> We also offer [Paid Services and technical support for the project](http://sourceforge.net/services/project_services.php?project_id=114020)

**Where can I find API-Documentation of the BlueCove Library?**

> BlueCove is JSR-82, All JSR-82 API Documentation apply. See  [Java APIs for Bluetooth Wireless Technology](http://java.sun.com/javame/reference/apis/jsr082/)

> [BlueCove Java docs](http://www.bluecove.org/bluecove/apidocs/index.html). For application it is not recommended to use any classes or API other than defined in JSR-82.

> Also these links may be useful
    * [Nokia Bluetooth RFCOMM and L2CAP Examples](http://forum.nokia.com/info/sw.nokia.com/id/0b51461e-5f77-40f4-b755-7915ad4d0e31/MIDP_Bluetooth_RFCOMM_Example_v1_0_en.zip.html)
    * [Nokia Bluetooth API Developer's Guide v2.0](http://forum.nokia.com/info/sw.nokia.com/id/125b7ff5-f2dd-4441-8cfe-59e23c006373/MIDP_Bluetooth_API_Developers_Guide_v2_0_en.pdf.html)
    * [JSR-82 Samples](http://www.jsr82.com/)

**Where can I download current development version snapshot/nightly build?**

> BlueCove is built automatically by CruiseControl. [http://snapshot.bluecove.org/](http://snapshot.bluecove.org/). Follow the link  on the left Download \ Snapshot.  This will bring you maven repository. Take the latest jar files.


**Can I use BlueCove Bluetooth stack to ...?**

> BlueCove is not Bluetooth Protocol Stack! BlueCove is Java JSR-82 interface for following Bluetooth Profiles:
    * SDAP - Service Discovery Application Profile
    * RFCOMM - Serial Cable Emulation Protocol
    * L2CAP - Logical Link Control and Adaptation Protocol
    * OBEX - Generic Object Exchange Profile (GOEP) profile on top of RFCOMM and TCP

**Are there any books on Java and Bluetooth?**

  * Bluetooth Application Programming with the Java APIs, by C Bala Kumar, Paul Kline, Tim Thompson, ISBN-10: 1558609342, ISBN-13: 978-1558609341
> > 2008 edition: ISBN-10: 0123743427, ISBN-13: 978-0123743428
> > [Companion Website](http://books.elsevier.com/companions/9781558609341)  [Source Code](http://books.elsevier.com/companions/1558609342/software/bluetooth_application_programming_with_the_java_apis_-_application_source_code.zip)

  * Bluetooth for Java, by Bruce Hopkins and Ranjith Antony, ISBN-10: 1590590783, ISBN-13: 978-1590590782
> > [Companion Website](http://www.javabluetooth.com/index.html)    [Source Code](http://www.javabluetooth.com/jb_source.zip)

  * Bluetooth Essentials for Programmers, by Albert Huang and Larry Rudolph, ISBN-13: 9780521703758
> > [Companion Website](http://www.btessentials.com/)


**Can I distribute a non-'open source' Java application that imports BlueCove LGPL libraries?**


> Yes. Your application's license needs to allow users to modify the library, and reverse engineer your code to debug these modifications, this is only for the purposes of replacing the LGPL code with a different version of the LGPL code. (LGPL section 6)
> This doesn't mean you need to provide source code or any details about the internals of your application.

> Q: But any reverse engineering of company's code (ie. that written by company) would be unacceptable! A: This is not true. Reverse engineering (decompilation-for-interoperability) is a right given by the law in Europe. In the US, the only legally working restriction on reverse engineering is the abominable DMCA. It prohibits reverse engineering, but only when used to circumvent copy protection.

> N.B. This is my (Vlad) personal opinion on licenses. For any questions contact your corporate lawyer.

**Can I make a non-'open source' modification to BlueCove LGPL library?**

> No. You should redistribute your modified version of library under the LGPL or GPL.

**Допомога**

> Мы свои, пишите, поможем.