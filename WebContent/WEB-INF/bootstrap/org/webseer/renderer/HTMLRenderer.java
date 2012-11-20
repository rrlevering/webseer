package org.webseer.renderer;

import org.webseer.java.JavaFunction;
import org.webseer.transformation.InputChannel;
import org.webseer.transformation.OutputChannel;
import org.webseer.web.DocumentView;

import com.google.protobuf.ByteString;

public class HTMLRenderer implements JavaFunction {

	@OutputChannel
	public String contentType = "text/html";

	@OutputChannel
	public ByteString httpResponse;

	@InputChannel
	public DocumentView docToRender;

	@Override
	public void execute() {
		httpResponse = docToRender.docData;
	}

}
