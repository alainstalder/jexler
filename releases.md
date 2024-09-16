[![image](jexler.jpg)](https://grengine.ch/jexler/)

# jexler releases

### 4.1.0 (20XX-YY-ZZ)

* Groovy 4.0.22, Grengine 3.1.0 (works also with Groovy 2 and 3).
* Packages renamed to `ch.grengine.jexler.*`.
* No longer JAR release of core classes, just WAR is released
  (core API unchanged, Groovydoc+JaCoCo on website updated).

### 4.0.2 (2023-02-26)

* Groovy 4.0.9, Grengine 3.0.2 (works also with Groovy 2 and 3).
* Shows Java version in Jexler tooltip.

### 4.0.1 (2019-01-22)

* Fixed Maven dependencies.

### 4.0.0 (2019-01-22)

* Changed package names from `net.jexler` to `ch.artecat.jexler`.
* If the source of a running jexler is removed, no longer adding
  it to the sources of jexlers to compile.
* Eliminated dependency on `javax.xml.bind.DatatypeConverter`.
* Grengine 3.0.0, Groovy 2.5.5, quartz 2.3.0.

### 3.0.2 (2017-03-04)

* Using Grengine 1.2.0 for Grape support.

### 3.0.1 (2017-02-28)

* Jexlers are already compiled and loaded in separate threads,
  for faster startup of all jexlers.
* Fix/workaround around issue with Grape that loading a class
  with Grape dependencies only works reliably if a parent
  GroovyClassLoader already added the respective dependencies
  to its classpath.

### 3.0.0 (2017-02-26)

* Powered by Grengine, for more robust and economic operation.
* Removed: jexlerBinding, autoimport.
* Changed: meta info map to meta config comment to distinguish
  runnable jexlers from utility classes, config, etc; the
  workaround for Groovy issue GROOVY-7407 is now active by default.
* New: getAsConfig() for faster and more economic config slurping,
  using Grengine.

### 2.1.9 (2017-02-20)

* Servlet for easier handling of HTTP REST calls by jexler scripts;
  see user guide and source code and default settings for details.

### 2.1.8 (2017-02-19)

* GUI visually refined.
* HTTP headers for no storing or caching.
* Hopefully this can now remain essentially as is for a while.

### 2.1.7 (2017-02-18)

* Restart all jexlers in GUI now again skips the ones without
  autostart set.

### 2.1.6 (2017-02-18)

* Post+redirect for actions that modify state, so that the
  reload browser button can be routinely used.
* Info button with link to user guide.
* Easier customization of web GUI, settings in separate files,
  no longer in web.xml.

### 2.1.5 (2017-02-16)

* GUI start/stop/restart/zap to perfection.

### 2.1.4 (2017-02-15)

* When waiting for container timeout, report issues in jexlers
  that failed to start or stop, no longer in the container.
* Busy wheel in GUI when starting/stopping jexlers.

### 2.1.3 (2017-02-15)

* New methods `events.isNextStop()` and `events.hasStop()`
  to detect if stopping is desired between polling events
  from the jexler event queue.

### 2.1.2 (2017-02-13)

* Access to binding variables with 'jexlerBinding' instead
  of with 'Jex.vars'.

### 2.1.1 (2017-02-12)

* Refinements in code and documentation.
* DirWatchService allows to set kinds and modifiers;
  the latter is useful on Mac OS X to speed up reaction times.
* Faster build.

### 2.1.0 (2017-02-11)

* Streamlined internally, practically no impact on scripts.
* Requires JDK 8.
* Starting/stopping jexlers more responsive in GUI.

### 2.0.5 (2017-02-04)

* New look.

### 2.0.4 (2017-02-03)

* Added zap() method to Service interface for quick forceful
  termination of services, including jexlers.
* Allowing to zap unresponsive jexlers in GUI after failing to
  stop after timeout (show zap icon instead of stop icon then).
* Handling *.ttf with default servlet.

### 2.0.3 (2017-02-02)

* Access to binding variables in other classes more robust.
* More tooltips in GUI.
* Web error page highlights jexlers and uses Google fonts.

### 2.0.2 (2017-02-02)

* Access to binding variables in other classes (e.g. Jex.vars.log).
* No longer duplicate groovy*.jar in jexler war.
* Highlight jexlers in issue (stacktrace and cause) in GUI.
* Google fonts.

### 2.0.1 (2015-10-31)

* Jexler can now handle HTTP requests to the webapp its GUI is running in,
  i.e. individual jexlers can also offer a simple web GUI to present their
  status and results, or for similar simple use cases.
* The Jexler class has a new method `Script getScript()` to get the instance
  of its compiled script or null if not operational.

### 2.0.0 (2015-05-20)

* Jexler is now written in Groovy (unit tests with Spock), no longer
  Java 7 (and JUnit).
* Scheduling is now based on Quartz, no longer on Cron4j, which allows
  now to schedule at shorter intervals (seconds instead of minutes).
* Various small changes and simplifications, which should almost always
  have no effect on existing jexler scripts.

### 1.0.16 (2015-05-12)

* JexlerDispatcher (new): Allows to dispatch a typical stages in a jexler's
  life cycle and during event handling to individual handler methods, like
  declare(), start(), handleCronEvent(event), stop().
* Renamed the `Jexlers` class to `JexlerContainer` and the corresponding variable
  available in jexler scripts from `jexlers` to `container`.
* The shared cron4j Scheduler for CronService and DirWatcherService is no longer
  global, but per jexler container and can alternatively be explicitly set;
  stop the shared instance with the close() method of Jexlers.

### 1.0.15 (2015-05-10)

* Removed obsolete older workaround for GROOVY-7407 with compile retries.
* Dependence to indy version of groovy-all.

### 1.0.14 (2015-05-05)

* CronService and DirWatcherService now internally use a shared instance
  of a cron4j Scheduler to reduce the number of threads needed per service.
  Previously, each new service created its own new Scheduler instance,
  which then created a new thread. In DirWatcherService, the setter
  setSleepTimeMs() has been superseded by a new setter setCron().
* Improved multi-threading support.

### 1.0.13 (2015-05-03)

* Improved multi-threading support.
* Various internal code refinements after inspection.
* GUI: Javascript jexlers status update requests wait until previous call
  is done (load, abort, error or timeout).

### 1.0.12 (2015-05-02)

* New better workaround for GROOVY-7407, see user guide.

### 1.0.11 (2015-05-01)

* Optional partial workaround for a fundamental bug with Groovy/Grape/Ivy:
  "Compilation not thread safe if Grape / Ivy is used in Groovy scripts",
  https://issues.apache.org/jira/browse/GROOVY-7407, see user guide.
* A compiled jexler is now only instantiated and run if it is an
  instance of groovy.lang.Script.
* GUI fix: List of jexlers updates again when showing logs/issues.

### 1.0.10 (2015-04-18)

* When running a jexler Groovy script, now any Throwable is caught,
  not just Exception. Consequently, issues now have a getCause() method
  that returns the causing Throwable, no longer a getException() method.
* Renamed StrongerObfuscatorTool to StringObfuscatorTool and removed
  the deprecated ObfuscatorTool.
* Updated dependency to Groovy 2.4.3.

### 1.0.9 (2015-02-23)

* Updated dependencies to current versions (like Groovy 2.4.0).
* GUI refinements: Saving jexler preserves scroll+cursor position;
  indicators if text has changed or new jexler name has been entered.
* GUI: Jexlers are saved with unix linebreaks (LF).

### 1.0.8 (2015-02-15)

* Only GUI changed, no release of jexler-core.
* Improved GUI: Automatic scroll bars in source and status if window too small;
  dimmed status if cannot connect to web server.

### 1.0.7 (2014-05-14)

* StrongerObfuscatorTool: Supersedes the (now deprecated) ObfuscatorTool
  for somewhat more security, see user's guide and source code.

### 1.0.6 (2014-05-11)

* ShellTool: Fixed a bug that could cause the run() methods to hang
  depending on output size and added a way to handle each line of
  stdout and stderr with closures (see user's guide).
* User's guide: Updated use cases.

### 1.0.5 (2013-07-29)

* Users' guide.
* CronService: Cron string "now+stop" for a single CronEvent immediately,
  followed by a single StopEvent.
* Bugfix: Catching checked Exceptions in BasicJexler and BasicMetaInfo
  around calling Groovy scripts (because Groovy scripts may throw such
  checked Exceptions without the Java compiler being aware of the
  possibility).
* Two new context parameters in web.xml: jexler.safety.script.confirmSave 
  and jexler.safety.script.confirmDelete, see user's guide for details.

### 1.0.4 (2013-07-23)

* ShellTool: Methods with lists and maps instead of arrays.
* CronService: Cron string "now" for a single event immediately.
* Unit test coverage of jexler-core close to 100% (except for artefacts).
* GUI: Automatically updates status of jexlers every second.

== 1.0.3 (2013-07-16)

* Separated public API from internal classes.
* Added lots of unit tests.
* Javadoc.
* Maven pom and artefacts for publishing jexler-core to the
  maven central repository.

### 1.0.2 (2013-07-05)

* Some changes and new features.

### 1.0.1 (2013-06-28)

* Some changes and new features.

### 1.0.0 (2013-04-16)

* Initial release.

### 0.1.2 (early prototype, 2013-03-29)

* Some refinements after using it a bit.

### 0.0.3 (early prototype, 2013-03-16)

* Just Groovy.
* Webapp only.

### 0.0.2 (early prototype, 2013-02-24)

* Simple framework.
* Webapp that allows to start/stop jexlers, edit scripts
  (in jruby, jython or groovy), view issues and log file.
* Basic command line app that allows to start/stop jexlers.

### 0.0.1 (early prototype, 2013-02-13)

* Basic framework, unit tests, some handlers, command line and web app.
* Please ignore - about to be refactored and simplified completely.
