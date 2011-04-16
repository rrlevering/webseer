(function() {
    var util = YAHOO.util,lang = YAHOO.lang;
    var Event = util.Event, Dom = util.Dom, Connect = util.Connect,JSON = lang.JSON,widget = YAHOO.widget;

    
Webseer.WebseerEditor = function(options) {

	this.moduleDivs = [];
	  
  Webseer.WebseerEditor.superclass.constructor.call(this, options);
  
};

lang.extend(Webseer.WebseerEditor, WireIt.WiringEditor, {
	
	setOptions: function(options) {
	    
		Webseer.WebseerEditor.superclass.setOptions.call(this, options);
		
	    // Optional outer layout
	    this.options.outerLayoutOptions = options.outerLayoutOptions;
	    
	    // Set the actual modules
	    this.adapter.getModules({
			success: function(result) {
				this.modules = result;
			    for(var i = 0 ; i < this.modules.length ; i++) {
			       var m = this.modules[i];
			       this.modulesByName[m.name] = m;
			    }
			    
				 // Left Accordion
				 this.renderModulesAccordion();

			    // Render module list
			    this.buildModulesList();
				 
			},
			failure: function(errorStr) {
				// Do nothing
			},
			scope: this
		});
		
	},
	
	/**
	 * Unfortunately, to add the outer layouts, we need to override the whole thing.  TODO: Change this
	 */
	render: function() {

		 // Render the help panel
	    this.renderHelpPanel();

	    /**
	     * @property layout
	     * @type {YAHOO.widget.Layout}
	     */
	    if (this.options.outerLayoutOptions) {
	    	this.layout = new widget.Layout(this.el, this.options.outerLayoutOptions);
	    } else {
	    	this.layout = new widget.Layout(this.el, this.options.layoutOptions);
	    }
	    this.layout.render();
	    
	    if (this.options.outerLayoutOptions) {
	    	var innerContainer = this.layout.getUnitByPosition('center').get('wrap');
	        this.innerLayout = new widget.Layout(innerContainer, this.options.layoutOptions);
	        this.innerLayout.render();
	        this.innerLayout.getUnitByPosition('bottom').collapse();
	    }

		 // Right accordion
	    this.renderPropertiesAccordion();

	    // Properties Form
	    this.renderPropertiesForm();
	    
	    this.layer = new WireIt.Layer(this.options.layerOptions);
		 this.layer.eventContainerDragged.subscribe(this.onContainerDragged, this, true);

		 this.layer.eventAddContainer.subscribe(this.onContainerAdded, this, true);
		 this.layer.eventRemoveContainer.subscribe(this.onContainerRemoved, this, true);
		 this.layer.eventAddWire.subscribe(this.onWireAdded, this, true);
		 this.layer.eventRemoveWire.subscribe(this.onWireRemoved, this, true);
		 // this.layer.eventContainerDragged.subscribe(this.onUIChange, this, true);
		 this.layer.eventContainerResized.subscribe(this.onUIChange, this, true);
		 
	},
	
	renderModulesAccordion: function() {
		
		Webseer.WebseerEditor.superclass.renderModulesAccordion.call(this);
		
		// Remove the temporary panel
		this.modulesAccordionView.removePanel(0);
	},
	
	addModuleToList: function(module) {
		
			var div = WireIt.cn('div', {className: "WiringEditor-module"});
			
			if(module.description) {
				div.title = module.description;
			}
			
	      if(module.container.icon) {
	         div.appendChild( WireIt.cn('img',{src: module.container.icon}) );
	      }
	      div.appendChild( WireIt.cn('span', null, null, module.name) );
	      
	      if (module.category == 'Bucket' && module.name != 'New Bucket') {
	    	  var deleteIcon = WireIt.cn('div', {style: "float:right; cursor: pointer"}, {}, 'X');
				
				YAHOO.util.Event.addListener(deleteIcon, "click", this.onBucketDelete, this, true);
				
				div.appendChild(deleteIcon);
	      }

	      var ddProxy = new WireIt.ModuleProxy(div, this);
	      ddProxy._module = module;

			// Get the category element in the accordion or create a new one
			var category = module.category || "main";
			var el = Dom.get("module-category-"+category);
			if( !el ) {
				this.modulesAccordionView.addPanel({
					label: category,
					content: "<div id='module-category-"+category+"'></div>"
				});
				this.modulesAccordionView.openPanel(this.modulesAccordionView._panels.length-1);
				el = Dom.get("module-category-"+category);
			}
			if (module.category == 'Bucket' && module.name != 'New Bucket') {
				el.insertBefore(div, el.firstChild);
			} else {
				el.appendChild(div);
			}
			
			this.moduleDivs[module.name] = div;
	},
	
	addModule: function(module, pos) {
	    try {
	       if (pos[0] < 0) pos[0] = 0;
	       if (pos[1] < 0) pos[1] = 0;
	       var containerConfig = module.container;
	       containerConfig.position = pos;
	       containerConfig.title = module.name;
	       var holder = containerConfig.label;
			if (module.name == 'New Bucket') {
				var postfix = "";
				var counter = 2;
				while (this.modulesByName[containerConfig.label + postfix]) {
					postfix = " " + counter;
					counter++;
				}
				containerConfig.label = containerConfig.label + postfix
			}
	       var temp = this;
	       containerConfig.getGrouper = function() { return temp.getCurrentGrouper(temp); };
	       var container = this.layer.addContainer(containerConfig);
	
			 // Adding the category CSS class name
			 var category = module.category || "main";
			 Dom.addClass(container.el, "WiringEditor-module-category-"+category.replace(/ /g,'-'));
			
			 // Adding the module CSS class name
	       Dom.addClass(container.el, "WiringEditor-module-"+module.name.replace(/ /g,'-'));
	       containerConfig.label = holder;
	    }
	    catch(ex) {
	       this.alert("Error Layer.addContainer: "+ ex.message);
			 if(window.console && YAHOO.lang.isFunction(console.log)) {
				console.log(ex);
			}
	    }    
	},

	onBucketDelete: function(event) {
		var moduleName = event.target.previousSibling.innerHTML;
		this.adapter.deleteBucket(moduleName, {
			success: function(result) {
				// Remove the module
				for (var i = 0; i < this.modules.length; i++) {
					if (this.modules[i].name == moduleName) {
						this.modules.splice(i, i);
						break;
					}
				}
				this.modulesByName[moduleName] = null;
				this.moduleDivs[moduleName].parentNode.removeChild(this.moduleDivs[moduleName]);
				this.moduleDivs[moduleName] = null;
				
				// Now remove all the bucket nodes that go along
				this.preventLayerChangedEvent = true;
		     	for (var i = 0; i < this.layer.containers.length; i++) {
		     		if (this.layer.containers[i].label == moduleName) {
		     			this.layer.removeContainer(this.layer.containers[i]);
		     			i--;
		     		}
		     	}
		     	this.preventLayerChangedEvent = false;
		     	this.clearPreview();
				
			},
			failure: function(errorStr) {
				// Do nothing
			},
			scope: this
		});
	},
	
	getValue: function() {
		
		var value = Webseer.WebseerEditor.superclass.getValue.call(this);
		if (!value.name) {
			value.name = "Main";
		}
		return value;
		
	},
	
	onUIChange: function() {
		// this.save();
	},
	
	onContainerDragged: function(e, params) {
		var container = params[0];
		
		this.adapter.moveContainer(container, {
			success: function(result) {
				// Do nothing
			},
			failure: function(errorStr) {
				// Do nothing
			},
			scope: this
		});
	},
	
	onLayerChanged: function() {
		
	},
	
	/**
	 * Override since we only expect a single wiring back.
	 */
	onLoadSuccess: function(wirings) {
		this.pipes = wirings;
		this.pipesByName = {};
	
		this.renderLoadPanel();
    	this.updateLoadPanelList();
    	
    	if (this.pipes.length > 0) {
    		this.loadPipe("Main");
    	}
	},
	
	onContainerAdded: function(e, params) {
		var container = params[0];
		container.eventFocus.subscribe(this.onContainerFocus, this, true);
		container.eventBlur.subscribe(this.onContainerBlur, this, true);
		
		if (container.xtype == 'Webseer.WebseerBucketContainer') {
			container.eventFill.subscribe(this.onBucketFill, this, true);
			container.eventRename.subscribe(this.onBucketRename, this, true);
		} else if (container.xtype == 'Webseer.RendererContainer') {
			for (var i = 0; i < container.terminals.length; i++) {
				container.terminals[i].eventSetValue.subscribe(this.onSetInput, this, true);
				container.terminals[i].eventToggleExternal.subscribe(this.onToggleInput, this, true);
			}
			container.eventView.subscribe(this.onRendererView, this, true);
			container.eventViewSource.subscribe(this.onViewSource, this, true);
		} else {
			for (var i = 0; i < container.terminals.length; i++) {
				container.terminals[i].eventSetValue.subscribe(this.onSetInput, this, true);
				container.terminals[i].eventToggleExternal.subscribe(this.onToggleInput, this, true);
			}
			container.eventViewSource.subscribe(this.onViewSource, this, true);
		}
		
		if(!this.preventLayerChangedEvent) {
			if (!container.xtype || container.xtype == 'Webseer.WebseerContainer' || container.xtype == 'Webseer.RendererContainer') {
				// Ask the backend for the id of the new container
				this.adapter.addContainer(container, {
					success: function(result) {
						// Assign the id to the container and save
						container.dbId = result.dbId;
					},
					failure: function(errorStr) {
						container.remove();
						this.alert("Unable to add this module");
					},
					scope: this
				});
			} else {
				
				// Ask the backend for the id of the new container
				this.adapter.addBucketContainer(container, {
					success: function(result) {
						// Assign the id to the container and save
						container.dbId = result.dbId;
						
						if (!this.modulesByName[container.label]) {
							// Add to the module list
							var module = {
									"name": container.label,
								      "category": "Bucket",
								
								      "container": {
								         "label": container.label, // What is displayed
								         "xtype": "Webseer.WebseerBucketContainer"
								      }
							};
							this.modules[this.modules.length] = module;
							this.modulesByName[container.label] = module;
							
							this.addModuleToList(module);
						}
					},
					failure: function(errorStr) {
						container.remove();
						this.alert("Unable to add this module");
					},
					scope: this
				});
			}
			
		}
	},
	
	onRendererView: function(e, params) {
		var container = params[0];
		// Issue an HTTP request for the renderer
		window.open("render?nodeId=" + container.dbId);
	},
	
	onSetInput: function(e, params) {
		var terminal = params[0];
		var value = params[1];
		
		// Report the value to the server
		this.adapter.setInputValue(terminal, value, {
			success: function(result) {
				// Do nothing
			},
			failure: function(errorStr) {
				// Do nothing
			},
			scope: this
		});
	},
	
	onToggleInput: function(e, params) {
		var terminal = params[0];
		
		if (!terminal.external) {
			// Remove the wires
			for (var i = 0; terminal.wires.length > 0; ) {
				terminal.wires[i].remove();
			}
		}
	},
	
	onViewSource: function(e, params) {
		var container = params[0];
		this.adapter.getSource(container, {
			success: function(result) {
				this.displaySource(container, result);
			},
			failure: function(errorStr) {
				
			},
			scope: this
		});
	},
	
	displaySource: function(container, source) {
		if (!this.overlay2) {
			this.overlay2 = new YAHOO.widget.Panel("source", { fixedcenter: true, 
					                                                                          visible:false, 
					                                                                          width:"850px",
					                                                                          height:"800px",
					                                                                          modal: true,
					                                                                          draggable: false,
					                                                                          close: true } );
			this.overlay2.render();
		}
		Dom.get("sourceTitle").innerHTML = container.typeId + " (version " + container.version + ")";
		Dom.get("sourceBody").innerHTML = "<pre>" + source + "</pre>";
		this.overlay2.show();
	},
	
	onBucketRename: function(e, params) {
		var bucket = params[0];
		var oldName = params[1];
		this.adapter.renameBucketContainer(bucket, {
			success: function(result) {
				// Change the bucket module name
				var modules = this.modules;
		     	for(var i = 0 ; i < modules.length ; i++) {
				  	if (modules[i].category == 'Bucket' && modules[i].name == oldName) {
				  		modules[i].name = bucket.label;
				     	this.modulesByName[oldName] = null;
				     	this.modulesByName[bucket.label] = modules[i];
				  	}
		     	}
		     	var el = this.moduleDivs[oldName];
		     	for (var child = el.firstChild; child; child = child.nextSibling) {
		     		if (child.nodeName == 'SPAN') {
		     			child.innerHTML = bucket.label;
		     		}
		     	}
		     	this.moduleDivs[oldName] = null;
		     	this.moduleDivs[bucket.label] = el;
				
				// Change all the other bucket labels
		     	for (var i = 0; i < this.layer.containers.length; i++) {
		     		if (this.layer.containers[i].label == oldName) {
		     			this.layer.containers[i].label = bucket.label;
		     			this.layer.containers[i].titleDiv.innerHTML = bucket.label;
		     		}
		     	}
				
			},
			failure: function(errorStr) {
				bucket.label = oldName;				
				bucket.titleDiv.innerHTML = oldName;
				
				window.alert('Unable to rename bucket, name already in use');
			},
			scope: this
		});
	},
	
	onBucketFill: function(e, params) {
		var bucket = params[0];
		this.adapter.fillBucketContainer(bucket, {
			success: function(result) {
				this.clearPreview();
				this.previewBucket(bucket);
				bucket.setType(result);
			},
			failure: function(errorStr) {
				
			},
			scope: this
		});
	},
	
	onContainerFocus: function(e, params) {
		var container = params[0];
		if (this.selectedWire) {
			this.selectedWire.onDeselected();
			this.selectedWire = null;
			this.clearPreview();
		}
		if (container.xtype == 'Webseer.WebseerBucketContainer') {
			this.previewBucket(container);
		}
	},
	
	onContainerBlur: function(e, params) {
		var container = params[0];
		this.clearPreview();
	},
	
	onContainerRemoved: function(e, params) {
		if(!this.preventLayerChangedEvent) {
			var container = params[0];
			
			this.adapter.removeContainer(container, {
				success: function(result) {
					if (container.xtype == 'Webseer.WebseerBucketContainer') {
						// Only if it's selected
						this.clearPreview();
					}
				},
				failure: function(errorStr) {
					this.alert("Unable to contact the backend to remove a container: "+ errorStr);
				},
				scope: this
			});
		}
	},
	
	onWireAdded: function(e, params) {
		var wire = params[0];
		// Add wire click hook
		wire.eventMouseClick.subscribe(this.onWireClicked, this, true);
		wire.eventSetWeir.subscribe(this.onWeirSet, this, true);
		
		if(!this.preventLayerChangedEvent && wire.xtype == 'Webseer.WebseerWire') {
			// Check whether or not this requires a type map dialog
//			if (wire.terminal1.type != null && wire.terminal2.type != null // Not attaching to empty bucket
//					&& wire.terminal1.type.name != wire.terminal2.type.name // Different types 
//					&& !(wire.terminal1.type.fields.length == 0 && wire.terminal2.type.fields.length == 0) // Not both primitive
//					) {
//				wire.remove();
//				this.alert("Unable to make this connection");
//			}
			
			// Ask the backend for the id of the new connection
			this.adapter.addWire(wire, {
				success: function(result) {
					// Assign the id to the wire
					wire.dbId = result.dbId;
				},
				failure: function(errorStr) {
					wire.remove();
					this.alert("Unable to make this connection");
				},
				scope: this
			});
		}
		
		if (!wire.terminal2.external) {
			// Need to toggle the config off
			wire.terminal2.toggleExternal();
		}
	},
	
	onWeirSet: function(e, params) {
		var wire = params[0];
		var weirId = params[1];
		
		// Send it to the server
		this.adapter.setWeir(wire, weirId, {
			success: function(result) {
				// If we succeeded, request a new preview
				this.previewWire(wire);
			},
			failure: function(errorStr) {
				// Nothing
			},
			scope: this
		});
	},
	
	onWireClicked: function(e, params) {
		var wire = params[0];
		
		if (this.selectedWire && this.selectedWire != wire) {
			this.selectedWire.onDeselected();
			this.selectedWire = null;
			this.clearPreview();
		} else if (this.layer.focusedContainer) {
            this.layer.focusedContainer.removeFocus();
            this.layer.focusedContainer = null;
         }
		if (this.selectedWire != wire) {
			this.selectedWire = wire;
			// Query the server to see what the options are for this wire
			this.adapter.getWireOptions(wire, {
				success: function(result) {
					wire.onSelected(result);
				},
				failure: function(errorStr) {
					// Nothing
				},
				scope: this
			});
			
			this.previewWire(wire);
		}
	},
	
	clearPreview: function() {
		var previewPanel = Dom.get('previewPanel');
		for (; previewPanel.hasChildNodes(); ) {
			previewPanel.removeChild(previewPanel.firstChild);
		}
		var historyPanel = Dom.get('item');
		historyPanel.innerHTML = '';
		this.innerLayout.getUnitByPosition('bottom').collapse();		
	},
	
	previewBucket: function(container) {
		this.adapter.previewBucketContainer(container, {
			success: function(result) {
				// Update the preview pane with the bucket contents
				// result is an array of items with ids and descriptions
				for (var i = 0; i < result.length; i++) {
					var item = WireIt.cn('div', {className: "WiringEditor-module", id: result[i].id }, {});
					
					item.bucketId = container.dbId;
					
					var span = WireIt.cn('span', {}, {}, result[i].description);
					
					item.appendChild(span);
					
					YAHOO.util.Event.addListener(span, "click", this.onItemSelected, this, true);
					
					Dom.get('previewPanel').appendChild(item);
					
					if (result[i].id >= 0) {
						var deleteIcon = WireIt.cn('div', {style: "float:right; cursor: pointer"}, {}, 'X');
						
						YAHOO.util.Event.addListener(deleteIcon, "click", this.onItemDeleted, this, true);
						
						item.appendChild(deleteIcon);
					}
				}
				this.accordionView.openPanel(1);
			},
			failure: function(error) {
				// Don't do anything, the preview panel is most likely already cleared
			},
			scope: this
		});
	},
	
	previewWire: function(wire) {
		this.adapter.previewWire(wire, {
			success: function(result) {
				// If this took a long time, we might not want this anymore
				if (this.selectedWire != wire) {
					return;
				}
				this.clearPreview();
				
				// Update the preview pane with the results from the pull
				// result is an array of items with ids and descriptions
				for (var i = 0; i < result.length; i++) {
					var item = WireIt.cn('div', {className: "WiringEditor-module", id: result[i].id }, {});
					
					var span = WireIt.cn('span', {}, {}, result[i].description);
					
					item.appendChild(span);
					
					YAHOO.util.Event.addListener(span, "click", this.onItemSelected, this, true);
					
					Dom.get('previewPanel').appendChild(item);
				}
				this.accordionView.openPanel(1);
			},
			failure: function(errorStr) {
				// Clear the preview pane
				window.alert(error);
			},
			scope: this
		});
	},
	
	onItemDeleted: function(event, args) {
		this.adapter.deleteItem(event.target.parentNode.id, event.target.parentNode.bucketId, {
			success: function(result) {
				event.target.parentNode.parentNode.removeChild(event.target.parentNode);
			},
			failure: function(errorStr) {
				// Nothing
			},
			scope: this
		});
	},
	
	onItemSelected: function(event, args) {
		this.adapter.viewItemHistory(event.target.parentNode.id, {
			success: function(result) {
				this.updateHistory(result);
			},
			failure: function(errorStr) {
				// Nothing
			},
			scope: this
		});
	},
	
	updateHistory: function(graph) {
		var historyPanel = Dom.get('item');
		WireIt.sn(historyPanel, {style: "height:100%"}, {});
		for (; historyPanel.hasChildNodes(); ) {
			historyPanel.removeChild(historyPanel.firstChild);
		}
		
		this.innerLayout.getUnitByPosition('bottom').expand();
		
		// Create a graph layout
		var containers = [];
		var wires = [];
		
		offset = 700 * (graph.transformations.length - 1);
		
		// First one is for the data element
		containers[containers.length] = {
				 position:[offset,20],
				 "label": graph.transformations[0].name,
			    "xtype": "Webseer.StringContainer",
			    "terminals": [
						{ "name": "item", "direction": [-1,0], "offsetPosition": [-14,0],  "relativePosition": [0,.5]}
			 		],
			 		"height": "100",
						draggable: false,
					close: false
			 };
		// Then add all the rest of the nodes
		for (var i = 1; i < graph.transformations.length; i++) {
			var container = graph.transformations[i];
			containers[containers.length] = container;
			container.position = [offset - (700 * i),20];
			container.draggable = false;
			container.close = false;
			container.allowFill = false;
			container.contents = "Took " + graph.transformations[i].time + "ms";
		}
		for (var i = 0; i < graph.edges.length; i++) {
			wires[wires.length] = {
					src: {
						moduleId: graph.edges[i].sourceId,
						terminal: graph.edges[i].sourceOutput
					},
					tgt: {
						moduleId: graph.edges[i].targetId,
						terminal: graph.edges[i].targetInput
					},
					label: graph.edges[i].label,
					
					xtype: "Webseer.WebseerWire"
				};
		}
		
		var demoLayer = new WireIt.Layer({layerMap: false, parentEl: historyPanel});
		
		// You can use a global setWiring method	
		demoLayer.setWiring({
			containers: containers,
			
			wires: wires
			
		});
		
		demoLayer.render();
		
		for (var i = 0; i < demoLayer.containers.length; i++) {
			if (demoLayer.containers[i].dbId) {
				if (demoLayer.containers[i].eventViewSource) {
					demoLayer.containers[i].eventViewSource.subscribe(this.onViewSource, this, true);
				}
			}
		}
		
	},
	
	loadPipe: function(name) {

		if(!this.isSaved()) {
			if( !confirm("Warning: Your work is not saved yet ! Press ok to continue anyway.") ) {
				return;
			}
		}

		try {

			this.preventLayerChangedEvent = true;
			this.loadPanel.hide();

			var wiring = this.getPipeByName(name), i;

			if(!wiring) {
				this.alert("The wiring '"+name+"' was not found.");
				return;
		 	}

		   // TODO: check if current wiring is saved...
		   this.layer.clear();

		   this.propertiesForm.setValue(wiring.properties, false); // the false tells inputEx to NOT fire the updatedEvt

		   if(lang.isArray(wiring.modules)) {
  
		      // Containers
		      for(i = 0 ; i < wiring.modules.length ; i++) {
		         var m = wiring.modules[i];
	            m.config.title = m.name;
	            var container = this.layer.addContainer(m.config);
	            Dom.addClass(container.el, "WiringEditor-module-"+m.name);
	            container.setValue(m.value);
		      }
   
		      // Wires
		      if(lang.isArray(wiring.wires)) {
		          for(i = 0 ; i < wiring.wires.length ; i++) {
		             // On doit chercher dans la liste des terminaux de chacun des modules l'index des terminaux...
		             this.layer.addWire(wiring.wires[i]);
		          }
		      }
		  }
  
		  this.markSaved();
		
		  this.preventLayerChangedEvent = false;

 		}
 		catch(ex) {
    		this.alert(ex);
			if(window.console && YAHOO.lang.isFunction(console.log)) {
				console.log(ex);
			}
 		}
	},
	
	onWireRemoved: function(e, params) {
		var wire = params[0];
		if (this.selectedWire == wire) {
			this.selectedWire = null;
			this.clearPreview();
		}
		if(!this.preventLayerChangedEvent && wire.xtype == 'Webseer.WebseerWire') {
			this.adapter.removeWire(wire, {
				success: function(result) {
					this.save();
				},
				failure: function(errorStr) {
					this.alert("Unable to contact the backend to remove a wire: "+ errorStr);
				},
				scope: this
			});
		}
	}, 
	
	markSaved: function() {
		// Do nothing
	},
	
	markUnsaved: function() {
		// Do nothing
	},

	isSaved: function() {
		return true;
	},

	saveModuleSuccess: function(o) {
		// Silent
	}

	
});

})();