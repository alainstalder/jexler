[![image](../jexler.jpg)](https://grengine.ch/jexler/)

# Jexler Docker Image

The Jexler webapp with Web GUI as a Docker image...

## Docker Hub

See https://hub.docker.com/r/jexler/jexler/tags or simply use `jexler/jexler:latest`.

## Run

Run with enclosed sample Jexler Groovy scripts:

    $ docker run --rm -it -p 9090:9090 jexler/jexler:latest

Run with mounted volume for your own Jexler Groovy scripts at `~/myjexlers`
and with desired user/group ID for the "jexler" user  (typically to match
the user/group ID of the owner of the mounted volume on the host):

    $ docker run --rm -it -p 9090:9090 -v ~/myjexlers:/jexler/jexlers -e JEXLER_UID=5555 -e JEXLER_GID=7777 jexler/jexler:latest

The jexler web GUI is then available at http://localhost:9090/.

## Configure

The Jexler Docker image extends one of the standard Tomcat images
(see FROM in the Dockerfile for exact image version).
Adapt as usual to configure e.g. SSL or user login, possibly even
in a Docker image that extends the Jexler Docker image.

To direct Tomcat logs (including jexler.log) outside the container,
mount a volume for Tomcat logs at `/usr/local/tomcat/logs`.

See also the section "Grape".

## Grape

Groovy [Grape](http://docs.groovy-lang.org/latest/html/documentation/grape.html)
is the mechanism that allows to grab (usually Maven) dependencies at runtime
with the `@Grab` annotation before import statements, like this:

```
@Grab('org.springframework:spring-orm:3.2.5.RELEASE')
import org.springframework.jdbc.core.JdbcTemplate
```

To debug eventual issues with Grape, you can add this parameter to `docker run`:
```
-e JAVA_OPTS="-Dgroovy.grape.report.downloads=true -Divy.message.logger.level=4"
```

Grape config is in `/jexlers/grape/grapeConfig.xml`, which is based on Groovy's default
config inside `groovy-<version>.jar` at `/groovy/grape/defaultGrapeConfig.xml`, but
with directories adapted and stripped of `~/.m2`, which does not exist in the container.

To use your own Grape config, simply mount
`-v /path/to/myGrapeConfig.xml:/jexlers/grape/grapeConfig.xml`,
but note that you can also modify behavior directly with annotations in Jexler
Groovy scripts, e.g. add additional repositories with `@GrabResolver`.

To share the Grape repository between different containers, mount it to
`/jexler/grape/grapes` in the container.
