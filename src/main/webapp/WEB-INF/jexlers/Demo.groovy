// Jexler { autostart = true }

@Grab('org.apache.httpcomponents:httpclient:4.2.3')
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.impl.client.DefaultHttpClient

services.add(new CronService(jexler, 'every-minute').setCron('* * * * *'))
services.start()

while (true) {
  event = events.take()
  if (event instanceof CronEvent) {
    
    // run shell command
    def handler = { line ->
      if (line.startsWith('hello')) {
        log.info("stdout matched: $line")
      }
    }
    shellTool = new ShellTool().setStdoutLineHandler(handler)
    result = shellTool.run('echo hello-world')
    log.trace(result.toString())
    
    // http get request
    httpclient = new DefaultHttpClient()
    httpget = new HttpGet('http://www.google.com/')
    responseHandler = new BasicResponseHandler()
    responseBody = httpclient.execute(httpget, responseHandler)
    log.trace("response body: ${JexlerUtil.toSingleLine(responseBody)}")
    
    Thread.sleep(5000)
    
    throw new RuntimeException('Demo RuntimeException')
        
  } else if (event instanceof StopEvent) {
    return
  }
}
