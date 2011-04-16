<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
<title>webseer<%if (request.getParameter("title") != null) {%>: ${ param.title } (${ param.type })<%}%></title>
<link href="styles/webseer.css" rel="stylesheet" type="text/css" />
<link href="lib/ajaxtabs/ajaxtabs.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="<c:url value="/lib/ajaxtabs/ajaxtabs.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/dojo/dojo.js" />"></script>
<script type="text/javascript" src="lib/ejs/EJSChart.js"> </script>
<script type="text/javascript" src="<c:url value="/lib/jquery/jquery.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/jquery/jquery-ui.min.js" />"></script>
<script type="text/javascript">

function loadTabs() {
	var tabs=new ddajaxtabs("${param.tabId}", "facetcontainer")
	tabs.setpersist(true)
	tabs.setselectedClassTarget("link") //"link" or "linkparent"
	tabs.init()
}
</script>
</head>
<body onLoad="loadTabs()" style="margin: 0">
<table style="margin: 0" width="100%" height="100%">
<tr height="1">
<td><a href="./"><img border="0" src="images/webseer-left.png" /></a></td>
<td valign="middle"><a href="./" class="header">webseer</a></td>
<td width="100%"><img height="57" style="width:100%" src="images/webseer-right.png"></td>
</tr>
<tr><td colspan="4" valign="top">
