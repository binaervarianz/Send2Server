package de.binaervarianz.sendtowebdav;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ConfigWebDAV extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		// Restore preferences
	    SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
	    ((EditText)findViewById(R.id.server_uri_input)).setText(settings.getString("serverURI", "https://"));
	    ((EditText)findViewById(R.id.user_name_input)).setText(settings.getString("username", ""));
	    ((EditText)findViewById(R.id.pass_input)).setText(settings.getString("password", ""));
	    
        final Button button = (Button) findViewById(R.id.saveButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// TODO: check server connection and user/pass
            	
            	// safe the preferences
        		// We need an Editor object to make preference changes.
        	    SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        	    SharedPreferences.Editor editor = settings.edit();
        	    editor.putString("serverURI", ((EditText)findViewById(R.id.server_uri_input)).getText().toString());
        	    editor.putString("username", ((EditText)findViewById(R.id.user_name_input)).getText().toString());
        	    editor.putString("password", ((EditText)findViewById(R.id.pass_input)).getText().toString());
        	    
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
        	    SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        	    SharedPreferences.Editor editor = settings.edit();
        	    editor.putString("serverURI", ((EditText)findViewById(R.id.server_uri_input)).getText().toString());
        	    editor.putString("username", ((EditText)findViewById(R.id.user_name_input)).getText().toString());
        	    editor.putString("password", ((EditText)findViewById(R.id.pass_input)).getText().toString());
        	    
        	    // Commit the edits!
        	    editor.commit();
            }
        });
    }
}