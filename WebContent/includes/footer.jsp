<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
</td></tr>
<tr height="57" id="bottomRow"><td colspan="3" width="100%">
<img height="57" width="100%" src="<c:url value="/images/webseer-right.png" />">
<table width="100%" style="margin-top:-20px"><tr><td>copyright 2010 Ryan Levering</td><td align="right"><c:choose><c:when test="${user == null}"><a href="<c:url value="/login" />" />login</a></c:when>
	<c:otherwise>Logged in as: <a href="<c:url value="/user" />">${user.name}</a> | <a href="<c:url value="/logout" />" />logout</a></c:otherwise></c:choose></td></tr></table>
</td></tr>
</table>
</body>
</html>
