![jexler logo](http://www.jexler.net/jexler.jpg)

jexler
======

Java 7, Eclipse/Gradle, Apache License

e.g.

* &gt; gradle build
* &gt; gradle eclipse

jexler webapp
-------------

* run JexlerJetty class in Eclipse
* => http://localhost:8080/
* => build/logs/jexler.log
* or deploy jexler-war/build/libs/jexler-war-*.war

jexler command line application
-------------------------------

* run JexlerCliTest class in Eclipse
* => build/logs/jexler.log
* or do this
  * &gt; cd jexler-cli
  * &gt; gradle installApp
  * &gt; build/install/jexler-cli/bin/jexler-cli ../jexler-war/src/main/webapp/WEB-INF/jexlers
  * => build/install/jexler-cli/logs/jexler.log


