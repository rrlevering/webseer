package org.webseer.web.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webseer.model.Workspace;
import org.webseer.model.WorkspaceBucket;
import org.webseer.model.meta.Transformation;
import org.webseer.transformation.TransformationFactory;
import org.webseer.web.WorkspaceServlet;
import org.webseer.web.beans.BucketBean;
import org.webseer.web.beans.TransformationBean;

public class TransformationLanguageScript extends WorkspaceServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void transactionalizedServiceWorkspace(Workspace workspace, HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {
		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);

		List<TransformationBean> systemTransformations = new ArrayList<TransformationBean>();
		List<TransformationBean> renderers = new ArrayList<TransformationBean>();
		for (Transformation transformation : factory.getAllTransformations()) {
			TransformationBean bean = new TransformationBean(transformation);
			if (transformation.getOutput("httpResponse") != null) {
				renderers.add(bean);
			} else {
				systemTransformations.add(bean);
			}
		}
		req.setAttribute("possibleRenderers", renderers);

		req.setAttribute("possibleTransformations", systemTransformations);

		List<BucketBean> buckets = new ArrayList<BucketBean>();
		for (WorkspaceBucket bucket : workspace.getWorkspaceBuckets()) {
			buckets.add(new BucketBean(bucket));
		}

		req.setAttribute("possibleBuckets", buckets);

		RequestDispatcher rd = req.getRequestDispatcher("/scripts/transformation-language.jsp");
		rd.forward(req, resp);
	}

}
