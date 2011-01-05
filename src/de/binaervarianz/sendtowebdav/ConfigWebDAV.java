package de.binaervarianz.sendtowebdav;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ConfigWebDAV extends Activity {
	private final String TAG = this.getClass().getName();
	
	public static final String PREFS_PRIVATE = "PREFS_PRIVATE";
	public static final String KEY_SERVER_URI = "SERVER_URI";
	public static final String KEY_USERNAME = "USERNAME";
	public static final String KEY_PASSWORD = "PASSWORD";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		// Restore preferences
	    SharedPreferences settings = getSharedPreferences(PREFS_PRIVATE, Context.MODE_PRIVATE);
	    ((EditText)findViewById(R.id.server_uri_input)).setText(settings.getString(KEY_SERVER_URI, "https://"));
	    ((EditText)findViewById(R.id.user_name_input)).setText(settings.getString(KEY_USERNAME, ""));
	    ((EditText)findViewById(R.id.pass_input)).setText(settings.getString(KEY_PASSWORD, ""));
	    
	    // save button
        final Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	String serverURI = ((EditText)findViewById(R.id.server_uri_input)).getText().toString();
            	String user = ((EditText)findViewById(R.id.user_name_input)).getText().toString();
            	String pass = ((EditText)findViewById(R.id.pass_input)).getText().toString();
            	
//            	if (!checkConnection(v, serverURI, user, pass))
 //           		return;
    
            	// save the preferences
        	    SharedPreferences settings = getSharedPreferences(PREFS_PRIVATE, Context.MODE_PRIVATE);
        	    SharedPreferences.Editor editor = settings.edit();
        	    editor.putString(KEY_SERVER_URI, serverURI);
        	    editor.putString(KEY_USERNAME, user);
        	    editor.putString(KEY_PASSWORD, pass); // TODO: encrypt pwd
        	    
        	    // Commit the edits!
        	    editor.commit();
        	    
        	    // TODO: modal spinning wheel when testing connection
        	    Toast.makeText(v.getContext(), "Settings saved!", Toast.LENGTH_SHORT).show();
            }
        });
        
        // test connection button
        final Button testButton = (Button) findViewById(R.id.testButton);
        testButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	String serverURI = ((EditText)findViewById(R.id.server_uri_input)).getText().toString();
            	String user = ((EditText)findViewById(R.id.user_name_input)).getText().toString();
            	String pass = ((EditText)findViewById(R.id.pass_input)).getText().toString();
            	
            	if (!checkConnection(v, serverURI, user, pass))
            		return;
            }
        });
    }
    
    private boolean checkConnection(View v, String serverURI, String user, String pass) {
    	WebDAVhandler httpHandler = new WebDAVhandler(serverURI, user, pass);
    	
    	try {
			httpHandler.testConnection();
		} catch (ClientProtocolException e) {
			Toast.makeText(v.getContext(), "ClientProtocolException: "+e, Toast.LENGTH_LONG).show();
			Toast.makeText(v.getContext(), "Settings NOT saved!", Toast.LENGTH_LONG).show();
			Log.e(TAG, "ClientProtocolException: "+e);
			return false;
		} catch (IOException e) {
			Toast.makeText(v.getContext(), "IOException: "+e, Toast.LENGTH_LONG).show();
			Toast.makeText(v.getContext(), "Settings NOT saved!", Toast.LENGTH_LONG).show();
			Log.e(TAG, "IOException: "+e);
			return false;
		}
		return true;
    }
}