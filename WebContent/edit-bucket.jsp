<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<div class="title">Bucket ${name}</div>
<table class="wsTable">
	<tr>
		<th width="400">data</th>
		<th width="200">type</th>
	</tr>
	<c:forEach var="data" items="${dataList}" varStatus="loop">
		<tr${((loop.index % 2) == 0) ? '' : ' class="alternate"'}>
		<td>${data.value }</td>
		<td>${data.type }</td>
		</tr>
	</c:forEach>
</table>
<form method="post">
<textarea name="newData"></textarea>
<input type="submit" value="Add Data" />
</form>
<jsp:include page="includes/footer.jsp" />