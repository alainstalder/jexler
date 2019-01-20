// Utility class, available to all jexlers,
// compiled together with all other utility classes.

@Grab('org.apache.httpcomponents:httpclient:4.5.3')
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import org.apache.http.conn.HttpHostConnectException

@Grab('org.apache.commons:commons-email:1.3.3')
import org.apache.commons.mail.*

class Util {
  
  private static Class exClazz = HttpHostConnectException.class

  static def hello() {
    return 'Hello World'
  }
  
  static def log = JexlerContainer.getLogger()

  static logMethodCall(def script) {
    log.trace("Method called for jexler '$script.jexler.id'")
    script.log.trace("Method called...")
  }
  
  def httpGet(String url) { 
    def httpclient = new DefaultHttpClient()
    def httpGet = new HttpGet(url)
    def response = httpclient.execute(httpGet)
    try {
      log.trace("status: $response.statusLine.statusCode")
      def entity = response.getEntity()
      if (entity != null) {
         return EntityUtils.toString(entity)
      } else {
         return '<no body>'
      }
    } finally {
      httpGet.releaseConnection()
    }
  }

  def sendMail(recipients, subject, msg) {
    try {
      new SimpleEmail().with {
        setFrom "jexler@artecat.ch"
        for (to in recipients) {
          addTo to
        }
        setHostName "localhost"
        setSubject subject
        setMsg msg
        send()
      }
      log.trace("mail successfully sent")
    } catch (EmailException e) {
      jexler.trackIssue(null, "Could not send mail.", e)
    }
  }

}
