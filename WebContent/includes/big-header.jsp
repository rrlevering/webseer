<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html style="height:100%">
<head>
<title>webseer<%if (request.getParameter("title") != null) {%>: ${ param.title } (${ param.type })<%}%></title>
<link href="<c:url value="/styles/webseer.css" />" rel="stylesheet" type="text/css" />
<link href="<c:url value="/lib/jquery/jquery-ui.css" />" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="<c:url value="/lib/jquery/jquery.js" />"></script>
<script type="text/javascript" src="<c:url value="/lib/jquery/jquery-ui.min.js" />"></script>
</head>
<body style="height:100%;margin: 0">
<div id="container">
<div id="header" style="overflow:hidden;height:57">
	<div style="float:left;height:57">
		<a href="<c:url value="/" />"><img src="<c:url value="/images/webseer-left.png" />" /></a>
	</div>
	<div style="float:left;height:28;margin-top:15">
		<a href="<c:url value="/" />" class="header">webseer</a>
	</div>
	<div style="overflow:hidden;height:57">
		<img height="57" style="width:100%" src="<c:url value="/images/webseer-right.png" />">
	</div>
</div>
<div id="body">
