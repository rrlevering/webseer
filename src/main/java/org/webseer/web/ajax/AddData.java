package org.webseer.web.ajax;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webseer.bucket.BucketFactory;
import org.webseer.bucket.FileBucket;
import org.webseer.java.JavaTypeTranslator;
import org.webseer.model.meta.Bucket;
import org.webseer.transformation.OutputWriter;
import org.webseer.web.SeerServlet;

import com.google.gson.JsonObject;

public class AddData extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void transactionalizedService(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		String data = getRequiredParameter(req, "newData");
		String bucketPath = (String) getRequiredAttribute(req, "bucketPath");

		BucketFactory factory = BucketFactory.getBucketFactory(getNeoService());
		Bucket bucket = factory.getBucket(bucketPath);

		if (bucket == null) {
			bucket = new FileBucket(getNeoService(), bucketPath);
		}

		OutputWriter bucketWriter = bucket.getBucketWriter();
		bucketWriter.writeData(JavaTypeTranslator.convertObject(data));

		JsonObject response = new JsonObject();

		Writer writer = resp.getWriter();
		writer.write(response.toString());
		writer.close();

	}

}
