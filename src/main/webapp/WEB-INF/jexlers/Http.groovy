// Jexler { autostart = true }

JexlerDispatcher.dispatch(this)

void declare() {}

void start() {}

// Invoked when accessing the jexler webapp with ?cmd=http&jexler=<jexler>
// The single parameter 'p' is a JSP PageContext.
// (The handler is invoked independently of normal event handling in jexler,
// in separate threads, one for each parallel HTTP request.)
void handleHttp(def p) {
  log.trace('-- handleHttp')

  if (p.request.getParameter('throw') != null) {
    throw new RuntimeException("*** Failed intentionally - due to request parameter 'throw' ***")
  }

  def sysProps = new StringBuilder()
  System.properties.sort().each { key, value ->
    sysProps.append("<span style='color:darkblue'>$key</span><span style='color:darkred'>=</span>${value.replace('<', '&lt;')}\n")
  }

  p.response.status = 200
  p.out.println("""\
<html>
  <head>
    <title>Jexler Http</title>
    <link rel="stylesheet" href="jexler.css"/>
  </head>
  <body>
    <a href="."><img class="jexler" src="jexler.jpg" title="Back to jexler main view"></a>
    <h1>jexler HTTP demo</h1>
    <p>Demo: <a href="?cmd=http&jexler=${jexler.id}&throw=true" style="color:darkred;text-decoration:none">Throw exception</a></p>
    <h3>System Properties</h3>
    <pre>
${sysProps}
    </pre>
  </body>
</html>
""")
}

// HTTP REST call support: The method below is invoked for requests
// to <context-path>/rest/ (see web.xml) for HTTP requests containing
// a request header 'jexler: Http' (see settings.groovy).
void service(def req, def resp) {
  resp.status = 200
  resp.writer.println("hello world")
}

void handle(def event) {
  log.trace("got event $event.service.id")
}

void stop() {}