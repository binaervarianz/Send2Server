package de.binaervarianz.sendtowebdav;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

public class SendToServer extends Activity {
	private final String TAG = this.getClass().getName();
	
	private WebDAVhandler httpHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Activity started");

		// read preferences
		SharedPreferences settings = getSharedPreferences(ConfigWebDAV.PREFS_PRIVATE, Context.MODE_PRIVATE);
		String serverURI = settings.getString(ConfigWebDAV.KEY_SERVER_URI, "https://");
		boolean trustAllSSLCerts = settings.getBoolean(ConfigWebDAV.KEY_TRUSTALLSSL, false);
		String user = settings.getString(ConfigWebDAV.KEY_USERNAME, "");
		String pass = settings.getString(ConfigWebDAV.KEY_PASSWORD, "");

		try {
			new URI(serverURI);
		} catch (URISyntaxException e) {
			Log.e(TAG, "URISyntaxException: "+e);
			// TODO: user error message
			// TODO: alert dialog notifying users of broken url and redirecting them to the config app (only happens when no prefs have been saved)
			// "Please provide a valid Server URI in the Config App first"
			return;
		}
		
		httpHandler = new WebDAVhandler(serverURI, user, pass);
		httpHandler.setTrustAllSSLCerts(trustAllSSLCerts);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		
		if ((intent.getAction().equals(Intent.ACTION_SEND) || intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) 
				&& extras != null) {
			//figure out what's to be send
	
			String url = "";
			SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMddHHmmss");
			
			// simple text like URLs
			if (extras.containsKey(Intent.EXTRA_TEXT)) {
				url += extras.getString(Intent.EXTRA_TEXT);
				
				// TODO: support multiple strings (not sure which app would do that, but there is probably one out there)
				try {
					// sending an URL is fast, so do it here
					// TODO: also put this in a thread. makes the user experience more responsive and consistent
					httpHandler.putFile("URL-"+dateFormater.format(new Date())+".txt", "", url);
				} catch (Exception e) {
					Toast.makeText(this, this.getString(R.string.app_name) + ": " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
					return;
				}
				
				// just a small message that the URL was send
				Toast.makeText(this, R.string.data_send, Toast.LENGTH_SHORT).show();
				this.finish();
				
			// binary files
			} else if (extras.containsKey(Intent.EXTRA_STREAM)) {
				ArrayList<Uri> files = new ArrayList<Uri>();
				if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
					files = (ArrayList<Uri>)extras.get(Intent.EXTRA_STREAM);
				} else {
					files.add((Uri)extras.get(Intent.EXTRA_STREAM));
				}

				// get the MIME type of the whole intent
				String intentType = intent.resolveType(this);
				
				// create a basename out of the type identifier -- awesome! [matsch]
				String basename = intentType.toUpperCase().charAt(0) + intentType.substring(1, intentType.indexOf('/'));
				basename = basename + "-" + dateFormater.format(new Date());
				
				for (Uri contentUri : files) {
					String filePath = "";
					String fileType = "";
					
					//debug
					Log.d(TAG, contentUri.toString());
					
					// there are real file system paths and logical content URIs
					if (contentUri.toString().startsWith("content:")) {
						filePath = this.getRealPathFromURI(contentUri);
						Log.d(TAG, "path: " + filePath);
						fileType = this.getContentResolver().getType(contentUri);
					} else if (contentUri.toString().startsWith("file:")){
						filePath = contentUri.getPath();
						Log.d(TAG, "path: " + filePath);
						fileType = intentType; // TODO: I guess this is not always correct (see not other way to do it right now)
					}
					
					Log.d(TAG, "fileType: "+fileType);
					
					// sending files may take time, so do it in another thread
					Toast.makeText(this, this.getString(R.string.app_name) + ": " + this.getString(R.string.data_sending), Toast.LENGTH_SHORT).show();
					new SendThread(handler, basename, "", filePath, fileType, (files.size()>1)).start();
				}
				
				this.finish(); // seems to work fine here (ie. the thread does continue and also reports success/failure)
			}	
		}
		Log.d(TAG, "Activity closed");
	}
	
	/**
	 * convert a <content:> URI (like from the gallery) to an absolute path
	 * TODO: is this generally applicable or only for images? what about videos in the gallery? what about other media in the mediastore outside of the gallery?
	 * 
	 * This is stolen from http://stackoverflow.com/questions/3401579/android-get-filename-and-path-from-uri-from-mediastore
	 * @param contentUri
	 * @return String with absolute path to file
	 */
	private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
	
	// Define the Handler that receives messages from the thread and update the progress
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
        	if (msg.what == 0) {
        		Toast.makeText(SendToServer.this, getString(R.string.app_name) + ": " + getString(R.string.data_send), Toast.LENGTH_LONG).show();
        	} else {
        		Toast.makeText(SendToServer.this, getString(R.string.app_name) + ": " + getString(R.string.data_sending_failure) + ": /r/n" + (String)msg.obj, Toast.LENGTH_LONG).show();
        	}
        	SendToServer.this.finish();
        }
    };
    
	private class SendThread extends Thread {
        Handler mHandler;
        String name, path, localPath, type;
        boolean subsequentCalls;
        
        SendThread(Handler h, String name, String path, String localPath, String type, boolean subsequentCalls) {
            mHandler = h;
            this.name= name;
            this.path = path;
            this.localPath = localPath;
            this.type = type;
            this.subsequentCalls = subsequentCalls;
        }
       
        public void run() {
        	
        	try {
        		httpHandler.putBinFile(name, path, localPath, type, subsequentCalls);   
        	} catch (Exception e) {
        		// can be:
        		// - ClientProtocolException:
        		// - IOException
        		// - IllegalArgumentException: invalid URL supplied (e.g. only "https://")
        		// - HttpException: HTTP response status codes >= 400
        		Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
        		
        		Message msg = mHandler.obtainMessage();
        		msg.obj = e.getClass().getSimpleName() + ": " + e.getLocalizedMessage();
                msg.what = 1;
                mHandler.sendMessage(msg);
        		return;
        	}
        	mHandler.sendEmptyMessage(0);
        }
    }
}