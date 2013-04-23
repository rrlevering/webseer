package org.webseer.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.webseer.web.beans.UserBean;

public class GoogleSigninServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String CLIENT_SECRET = "BApvlm5uVOYnFJytEtvd2N8F";

	private static final String CLIENT_ID = "651154329698.apps.googleusercontent.com";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String code = request.getParameter("code");
		
		String redirect = request.getParameter("state");

		HttpClient httpClient = HttpClientBuilder.create().build();

		HttpPost postMethod = new HttpPost("https://accounts.google.com/o/oauth2/token");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("code", code));
		nameValuePairs.add(new BasicNameValuePair("client_id", CLIENT_ID));
		nameValuePairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
		nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
		nameValuePairs.add(new BasicNameValuePair("redirect_uri", "https://localhost:8443/webseer/googleSignin"));
		postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		HttpResponse serverResponse = httpClient.execute(postMethod);
		JsonParser parser = new JsonFactory().setCodec(new ObjectMapper()).createJsonParser(
				serverResponse.getEntity().getContent());
		JsonNode node = parser.readValueAsTree();
		String accessToken = node.get("access_token").asText();

		HttpGet getMethod = new HttpGet("https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + accessToken);
		serverResponse = httpClient.execute(getMethod);
		parser = new JsonFactory().setCodec(new ObjectMapper()).createJsonParser(
				serverResponse.getEntity().getContent());
		node = parser.readValueAsTree();

		String emailAddress = node.get("email").asText();
		String name = node.get("name").asText();
		
		request.getSession().setAttribute("user", new UserBean(emailAddress, emailAddress, name));
		
		response.sendRedirect(redirect);
	}
}
