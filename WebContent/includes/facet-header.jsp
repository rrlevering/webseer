<html>
<head>
<title>webseer<%if (request.getParameter("title") != null) {%>: ${ param.title } <%}%></title>
<link href="styles/webseer.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="lib/ejs/EJSChart.js"> </script>
<script type="text/javascript" src="lib/ejs/EJSChart_Stock.js"></script>
</head>
<body onload="resizeInnerFrame()">