Webseer.WebseerContainer = function(options, layer) {
	Webseer.WebseerContainer.superclass.constructor.call(this, options, layer);
	
	/**
	 * Event that is fired when the view source button is clicked on this container.
	 */
	this.eventViewSource = new YAHOO.util.CustomEvent("viewSource");
};

YAHOO.lang.extend(Webseer.WebseerContainer, WireIt.FormContainer, {
	
	contents: null, //TODO Eventually make this a separate container
	
	version: 0,
	
	/**
    * Redraw all the terminals, as with a resize or the like.
    * @method redrawAllTerminals
    */
	redrawAllTerminals: function() {
		for(var i = 0 ; i < this.terminals.length ; i++) {
			this.terminals[i].redraw();
			this.terminals[i].redrawAllWires();
		}
	},

	render: function() {
		Webseer.WebseerContainer.superclass.render.call(this);
		if (this.contents) {
			this.bodyEl.innerHTML = this.contents;
		}
		if (this.ddHandle) {
			this.viewSourceButton = WireIt.cn('div', {className: 'viewSource',style: 'position: absolute; right: 24px; top: 4px'} );
			YAHOO.util.Event.addListener(this.viewSourceButton, "click", this.viewSourceClicked, this, true);
			this.ddHandle.insertBefore(this.viewSourceButton, this.closeButton);
		}
		this.redraw();
	},
	
	viewSourceClicked: function(event, args) {
		this.eventViewSource.fire(this);
	},
	
	onResize: function(event, args) {
		var size = args[0];
		this.resizeBody(size[0], size[1]);
		this.redrawAllTerminals();
	},
	
	resizeBody: function(containerWidth, containerHeight) {
		if (containerWidth) {
			WireIt.sn(this.bodyEl, null, {minWidth: (containerWidth-14)+"px"});
		}
		if (containerHeight) {
			WireIt.sn(this.bodyEl, null, {minHeight: (containerHeight-44)+"px"});
		}
	},

	redraw: function() {
		if(this.width) {
			this.el.style.width = this.width+"px";
		}
		if(this.height) {
			this.el.style.height = this.height+"px";
		}
		this.resizeBody(this.width, this.height);
	},

	getConfig: function() {
		var obj = Webseer.WebseerContainer.superclass.getConfig.call(this);

		if (this.el.style.width) {
			obj.width = parseInt(this.el.style.width);
		}
		if (this.el.style.height) {
			obj.height = parseInt(this.el.style.height);
		}
		obj.dbId = this.dbId;

		return obj;
	},
	
	addTerminal: function(terminalConfig) {
	      terminalConfig.xtype = "Webseer.WebseerTerminal";
	      
	      return Webseer.WebseerContainer.superclass.addTerminal.call(this, terminalConfig);
	},

	onMouseDown: function(event) {
		if(this.layer) {
			if(this.layer.focusedContainer && this.layer.focusedContainer != this) {
				this.layer.focusedContainer.removeFocus();
			}
			if (this.layer.focusedContainer != this) {
				this.setFocus();
				this.layer.focusedContainer = this;
			}
		}
	}
});
   
