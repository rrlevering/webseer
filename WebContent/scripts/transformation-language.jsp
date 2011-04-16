<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
var workspaceLanguage = {
	
	// Set a unique name for the language
	languageName: "webseer",
	
	layoutOptions: {
		units: [
	      { position: 'left', width: 250, resize: true, body: 'left', gutter: '5px', collapse: true, 
	        collapseSize: 25, scroll: true, animate: true },
	      { position: 'center', body: 'center', gutter: '5px' },
	      { position: 'right', width: 320, resize: true, body: 'right', gutter: '5px', collapse: true, 
	        collapseSize: 25, /*header: 'Properties', scroll: true,*/ animate: true },
	      { position: 'bottom', body: 'item', height: 200, resize: true, collapse: true, gutter: '5px' }
	   ]
	},
	
	outerLayoutOptions: {
		units: [
			      { position: 'bottom', body: 'bottom', height: 58 },
				  { position: 'top', height: 58, body: 'top'},
				  { position: 'center'}, /* Filled with inner layout */
		        ]
	},
	
	adapter: Webseer.WebseerAdapter

};