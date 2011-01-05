package de.binaervarianz.sendtowebdav;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class WebDAVhandler {
	private final String TAG = this.getClass().getName();
	
	private String serverURI;
	private String user;
	private String pass;
	
	public WebDAVhandler(String serverURI, String user, String pass) {
		super();
		this.serverURI = serverURI;
		this.user = user;
		this.pass = pass;
	}
	
	public void putFile(String filename, String path, String data)
			throws ClientProtocolException, IOException {
		//
		CredentialsProvider credProvider = new BasicCredentialsProvider();
		credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
				new UsernamePasswordCredentials(user, pass));
		//
		DefaultHttpClient http = new DefaultHttpClient();
		http.setCredentialsProvider(credProvider);
		//
		HttpPut put = new HttpPut(serverURI + "/" + path + filename);
		try {
			put.setEntity(new StringEntity(data, "UTF8"));
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "UnsupportedEncoding: ", e);
		}
		put.addHeader("Content-type", "text/plain");
		//
		HttpResponse response = http.execute(put);
		
		// TODO: just send every response as exception for now
		Log.d(TAG, "StatusLine: "
				+ response.getStatusLine().toString() + ", "
				+ response.getEntity().toString());

		// TODO: user hint: check permissions (eg. show the HTTP status code)
		// TODO: do we need/make use of the entity
	}
	
	// TODO: how can we check for WebDAV specifically? --> PUT a file (with timestamp)
	public void testConnection() throws IllegalArgumentException, ClientProtocolException, IOException {
		CredentialsProvider credProvider = new BasicCredentialsProvider();
		credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
				new UsernamePasswordCredentials(user, pass));
		//
		DefaultHttpClient http = new DefaultHttpClient();
		http.setCredentialsProvider(credProvider);
		//
		HttpGet get = new HttpGet(serverURI);
		//
		HttpResponse response = http.execute(get);
		response.getStatusLine().getStatusCode();
		response.getStatusLine().getReasonPhrase();
		// TODO: evaluate return status code
	}

	public String getServerURI() {
		return serverURI;
	}

	public void setServerURI(String serverURI) {
		this.serverURI = serverURI;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

}