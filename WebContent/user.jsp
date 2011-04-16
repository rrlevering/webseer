<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<div style="color:red;height:20px;text-align:center">${error}</div>
<br />
<form method="POST">
<input type="hidden" name="action" value="edit" />
<table style="margin-left:auto;margin-right:auto">
<c:choose>
<c:when test="${user != null}">
<input type="hidden" name="userid" value="${user.login }" />
<tr><td>Username:</td><td>${user.login}</td></tr>
</c:when>
<c:otherwise>
<tr><td>Username:</td><td><input type="text" name="newUserId" /></td></tr>
</c:otherwise>
</c:choose>
<tr><td>Email:</td><td><input type="text" name="email" value="${user.email }" /></td></tr>
<tr><td>Name:</td><td><input type="text" name="name" value="${user.name }" /></td></tr>
<tr><td height="20px"></td></tr>
<tr><td>New Password:</td><td><input type="password" name="password" /></td></tr>
<tr><td>Confirm New Password:</td><td><input type="password" name="passwordCheck" /></td></tr>
<c:if test="${user != null}">
<tr><td height="20px"></td></tr>
<tr><td>Current Password:</td><td><input type="password" name="oldPassword" /></td></tr>
</c:if>
<c:choose>
<c:when test="${user != null}">
<tr><td colspan="2" style="height:2.5em"><input type="submit" value="Update" /></td></tr>
</c:when>
<c:otherwise>
<tr><td colspan="2" style="height:2.5em"><input type="submit" value="Create" /></td></tr>
</c:otherwise>
</c:choose>

</table>
</form>
<jsp:include page="includes/footer.jsp" />