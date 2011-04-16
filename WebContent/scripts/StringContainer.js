/*global YAHOO,WireIt */
/**
 * Container represented by an image
 * @class ImageContainer
 * @extends WireIt.Container
 * @constructor
 * @param {Object} options
 * @param {WireIt.Layer} layer
 */
Webseer.StringContainer = function(options, layer) {
	Webseer.StringContainer.superclass.constructor.call(this, options, layer);
};

YAHOO.lang.extend(Webseer.StringContainer, WireIt.Container, {
	
	/** 
    * @property xtype
    * @description String representing this class for exporting as JSON
    * @default "WireIt.ImageContainer"
    * @type String
    */
   xtype: "Webseer.StringContainer",
	
	/** 
    * @property resizable
    * @description boolean that makes the container resizable
    * @default false
    * @type Boolean
    */
	resizable: false,
	
	/** 
    * @property ddHandle
    * @description (only if draggable) boolean indicating we use a handle for drag'n drop
    * @default false
    * @type Boolean
    */
	ddHandle: false,
	
	/** 
    * @property className
    * @description CSS class name for the container element
    * @default ""WireIt-Container WireIt-ImageContainer"
    * @type String
    */
	className: "WireIt-Container WireIt-ImageContainer",
	
	/** 
    * @property label
    * @description label to display
    * @default null
    * @type String
    */
	label: null,
   
   /**
 	 * Add the image property as a background image for the container
    * @method render
    */
   render: function() {
      WireIt.ImageContainer.superclass.render.call(this);
      this.bodyEl.innerHTML = this.label;
      this.bodyEl.style.width = "100px";
	  this.bodyEl.style.height = "100px";
   }
   
});