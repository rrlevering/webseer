Webseer.WebseerBucketContainer = function(options, layer) {
	this.image = "/webseer/images/database.png";
	
	Webseer.WebseerBucketContainer.superclass.constructor.call(this, options, layer);

	/**
	 * Event that is fired when the user desires to pull data into the bucket.
	 */
	this.eventFill = new YAHOO.util.CustomEvent("eventFill");
	
	/**
	 * Event that is fired when the bucket is renamed.
	 */
	this.eventRename = new YAHOO.util.CustomEvent("eventRename");
	
	this.image = "/webseer/images/database.png";
	
	this.initTerminals([
	      			{ "type": this.type, "name": "itemToOutput", "direction": [1,0], "offsetPosition": [96,48], "showLabel": false},
	    			{ "type": this.type, "name": "itemToAdd", "direction": [-1,0], "offsetPosition": [0,48], "showLabel": false}
	     		]);
	
 	this.height = "128";
	
};

YAHOO.lang.extend(Webseer.WebseerBucketContainer, WireIt.ImageContainer, {
	
	allowFill: true,
	
	label: null,
	
	type: null,
	
	render: function() {
		Webseer.WebseerBucketContainer.superclass.render.call(this);
		
		if (!this.label) {
			this.label = this.title;
		}
		if (this.allowFill) {
			var fillButtonEl = WireIt.cn('button', null, null, 'Fill');
			this.bodyEl.appendChild(fillButtonEl);
			var fillButton = new YAHOO.widget.Button(fillButtonEl);
			fillButton.on("click", this.onFillClick, this, true);
			fillButton.addClass('fillButton');
		}
		this.titleDiv = WireIt.cn('div', {className: 'bucketTitle'}, {position:'absolute', cursor: 'pointer', width: '128px', left: '-1px', bottom: '-5px', textAlign:'center'}, this.label);
		this.bodyEl.appendChild(this.titleDiv);		
		YAHOO.util.Event.addListener(this.titleDiv, "dblclick", this.toggleTitleEdit, this, true);
	},
	
	setType: function(type) {
		if ((type && !this.type) || (!type && this.type) || (type && this.type && type.name != this.type.name)) {
			this.type = type;
			for (var i = 0; i < this.terminals.length; i++) {
				this.terminals[i].setType(type);
			}
		}
	},
	
	toggleTitleEdit: function() {
		this.titleDiv.innerHTML = ''; // Hack
		var titleInput = WireIt.cn('input', {type: 'text', value: this.label }, null);
		this.titleDiv.appendChild(titleInput);
		
		YAHOO.util.Event.addListener(titleInput, "blur", this.updateTitle, this, true);
		YAHOO.util.Event.addListener(titleInput, "keydown", function(event, args) { if (event.keyCode == 13) { this.updateTitle(event, args); } }, this, true);
		titleInput.focus();
	},
	
	updateTitle: function(event, args) {
		var oldName = this.label;
		this.label = event.target.value;
		
		this.titleDiv.innerHTML = this.label;
		
		this.eventRename.fire(this, oldName);
	},

	onFillClick: function(event, args) {
		this.eventFill.fire(this);
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