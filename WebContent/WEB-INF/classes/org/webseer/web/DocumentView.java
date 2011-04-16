package org.webseer.web;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.webseer.type.Optional;
import org.webseer.type.Required;
import org.webseer.type.Type;

import com.google.protobuf.ByteString;

/**
 * An document view is a snapshot of an HTML file in time. It possibly includes the associated resources to make the
 * document load in a browser as it was seen at the time it was downloaded. If this is the case, the complete flag is
 * set to true. The actual byte data is a zipped file that at least contains a single internal file called file.html. It
 * may also contain a directory of nested resources.
 * 
 * @author ryan
 */
@Type
public class DocumentView {

	@Required
	public ByteString docData;

	@Required
	public String url;

	@Required
	public long date;

	@Optional
	public boolean complete;

	public String toString() {
		DateFormat format = new SimpleDateFormat();
		String returnString = url;
		if (date >= 0) {
			returnString += " [" + format.format(new Date(date)) + "]";
		}
		return returnString;
	}

}
