<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<link href="<c:url value="/lib/prettify/prettify.css" />" type="text/css" rel="stylesheet" />
<script type="text/javascript" src="<c:url value="/lib/prettify/prettify.js" />" ></script>
<div style="font-weight:bold;margin:10 0 30 0">${name}</div>
<jsp:useBean id="lastModified" class="java.util.Date" />
<jsp:setProperty name="lastModified" property="time" value="${version}" />
<form method="post">
<div>Dependencies:</div>
<c:forEach var="dependency" items="${dependencies}" varStatus="loop">
	<div>${dependency.group}:${dependency.name }:${dependency.version} <input type="button" value="delete" /></div>
</c:forEach>
<input type="text" style="width:200px" name="newDependency" />
<div>Source (last modified <fmt:formatDate pattern="yyyy-MM-dd hh:mm" value="${lastModified}" />)</div>
<script type="text/javascript" src="<c:url value="/lib/codemirror/lib/codemirror.js" />" ></script>
<link rel="stylesheet" href="<c:url value="/lib/codemirror/lib/codemirror.css" />">
<link rel="stylesheet" href="<c:url value="/lib/codemirror/theme/eclipse.css" />">
<script type="text/javascript" src="<c:url value="/lib/codemirror/mode/clike/clike.js" />" ></script>
<script src="<c:url value="/lib/codemirror/addon/edit/matchbrackets.js" />"></script>
<script src="<c:url value="/lib/codemirror/addon/selection/active-line.js" />"></script>
<script src="<c:url value="/lib/codemirror/addon/edit/closebrackets.js" />"></script>
    <style>
    .CodeMirror {border: 2px inset #dee; height:600px; width:800px;}
    .CodeMirror-activeline-background {background: #e8f2ff !important;}
    .CodeMirror-lines * { font-family: monospace !important; }
    </style>
	<textarea name="source" id="source" style="width:800px;height:600px">${currentSource}</textarea>
    <script>
      var editor = CodeMirror.fromTextArea(document.getElementById("source"), {
        lineNumbers: true,
        indentUnit: 4,
        matchBrackets: true,
        autoCloseBrackets: true,
        styleActiveLine: true,
        mode: "text/x-java",
        theme: "eclipse"
      });
    </script>

	<input type="submit" value="Save" />
</form>
<div style="color:red">${errorMessage}</div>
<jsp:include page="includes/footer.jsp" />