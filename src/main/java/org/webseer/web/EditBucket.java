package org.webseer.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webseer.bucket.BucketFactory;
import org.webseer.bucket.Data;
import org.webseer.bucket.FileBucket;
import org.webseer.java.JavaTypeTranslator;
import org.webseer.model.meta.Bucket;
import org.webseer.transformation.InputReader;
import org.webseer.transformation.OutputWriter;

public class EditBucket extends SeerServlet {

	private static final long serialVersionUID = 6130317513407572333L;

	@Override
	protected void transactionalizedGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String bucketPath = (String) getRequiredAttribute(request, "bucketPath");

		BucketFactory factory = BucketFactory.getBucketFactory(getNeoService());

		Bucket bucket = (Bucket) factory.getBucket(bucketPath);

		request.setAttribute("name", bucketPath);

		if (bucket != null) {
			// Populate information from the bucket
			List<DataBean> stringData = new ArrayList<DataBean>();
			InputReader bucketReader = bucket.getBucketReader();
			for (Data data : bucketReader) {
				stringData.add(new DataBean(data));
			}

			request.setAttribute("dataList", stringData);
		}

		RequestDispatcher rd = request.getRequestDispatcher("edit-bucket.jsp");
		rd.forward(request, response);
	}
	
	@Override
	protected void transactionalizedPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		String newData = getRequiredParameter(request, "newData");
		String bucketPath = (String) getRequiredAttribute(request, "bucketPath");

		BucketFactory factory = BucketFactory.getBucketFactory(getNeoService());
		Bucket bucket = factory.getBucket(bucketPath);
		
		if (bucket == null) {
			bucket = new FileBucket(getNeoService(), bucketPath);
			factory.addBucket(getCurrentUser(request).getLogin(), bucket);
		}

		OutputWriter bucketWriter = bucket.getBucketWriter();
		bucketWriter.writeData(JavaTypeTranslator.convertObject(newData));

		request.setAttribute("name", bucketPath);

		if (bucket != null) {
			// Populate information from the bucket
			List<DataBean> stringData = new ArrayList<DataBean>();
			InputReader bucketReader = bucket.getBucketReader();
			for (Data data : bucketReader) {
				stringData.add(new DataBean(data));
			}

			request.setAttribute("dataList", stringData);
		}

		RequestDispatcher rd = request.getRequestDispatcher("edit-bucket.jsp");
		rd.forward(request, response);
	}

	public static class DataBean {
		
		private String value;
		private String type;

		DataBean(Data data){
			this.type = data.getType().getName();
			this.value = JavaTypeTranslator.convertData(data,String.class);
		}
		
		public String getValue() {
			return this.value;
		}
		
		public String getType() {
			return this.type;
		}
		
	}

}
