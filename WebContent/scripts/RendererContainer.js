Webseer.RendererContainer = function(options, layer) {
	this.image = "/webseer/images/database.png";
	
	Webseer.RendererContainer.superclass.constructor.call(this, options, layer);

	/**
	 * Event that is fired when the user wants to view the renderer.
	 */
	this.eventView = new YAHOO.util.CustomEvent("eventView");
	
};

YAHOO.lang.extend(Webseer.RendererContainer, Webseer.WebseerContainer, {
	
	allowView: true,
	
	label: null,
	
	render: function() {
		Webseer.RendererContainer.superclass.render.call(this);
		
		if (!this.label) {
			this.label = this.title;
		}
		if (this.allowView) {
			var fillButtonEl = WireIt.cn('button', null, null, 'Render');
			this.bodyEl.appendChild(fillButtonEl);
			WireIt.sn(this.bodyEl, {}, {textAlign: 'right'});
			var fillButton = new YAHOO.widget.Button(fillButtonEl);
			fillButton.on("click", this.onViewClick, this, true);
		}
	},
	
	onViewClick: function(event, args) {
		this.eventView.fire(this);
	},
	
	// Copied from WebseerContainer
	getConfig: function() {
		var obj = Webseer.WebseerContainer.superclass.getConfig.call(this);

		if (this.el.style.width) {
			obj.width = parseInt(this.el.style.width);
		}
		if (this.el.style.height) {
			obj.height = parseInt(this.el.style.height);
		}
		obj.dbId = this.dbId;
		
		obj.label = this.label;

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