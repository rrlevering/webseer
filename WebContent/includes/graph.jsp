<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<script type="text/javascript" src="lib/ECOTree/ECOTree.js"></script>
<script type="text/javascript" src="lib/dojo/dojo.js" djConfig="parseOnLoad:true, isDebug:true"></script>
<link type="text/css" rel="stylesheet" href="lib/ECOTree/ECOTree.css" />
<script type="text/javascript" src="lib/jit/jit.js"></script>
<script type="text/javascript" src="scripts/webseer-graph.js"></script>
			<script type="text/javascript">
		function changeNodeFocus(node) {
			var contentNode = dojo.byId("editDiv");

			var url;
			if (node.data.type == 'transformation') {
				url = "edit-transformation?graphId=${graphId}&groupId=${group.id}&transformationId=" + node.data.id;
			} else {
				url = "edit-model?graphId=${graphId}&groupId=${group.id}&modelId=" + node.data.id;
			}
			
			dojo.xhrGet({
			    url: url,
			    handleAs: "text",
			    load: function(data,args){
					contentNode.innerHTML = data;
			    },
			    // if any error occurs, it goes here:
			    error: function(error,args){
				
			    }
			});
					
		}
		
				var json = [
<c:forEach var="node" items="${nodes}">
{
				<c:if test="${node.classId == 4}">
				"id": "transformationNode${node.nodeId}",
			    "name": "${node.label}",
			    "data": {
				"$type": "rectangle",
				"$color": "white",
				"type": "transformation",
				"label": "${node.label}",
				"uri": "${node.formalURI}",
				"id": "${node.linkId}"
			    },
				"adjacencies":[<c:if test="${node.parentId != -1}">"modelNode${node.parentId}"</c:if>]
				</c:if>
				<c:if test="${node.classId == 2}">
				"id": "modelNode${node.nodeId}",
			    "name": "${node.label}",
			    "data": {
				"$type": "rectangle",
				"$color": "#9C8AA5",
				"type": "model",
				"label": "${node.label}",
				"uri": "${node.formalURI}",
					"id": "${node.linkId}"
			    },
				"adjacencies":[<c:if test="${node.parentId != -1}">"transformationNode${node.parentId}"</c:if>]
				</c:if>
				<c:if test="${node.classId == 3}">
				"id": "transformationNode${node.nodeId}",
			    "name": "${node.label}",
			    "data": {
				"$type": "rectangle",
				"$color": "white",
				"type": "transformation",
				"label": "${node.label}",
				"uri": "${node.formalURI}",
					"id": "${node.linkId}"
			    },
				"adjacencies":[<c:if test="${node.parentId != -1}">"modelNode${node.parentId}"</c:if>]
				</c:if>
				<c:if test="${node.classId == 1}">
				"id": "modelNode${node.nodeId}",
			    "name": "${node.label}",
			    "data": {
				"$type": "rectangle",
				"$color": "#99CCFF",
				"type": "model",
				"label": "${node.label}",
				"uri": "${node.formalURI}",
					"id": "${node.linkId}"
			    },
				"adjacencies":[<c:if test="${node.parentId != -1}">"transformationNode${node.parentId}"</c:if>]
				</c:if>
},
</c:forEach>
					];
			
			</script>
			<div id="jitDiv"></div>
<div id="editDiv" style="text-align:left">
</div>
<script>
init(json, changeNodeFocus);
</script>
			