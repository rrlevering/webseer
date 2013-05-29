<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<script type="text/javascript" src="<c:url value="/lib/codemirror/lib/codemirror.js" />" ></script>
<link rel="stylesheet" href="<c:url value="/lib/codemirror/lib/codemirror.css" />">
<link rel="stylesheet" href="<c:url value="/lib/codemirror/theme/eclipse.css" />">
<script type="text/javascript" src="<c:url value="/lib/codemirror/mode/clike/clike.js" />" ></script>
<script src="<c:url value="/lib/codemirror/addon/edit/matchbrackets.js" />"></script>
<script src="<c:url value="/lib/codemirror/addon/selection/active-line.js" />"></script>
<script src="<c:url value="/lib/codemirror/addon/edit/closebrackets.js" />"></script>
<style>
	.CodeMirror {border: 2px inset #dee; height:600px; width:800px;}
	.CodeMirror-activeline-background {background: #e8f2ff !important;}
	.CodeMirror-lines * { font-family: monospace !important; }
	
	.lint-error {font-family: arial; font-size: 70%; background: #ffa; color: #a00; padding: 2px 5px 3px; }
    .lint-error-icon {color: white; background-color: red; font-weight: bold; border-radius: 50%; padding: 0 3px; margin-right: 7px;}
	
</style>
<textarea name="${param.codeAreaName}" id="${param.codeAreaName}" style="width:800px;height:600px">${param.startCode}</textarea>
<script>
	var widgets = [];

	function updateHints(errors) {
	  editor.operation(function(){
	    for (var i = 0; i < widgets.length; ++i)
	      editor.removeLineWidget(widgets[i]);
	    widgets.length = 0;

	    for (var i = 0; i < errors.length; ++i) {
	      var err = errors[i];
	      if (!err) continue;
	      var msg = document.createElement("div");
	      var icon = msg.appendChild(document.createElement("span"));
	      icon.innerHTML = "!!";
	      icon.className = "lint-error-icon";
	      msg.appendChild(document.createTextNode(err.reason));
	      msg.className = "lint-error";
	      widgets.push(editor.addLineWidget(err.line - 1, msg, {coverGutter: false, noHScroll: true}));
	    }
	  });
	  
	  var info = editor.getScrollInfo();
	  var after = editor.charCoords({line: editor.getCursor().line + 1, ch: 0}, "local").top;
	  if (info.top + info.clientHeight < after)
	    editor.scrollTo(null, after - info.clientHeight + 3);
	}

  var editor = CodeMirror.fromTextArea(document.getElementById("${param.codeAreaName}"), {
    lineNumbers: true,
    indentUnit: 4,
    matchBrackets: true,
    autoCloseBrackets: true,
    styleActiveLine: true,
    mode: "text/x-java",
    theme: "eclipse",
    <c:if test="${not empty param.readOnly}">readOnly: ${param.readOnly}</c:if>
  });
  
  if (compile) {
	  var waiting;
	  editor.on("change", function() {
	    clearTimeout(waiting);
	    waiting = setTimeout(compile, 500);
	  });
	  setTimeout(compile, 100);
  }

</script>
