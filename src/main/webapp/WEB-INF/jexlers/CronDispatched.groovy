// Jexler { autostart = true }

// dispatch to methods below
JexlerDispatcher.dispatch(this)

// optional, called first
void declare() {
  log.trace('-- declare()')
  cron = '* * * * *'
}

// mandatory, called after declare(), before the event loop
void start() {
  log.trace('-- start()')
  services.add(new CronService(jexler, 'EveryMinute').setCron(cron))
  services.add(new CronService(jexler, 'Every30Seconds').setCron('0/30 * * * * *'))
  services.add(new CronService(jexler, 'OnceImmediately').setCron('now'))
  services.start()
}

// optional*, called during event loop
// handle<event-class><service-id>(event), searched first
void handleCronEventEveryMinute(def event) {
  log.trace('-- handleCronEventEveryMinute(event)')
  log.trace("every minute, it is now ${new Date()} ($event.service.id)")
}

// optional*, called during event loop
// handle<event-class><service-id>(event), searched second
void handleCronEventEvery30Seconds(def event) {
  log.trace('-- handleCronEventEvery30Seconds(event)')
  log.trace('every 30 seconds')
}

// optional*, called during event loop
// handle(event), fallback, searched last
// * if no matching handler was found, an issue is tracked
void handle(def event) {
  log.trace('-- handle(event)')
  log.trace("got event $event.service.id")
}

// optional, called after receiving StopEvent in the event loop, just before the script returns
void stop() {
  log.trace('-- stop()')
  // nothing to do, services.stop() is called automatically after the script returns
}