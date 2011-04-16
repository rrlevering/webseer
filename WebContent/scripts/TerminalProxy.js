(function() {
    var util = YAHOO.util,lang = YAHOO.lang;
    var Event = util.Event, Dom = util.Dom, Connect = util.Connect,JSON = lang.JSON,widget = YAHOO.widget;

Webseer.TerminalProxy = function(terminal, relativeEl, options) {
	Webseer.TerminalProxy.superclass.constructor.call(this, terminal, options);
	
	this.relativeEl = relativeEl;
};

YAHOO.lang.extend(Webseer.TerminalProxy, WireIt.TerminalProxy, {
	relativeEl: null,
	
	/**
	 * Override for better relative container handling.
	 */
	onDrag: function(e) {
   
	   // Prevention when the editing wire could not be created (due to nMaxWires)
	   if(!this.editingWire) { return; }
   
	   if(this.terminal.container) {
			var obj = this.terminal.container.layer.el;
         var curleft = 0;
         // Applied patch from http://github.com/neyric/wireit/issues/#issue/27
         // Fixes issue with Wire arrow being drawn offset to the mouse pointer
         var curtop = 0;
         if (obj.offsetParent) {
           do {
             curleft += obj.scrollLeft;
             curtop += obj.scrollTop;
             obj = obj.offsetParent ;
           } while ( obj );
         }
         this.fakeTerminal.pos = [e.clientX+curleft, e.clientY+curtop];
	   }
	   else {
		   var obj = this.relativeEl;
	         var curleft = 0;
	         // Applied patch from http://github.com/neyric/wireit/issues/#issue/27
	         // Fixes issue with Wire arrow being drawn offset to the mouse pointer
	         var curtop = 0;
	         if (obj.offsetParent) {
	           do {
	             curleft += obj.scrollLeft;
	             curtop += obj.scrollTop;
	             obj = obj.offsetParent ;
	           } while ( obj );
	         }
	         window.alert(curtop + "," + curleft);
	      this.fakeTerminal.pos = (YAHOO.env.ua.ie) ? [e.clientX - curleft, e.clientY - curtop] : [e.clientX+window.pageXOffset - curleft, e.clientY+window.pageYOffset - curtop];
	   }
	   this.editingWire.redraw();
	}

});


})();