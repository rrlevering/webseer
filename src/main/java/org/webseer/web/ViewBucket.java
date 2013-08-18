package org.webseer.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.bucket.BucketFactory;
import org.webseer.bucket.Data;
import org.webseer.bucket.FileBucket;
import org.webseer.java.JavaTransformation;
import org.webseer.model.meta.Bucket;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.transformation.InputReader;
import org.webseer.transformation.OutputWriter;
import org.webseer.transformation.PullRuntimeTransformation;
import org.webseer.transformation.RuntimeTransformationException;
import org.webseer.transformation.TransformationFactory;
import org.webseer.web.EditBucket.DataBean;
import org.webseer.web.beans.TransformationBean;
import org.webseer.web.beans.UserBean;

public class ViewBucket extends SeerServlet {

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

		GraphDatabaseService service = getNeoService();
		TransformationFactory transFactory = TransformationFactory.getTransformationFactory(service);

		UserBean currentUser = (UserBean) request.getSession().getAttribute("user");
		Iterable<Transformation> transformations = transFactory.getAllTransformations();
		List<TransformationBean> transformationBeans = new ArrayList<TransformationBean>();
		for (Transformation transformation : transformations) {
			if ((currentUser != null && currentUser.getLogin().equals(transformation.getOwner()))
					|| transformation.getOwner() == null) {
				transformationBeans.add(new TransformationBean(transformation));
			}
		}
		request.setAttribute("transformations", transformationBeans);
		
		request.setAttribute("editable", (currentUser != null && currentUser.getLogin().equals(bucket.getOwner())));

		RequestDispatcher rd = request.getRequestDispatcher("bucket.jsp");
		rd.forward(request, response);
	}

	@Override
	protected void transactionalizedPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String transformationToRun = getRequiredParameter(request, "transformationToRun");
		String newBucketName = getRequiredParameter(request, "newBucketName");
		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);

		JavaTransformation transformation = (JavaTransformation) factory.getLatestTransformationByName(transformationToRun);

		String bucketPath = (String) getRequiredAttribute(request, "bucketPath");

		BucketFactory bucketFactory = BucketFactory.getBucketFactory(getNeoService());

		Bucket bucket = bucketFactory.getBucket(bucketPath);
		
		InputReader reader = bucket.getBucketReader();

		Bucket newBucket = new FileBucket(getNeoService(), newBucketName);
		bucketFactory.addBucket(getCurrentUser(request).getLogin(), newBucket);
		
		OutputWriter writer = newBucket.getBucketWriter();

		PullRuntimeTransformation runtimeTransformation;
		try {
			// TODO: This is hacked to assume there is a single input
			InputPoint firstInput = transformation.getInputPoints().iterator().next();
			OutputPoint firstOutput = transformation.getOutputPoints().iterator().next();
			
			runtimeTransformation = transformation.getPullRuntimeTransformation();
			runtimeTransformation.addInputChannel(firstInput.getName(), reader);
			runtimeTransformation.addOutputChannel(firstOutput.getName(), writer);
			
			while (runtimeTransformation.transform()) {
				// Fill
			}
		} catch (TransformationException e) {
			// This is an application error, there shouldn't be a mismatch here
			throw new IllegalStateException("Input points in runtime transformation should match transformation", e);
		} catch (RuntimeTransformationException e) {
			throw new ServletException("Problem running transformation", e);
		}
		
		response.sendRedirect(this.getServletContext().getContextPath() + "/bucket/" + newBucketName);
	}
}