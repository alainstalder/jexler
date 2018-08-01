// Jexler { autostart = true }

services.add(new DirWatchService(jexler, 'watch-jexler-dir'))
services.start()

while (true) {
  event = events.take()
  if (event instanceof DirWatchEvent) {
    log.trace("Got file change: ${event.kind} '${event.file.name}'")
  } else if (event instanceof StopEvent) {
    return
  }
}
