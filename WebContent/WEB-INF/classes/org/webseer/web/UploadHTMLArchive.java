package org.webseer.web;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.webseer.transformation.BucketOutputStream;
import org.webseer.transformation.InputChannel;
import org.webseer.transformation.JavaFunction;
import org.webseer.transformation.OutputChannel;

import com.csvreader.CsvReader;
import com.google.protobuf.ByteString;

/**
 * This handles a zip file of expanded HTML documents, with possible resources and associates them with meta data inside
 * a contents.csv file inside the zip.
 * <p>
 * The zip file should look like:
 * 
 * <pre>
 * /contents.csv
 * /file1.html
 * /file1_files (the name without any extension + "_files")
 * ...
 * </pre>
 * <p>
 * The contents.csv file should look like:
 * 
 * <pre>
 * file,url,date
 * file1.html,http://myurl.com/,123423534543
 * </pre>
 * 
 * All meta fields are optional
 */
public class UploadHTMLArchive implements JavaFunction {

	@OutputChannel
	public BucketOutputStream<DocumentView> documents;

	@InputChannel
	public InputStream file;

	@InputChannel
	public InputStream metaFile;

	@InputChannel
	public int filenameField = 0;

	@InputChannel
	public int urlField = 1;

	@InputChannel
	public int dateField = 2;

	@InputChannel
	public String delimiter = ";";

	@Override
	public void execute() throws Throwable {
		Map<String, ByteArrayOutputStream> files = new HashMap<String, ByteArrayOutputStream>();
		Map<String, String> urls = new HashMap<String, String>();
		Map<String, Long> dates = new HashMap<String, Long>();

		// Parse the manifest
		CsvReader manifest = new CsvReader(new InputStreamReader(metaFile), delimiter.charAt(0));

		manifest.readHeaders();

		while (manifest.readRecord()) {
			String filename = manifest.get(filenameField);
			if (filename.indexOf('.') > 0) {
				filename = filename.substring(0, filename.indexOf('.'));
			}
			String date = manifest.get(dateField);
			if (date != null) {
				try {
					dates.put(filename, Long.parseLong(date));
				} catch (NumberFormatException e) {
					// Don't set the date
				}
			}
			String url = manifest.get(urlField);
			if (url != null) {
				urls.put(filename, url);
			}
		}

		ZipInputStream zip = new ZipInputStream(file);

		ZipEntry entry;
		while ((entry = zip.getNextEntry()) != null) {
			String name = entry.getName();
			if (name.matches("[^/]+")) {
				String filename = name;
				if (name.indexOf('.') > 0) {
					filename = name.substring(0, name.indexOf('.'));
				}
				// Parse an HTML file
				ByteArrayOutputStream output = files.get(filename);
				if (output == null) {
					output = new ByteArrayOutputStream();
					files.put(filename, output);
				}
				ZipOutputStream asZip = new ZipOutputStream(output);
				ZipEntry fileEntry = new ZipEntry("file.html");
				asZip.putNextEntry(fileEntry);
				IOUtils.copy(zip, asZip);
				asZip.closeEntry();

				if (!dates.containsKey(filename)) {
					dates.put(filename, entry.getTime());
				}
			} else if (name.indexOf("_files/") >= 0) {
				String filename = name.substring(0, name.indexOf("_files/"));

				// Add to the zip for the HTML file
				ByteArrayOutputStream output = files.get(filename);
				if (output == null) {
					output = new ByteArrayOutputStream();
					files.put(filename, output);
				}
				ZipOutputStream asZip = new ZipOutputStream(output);
				ZipEntry fileEntry = new ZipEntry(name);
				asZip.putNextEntry(fileEntry);
				IOUtils.copy(zip, asZip);
				asZip.closeEntry();
			}
		}
		for (Entry<String, ByteArrayOutputStream> fileEntry : files.entrySet()) {
			String filename = fileEntry.getKey();
			ByteArrayOutputStream fileData = fileEntry.getValue();

			DocumentView document = new DocumentView();
			document.docData = ByteString.copyFrom(fileData.toByteArray());
			document.date = dates.get(filename) == null ? -1 : dates.get(filename);
			document.url = urls.get(filename);

			this.documents.writeObject(document);
		}
	}
}
