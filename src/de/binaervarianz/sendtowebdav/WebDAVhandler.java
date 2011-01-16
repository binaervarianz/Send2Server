package de.binaervarianz.sendtowebdav;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.net.URLEncoder;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

public class WebDAVhandler {
	private final String TAG = this.getClass().getName();
	
	private String serverURI;
	private String user;
	private String pass;
	private boolean trustAllSSLCerts;
	
	public WebDAVhandler(String serverURI, String user, String pass) {
		super();
		this.serverURI = serverURI;
		this.user = user;
		this.pass = pass;
	}
	
	/**
	 * Connects to the previously saved server address and puts a file with the given name and content there.
	 * 
	 * @param filename
	 * @param path
	 * @param data : file contents
	 * @return boolean evaluating the http response code
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void putFile(String filename, String path, String data)
			throws IllegalArgumentException, ClientProtocolException, IOException, HttpException {

		HttpPut put = new HttpPut(serverURI + "/" + path + filename);
		try {
			put.setEntity(new StringEntity(data, "UTF8"));
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "UnsupportedEncoding: ", e);
		}
		put.addHeader("Content-type", "text/plain");

		DefaultHttpClient http = this.prepareHttpClient(user, pass);
		HttpResponse response = http.execute(put);
		StatusLine responseStatus = response.getStatusLine();		

		// debug
		Log.d(TAG, "StatusLine: "
				+ responseStatus.toString() + ", "
				+ " URL: " + serverURI);
		
		// evaluate the HTTP response status code
		if (responseStatus.getStatusCode() >= 400)
			throw new HttpException(responseStatus.toString());
	}
	
	public void putBinFile(String fileFolderName, String serverPath, String localFilePath, String type) 
			throws IllegalArgumentException, ClientProtocolException, IOException, HttpException {
		putBinFile(fileFolderName, serverPath, localFilePath, type, false);
	}
	
	/**
	 * Connects to the previously saved server address and puts a binary file with the given name and content there.
	 * 
	 * @param fileFolderName String with filename to be created on the server
	 * @param serverPath String with the server side path to put the file
	 * @param localFilePath String with the client side (Android) path to the source file
	 * @param type : String with MIME type of the data
	 * @param subsequentCalls : if this request has subsequent calls (ie. files put into the same directory)
	 * @return boolean evaluating the http response code
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void putBinFile(String fileFolderName, String serverPath, String localFilePath, String type, boolean subsequentCalls)
			throws IllegalArgumentException, ClientProtocolException, IOException, HttpException {
		
		// create collection on server
		HttpMkcol mkcol = new HttpMkcol(serverURI  + serverPath + fileFolderName + "/");
		Log.d(TAG, "Folder to be created: " + fileFolderName + "/");
		
		String file = localFilePath.substring(localFilePath.lastIndexOf('/')+1);
		file = URLEncoder.encode(file, "UTF-8");
		Log.d(TAG, "File to be created: " + file);
		
		HttpPut put = new HttpPut(serverURI  + serverPath + fileFolderName + "/" + file);	
		put.setEntity(new FileEntity(new File(localFilePath), type));
				
		put.addHeader("Content-type", type);

		DefaultHttpClient http = this.prepareHttpClient(user, pass);
		
		Log.d(TAG, "HTTP MKCOL Request");
		HttpResponse response = http.execute(mkcol);
		StatusLine responseStatus = response.getStatusLine();	
		// debug
		Log.d(TAG, "StatusLine: "
				+ responseStatus.toString() + ", "
				+ " URL: " + serverURI);
		
		// evaluate the HTTP response status code
		// ignore 405 (Method Not Allowed) for subsequent calls
		if (responseStatus.getStatusCode() >= 400 && (responseStatus.getStatusCode() != 405 && subsequentCalls))
			throw new HttpException(responseStatus.toString());
		
		Log.d(TAG, "HTTP PUT Request");
		response = http.execute(put);
		responseStatus = response.getStatusLine();		

		// debug
		Log.d(TAG, "StatusLine: "
				+ responseStatus.toString() + ", "
				+ " URL: " + serverURI);
		
		// evaluate the HTTP response status code
		if (responseStatus.getStatusCode() >= 400)
			throw new HttpException(responseStatus.toString());
	}
	
	/**
	 * Removes file on remote server given by remotefile using HTTP DELETE method.
	 * 
	 * @param remotefile
	 * @return void
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private void deleteFile(String filename, String path) throws ClientProtocolException, IOException {
		
		HttpDelete delete = new HttpDelete(serverURI + "/" + path + filename);
		
		DefaultHttpClient http = this.prepareHttpClient(user, pass);
		HttpResponse response = http.execute(delete);
		StatusLine responseStatus = response.getStatusLine();
		
		// debug
		Log.d(TAG, "StatusLine: " + responseStatus.toString() + ", "
				+ " URL: " + filename);
	}
	
		
	/**
	 * Connects to the previously saved server address and tries to download the specified file.
	 * 
	 * @param filename String with filename to be downloaded from the server
	 * @param path String with the server side path to the file
	 * @return boolean evaluating the http response code
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String getFile(String filename, String path)
			throws IllegalArgumentException, ClientProtocolException, IOException, HttpException {		
		
		HttpGet get = new HttpGet(serverURI + "/" + path + filename);	
				
		DefaultHttpClient http = this.prepareHttpClient(user, pass);
		
		HttpResponse response = http.execute(get);
		StatusLine responseStatus = response.getStatusLine();		
		
		if (responseStatus.getStatusCode() == 200) {
			InputStream is = response.getEntity().getContent();
			StringBuffer sb = new StringBuffer();
			int chr;
			while ((chr = is.read()) != -1)
				sb.append((char) chr);
		
			// debug
			Log.d(TAG, "Content: " + sb.toString());
		
			Log.d(TAG, "StatusLine: "
				+ responseStatus.toString() + ", "
				+ " URL: " + serverURI);
			return(sb.toString());
		} else {
			// evaluate the HTTP response status code
			if (responseStatus.getStatusCode() >= 400)
				throw new HttpException(responseStatus.toString());
		
			return("");
		}
		
	}
	
	/**
	 * Tests the connection by sending a GET request (no evaluation of results so far, just throwing exceptions if failing)
	 */
	public boolean testConnection() throws IllegalArgumentException, ClientProtocolException, IOException, HttpException {
		
		SimpleDateFormat dateformater = new SimpleDateFormat("yyyyMMddHHmmss");
		String timestamp = dateformater.format(new Date());
		Log.d(TAG, "PUT");
		putFile("ConnectionTest-"+ timestamp +".txt", "", "please delete! " + timestamp);
		
		Log.d(TAG, "GET");
		String ret = getFile("ConnectionTest-"+ timestamp +".txt", "");
		
		boolean retValue;
		
		if (ret.equals(("please delete! " + timestamp))) {
			retValue = true;
			Log.d(TAG, "Success");
		} else {
			retValue = false;
		}
		Log.d(TAG, "DEL");
		deleteFile("ConnectionTest-"+ timestamp +".txt", "");	
		
		return(retValue);

	}
	
	/**
	 * Creates a new Connection Manager needed for http/https communication
	 * @param params
	 * @return
	 */
	private ClientConnectionManager getConManager (HttpParams params) {
		SchemeRegistry registry = new SchemeRegistry();
	    registry.register(new Scheme("http", new PlainSocketFactory(), 80));
	    registry.register(new Scheme("https", (trustAllSSLCerts ? new TrustAllSocketFactory() : SSLSocketFactory.getSocketFactory()), 443));
		return new ThreadSafeClientConnManager(params, registry);
	}
	
	/**
	 * Create a DefaultHttpClient instance preconfigured with username and password
	 * @param user
	 * @param pass
	 * @return
	 */
	private DefaultHttpClient prepareHttpClient(String user, String pass) {
		BasicCredentialsProvider credProvider = new BasicCredentialsProvider();
		credProvider.setCredentials(AuthScope.ANY,
				new UsernamePasswordCredentials(user, pass));		
		
	    HttpParams params = new BasicHttpParams();
		DefaultHttpClient http = new DefaultHttpClient(this.getConManager(params), params);
		http.setCredentialsProvider(credProvider);
		
		return http;
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

	public boolean isTrustAllSSLCerts() {
		return trustAllSSLCerts;
	}

	public void setTrustAllSSLCerts(boolean trustAllSSLCerts) {
		this.trustAllSSLCerts = trustAllSSLCerts;
	}
}