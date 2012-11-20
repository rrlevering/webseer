package org.webseer.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.webseer.java.JavaFunction;
import org.webseer.transformation.FunctionDef;
import org.webseer.transformation.ImportLibrary;
import org.webseer.transformation.InputChannel;
import org.webseer.transformation.OutputChannel;

import com.google.protobuf.ByteString;

@FunctionDef(description = "Downloads a document from a URL", keywords = {
		"fetch", "url" }, libraries = { @ImportLibrary(group = "commons-io", name = "commons-io", version = "1.4") })
public class URLFetcher implements JavaFunction {

	@InputChannel
	public String url;

	@OutputChannel
	public DocumentView fetchedDoc;

	@Override
	public void execute() {
		try {
			URL urlObject = new URL(url);
			InputStream stream = urlObject.openStream();
			ByteArrayOutputStream cache = new ByteArrayOutputStream();
			IOUtils.copy(stream, cache);
			fetchedDoc = new DocumentView();
			fetchedDoc.docData = ByteString.copyFrom(cache.toByteArray());
			fetchedDoc.url = url;
			fetchedDoc.date = System.currentTimeMillis();
		} catch (MalformedURLException e) {
			// Do nothing for now
		} catch (IOException e) {
			// Do nothing for now
		}
	}

}
