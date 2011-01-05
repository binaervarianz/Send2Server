package de.binaervarianz.sendtowebdav;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class ConfigWebDAV extends Activity {
	private final String TAG = this.getClass().getName();
	
	public static final String PREFS_PRIVATE = "PREFS_PRIVATE";
	public static final String KEY_SERVER_URI = "SERVER_URI";
	public static final String KEY_USERNAME = "USERNAME";
	public static final String KEY_PASSWORD = "PASSWORD";
	public static final String KEY_TRUSTALLSSL = "TRUSTALLSSL";
	
	private ProgressDialog dialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		// Restore preferences
	    SharedPreferences settings = getSharedPreferences(PREFS_PRIVATE, Context.MODE_PRIVATE);
	    ((EditText)findViewById(R.id.server_uri_input)).setText(settings.getString(KEY_SERVER_URI, "https://"));
	    ((CheckBox)findViewById(R.id.trustAllSSLCerts)).setChecked(settings.getBoolean(KEY_TRUSTALLSSL, false));
	    ((EditText)findViewById(R.id.user_name_input)).setText(settings.getString(KEY_USERNAME, ""));
	    ((EditText)findViewById(R.id.pass_input)).setText(settings.getString(KEY_PASSWORD, ""));
	    
	    // save button
        final Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	dialog = ProgressDialog.show(ConfigWebDAV.this, "", ConfigWebDAV.this.getString(R.string.testing_saving_progress), true);
            	new SaveThread(handler).start();
            }
        });
        
        // test connection button
        final Button testButton = (Button) findViewById(R.id.testButton);
        testButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	dialog = ProgressDialog.show(ConfigWebDAV.this, "", ConfigWebDAV.this.getString(R.string.testing_progress), true);
            	new TestThread(handler).start();
            }
        });
        
        // deploy server button
        final Button deployServerButton = (Button) findViewById(R.id.deployServerButton);
        deployServerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// TODO: put an attached .zip file with server components and a README on server

            	Toast.makeText(ConfigWebDAV.this, "TODO", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Call the testConnectivity Method of the WebDAVhandler and wait for exceptions
	 *
     * @param serverURI URL to test (doesn't need to be saved)
     * @param user
     * @param pass
     * @param trustSSLCerts
     * @return boolean, true for test passed
     */
	private boolean checkConnection(String serverURI, String user, String pass, boolean trustSSLCerts) {
    	WebDAVhandler httpHandler = new WebDAVhandler(serverURI, user, pass);
    	httpHandler.setTrustAllSSLCerts(trustSSLCerts);
    	
    	try {
			httpHandler.testConnection();
		} catch (ClientProtocolException e) {
			Toast.makeText(ConfigWebDAV.this, "ClientProtocolException: "+e, Toast.LENGTH_LONG).show();
			Toast.makeText(ConfigWebDAV.this, R.string.settings_not_saved, Toast.LENGTH_LONG).show();
			Log.e(TAG, "ClientProtocolException: "+e);
			return false;
		} catch (IOException e) {
			Toast.makeText(ConfigWebDAV.this, "IOException: "+e, Toast.LENGTH_LONG).show();
			Toast.makeText(ConfigWebDAV.this, R.string.settings_not_saved, Toast.LENGTH_LONG).show();
			Log.e(TAG, "IOException: "+e);
			return false;
		} catch (IllegalArgumentException e) {
			// invalid URL supplied (e.g. only "https://") 
			Toast.makeText(ConfigWebDAV.this, "IllegalArgumentException: "+e, Toast.LENGTH_LONG).show();
			Toast.makeText(ConfigWebDAV.this, R.string.settings_not_saved, Toast.LENGTH_LONG).show();
			Log.e(TAG, "IllegalArgumentException: "+e);
			return false;
		} catch (HttpException e) {
			// HTTP response status codes >= 400
			Toast.makeText(ConfigWebDAV.this, "HttpException: "+e, Toast.LENGTH_LONG).show();
			Toast.makeText(ConfigWebDAV.this, R.string.settings_not_saved, Toast.LENGTH_LONG).show();
			Log.e(TAG, "HttpException: "+e);
			return false;
		}
		return true;
    }

    // Define the Handler that receives messages from the thread and update the progress
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
        	dialog.dismiss();
        	Toast.makeText(ConfigWebDAV.this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        }
    };

    private class SaveThread extends Thread {
        Handler mHandler;
       
        SaveThread(Handler h) {
            mHandler = h;
        }
       
        public void run() {
        	String serverURI = ((EditText)findViewById(R.id.server_uri_input)).getText().toString();
        	boolean trustAllSSLCerts = ((CheckBox)findViewById(R.id.trustAllSSLCerts)).isChecked();
        	String user = ((EditText)findViewById(R.id.user_name_input)).getText().toString();
        	String pass = ((EditText)findViewById(R.id.pass_input)).getText().toString();

        	// sanitize the URL string & append '/' if not already present
        	serverURI = serverURI.trim();
        	if  (!serverURI.endsWith("/"))
        		serverURI += "/";
        	
        	if (!checkConnection(serverURI, user, pass, trustAllSSLCerts))
        		return;
        	
        	// save the preferences
    	    SharedPreferences settings = getSharedPreferences(PREFS_PRIVATE, Context.MODE_PRIVATE);
    	    SharedPreferences.Editor editor = settings.edit();
    	    editor.putString(KEY_SERVER_URI, serverURI);
    	    editor.putBoolean(KEY_TRUSTALLSSL, trustAllSSLCerts);
    	    editor.putString(KEY_USERNAME, user);
    	    editor.putString(KEY_PASSWORD, pass); // TODO: encrypt pwd

    	    // Commit the edits!
    	    editor.commit();
    	    
        	mHandler.sendEmptyMessage(0);
        }
    }
    
    private class TestThread extends Thread {
        Handler mHandler;
       
        TestThread(Handler h) {
            mHandler = h;
        }
       
        public void run() {
        	String serverURI = ((EditText)findViewById(R.id.server_uri_input)).getText().toString();
        	boolean trustAllSSLCerts = ((CheckBox)findViewById(R.id.trustAllSSLCerts)).isChecked();
        	String user = ((EditText)findViewById(R.id.user_name_input)).getText().toString();
        	String pass = ((EditText)findViewById(R.id.pass_input)).getText().toString();
        	
        	// sanitize the URL string & append '/' if not already present
        	serverURI = serverURI.trim();
        	if  (!serverURI.endsWith("/"))
        		serverURI = serverURI + "/";
        	
        	if (!checkConnection(serverURI, user, pass, trustAllSSLCerts)) {
        		dialog.dismiss();
        		return;
        	}
    	    
        	mHandler.sendEmptyMessage(0);
        }
    }
}
