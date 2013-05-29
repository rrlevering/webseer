<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
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
<script>
function compile() {
	var source = editor.getValue();
	$.ajax({
		url: '<c:url value="/compile-transformation/${fn:replace(name, \".\", \"/\")}" />',
		dataType: "json",
		data: { name: '${name}', source: source },
		success: function(response) {
			updateHints(response.errors);
		},
		type: 'POST'
	});
}
</script>
<jsp:include page="includes/codemirror.jsp">
	<jsp:param name="startCode" value="${currentSource}" />
	<jsp:param name="codeAreaName" value="source" />
</jsp:include>
<input type="submit" name="action" value="Save" />
<input type="submit" name="action" value="Save and Close" />
<input type="submit" name="action" value="Cancel" />
</form>
<c:forEach var="errorMessage" items="${errorMessages}" varStatus="loop">
<div style="color:red"><c:out value="${errorMessage}" /></div>
</c:forEach>
<c:forEach var="error" items="${errors}" varStatus="loop">
	<script>
		
	</script>
</c:forEach>
<jsp:include page="includes/footer.jsp" />