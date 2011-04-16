<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
<title>webseer<%if (request.getParameter("title") != null) {%>: ${ param.title } (${ param.type })<%}%></title>
<link href="<c:url value="/styles/webseer.css" />" rel="stylesheet" type="text/css" />
<link href="<c:url value="/lib/ajaxtabs/ajaxtabs.css" />" rel="stylesheet" type="text/css" />
<link type="text/css" href="<c:url value="/lib/jquery/jquery-ui.css" />" rel="stylesheet"/>
<script type="text/javascript" src="<c:url value="/lib/dojo/dojo.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/jquery/jquery.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/jquery/jquery-ui.js" />"></script>
<script type="text/javascript">

function loadTabs() {
	var tabs=new ddajaxtabs("${param.tabId}", "facetcontainer");
	tabs.setpersist(true);
	tabs.setselectedClassTarget("link"); //"link" or "linkparent"
	tabs.init();
}
</script>
</head>
<body style="margin: 0">
<div id="headerDiv" style="float:right">
<table>
<tr height="1"><td valign="middle"><a href="<c:url value="/" />" class="header">webseer</a></td><td><a href="<c:url value="/" />"><img border="0" src="<c:url value="/images/webseer-flip.png" />" /></a></td></tr>
</table>
</div>
<table style="margin: 0" width="100%" height="100%">
<tr><td colspan="3" valign="top">
