<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<br />
<c:if test="${user != null }">
<script type="text/javascript">
	function newTransformation() {
		var createDiv = $("#createDiv");
		createDiv.html('<input type="text" id="transformationName" name="transformationName" value="${newTransformationName}" /><input type="button" onclick="validateTransformation()" value="Create Transformation" />');
	}

	function validateTransformation() {
		var name = $("#transformationName").val().replace('.', '/');
		window.location.href = '<c:url value="edit-transformation/" />' + name;
	}
</script>
<div class="frontDiv">
	<div class="title">Your Transformations</div>
	<table class="wsTable">
		<tr>
			<th width="400">transformation</th>
		</tr>
		<c:forEach var="transformation" items="${ownedTransformations}" varStatus="loop">
			<tr${((loop.index % 2) == 0) ? '' : ' class="alternate"'}>
			<td><a href="transformation/${fn:replace(transformation.uri, '.', '/')}">${transformation.uri}</a></td>
			</tr>
		</c:forEach>
		<tr><td colspan="4"><div id="createDiv"><a class="action" href="javascript:newTransformation()"><img height="20" width="20" border="0" style="vertical-align:middle" src="images/new.png" />Create a new transformation</a></div></td></tr>
	</table>
</div>
</c:if>
<div class="frontDiv">
	<div class="title">Public Transformations</div>
	<table class="wsTable">
		<tr>
			<th width="400">transformation</th>
		</tr>
		<c:forEach var="transformation" items="${publicTransformations}" varStatus="loop">
			<tr${((loop.index % 2) == 0) ? '' : ' class="alternate"'}>
			<td><a href="transformation/${fn:replace(transformation.uri, '.', '/')}">${transformation.uri}</a></td>
			</tr>
		</c:forEach>
	</table>
</div>
<jsp:include page="includes/footer.jsp" />