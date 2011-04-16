<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
	<title>${ workspace.name }</title>
	<link rel="icon" href="../favicon.ico" type="image/png" />
  <link rel="SHORTCUT ICON" href="../favicon.ico" type="image/png" />
<link href="<c:url value="/styles/webseer.css" />" rel="stylesheet" type="text/css" />

<!-- YUI -->
<link rel="stylesheet" type="text/css" href="<c:url value="/lib/wireit/lib/yui/reset-fonts-grids/reset-fonts-grids.css" />" />
<link rel="stylesheet" type="text/css" href="<c:url value="/lib/wireit/lib/yui/assets/skins/sam/skin.css" />" />

<!-- InputEx CSS -->
<link type="text/css" rel="stylesheet" href="<c:url value="/lib/wireit/plugins/inputex/lib/inputex/css/inputEx.css" />" />

<!-- YUI-accordion CSS -->
<link rel="stylesheet" type="text/css" href="<c:url value="/lib/wireit/plugins/editor/lib/accordionview/assets/skins/sam/accordionview.css" />" />

<!-- WireIt CSS -->
<link rel="stylesheet" type="text/css" href="<c:url value="/lib/wireit/assets/WireIt.css" />" />
<link rel="stylesheet" type="text/css" href="<c:url value="/lib/wireit/plugins/editor/assets/WireItEditor.css" />" />

<style>
div.WireIt-Container {
	width: 350px; /* Prevent the modules from scratching on the right */
}

div.WireIt-InOutContainer {	
	width: 150px;
}

div.WireIt-InputExTerminal {
	float: left;
	width: 21px;
	height: 21px;
	position: relative;
}
div.WireIt-InputExTerminal div.WireIt-Terminal {
	top: -3px;
	left: -7px;
}
div.inputEx-Group div.inputEx-label {
	width:100px;
}

div.WireIt-ImageContainer {
	width: auto;
}

div.Bubble div.body {
	width: 70px;
	height: 45px;
	opacity: 0.8;
	cursor: move;
}

.WiringEditor-module span {
	position: relative;
	top: -3px;
}

</style>


<!-- YUI -->
<script type="text/javascript" src="<c:url value="/lib/wireit/lib/yui/utilities/utilities.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/wireit/lib/yui/resize/resize-min.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/wireit/lib/yui/layout/layout-min.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/wireit/lib/yui/container/container-min.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/wireit/lib/yui/json/json-min.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/wireit/lib/yui/button/button-min.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/wireit/lib/yui/tabview/tabview-min.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/wireit/lib/yui/container/container-min.js" />"></script>

<!-- YUI-Accordion -->
<script src="<c:url value="/lib/wireit/plugins/editor/lib/accordionview/accordionview-min.js" />" type='text/javascript'></script>

<script src="<c:url value="/lib/wireit/lib/excanvas.js" />" type='text/javascript'></script>

<!-- WireIt -->
<script type="text/javascript" src="<c:url value="/lib/wireit/build/wireit-inputex-editor-min.js" />"></script>

<script type="text/javascript" src="<c:url value="/scripts/Webseer.js" />"></script>
<script type="text/javascript" src="<c:url value="/scripts/TerminalProxy.js" />"></script>
<script type="text/javascript" src="<c:url value="/scripts/WebseerTerminal.js" />"></script>
<script type="text/javascript" src="<c:url value="/scripts/WebseerWire.js" />"></script>
<script type="text/javascript" src="<c:url value="/scripts/WebseerContainer.js" />"></script>
<script type="text/javascript" src="<c:url value="/scripts/WebseerBucketContainer.js" />"></script>
<script type="text/javascript" src="<c:url value="/scripts/WebseerEditor.js" />"></script>
<script type="text/javascript" src="<c:url value="/scripts/WebseerAdapter.js" />"></script>
<script type="text/javascript" src="<c:url value="/scripts/StringContainer.js" />"></script>
<script type="text/javascript" src="<c:url value="/scripts/RendererContainer.js" />"></script>
<script type="text/javascript" src="<c:url value="getLanguage" />"></script>


<style>
/* Comment Module */
div.WireIt-Container.WiringEditor-module-Text-Input { width: 200px; }
div.WireIt-Container.WiringEditor-module-Text-Input div.body textarea { width:100%; background-color: transparent; font-weight: bold; border: 0; }
</style>

<style>
.yui-skin-sam .yui-layout .yui-layout-unit div.yui-layout-bd {
    background-color: white;
}
</style>
<script>

// InputEx needs a correct path to this image
inputEx.spacerUrl = "/inputex/trunk/images/space.gif";


YAHOO.util.Event.onDOMReady( function() {
	var editor = new Webseer.WebseerEditor(workspaceLanguage); 
	
	// Open the infos panel
	editor.accordionView.openPanel(2);
	
	var workspaceTitle = YAHOO.util.Dom.get('titleSpan');
	YAHOO.util.Event.addListener(workspaceTitle, "dblclick", toggleTitleEdit, null, true);
});

function toggleTitleEdit() {
	var workspaceTitle = YAHOO.util.Dom.get('titleSpan');
	var titleInput = WireIt.cn('input', {type: 'text', value: workspaceTitle.innerHTML }, null);
	workspaceTitle.innerHTML = '';
	workspaceTitle.appendChild(titleInput);
	
	YAHOO.util.Event.addListener(titleInput, "blur", updateTitle, this, true);
	YAHOO.util.Event.addListener(titleInput, "keydown", function(event, args) { if (event.keyCode == 13) { this.updateTitle(event, args); } }, this, true);
	titleInput.focus();
}

function updateTitle(event, args) {
	// Actually save it
	Webseer.WebseerAdapter.changeWorkspaceName(event.target.value, {
		success: function(result) {
			// Do nothing
		},
		failure: function(error) {
			
		}
	});
	
	var workspaceTitle = YAHOO.util.Dom.get('titleSpan');
	workspaceTitle.innerHTML = event.target.value;
}

</script>

</head>

<body class="yui-skin-sam">

	<div id="top">
		<div class="title" id="titleDiv" style="float:left">
			<div><span id="titleSpan">${ workspace.name }</span></div>
		</div>
	
		<div id="headerDiv" style="float:right">
			<table>
			<tr height="1"><td valign="middle"><a href="<c:url value="/" />" class="header">webseer</a></td><td><a href="<c:url value="/" />"><img border="0" src="<c:url value="/images/webseer-flip.png" />" /></a></td></tr>
			</table>
		</div>
	</div>


	<div id="left">
	</div>
	
	<div id="right">
	  <ul id="accordionView">
		<li>
			<h2>Minimap</h2>
			<div style='position: relative;'>
				<div id="layerMap"></div>

			</div>
		</li>
		<li>
			<h2>Data Preview</h2>

			<div>
				<div id="previewPanel"></div>
			</div>
		</li>

		
	  </ul>
	</div>
	
	<div id="item">
	</div>

	<div id="bottom">
	    <img height="57" width="100%" src="<c:url value="/images/webseer-right.png" />">
		<table width="100%" style="margin-top:-20px"><tr><td>copyright 2011 Ryan Levering</td><td align="right"><c:choose><c:when test="${user == null}"><a href="<c:url value="/login" />" />login</a></c:when>
		<c:otherwise>Logged in as: <a href="<c:url value="/user" />">${user.name}</a> | <a href="<c:url value="/logout" />" />logout</a></c:otherwise></c:choose></td></tr></table>
	</div>

	<div id="center">
	</div>

	<div id="source">
		<div class="hd" id="sourceTitle">
		</div>
		<div id="sourceBody" style="text-align: left; background-color: white; overflow: auto; height: 100%">
		</div>
	</div>
	
	<div id="typePopup">
		<div class="hd" id="typeTitle">How do these types map?
		</div>
		<div id="typeBody" style="text-align: left; background-color: white; overflow: auto; height: 100%; position: relative">
		</div>
	</div>
</body>
</html>