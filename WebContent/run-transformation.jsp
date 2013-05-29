<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:include page="includes/big-header.jsp">
	<jsp:param value="index-tabs" name="tabId" />
</jsp:include>
<jsp:useBean id="lastModified" class="java.util.Date" />
<jsp:setProperty name="lastModified" property="time" value="${transformation.version}" />
<div style="font-weight:bold;margin:10 0 10 0">${transformation.simpleName}<br /><span style="font-size:75%">${transformation.name} (<fmt:formatDate pattern="yyyy-MM-dd hh:mm" value="${lastModified}" />)</span></div>


<div style="margin-bottom:10px">
<c:forEach var="input" items="${inputs}" varStatus="loop">
	${input.key}: ${input.value}<br />
</c:forEach>
</div>

<div>
<c:forEach var="output" items="${outputs}" varStatus="loop">
	${output.key}: ${output.value}<br />
</c:forEach>
<jsp:include page="includes/footer.jsp" />
</div>