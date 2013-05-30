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
<div id="dependencies">Dependencies:
	<c:forEach var="dependency" items="${dependencies}" varStatus="loop">
		<div id="${dependency.safeId}">${dependency.id} <input type="button" value="delete" onclick="removeDependency('${dependency.id}', '${dependency.safeId}')" /></div>
	</c:forEach>
</div>
<style>
  .ui-autocomplete {
    max-height: 200px;
    overflow-y: auto;
    /* prevent horizontal scrollbar */
    overflow-x: hidden;
   }
</style>
<input id="dependency" type="text" style="width:300px" name="newDependency" />
<script>
$(function() {
	var dependency = $('#dependency');
	
	dependency.autocomplete({
		minLength: 2,
		delay: 500,
		source: function(request, response) {
			 $.ajax({
				url: '<c:url value="/search-dependencies" />',
				dataType: "json",
				data: { q: request.term },
				success: function(data) {
					response($.map(data.dependencies, function(item) {
						return {
							label: item.id,
							value: item.safeId
						};
					}));
				},
				type: 'GET'
			});
		},
		select: function(event, ui) {
			$.ajax({
				url: '<c:url value="/add-dependency/${fn:replace(name, \".\", \"/\")}" />',
				dataType: "json",
				data: { dependency: ui.item.label },
				success: function() {
					dependency.val('');
					$("#dependencies").append("<div id=\"" + ui.item.value + "\">" + ui.item.label + " <input type='button' value='delete' onclick='removeDependency(\"" + ui.item.label + "\", \"" + ui.item.value + "\")' /></div>")
					compile();
				},
				type: 'POST'
			});
			return false;
		}
	});
});
</script>
<div>Source (last modified <fmt:formatDate pattern="yyyy-MM-dd hh:mm" value="${lastModified}" />)</div>
<script>
function removeDependency(dependencyToRemove, divId) {
	$.ajax({
		url: '<c:url value="/remove-dependency/${fn:replace(name, \".\", \"/\")}" />',
		dataType: "json",
		data: { dependency: dependencyToRemove },
		success: function() {
			$("#" + divId).remove();
			compile();
		},
		type: 'POST'
	});
}

function compile() {
	var source = editor.getValue();
	$.ajax({
		url: '<c:url value="/compile-transformation/${fn:replace(name, \".\", \"/\")}" />',
		dataType: "json",
		data: { source: source },
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