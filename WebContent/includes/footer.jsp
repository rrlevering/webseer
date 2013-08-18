<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
</div>
<div id="footer">
	<img style=""height="57" width="100%" src="<c:url value="/images/webseer-right.png" />">
	<div style="position:relative;height:43px;margin-top:-10px">
		<span style="position:absolute;left:0px;padding:5px">copyright 2013 Ryan Levering</span>
		<span style="position:absolute;right:0px;padding:5px">
			<c:choose>
				<c:when test="${user == null}"><a href="<c:url value="/login" />" />login</a></c:when>
				<c:otherwise>Logged in as: ${user.name} | <a href="<c:url value="/logout" />" />logout</a></c:otherwise>
			</c:choose>
		</span>
	</div>
</div>
</div>
</body>
</html>
