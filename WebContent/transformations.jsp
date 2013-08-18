<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<div class="frontDiv">
	<div class="title">Transformations</div>
	<table class="wsTable">
		<tr>
			<th width="400">data</th>
		</tr>
		<c:forEach var="transformation" items="${transformations}" varStatus="loop">
			<tr${((loop.index % 2) == 0) ? '' : ' class="alternate"'}>
			<td><a href="transformation/${fn:replace(transformation.name, '.', '/')}">${transformation.name}</a></td>
			<td><a href="transformation/${fn:replace(transformation.name, '.', '/')}">${transformation.name}</a></td>
			</tr>
		</c:forEach>
	</table>
</div>
<jsp:include page="includes/footer.jsp" />