<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<link href="<c:url value="/lib/prettify/prettify.css" />" type="text/css" rel="stylesheet" />
<script type="text/javascript" src="<c:url value="/lib/prettify/prettify.js" />" ></script>
<div style="font-weight:bold;margin:10 0 10 0">${transformation.simpleName}<br /><span style="font-size:75%">${transformation.name} (v${source.version})</span></div>
<div style="margin:10 0 10 0">${transformation.description }</div>
<c:if test="${fn:length(transformation.keyWords) > 0 }">
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
<c:if test="${transformation.libraries.iterator().hasNext() }">
<table class="wsTable" cellpadding="0" cellspacing="0">
	<tr>
		<th width="200">dependency</th>
		<th width="400">group</th>
		<th width="100">version</th>
	</tr>
	<c:forEach var="library" items="${transformation.libraries.iterator()}" varStatus="loop">
		<tr${((loop.index % 2) == 0) ? '' : ' class="alternate"'}>
			<td>${library.name}</td>
			<td>${library.group}</td>
			<td>${library.version}</td>
		</tr>
	</c:forEach>
</table>
</c:if>
<div>Language: ${transformation.language }</div>
<div>Runtime: ${transformation.runtime }</div>
<pre style="font-size:75%" class="prettyprint"><c:out value="${source.code }" /></pre>
<script>
prettyPrint();
</script>
<form method="post">
<table>
<c:forEach var="inputPoint" items="${transformation.inputPoints.iterator()}" varStatus="loop">
	<tr><td>${inputPoint.name}:</td><td><textarea name="${inputPoint.name}"></textarea></td></tr>
</c:forEach>
<tr><td colspan="2"><input type="submit" name="Transform" /></td></tr>
</table>
</form>
<jsp:include page="includes/footer.jsp" />