package de.binaervarianz.sendtowebdav;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
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
	
	private static int sender = 0;
	private static final int SENDER_TEST = 1;
	private static final int SENDER_SAVE = 2;
	private static final int SENDER_DEPLOY = 3;
	
	private ProgressDialog dialog;
	
	private WebDAVhandler httpHandler;
	
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
        final Button saveButton = (Button)findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	dialog = ProgressDialog.show(ConfigWebDAV.this, "", ConfigWebDAV.this.getString(R.string.testing_saving_progress), true);
            	sender = SENDER_SAVE;
            	new SaveThread(handler).start();
            }
        });
        
        // test connection button
        final Button testButton = (Button)findViewById(R.id.testButton);
        testButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	dialog = ProgressDialog.show(ConfigWebDAV.this, "", ConfigWebDAV.this.getString(R.string.testing_progress), true);
            	sender = SENDER_TEST;
            	new TestThread(handler).start();
            }
        });
        
		// deploy server button
		final Button deployServerButton = (Button) findViewById(R.id.deployServerButton);
		deployServerButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog = ProgressDialog.show(ConfigWebDAV.this, "", ConfigWebDAV.this.getString(R.string.deploy_progress), true);
				sender = SENDER_DEPLOY;

				AssetManager assetManager = getAssets();
				InputStream stream = null;
				long length = 0L;
				try {
					stream = assetManager.open("SendToWebDAV.php");
					
					// ugly construct to get the length of the InputStream/file
					long skip = 1L;
					while (skip > 0L) {
						skip = stream.skip(Integer.MAX_VALUE);
						length += skip;
					} 
					stream =  assetManager.open("SendToWebDAV.php"); // reset the stream
					
					new SendThread(handler, "SendToWebDAV.php.txt", "php", stream, length).start();
				} catch (IOException e) {
					Log.d(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
				} finally {
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException e) {}
					}
				}
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
	private Exception checkConnection(String serverURI, String user, String pass, boolean trustSSLCerts) {
    	httpHandler = new WebDAVhandler(serverURI, user, pass);
    	httpHandler.setTrustAllSSLCerts(trustSSLCerts);
    	boolean testResult;
    	
    	try {
			testResult = httpHandler.testConnection();
    	} catch (Exception e) {
    		// can be:
    		// - ClientProtocolException:
    		// - IOException
    		// - IllegalArgumentException: invalid URL supplied (e.g. only "https://")
    		// - HttpException: HTTP response status codes >= 400
    		Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
			return e;
		}

    	if (!testResult)
			return new Exception("Connection Test Failed!");
		else
			return null;
    }

    // Define the Handler that receives messages from the thread
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
        	dialog.dismiss();

        	if (msg.what == 0) {
        		switch (sender) {
        			case SENDER_TEST:
        				Toast.makeText(ConfigWebDAV.this, R.string.connection_success, Toast.LENGTH_SHORT).show();
        				break;
        			case SENDER_SAVE:
        				Toast.makeText(ConfigWebDAV.this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        				break;
        			case SENDER_DEPLOY:
        				Toast.makeText(ConfigWebDAV.this, R.string.server_deployed, Toast.LENGTH_SHORT).show();
        				break;
        		}
        	} else {
        		Toast.makeText(ConfigWebDAV.this, (String)msg.obj, Toast.LENGTH_LONG).show();
        		switch (sender) {
    				case SENDER_TEST:
    					Toast.makeText(ConfigWebDAV.this, R.string.connection_failed, Toast.LENGTH_LONG).show();
    					break;
    				case SENDER_SAVE:
    					Toast.makeText(ConfigWebDAV.this, R.string.settings_not_saved, Toast.LENGTH_LONG).show();
    					break;
    				case SENDER_DEPLOY:
    					Toast.makeText(ConfigWebDAV.this, R.string.server_not_deployed, Toast.LENGTH_SHORT).show();
    					break;
        		}
        	}
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
        	
        	Exception e = checkConnection(serverURI, user, pass, trustAllSSLCerts); 
        	
        	Message msg = mHandler.obtainMessage();
        	if (e == null) {
	        	// save the preferences
	    	    SharedPreferences settings = getSharedPreferences(PREFS_PRIVATE, Context.MODE_PRIVATE);
	    	    SharedPreferences.Editor editor = settings.edit();
	    	    editor.putString(KEY_SERVER_URI, serverURI);
	    	    editor.putBoolean(KEY_TRUSTALLSSL, trustAllSSLCerts);
	    	    editor.putString(KEY_USERNAME, user);
	    	    editor.putString(KEY_PASSWORD, pass); // TODO: encrypt pwd (or not?)
	
	    	    // Commit the edits!
	    	    editor.commit();
	    	    
	    	    msg.what = 0;
        	} else {
        		msg.obj = e.getClass().getSimpleName() + ": " + e.getLocalizedMessage();
                msg.what = 1;
        	}
            mHandler.sendMessage(msg);
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
        	
        	Exception e = checkConnection(serverURI, user, pass, trustAllSSLCerts); 
        	
        	Message msg = mHandler.obtainMessage();
        	if (e == null) {
	    	    msg.what = 0;
        	} else {
        		msg.obj = e.getClass().getSimpleName() + ": " + e.getLocalizedMessage();
                msg.what = 1;
        	}
            mHandler.sendMessage(msg);
        }
    }
    
	private class SendThread extends Thread {
        Handler mHandler;
        String name, path;
        InputStream dataStream;
        long length;
        
        // for streams
        public SendThread(Handler h, String name, String path, InputStream dataStream, long length) {
			mHandler = h;
			this.name = name;
			this.path = path;
			this.dataStream = dataStream;
			this.length = length;
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
        	
        	Exception e = checkConnection(serverURI, user, pass, trustAllSSLCerts); 
        	
			Message msg = mHandler.obtainMessage();
			if (e == null) {
				msg.what = 0;
				
				try {
					if (dataStream != null)
						httpHandler.putStream(name, path, dataStream, length);
				} catch (Exception e1) {
					// can be:
					// - ClientProtocolException:
					// - IOException
					// - IllegalArgumentException: invalid URL supplied (e.g.
					// only "https://")
					// - HttpException: HTTP response status codes >= 400
					Log.e(TAG, e1.getClass().getSimpleName() + ": "
							+ e1.getMessage());

					msg.obj = e1.getClass().getSimpleName() + ": "
							+ e1.getLocalizedMessage();
					msg.what = 1;
				}
			} else {
				msg.obj = e.getClass().getSimpleName() + ": "
						+ e.getLocalizedMessage();
				msg.what = 1;
			}
			mHandler.sendMessage(msg);
        }
    }
}