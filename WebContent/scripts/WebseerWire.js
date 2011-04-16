Webseer.WebseerWire = function( terminal1, terminal2, parentEl, options) {

	/**
	 * Event that is fired when the stream source changes.
	 */
	this.eventSetWeir = new YAHOO.util.CustomEvent("eventSetWeir");
	
	// Find the actual terminal to connect to if we're actually to a subterminal
	if (!options.srcField) {
		this.srcField = terminal1.fieldName;
	}
	if (!options.targetField) {
		this.targetField = terminal2.fieldName;
	}

	Webseer.WebseerWire.superclass.constructor.call(this, terminal1, terminal2, parentEl, options);
	
};

YAHOO.lang.extend(Webseer.WebseerWire, WireIt.BezierArrowWire, {
	
	xtype: "Webseer.WebseerWire",
		
   onSelected: function(possibilities) {
	   this.color = 'rgb(255, 0, 0)';
		this.redraw();
		
		if (!this.labelEl) {
			this.renderLabel();
		}

		if (possibilities.length > 1) {
			// Draw a select box on top
			var select = WireIt.cn('select');
			
			for (var i = 0; i < possibilities.length; i++) {
				if (possibilities[i].label == this.label) {
					select.appendChild(WireIt.cn('option', {selected: 'selected', value: possibilities[i].id }, null, possibilities[i].label));
				} else {
					select.appendChild(WireIt.cn('option', {value: possibilities[i].id }, null, possibilities[i].label));
				}
			}
			
			YAHOO.util.Event.addListener(select, "change", this.setWeir, this, true);
			
			this.labelEl.innerHTML = '';
			this.labelEl.appendChild(select);
			
			this.positionLabel();
			
		}
		
   },
   
   setWeir: function(event, args) {
	   this.eventSetWeir.fire(this, event.target.value);
	   if (event.target.options[event.target.selectedIndex].text == this.terminal1.name) {
		   this.label = '';
	   } else {
		   this.label = event.target.options[event.target.selectedIndex].text;
	   }
   },
   
   onDeselected: function() {
	   	this.color = 'rgb(173, 216, 230)';
		this.redraw();
		
		this.labelEl.innerHTML = this.label;
   }
   
	
});