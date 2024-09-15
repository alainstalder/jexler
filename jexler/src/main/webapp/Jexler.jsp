<%--
Copyright 2012-now by Alain Stalder. Made in Switzerland.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page trimDirectiveWhitespaces="true" %>

<%-- Set 'container' to main view --%>
<jsp:useBean id="container" class="ch.artecat.jexler.war.JexlerContainerView">

<%-- Handle request (start/stop jexler, etc.) --%>
<%= container.handleCommands(pageContext) %>

<%-- Set 'jexler' to container or addressed jexler --%>
<c:set var="jexler" value="${container.jexlers[param.jexler]}"/>
<c:set var="jexler" value="${jexler != null ? jexler : container }"/>

<%-- Skip alltogether if request is handled by a jexler --%>
<c:if test="${param.cmd != 'http'}">

<%-- Skip if getting status table from js, not if requesting page in browser --%>
<c:if test="${param.cmd != 'status'}">

  <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

  <html>

  <head>

  <title>Jexler</title>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
  <link rel="shortcut icon" href="favicon.ico">
  <link rel="icon" href="favicon.ico">
  <link rel="stylesheet" href="cm/lib/codemirror.css">
  <link rel="stylesheet" href="jexler.css">
  <link rel="stylesheet" href="jexler-custom.css">
  <script src="cm/lib/codemirror.js"></script>
  <script src="cm/addon/edit/matchbrackets.js"></script>
  <script src="cm/mode/groovy/groovy.js"></script>
  <script src="jexler.js"></script>
  <script src="jexler-custom.js"></script>

  </head>

  <body onLoad="onPageLoad()">

  <form action="${jexler.formAction}" method="post">

  <table class="frame">
  <tr id="header">
  <td class="frame">
  <a href="https://grengine.ch/jexler/"><img class="jexler" src="jexler.jpg" title="${container.jexlerTooltip}"></a>
  </td>
  <td class="frame frame-buttons">
    <c:choose>
    <c:when test="${param.cmd == 'log' || param.cmd == 'clearissues'}">
      <button type="submit" name="cmd" value="forget" title="Forget issues">Forget</button>
      <button type="submit" name="cmd" value="forgetall" title="Forget issues for all jexlers and container">Forget All</button>
    </c:when>
    <c:otherwise>
      <table class="frame">
      <tr>
        <td><button type="submit" name="cmd" value="save" ${container.disabledIfReadonly}
                    title="Save/create script file"
                    onclick="return isPostSave(${container.confirmSave}, '${jexler.jexlerId}')">Save as...</button></td>
        <td><button type="submit" name="cmd" value="delete" ${container.disabledIfReadonly}
                    title="Delete script file"
                    onclick="return isPostDelete(${container.confirmDelete}, '${jexler.jexlerId}')">Delete...</button></td>
        <td><input id="newjexlername" type="text" name="jexlername" onkeyup="updateSaveIndicator('${jexler.jexlerId}')"
                   title="Jexler script file name, without '.groovy' extension"
                   value="${jexler.jexlerId}" ${container.disabledIfReadonly}></td>
        <td><img id="savestatus" src="space.gif"></td>
      </tr>
      </table>
    </c:otherwise>
    </c:choose>
  </td>
  </tr>
  <tr>
  <td class="frame">
  
  <div id="statusdiv" class="autoscroll">
</c:if>

<%-- Status table --%>
<table class="status" id="status">

<tr class="status">
<td class="status">${container.startStop}</td>
<td class="status">${container.restartZap}</td>
<td class="status">${container.log}</td>
<td class="status status-name"><span title="Jexler Groovy script container"><strong>Jexlers</strong></span></td>
<td class="status">${container.web}</td>
</tr>

<c:forEach items="${container.jexlers}" var="loopJexler">
  <tr class="status">
  <td class="status">${loopJexler.value.startStop}</td>
  <td class="status">${loopJexler.value.restartZap}</td>
  <td class="status">${loopJexler.value.log}</td>
  <td class="status status-name">${loopJexler.value.jexlerIdLink}</td>
  <td class="status">${loopJexler.value.web}</td>
  </tr>
</c:forEach>

</table>

<p class="grengine">
  <a href="https://grengine.ch/"><img src="powered-by-grengine.gif" title="grengine.ch"></a>
</p>

<%-- Skip if getting status table from js, not if requesting page in browser --%>
<c:if test="${param.cmd != 'status'}">

  </div>
  
  </td>
  <td class="frame frame-text">

  <div id="sourcediv" class="autoscroll">
  <c:choose>
    <c:when test="${param.cmd == 'log' || param.cmd == 'clearissues'}">
      ${jexler.issues}
      ${jexler.logfile}
    </c:when>
    <c:otherwise>
      <textarea id="source" name="source">${jexler.source}</textarea>
      <script>
        var editor = CodeMirror.fromTextArea(document.getElementById("source"), {
          lineNumbers: true,
          mode: "${jexler.mimeType}",
          tabMode: "indent",
          matchBrackets: true,
          indentUnit: 2,
          readOnly: ${!container.scriptAllowEdit}
        });
        editor.on("change", function(cm, change) {
            updateSaveIndicator('${jexler.jexlerId}');
        });
      </script>
    </c:otherwise>
  </c:choose>
  </div>
  
  </td>
  </tr>
  </table>

  </form>

  </body>

  </html>

</c:if>

</c:if>

</jsp:useBean>
