package com.xwell.usbcan.ui;

import com.xwell.usbcan.R;
import com.xwell.usbcan.service.USBCANMonitor;
import com.xwell.usbcan.usb.HidBridge;
import com.xwell.usbcan.usb.IUsbConnectionHandler;

import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.content.ComponentName;

public class VolumeActivity extends Activity {
	private static final String TAG = "VolumeActivity";
	/** Messenger for communicating with service. */
	private Messenger mService = null;
	private String mAction = null;
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            Log.d(TAG, "Service Attached.");
            if(mAction == "com.xwell.usbcan.CANVOLUMEDOWN")
            {
            	try {
                    Message msg = Message.obtain(null,USBCANMonitor.MSG_VOLUME_DOWN);
                    if(mService != null)
                    	mService.send(msg);
                    finish();
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }
            
            if(mAction == "com.xwell.usbcan.CANVOLUMEUP")
            {
            	try {
                   Message msg = Message.obtain(null,USBCANMonitor.MSG_VOLUME_UP);
                   if(mService != null)
                   		mService.send(msg);
                   finish();
               } catch (RemoteException e) {
                   // There is nothing special we need to do if the service
                   // has crashed.
               }
            }
         
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            Log.d(TAG, "Disconnected.");
        }
    };
    
    private void doBindService() {
    	Log.i(TAG, "doBindService()" );
    	//start service, couild be running already this will have no affect if s
		Intent service = new Intent(this, USBCANMonitor.class);
		this.startService(service);
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        if(!bindService(new Intent(VolumeActivity.this,USBCANMonitor.class), mConnection, Context.BIND_ABOVE_CLIENT))
        {
        	//could not bind  to service and will not get the service object.
        	//try again? or abort app?
        	Log.d(TAG, "CAN'T BIND TO USB SERVICE");
        }
    }
    
    void doUnbindService() {
    	// If we have received the service, and hence registered with
        // it, then now is the time to unregister.
        if (mService != null) {
            // Detach our existing connection.
            unbindService(mConnection);
            Log.d(TAG, "Unbinding.");
        }
    }
	  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		
		mAction = getIntent().getAction();
		doBindService();
		
		
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume()");
		super.onResume();
	}
	
    @Override
    public void onPause() {
    	Log.i(TAG, "onPause()");
        super.onPause();
    }
	
	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy()");
		doUnbindService();
		super.onDestroy();
	}
	
}
