(function() {
    var util = YAHOO.util,lang = YAHOO.lang;
    var Event = util.Event, Dom = util.Dom, Connect = util.Connect,JSON = lang.JSON,widget = YAHOO.widget;

Webseer.WebseerTerminal = function(parentEl, options, container) {
	// this.ddConfig = { type: !options.type ? "unknown" : (options.type.fields.length > 0 ? options.type.name : "primitive"), allowedTypes: [(options.type && options.type.fields.length > 0 ? options.type.name : "primitive"), "unknown"] };
	
	Webseer.WebseerTerminal.superclass.constructor.call(this, parentEl, options, container);
	
	/**
	 * Event that is fired when the user flips an input terminal to be a config parameter.
	 */
	this.eventToggleExternal = new YAHOO.util.CustomEvent("eventToggleExternal");
	
	/**
	 * Event that is fired when the user sets the value for this input.
	 */
	this.eventSetValue = new YAHOO.util.CustomEvent("eventSetValue");
	
	this.subterminals = [];
	
	// Create terminals in the parent container for all of the subterminals
	if (this.type) {
		for (var i = 0; i < this.type.fields.length; i++) {
			this.subterminals[this.subterminals.length] = container.addTerminal({hidden: true, rootField: this.fieldName ? this.rootField : this.name, fieldName: this.fieldName ? (this.fieldName + "." + this.type.fields[i].field) : this.type.fields[i].field, name: this.type.fields[i].field, type: this.type.fields[i].type, direction: this.direction, offsetPosition:[0,0] });
		}
	}
	
	this.redraw();
	
};

YAHOO.lang.extend(Webseer.WebseerTerminal, WireIt.Terminal, {
	
	wireConfig: { xtype: 'Webseer.WebseerWire' },
	
	type: null,
	
	value: null, // Only on input terminals
	
	external: true, // Only on input terminals
	
	multiple: false, // Only on input terminals
	
	varargs: false, // Only on input terminals
	
	hidden: false,
	
	showLabel: true,
	
	rootField: null,
	
	fieldName: null,
	
	setOptions: function(options) {
	    
		Webseer.WebseerTerminal.superclass.setOptions.call(this, options);
		
	    if (options.direction && options.direction[0] == -1 && !this.varargs) {
	    	// Don't let input points have more than one wire
	    	this.nMaxWires = 1;
	    }
	    
	    if (!this.fieldName) {
	    	this.rootField = this.name;
	    }		
	},
	
	setType: function(type) {
		this.type = type;
		
		if (this.expanded) {
			this.expandType();
		}
		
//		for (var i = 0; i < this.subterminals.length; i++) {
//			for(var j = 0 ; j < this.container.terminals.length ; j++) {
//		         this.container.terminals.splice(j, 1);
//		         j--;
//		      }
//			this.subterminals[i].remove();
//		}
//		this.subterminals = [];
//		
//		if (this.type) {
//			for (var i = 0; i < this.type.fields.length; i++) {
//				this.subterminals[this.subterminals.length] = this.container.addTerminal({hidden: true, rootField: this.fieldName ? this.rootField : this.name, fieldName: this.fieldName ? (this.fieldName + "." + this.type.fields[i].field) : this.type.fields[i].field, name: this.type.fields[i].field, type: this.type.fields[i].type, direction: this.direction, offsetPosition:[0,0] });
//			}
//		}
//		
//		this.redraw();
	},
	
	render: function() {
		// Create the DIV element
		this.el = WireIt.cn('div', {className: this.className} );
		
		if (this.showLabel && this.direction[0] == -1) {
			this.inputField = WireIt.cn('div', {style: "position: absolute; top: 6px; left: 30px; white-space:nowrap"} );
			this.inputField.innerHTML = this.name;
			this.el.appendChild(this.inputField);
		} else if (this.showLabel) {
			this.inputField = WireIt.cn('div', {style: "position: absolute; top: 6px; right: 30px;"} );
			this.inputField.innerHTML = this.name;
			this.el.appendChild(this.inputField);
		}
		
		this.redraw();
		
		if (this.direction[0] == -1) {
			YAHOO.util.Event.addListener(this.el, "dblclick", this.toggleExternal, this, true);
		}
		
		YAHOO.util.Event.addListener(this.el, "click", this.expandType, this, true);
		
		if (this.value) {
			this.drawInput();
		}

		// Append the element to the parent
		this.parentEl.appendChild(this.el);
	},
	
	expandType: function() {
		if (this.expanded) {
			// Collapse the wires from the children
			for (var i = 0; i < this.subterminals.length; i++) {
				this.subterminals[i].hidden = true;
				for (var j = 0; j < this.subterminals[i].wires.length; j++) {
					var wire = this.subterminals[i].wires[j];
					if (wire.terminal1 == this.subterminals[i]) {
						wire.terminal1 = this;
					} else {
						wire.terminal2 = this;
					}
					this.wires[this.wires.length] = wire;
					Dom.removeClass(this.subterminals[i].el, this.connectedClassName);
					Dom.addClass(this.el, this.connectedClassName);
				}
				this.subterminals[i].wires = [];
			}
		} else {
			// Push the wires to the correct fields
			for (var i = 0; i < this.subterminals.length; i++) {
				this.subterminals[i].hidden = false;
				for (var j = 0; j < this.wires.length; j++) {
					var wire = this.wires[j];
					if (wire.srcField == this.subterminals[i].fieldName) {
						if (wire.terminal1 == this) {
							wire.terminal1 = this.subterminals[i];
						} else {
							wire.terminal2 = this.subterminals[i];
						}
						Dom.addClass(this.subterminals[i].el, this.connectedClassName);
						this.subterminals[i].wires[this.subterminals[i].wires.length] = wire;
						this.wires.splice(j, 1);
						j--;
						if (this.wires.length  == 0) {
							Dom.removeClass(this.el, this.connectedClassName);
						}
					}
				}
			}

		}
		this.expanded = !this.expanded;
		this.redraw();
		this.container.layer.layermap.draw();
	},
	
	drawInput: function() {
		var topTypeName = this.type ? this.type.name : null;
		
		if (topTypeName == 'string' || topTypeName == 'int64' || topTypeName == 'int32' || topTypeName == 'float' || topTypeName == 'double') {
			var input = WireIt.cn('input', {type: "text", value: this.value} );
			YAHOO.util.Event.addListener(input, "change", this.setValue, this, true);
		} else if (topTypeName == 'bool') {
			var input = WireIt.cn('select', {}, {display: 'inline'});
			var trueOption = WireIt.cn('option', {value: 'true'}, {}, 'true');
			var falseOption = WireIt.cn('option', {value: 'false'}, {}, 'false');
			input.appendChild(trueOption);
			input.appendChild(falseOption);
			YAHOO.util.Event.addListener(input, "change", this.setValue, this, true);
		} else if (topTypeName == 'bytes') {
			if (this.value != null) {
				var input = WireIt.cn('span');
				var a = WireIt.cn('a', { href: "getFileFrom?nodeId=" + this.container.dbId + "&inputName=" + this.name, target: "_blank" }, null, this.value);
				var change = WireIt.cn('button', null, null, 'Change');
				YAHOO.util.Event.addListener(change, "click", this.changeFileValue, this, true);
				input.appendChild(a);
				input.appendChild(change);
			} else {
				this.fileForm = WireIt.cn('form', {target: "uploadFrame", action: "uploadFileTo", method: "post", enctype:"multipart/form-data"}, {display:"inline"} );
				var fileUpload = WireIt.cn('input', {type: "file", name: "file"} );
				var nodeId = WireIt.cn('input', {type: "hidden", name: "nodeId", value: this.container.dbId} );
				var inputName = WireIt.cn('input', {type: "hidden", name: "inputName", value: this.name} );
				YAHOO.util.Event.addListener(fileUpload, "change", function() { 
					this.fileForm.submit();
					}, this, true);
				this.iframe = WireIt.cn('iframe', {name: "uploadFrame"}, {width:"0", height:"0", border: "0px solid #fff"});
				YAHOO.util.Event.addListener(this.iframe, "load", this.setFileValue, this, true);
				this.fileForm.appendChild(fileUpload);
				this.fileForm.appendChild(nodeId);
				this.fileForm.appendChild(inputName);
				this.fileForm.appendChild(this.iframe);
				var input = this.fileForm;
			}
		} else {
			return;
		}
		this.inputField.innerHTML = this.name + ": ";
		
		this.inputField.appendChild(input);
		
		YAHOO.util.Event.addListener(this.inputField, "dblclick", this.toggleExternal, this, true);
		
		this.external = false;
		
	},
	
	changeFileValue: function() {
		this.fileForm = WireIt.cn('form', {target: "uploadFrame", action: "uploadFileTo", method: "post", enctype:"multipart/form-data"}, {display:"inline"} );
		var fileUpload = WireIt.cn('input', {type: "file", name: "file"} );
		var nodeId = WireIt.cn('input', {type: "hidden", name: "nodeId", value: this.container.dbId} );
		var inputName = WireIt.cn('input', {type: "hidden", name: "inputName", value: this.name} );
		YAHOO.util.Event.addListener(fileUpload, "change", function() { 
			this.fileForm.submit();
			}, this, true);
		this.iframe = WireIt.cn('iframe', {name: "uploadFrame"}, {width:"0", height:"0", border: "0px solid #fff"});
		YAHOO.util.Event.addListener(this.iframe, "load", this.setFileValue, this, true);
		this.fileForm.appendChild(fileUpload);
		this.fileForm.appendChild(nodeId);
		this.fileForm.appendChild(inputName);
		this.fileForm.appendChild(this.iframe);
		var input = this.fileForm;
		
		this.inputField.innerHTML = this.name + ": ";
		
		this.inputField.appendChild(input);		
	},
	
	removeInput: function() {
		this.inputField.innerHTML = this.name;
		
		this.external = true;
	},
	
	toggleExternal: function() {
		if (this.external) {
		
			this.drawInput();
			
		} else {
			this.removeInput();
		}
		
		this.eventToggleExternal.fire(this);
		
	},
	
	setFileValue: function(event) {
		if (this.iframe.contentDocument.body.innerHTML != '') {
			this.value = this.iframe.contentDocument.body.firstChild.innerHTML;
			
			var input = WireIt.cn('span');
			var a = WireIt.cn('a', { href: "getFileFrom?nodeId=" + this.container.dbId + "&inputName=" + this.name, target: "_blank" }, null, this.value);
			var change = WireIt.cn('button', null, null, 'Change');
			YAHOO.util.Event.addListener(change, "click", this.changeFileValue, this, true);
			input.appendChild(a);
			input.appendChild(change);
			
			this.inputField.innerHTML = this.name + ": ";
			
			this.inputField.appendChild(input);
		}
	},
	
	setValue: function(event) {
		this.eventSetValue.fire(this, event.target.value);
		
		this.value = event.target.value;
	},

	redraw: function() {
		if (this.el) {
			if(this.name) { this.el.title = this.name + ":" + (this.type ? this.type.name : "unknown"); }

			this.el.style.left = '0px';
			this.el.style.top = '0px';

			// Set the offset position
			var pos = this.offsetPosition;
			if(pos) {
				// Kept old version [x,y] for retro-compatibility
				if( lang.isArray(pos) ) {
					this.el.style.left = pos[0]+"px";
					this.el.style.top = pos[1]+"px";
				}
				// New version: {top: 32, left: 23}
				else if( lang.isObject(pos) ) {
					for(var key in pos) {
						if(pos.hasOwnProperty(key) && pos[key] != ""){
							this.el.style[key] = pos[key]+"px";
						}
					}
				}
			}

			var relativePos = this.relativePosition;
			if (relativePos) {
				this.el.style.left = ((this.el.style.left ? parseInt(this.el.style.left) : '') + (relativePos[0] * this.parentEl.offsetWidth)) + "px";
				this.el.style.top = ((this.el.style.top ? parseInt(this.el.style.top) : '') + (relativePos[1] * this.parentEl.offsetHeight)) + "px";
			}
			
			if (this.subterminals) {
				for (var i = 0; i < this.subterminals.length; i++) {
					if (this.direction[0] == -1) {
						this.subterminals[i].offsetPosition = [this.el.offsetLeft - 100, this.el.offsetTop + i * 30];
					} else {
						this.subterminals[i].offsetPosition = [this.el.offsetLeft + 100, this.el.offsetTop + i * 30];
					}
					this.subterminals[i].redraw();
				}
			}
			
			if (this.hidden) {
				this.el.style.visibility = 'hidden';
			} else {
				this.el.style.visibility = 'visible';
			}
			
			this.redrawAllWires();
		}
	}
	
});

})();