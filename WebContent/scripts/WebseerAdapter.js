/**
 * Ajax Adapter. Expect JSON response for all queries.
 * @class WireIt.WiringEditor.adapters.Ajax
 * @static 
 */
Webseer.WebseerAdapter = {
	
		config: {
			url: 'modify'
		},
		
		init: function() {
			YAHOO.util.Connect.setDefaultPostHeader('application/json');
		},
		
		saveWiring: function(val, callbacks) {
			this._sendJsonRpcRequest("saveWiring", val, callbacks);
		},
		
		deleteWiring: function(val, callbacks) {
			this._sendJsonRpcRequest("deleteWiring", val, callbacks);
		},
		
		listWirings: function(val, callbacks) {
			this._sendJsonRpcRequest("listWirings", val, callbacks);
		},
		
		addContainer: function(container, callbacks) {
			this._sendJsonRpcRequest("addContainer", { typeId: container.typeId, position: container.position }, callbacks);
		},
		
		addBucketContainer: function(container, callbacks) {
			this._sendJsonRpcRequest("addBucketContainer", { name: container.label, position: container.position }, callbacks);
		},
		
		removeContainer: function(container, callbacks) {
			this._sendJsonRpcRequest("removeContainer", { containerId : container.dbId }, callbacks);
		},
		
		moveContainer: function(container, callbacks) {
			this._sendJsonRpcRequest("moveContainer", { containerId : container.dbId, position: container.getXY() }, callbacks);
		},
		
		addWire: function(wire, callbacks) {
			this._sendJsonRpcRequest("addWire", { srcId: wire.terminal1.container.dbId, srcOutput: wire.terminal1.rootField, srcField: wire.srcField,
				targetId: wire.terminal2.container.dbId, targetInput: wire.terminal2.rootField, targetField: wire.targetField }, callbacks);
		},
		
		removeWire: function(wire, callbacks) {
			this._sendJsonRpcRequest("removeWire", { srcId: wire.terminal1.container.dbId, srcOutput: wire.terminal1.rootField, srcField: wire.srcField,
				targetId: wire.terminal2.container.dbId, targetInput: wire.terminal2.rootField, targetField: wire.targetField }, callbacks);
		},
		
		previewWire: function(wire, callbacks) {
			this._sendJsonRpcRequest("previewWire", { srcId: wire.terminal1.container.dbId, srcOutput: wire.terminal1.rootField, 
				targetId: wire.terminal2.container.dbId, targetInput: wire.terminal2.rootField }, callbacks);
		},
		
		previewBucketContainer: function(container, callbacks) {
			this._sendJsonRpcRequest("previewBucketContainer", { containerId : container.dbId }, callbacks);
		},
		
		fillBucketContainer: function(container, callbacks) {
			this._sendJsonRpcRequest("fillBucketContainer", { containerId : container.dbId }, callbacks);
		},
		
		renameBucketContainer: function(container, callbacks) {
			this._sendJsonRpcRequest("renameBucketContainer", { containerId : container.dbId, name: container.label }, callbacks);
		},
		
		viewItemHistory: function(itemId, callbacks) {
			this._sendJsonRpcRequest("viewItemHistory", { itemId : itemId }, callbacks);
		},
		
		changeWorkspaceName: function(newName, callbacks) {
			this._sendJsonRpcRequest("changeWorkspaceName", { newName : newName }, callbacks);
		},
		
		getSource: function(container, callbacks) {
			this._sendJsonRpcRequest("getSource", { containerId : container.dbId }, callbacks);
		},
		
		deleteItem: function(itemId, containerId, callbacks) {
			this._sendJsonRpcRequest("deleteItem", { itemId : itemId, containerId: containerId }, callbacks);
		},
		
		setInputValue: function(terminal, value, callbacks) {
			this._sendJsonRpcRequest("setInputValue", { containerId : terminal.container.dbId, input: terminal.name, value: value }, callbacks);
		},
		
		deleteBucket: function(name, callbacks) {
			this._sendJsonRpcRequest("deleteBucket", { name: name }, callbacks);
		},
		
		getWireOptions: function(wire, callbacks) {
			this._sendJsonRpcRequest("getWireOptions", { srcId: wire.terminal1.container.dbId, srcOutput: wire.terminal1.name }, callbacks);
		},
		
		getModules: function(callbacks) {
			this._sendJsonRpcRequest("getModules", { }, callbacks);
		},
		
		setWeir: function(wire, weirId, callbacks) {
			this._sendJsonRpcRequest("setWeir", { srcId: wire.terminal1.container.dbId, srcOutput: wire.terminal1.rootField, 
				targetId: wire.terminal2.container.dbId, targetInput: wire.terminal2.rootField, weirId: weirId }, callbacks);
		},
		
		// private method to send a json-rpc request using ajax
		_sendJsonRpcRequest: function(method, value, callbacks) {
			var postData = YAHOO.lang.JSON.stringify({"id":(this._requestId++),"method":method,"params":value,"version":"json-rpc-2.0"});

			YAHOO.util.Connect.asyncRequest('POST', this.config.url, {
				success: function(o) {
					var s = o.responseText,
						 r = YAHOO.lang.JSON.parse(s);
				 	callbacks.success.call(callbacks.scope, r.result);
				},
				failure: function(r) {
					callbacks.failure.call(callbacks.scope, r);
				}
			},postData);
		},
		_requestId: 1
	
};
