// Jexler { autostart = true }

services.add(new CronService(jexler, 'every-minute').setCron('* * * * *'))
services.add(new CronService(jexler, 'every-30-seconds').setCron('*/30 * * * * *'))
services.add(new CronService(jexler, 'once-immediately').setCron('now'))
services.start()

while (true) {
  event = events.take()
  if (event instanceof CronEvent) {
    log.trace("It is now: ${new Date()} (${event.service.id})")
    log.trace("Util.hello(): ${Util.hello()}")
    Util.logMethodCall(this)
    log.trace('body: ' + JexlerUtil.toSingleLine(new Util().httpGet('https://www.google.com/')))
  } else if (event instanceof StopEvent) {
    return
  }
}
