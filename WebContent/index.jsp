<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<br />
<!---
<form id="frontHook">
<input type="text" size="100" value="http://www.google.com" /><input class="wsSubmit" type="submit" value="Classify" />
</form>
--->
<c:if test="${user != null }">
<script language="JavaScript">
	function newWorkspace() {
		var createDiv = dojo.byId("createDiv");
		createDiv.innerHTML = '<input type="text" id="workspaceName" name="workspaceName" value="New Workspace" /><input type="button" onclick="validateWorkspace()" value="Create Workspace" />';
	}

	function validateWorkspace() {
		$.ajax({
			url: "createWorkspace?workspaceName=" + $("#workspaceName").val(),
			dataType: "json",
			success: function(newWorkspace) {
				window.location.href = "workspace/" + newWorkspace.id + "/edit"; 
			}
		});
	}
</script>
<div class="frontDiv">
	<div class="title">Your Workspaces</div>
	<table class="wsTable" cellpadding="0" cellspacing="0">
		<tr>
			<th width="400">workspace</th>
			<th width="200">programs</th>
			<th width="200">buckets</th>
		</tr>
		<c:forEach var="workspace" items="${ownedWorkspaces}" varStatus="loop">
			<tr${((loop.index % 2) == 0) ? '' : ' class="alternate"'}>
			<td><a href="workspace/${workspace.id}/edit">${workspace.name}</a></td>
			<td>${workspace.programCount}</td>
			<td>${workspace.bucketCount}</td>
			</tr>
		</c:forEach>
		<tr><td colspan="4"><div id="createDiv"><a class="action" href="javascript:newWorkspace()"><img height="20" width="20" border="0" style="vertical-align:middle" src="images/new.png" />Create a new workspace</a></div></td></tr>
	</table>
</div>
</c:if>
<c:if test="${fn:length(publicWorkspaces) > 0 }">
<div class="frontDiv">
	<div class="title">Public Workspaces</div>
	<table class="wsTable" cellpadding="0" cellspacing="0">
		<tr>
			<th width="400">workspace</th>
			<th width="200">owner</th>
			<th width="200">programs</th>
			<th width="200">buckets</th>
		</tr>
		<c:forEach var="workspace" items="${publicWorkspaces}" varStatus="loop">
			<tr${((loop.index % 2) == 0) ? '' : ' class="alternate"'}>
			<td><a href="workspace/${workspace.id}/view">${workspace.name}</a></td>
			<td>${workspace.ownerName}</td>
			<td>${workspace.programCount}</td>
			<td>${workspace.bucketCount}</td>
			</tr>
		</c:forEach>
	</table>
</div>
</c:if>
<jsp:include page="includes/footer.jsp" />