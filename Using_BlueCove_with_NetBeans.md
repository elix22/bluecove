# To make most of NetBeans, do the following #
  * put BlueCove libraries in whatever place you like.
  * open **Tools -> Libraries -> New Library**.
  * put **"BlueCove"** in **"Library Name"** and choose **"Class Libraries"** in **"Library Type"** and press **"Ok"**.
![http://bluecove.googlecode.com/svn/wiki/images/Screenshot-NewLibrary.png](http://bluecove.googlecode.com/svn/wiki/images/Screenshot-NewLibrary.png)
  * In **Classpath** tab press **"Add JAR/Folder"** and add **bluecove-`<`version`>`.jar** and if you use linux also add **bluecove-gpl-`<`version`>`.jar**.
  * Add sources and javadoc in other tabs if you would like and press **"Ok"**.
![http://bluecove.googlecode.com/svn/wiki/images/Screenshot-LibraryManager.png](http://bluecove.googlecode.com/svn/wiki/images/Screenshot-LibraryManager.png)
  * Right click on your project and click **Properties** and from left tree click **Libraries**
  * Press **"Add Library"** and choose **BlueCove** from dialog that appears and press **"Add Library"**
![http://bluecove.googlecode.com/svn/wiki/images/Screenshot-ProjectProperties.png](http://bluecove.googlecode.com/svn/wiki/images/Screenshot-ProjectProperties.png)
  * Everything should work now.

### Note ###
> This tutorial is written for NetBeans 6.5 on Ubuntu 8.10 but should also work for earlier versions of NetBeans and on other operating systems.

> Please verify that other Requirements for BlueCove-gpl http://www.bluecove.org/bluecove-gpl/ are met.