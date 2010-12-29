package de.binaervarianz.sendtowebdav;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
	    
        final Button button = (Button) findViewById(R.id.saveButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// TODO: check server connection and user/pass
            	
            	// safe the preferences
        		// We need an Editor object to make preference changes.
        	    SharedPreferences settings = getSharedPreferences(PREFS_PRIVATE, Context.MODE_PRIVATE);
        	    SharedPreferences.Editor editor = settings.edit();
        	    editor.putString(KEY_SERVER_URI, ((EditText)findViewById(R.id.server_uri_input)).getText().toString());
        	    editor.putString(KEY_USERNAME, ((EditText)findViewById(R.id.user_name_input)).getText().toString());
        	    editor.putString(KEY_PASSWORD, ((EditText)findViewById(R.id.pass_input)).getText().toString());
        	    
        	    // Commit the edits!
        	    editor.commit();
            }
        });
        
        final Button button1 = (Button) findViewById(R.id.testButton);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// checkServerConnection();
            	// TODO: check server connection and user/pass
            	
            	// safe the preferences
        		// We need an Editor object to make preference changes.
        	    SharedPreferences settings = getSharedPreferences(PREFS_PRIVATE, Context.MODE_PRIVATE);
        	    SharedPreferences.Editor editor = settings.edit();
        	    editor.putString(KEY_SERVER_URI, ((EditText)findViewById(R.id.server_uri_input)).getText().toString());
        	    editor.putString(KEY_USERNAME, ((EditText)findViewById(R.id.user_name_input)).getText().toString());
        	    editor.putString(KEY_PASSWORD, ((EditText)findViewById(R.id.pass_input)).getText().toString());
        	    
        	    // Commit the edits!
        	    editor.commit();
            }
        });
    }
}