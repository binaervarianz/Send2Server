package de.binaervarianz.sendtowebdav;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;

public class SendToServer extends Activity {
	private final String TAG = this.getClass().getName();
	
	private WebDAVhandler httpHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
		String serverURI = settings.getString("serverURI", "https://");
		String user = settings.getString("username", "");
		String pass = settings.getString("password", "");

		if (serverURI.equals("https://") || user.equals("") || pass.equals("")) {
			// TODO: break
		}
		
		Log.d(TAG, "context: "+this.getApplicationContext().toString());
		Log.d(TAG, "setting: "+serverURI+user+pass);
		//httpHandler = new WebDAVhandler(serverURI, user, pass);
		httpHandler = new WebDAVhandler("https://mario.binaervarianz.de/webdav/", "aaa", "bbb");

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();

		if (intent.getAction().equals(Intent.ACTION_SEND) && extras != null) {
			String url = "";
			if (extras.containsKey(Intent.EXTRA_TEXT)) {
				url += extras.getString(Intent.EXTRA_TEXT);
			}

			httpHandler.putFile("URL-"+DateFormat.format("yyyyMMddhhmmss", new Date())+".txt", "", url);
			
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setTitle("Send URL...");
			alertDialogBuilder.setMessage(url).setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
	}
}