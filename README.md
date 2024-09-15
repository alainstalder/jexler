[![image](jexler.jpg)](https://grengine.ch/jexler/)

# jexler

Jexler is a simple relaxed Groovy framework for starting/stopping
Groovy scripts as services and enabling them to react to events
of their choice - great for prototyping and useful for automating.

Comes as a core library (JAR) plus a web GUI (WAR).

* Groovy (Java VM 8 or later), Gradle, Apache License
* Web GUI: Groovy, Servlet 2.5
* Unit tests written with [Spock](https://code.google.com/p/spock/)

## Build

* Quick build: `./gradlew clean build`
* Full build: `./gradlew clean build pom jacoco`

## Try web GUI

* Demo: `./gradlew demo`
* Go to http://localhost:9080/
* or deploy jexler/build/libs/jexler-*.war
* or deploy jexler-*.war from sourceforge (link below)

## Resources

* Website: [grengine.ch/jexler](https://grengine.ch/jexler/)
* jexler-core (JAR): [Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cjexler-core)
* jexler (WAR): [Sourceforge](https://sourceforge.net/projects/jexler/)
* Groovydoc (jexler-core): [grengine.ch/jexler/groovydoc/](https://www.grengine.ch/jexler/groovydoc/)
* Users' Guide: [grengine.ch/jexler/guide/](https://www.grengine.ch/jexler/guide/)

## Screenshot

[![image](https://grengine.ch/jexler/guide/jexler-gui.jpg)](https://grengine.ch/jexler/guide/jexler-gui.jpg)
