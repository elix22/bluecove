# Introduction #

> Current BlueCove winsock (Microsoft Bluetooth stack) and WIDCOMM code are compiled for Windows Mobile.

> Version 2.0.0 has been tested with following JVMs:
    * [Mysaifu JVM ](http://www2s.biglobe.ne.jp/~dat/java/project/jvm/index_en.html) v0.3.3 - An open-source (GPL v.2 license) Java VM on Windows Mobile 2003
    * [IBMs WebSphere Everyplace Micro Environment](http://www.ibm.com/software/wireless/weme/) v5.7.2, CDC 1.0/Foundation 1.0/Personal Profile 1.0 for Windows XP/X86
    * [IBMs WebSphere Everyplace Micro Environment](http://www.ibm.com/software/wireless/weme/) v6.1.1, CDC 1.0/Foundation 1.0/Personal Profile 1.0 for Windows XP/X86 and Windows Mobile 2003
    * [IBMs WebSphere Everyplace Micro Environment](http://www.ibm.com/software/wireless/weme/) v5.7.2, CLDC 1.1, MIDP 2.0 for Windows XP/X86

## Windows Mobile devices with Microsoft Bluetooth Stack ##

### Windows Mobile 2003 Phone Edition with Microsoft Bluetooth Stack ###

  * Motorola MPx220
  * E-Plus PDA 2
  * O2 XDA II (T-Mobile MDA-II)
  * Qtek 2020
  * Samsung SGH-i1300
  * T-Mobile SDA
  * Vodafone VPA II
  * I-mate JAM


### Windows Mobile 5 Phone with Microsoft Bluetooth Stack ###

  * T-Mobile MDA
  * QTek 9100


### Windows Mobile 5 with Microsoft Bluetooth Stack ###

  * Dell Axim X51v


## Windows Mobile devices with WIDCOMM Bluetooth Stack ##

> All other not listed above :)

## JSR-82 Installation for IBM J9 on Windows Mobile ##

### J9 for MIDP 2.0 Profile ###

  * Copy to `bluecove.jar` %J9\_HOME%\lib\jclMidp20\ext directory
  * Copy intelbth\_ce.dll or bluecove\_ce.dll to %J9\_HOME%\bin directory
  * run app "%J9\_HOME%\bin\j9.exe" -jcl:midp20 -Dmicroedition.connection.pkgs=com.intel.bluetooth -cp yourMidpApp.jar "-jxe:%J9\_HOME%\lib\jclMidp20\jclMidp20.jxe" yourMidpApp.jad

### J9 for Personal Profile 1.0 ###

  * Add `bluecove.jar` to your classpath
  * Add system property `-Dmicroedition.connection.pkgs=com.intel.bluetooth` when running your application to enable JSR-82 connections.


## Links to Java on PDA sites ##

  * [List of Java Support on Pocket PC](http://users.comp.lancs.ac.uk/computing/users/fittond/ppcjava.html)

  * [JAVA for PocketPC PDA's](http://www.berka.name/stan/jvm-ppc/java_for_pda.html)