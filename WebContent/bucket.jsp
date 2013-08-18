<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<div class="title">Bucket ${name}
 <c:if test="${editable}">
		<a href="<c:url value="/edit-bucket/${name}" />" style="font-size:-1">[edit]</a>
 </c:if>
</div>
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
Generate a new bucket:
<form name="transform" method="post">
<select name="transformationToRun">
	<c:forEach var="transformation" items="${transformations}" varStatus="loop">
		<option>${transformation.uri}</option>
	</c:forEach>
</select>
<input type="text" name="newBucketName" />
<input type="submit" value="Transform" />
</form>
<jsp:include page="includes/footer.jsp" />