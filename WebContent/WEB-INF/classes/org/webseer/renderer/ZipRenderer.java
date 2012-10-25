package org.webseer.renderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.webseer.transformation.InputChannel;
import org.webseer.transformation.OutputChannel;
import org.webseer.transformation.java.JavaFunction;

import com.google.protobuf.ByteString;

public class ZipRenderer implements JavaFunction {

	@OutputChannel
	public String contentType = "application/zip";

	@OutputChannel
	public String fileName = "data.zip";

	@OutputChannel
	public ByteString httpResponse;

	@InputChannel
	public Iterator<ByteString> fileData;

	@InputChannel
	public Iterator<String>[] metaData;

	@InputChannel
	public String filename;

	@Override
	public void execute() {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ZipOutputStream zipStream = new ZipOutputStream(output);
			StringBuilder contentsFile = new StringBuilder();
			contentsFile.append("File");

			int counter = 1;

			while (fileData.hasNext()) {
				String nextFile = "file" + counter++;
				ByteString nextFileData = fileData.next();
				contentsFile.append('\n').append(nextFile);

				for (Iterator<String> metaField : metaData) {
					contentsFile.append(',').append(metaField.next());
				}

				zipStream.putNextEntry(new ZipEntry(nextFile));
				zipStream.write(nextFileData.toByteArray());
				zipStream.closeEntry();
			}

			zipStream.putNextEntry(new ZipEntry("contents.csv"));
			zipStream.write(contentsFile.toString().getBytes("UTF-8"));
			zipStream.closeEntry();

			zipStream.close();

			fileName = filename;
			httpResponse = ByteString.copyFrom(output.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
