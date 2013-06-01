<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<div style="font-weight:bold;margin:10 0 30 0">${name}</div>
<jsp:useBean id="lastModified" class="java.util.Date" />
<jsp:setProperty name="lastModified" property="time" value="${version}" />
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
<div style="margin-bottom:10px">
	Search: <input id="dependency" type="text" style="width:300px" name="newDependency" /> or <input type="button" value="Upload" onclick="uploadNewDependency()"/>
</div>
<div id="uploadDependency" title="Upload Dependency">
<form name="uploadForm" id="uploadForm">
<table>
Uploaded archives will be registered with a group name of your login email.
<tr><td>Library name:</td><td><input type="text" name="artifactId" /></td></tr>
<tr><td>Version:</td><td><input type="text" name="version" /></td></tr>
<tr><td>File:</td><td><input type="file" name="uploadedJar" accept="application/java-archive" id="uploadedJar" /></td></tr>
</table>
</form>
</div>
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
					$("#dependencies").append("<div id=\"" + ui.item.value + "\">" + ui.item.label + " <input type='button' value='delete' onclick='removeDependency(\"" + ui.item.label + "\", \"" + ui.item.value + "\")' /></div>");
					compile();
				},
				type: 'POST'
			});
			return false;
		}
	});
	
	$( "#uploadDependency" ).dialog({
	      autoOpen: false,
	      height: 300,
	      width: 500,
	      modal: true,
	      buttons: {
	          "Create library": function() {
					var formData = new FormData($("#uploadForm")[0]);
					$.ajax({
							url: '<c:url value="/upload-dependency/${fn:replace(name, \".\", \"/\")}" />',
							dataType: "json",
							success: function(dependency) {
								$("#dependencies").append("<div id=\"" + dependency.safeId + "\">" + dependency.id + " <input type='button' value='delete' onclick='removeDependency(\"" + dependency.id + "\", \"" + dependency.safeId + "\")' /></div>");
								compile();
							},
							error: function(xhr, error, thrown) {
	                            alert(thrown);
	                        },
							data: formData,
							type: 'POST',
							cache: false,
	                        contentType: false,
	                        processData: false
						});
 					$( this ).dialog( "close" );
	            },
	          Cancel: function() {
	            $( this ).dialog( "close" );
	          }
	        },
	    });
});
</script>
<form method="post">
<div>Source (last modified <fmt:formatDate pattern="yyyy-MM-dd hh:mm" value="${lastModified}" />)</div>
<script>
function uploadNewDependency() {
	$( "#uploadDependency" ).dialog( "open" );
}

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