package de.binaervarianz.sendtowebdav;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.widget.RemoteViews;
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
		
		// TODO: multiple files (Intent.ACTION_SEND_MULTIPLE ?)
		
		if (intent.getAction().equals(Intent.ACTION_SEND) && extras != null) {
			//figure out what's to be send
	
/** PLEASE DELETE
 * overview of common MIME types:
 * https://code.google.com/p/openintents/source/browse/trunk/filemanager/FileManager/res/xml/mimetypes.xml
 * Intent.ACTION_SEND docs: http://developer.android.com/reference/android/content/Intent.html#ACTION_SEND
			//this is for debugging and getting information on new media types
			this.type = intent.resolveType(this);
			//URLS give text/plain, images image/jpeg ....
			Log.d(TAG, this.type);
			for (String s : extras.keySet()) {
				Log.d(TAG, s);
				//Urls give EXTRA_TEXT, images EXTRA_STREAM
			}
			if (intent.getDataString() != null) {
				Log.d(TAG, intent.getDataString());	
				//no data so far attached to the intents
			}			
			/// end of debug, resume normal operation			
 */			
			String url = "";
			SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMddHHmmss"); 
			
			// simple text like URLs
			if (extras.containsKey(Intent.EXTRA_TEXT)) {
				url += extras.getString(Intent.EXTRA_TEXT);
				
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
				String filePath = "";
				Uri contentUri = (Uri)extras.get(Intent.EXTRA_STREAM);
				
				//debug
				Log.d(TAG, contentUri.toString());
				
				// there are real file system paths and logical content URIs
				if (contentUri.toString().startsWith("content:")) {
					filePath = this.getRealPathFromURI(contentUri);
					Log.d(TAG, "path: " + filePath);
				} else if (contentUri.toString().startsWith("file:")){
					filePath = contentUri.getPath();
					Log.d(TAG, "path: " + filePath);
				}
				
				// get the MIME type
				String type = intent.resolveType(this);
				
				// create a basename out of the type identifier -- awesome! [matsch]
				String basename = type.toUpperCase().charAt(0) + type.substring(1, type.indexOf('/'));
				basename = basename + "-" + dateFormater.format(new Date());
				
				// sending files may take time, so do it in another thread
				Toast.makeText(this, this.getString(R.string.app_name) + ": " + this.getString(R.string.data_sending), Toast.LENGTH_SHORT).show();
				new SendThread(handler, basename, "", filePath, type).start();
				
				this.finish(); // seems to work fine here (ie. the thread does continue and reports success/failure)
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
        Notification notification;
        NotificationManager notificationManager; 
        Intent intent = new Intent(SendToServer.this, SendToServer.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        
        SendThread(Handler h, String name, String path, String localPath, String type) {
            mHandler = h;
            this.name= name;
            this.path = path;
            this.localPath = localPath;
            this.type = type;
            
            int progress = 10;      
            this.notificationManager = (NotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
            
            this.notification = new Notification(R.drawable.icon, String.format("Sending %s",name), System.currentTimeMillis());
            this.notification.setLatestEventInfo(getApplicationContext(), "SendToWebDAV", String.format("Uploading %s to WebDAV", name), pendingIntent);
            this.notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
            this.notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
            this.notification.defaults = Notification.DEFAULT_ALL; //default virbrate, lights and sound
//            this.notification.contentIntent = pendingIntent;
             this.notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
             this.notification.contentView.setTextViewText(R.id.status_text, String.format("Uploading %s to WebDAV", name));
             this.notification.contentView.setProgressBar(R.id.status_progress, 100, progress, false);            

            this.notificationManager.notify(42, notification);
        }
       
        public void run() {
        	
        	try {
        		this.notification.contentView.setProgressBar(R.id.status_progress, 100, 30, false);                
                this.notificationManager.notify(42, notification);
                
        		httpHandler.putBinFile(name, path, localPath, type);  
        		this.notification.contentView.setProgressBar(R.id.status_progress, 100, 90, false);                
                this.notificationManager.notify(42, notification);
                
                notificationManager.cancel(42);
                
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