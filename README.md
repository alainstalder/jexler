[![image](jexler.jpg)](https://grengine.ch/jexler/)

# jexler

Jexler is a simple relaxed Groovy framework for starting/stopping
Groovy scripts as services and enabling them to react to events
of their choice - great for prototyping and useful for automating.

Contains a core library plus a web GUI (WAR).

* Groovy (Java VM 8 or later), Gradle, Apache 2.0 License
* Web GUI: Groovy, Servlet 2.5
* Unit tests written with [Spock](https://code.google.com/p/spock/)

## Build

* Build with only fast unit tests: `./gradlew clean build`
* Build with also slow unit tests: `./gradlew clean slowTests`

## Try web GUI

* Demo: `./gradlew demo`
* Go to http://localhost:9080/
* or deploy jexler/build/libs/jexler-*.war
* or deploy jexler-*.war from sourceforge (link below)

## Resources

* Website: [grengine.ch/jexler](https://grengine.ch/jexler/)
* Users' Guide: [guide.md](guide.md)
* Groovydoc: [grengine.ch/jexler/groovydoc/](https://www.grengine.ch/jexler/groovydoc/)
* JaCoCo: [grengine.ch/jexler/jacoco/](https://www.grengine.ch/jexler/jacoco/)
* Release Notes: [releases.md](releases.md)
* jexler (WAR): [TODO](https://TODO)

## Screenshot

[![image](https://raw.githubusercontent.com/alainstalder/jexler/master/guide/jexler-gui.jpg)](https://raw.githubusercontent.com/alainstalder/jexler/master/guide/jexler-gui.jpg)
