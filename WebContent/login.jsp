<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<div style="color:red;height:20px;text-align:center">${error}</div>
<br />
<form method="POST">
<table style="margin-left:auto;margin-right:auto">
<tr><td>Username:</td><td><input type="text" name="userid" /></td></tr>
<tr><td>Password:</td><td><input type="password" name="password" /></td></tr>
<tr><td colspan="2" style="height:2.5em"><input type="submit" value="Login" /></td></tr>
</table>
</form>
<div style="text-align:center;font-size:small"><a href="user">Create a new account</a> | <a href="resetPassword">Forgot your password?</a></div>
<jsp:include page="includes/footer.jsp" />