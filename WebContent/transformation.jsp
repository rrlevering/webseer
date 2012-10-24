<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<link href="<c:url value="/lib/prettify/prettify.css" />" type="text/css" rel="stylesheet" />
<script type="text/javascript" src="<c:url value="/lib/prettify/prettify.js" />" ></script>
<div style="font-weight:bold;margin:10 0 10 0">${transformation.simpleName}<br /><span style="font-size:75%">${transformation.name} (v${transformation.version})</span></div>
<div style="margin:10 0 10 0">${transformation.description }</div>
<c:if test="${transformation.keyWords.length > 0 }">
	<div style="margin:10 0 10 0">Keywords:
		<c:forEach var="keyWord" items="${transformation.keyWords}" varStatus="loop">
			<span style="border:1px solid black">${keyWord}</span>
		</c:forEach>
	</div>
</c:if>
<table class="wsTable" cellpadding="0" cellspacing="0">
	<tr>
		<th width="400">input</th>
		<th width="200">type</th>
	</tr>
	<c:forEach var="inputPoint" items="${transformation.inputPoints.iterator()}" varStatus="loop">
		<tr${((loop.index % 2) == 0) ? '' : ' class="alternate"'}>
			<td>${inputPoint.name}</td>
			<td>${inputPoint.type.name}</td>
		</tr>
	</c:forEach>
</table>
<table class="wsTable" cellpadding="0" cellspacing="0">
	<tr>
		<th width="400">output</th>
		<th width="200">type</th>
	</tr>
	<c:forEach var="outputPoint" items="${transformation.outputPoints.iterator()}" varStatus="loop">
		<tr${((loop.index % 2) == 0) ? '' : ' class="alternate"'}>
			<td>${outputPoint.name}</td>
			<td>${outputPoint.type.name}</td>
		</tr>
	</c:forEach>
</table>
<div>Language: ${transformation.language }</div>
<div>Runtime: ${transformation.runtime }</div>
<pre style="font-size:75%" class="prettyprint">${transformation.code }</pre>
<script>
prettyPrint();
</script>
<jsp:include page="includes/footer.jsp" />