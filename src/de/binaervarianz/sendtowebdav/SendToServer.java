package de.binaervarianz.sendtowebdav;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class SendToServer extends Activity {
	private final String TAG = this.getClass().getName();
	private static final boolean MODE_URL = true;
	private static final boolean MODE_BIN = false;	
	
	private WebDAVhandler httpHandler;
	private CountThread countThread;
	private SendThread sendThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Activity started");
		//just for debug reasons kill all ld notifications:
		//NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
		//notificationManager.cancel(42);
		
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
		Log.d(TAG, "Delivered Intent: " + intent.toString());
		
		Bundle extras = intent.getExtras();
		
		if ((intent.getAction().equals(Intent.ACTION_SEND) || intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) 
				&& extras != null) {
			//figure out what's to be send
	
			SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMddHHmmss");
			
			// simple text like URLs
			if (extras.containsKey(Intent.EXTRA_TEXT)) {
				ArrayList<String> urls = new ArrayList<String>();
				if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
					urls = (ArrayList<String>)extras.get(Intent.EXTRA_TEXT);
				} else {
					urls.add((String)extras.get(Intent.EXTRA_TEXT));
				}
				
				for (String url : urls) {
					this.sendThread = new SendThread(handler, "URL-"+dateFormater.format(new Date())+".txt", "", url);
					this.sendThread.start();
					
					if (urls.size() > 1) {
						try {
							wait(1000); // wait one second: just a lazy way to avoid duplicate timestamp problems
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}				
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
					Log.d(TAG, "ContentURI :" + contentUri.toString());
					
					// there are real file system paths and logical content URIs
					if (contentUri.toString().startsWith("content:")) {
						filePath = this.getRealPathFromURI(contentUri);
						Log.d(TAG, "path: " + filePath);
						fileType = this.getContentResolver().getType(contentUri);
					} else if (contentUri.toString().startsWith("file:")){
						filePath = contentUri.getPath();
						Log.d(TAG, "path: " + filePath);
						fileType = intentType; // TODO: I guess this is not always correct (I see no other way to do it right now)
					}
					
					Log.d(TAG, "fileType: "+fileType);
					
					// sending files may take time, so do it in another thread
					Toast.makeText(this, this.getString(R.string.app_name) + ": " + this.getString(R.string.data_sending), Toast.LENGTH_SHORT).show();
					Log.d(TAG, "Start SendThread");
					this.sendThread = new SendThread(handler, basename, "", filePath, fileType, (files.size()>1));
					this.countThread = new CountThread(handler, basename);
					this.countThread.start();
					this.sendThread.start();
					
				}				
				this.finish(); // seems to work fine here (ie. the thread does continue and also reports success/failure)
			}	
		}
		else if(intent.getAction().equals(Intent.ACTION_DELETE)){
			//TODO give a 'are you sure' dialog here
			//code below doesn't work; intent starts new activity, no access to original threads via member variables
			//this.sendThread.interrupt();
			//this.countThread.interrupt();
			Log.d(TAG,"Abort Upload");
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
        		// TODO: Hijacking this for normal finish procedure, maybe giving it an own msg?
        		SendToServer.this.countThread.stopCount();
        	} else {
        		Toast.makeText(SendToServer.this, getString(R.string.app_name) + ": " + getString(R.string.data_sending_failure) + ": /r/n" + (String)msg.obj, Toast.LENGTH_LONG).show();
        	}
        	SendToServer.this.finish();
        }
    };
    
	private class SendThread extends Thread {
        Handler mHandler;
        String name, path, localPath, type, data;       
        private boolean subsequentCalls;
        private boolean uploadType;
        
        // for URLs
        public SendThread(Handler h, String name, String path, String data) {
        	Log.d(TAG, "SendThread Constructor(URL) called");
			mHandler = h;
			this.name = name;
			this.path = path;
			this.data = data;
			this.uploadType = MODE_URL;
			
		}
        
        // for binary files
        public SendThread(Handler h, String name, String path, String localPath, String type, boolean subsequentCalls) {
        	Log.d(TAG, "SendThread Constructor(BIN) called");
            mHandler = h;
            this.name= name;
            this.path = path;
            this.localPath = localPath;
            this.type = type;
            this.uploadType = MODE_BIN;   

            //TODO create unique ID per upload to allow multiple parallel notifications
            this.subsequentCalls = subsequentCalls;            
        }
       
        
        public void run() {        	
        	try {
        		if (this.uploadType == MODE_BIN) {        			   
        			if (localPath != null)
        				httpHandler.putBinFile(name, path, localPath, type, subsequentCalls);
        			else
        				Log.e(TAG,"No local Path for send thread!");
        		} else {
        			if (data != null)
        				httpHandler.putFile(name, path, data);
        			else
        				Log.e(TAG,"No data for send thread!");
        		} 
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
        	//after this has been run, the data is sent
        	mHandler.sendEmptyMessage(0);
        }
    }
	
/**
 * parallel running thread to count the bytes sent by this process' user and update to notification area
 * @author chaos
 *
 */
	private class CountThread extends Thread {
        Handler mHandler;        
        Notification notification;
        NotificationManager notificationManager;        
        TrafficStats stats = new TrafficStats();
        Process proc;
        int uid;
        long txbytes;
                     
        Intent selfIntent = new Intent();
        PendingIntent pendingSelfIntent;
        int progress = 10;   
        boolean stopThis;
        
        /**
         * Constructor creating the initial notification
         * @param h the handler for messaging to the parent
         * @param name name of the file being uploadet to be displayed in the notification
         */
        public CountThread(Handler h, String name) {
        	Log.d(TAG, "CountThread Constructor called");        	
        	this.proc = new Process();
        	this.notificationManager = (NotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
			mHandler = h;
			uid = proc.myUid();
			this.stopThis = false;
			
            this.selfIntent.setAction(Intent.ACTION_DELETE);
            Log.d(TAG, "Intent Action set");
			this.selfIntent.setClass(SendToServer.this.getApplicationContext(), SendToServer.class);
			Log.d(TAG, "Intent created, category: ");
			//this.intent = new Intent(SendToServer.this.getApplicationContext(), SendToServer.class);
			this.pendingSelfIntent = PendingIntent.getActivity(getApplicationContext(), 0, this.selfIntent, 0);
			Log.d(TAG, "pendingIntent created");
			
            this.notificationManager = (NotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
            
            this.notification = new Notification(R.drawable.icon, String.format("Sending %s",name), System.currentTimeMillis());
            this.notification.setLatestEventInfo(getApplicationContext(), "SendToWebDAV", String.format("Uploading %s to WebDAV", name), this.pendingSelfIntent);
            this.notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;
            this.notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
            //this.notification.defaults = Notification.DEFAULT_ALL; //default vibrate, lights and sound

            this.notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
            this.notification.contentView.setTextViewText(R.id.status_text, String.format("Uploading %s to WebDAV", name));
            //this.notification.contentView.setProgressBar(R.id.status_progress, 100, progress, false);  
			
			}
		
        /**
         * main function to be run, includes loop which updates the notification area with bytes sent by the current user
         * Warnung: infinite loop within !!
         */
        public void run() {
        	long txbytesstart = this.stats.getUidTxBytes(this.uid);
        	while (! this.stopThis) {				
				txbytes = this.stats.getUidTxBytes(this.uid) - txbytesstart;
				
				if (txbytes > 1000)
					this.notification.contentView.setTextViewText(R.id.status_text, String.format("Bytes sent: %d kB", txbytes/1000));
				else if (txbytes > (1000 * 1000 * 10))
					this.notification.contentView.setTextViewText(R.id.status_text, String.format("Bytes sent: %d MB", txbytes/(1000*1000)));
				else
					this.notification.contentView.setTextViewText(R.id.status_text, String.format("Bytes sent: %d B", txbytes));
				
				//this.notification.contentView.setProgressBar(R.id.status_progress, (i/12)*100, progress, false); 
				this.notificationManager.notify(42, this.notification);
				Log.d(TAG, String.format("Sent Bytes: %d",txbytes));
				try {
					sleep(200);
				}
				catch (InterruptedException e){
					Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
	        		
	        		Message msg = mHandler.obtainMessage();
	        		msg.obj = e.getClass().getSimpleName() + ": " + e.getLocalizedMessage();
	                msg.what = 1;
	                mHandler.sendMessage(msg);
	        		break;
				}
        	}
        }
        /**
         * to be called from parent object to stop the notification
         */
        public void stopCount(){
        	this.stopThis = true;
        	this.notificationManager.cancel(42);
        	Log.d(TAG, "Count stopped");
        	
        }
	}
}