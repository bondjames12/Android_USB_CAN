package com.xwell.usbcan.ui;

import com.xwell.usbcan.R;
import com.xwell.usbcan.can.CAN_Frame;
import com.xwell.usbcan.service.USBCANMonitor;
import com.xwell.usbcan.ui.MainActivity.IncomingHandler;
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
import android.widget.ScrollView;
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

public class DebugActivity extends Activity {
	private static final String TAG = "DebugActivity";
	TextView mTextStatus;
	ScrollView mScrollView;
	StringBuilder stringtext = new StringBuilder(10000);
	/** Messenger for communicating with service. */
	private Messenger mService = null;
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	
	/**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	Log.d(TAG, "Received from service: " + msg.what);
            switch (msg.what) {
                case USBCANMonitor.MSG_USB_RAW_BYTES:
                    byte[] data = msg.getData().getByteArray("DATA");
                    if(data == null)
                    	{ Log.d(TAG, "Service message data array was NULL: " + msg.what); break;}    
                    stringtext.append(CAN_Frame.bytesToHexString(data));
                    mTextStatus.setText(stringtext);       
                    scrollToBottom();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
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
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,USBCANMonitor.MSG_REGISTER_CLIENT_DEBUG);
                msg.replyTo = mMessenger;
                mService.send(msg);
                Log.i(TAG, "onServiceConnected: Sent registration message to service");
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
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
        if(!bindService(new Intent(DebugActivity.this,USBCANMonitor.class), mConnection, Context.BIND_ABOVE_CLIENT))
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
            try {
                Message msg = Message.obtain(null,USBCANMonitor.MSG_UNREGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service
                // has crashed.
            }
            // Detach our existing connection.
            unbindService(mConnection);
            Log.d(TAG, "Unbinding.");
        }
    }
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usbcan_debug);
        mTextStatus = (TextView) findViewById(R.id.text_status_ID);
        mScrollView = (ScrollView) findViewById(R.id.scroller_ID);
        
        doBindService();
        
        
	}
	
	@Override
	protected void onResume() {
		//Log.i(TAG, "onResume()");
		super.onResume();
	}
	
	@Override
    public void onPause() {
    	//Log.i(TAG, "onPause()");
        super.onPause();
    }
	
	@Override
	protected void onDestroy() {
		//Log.i(TAG, "onDestroy()");
		doUnbindService();
		super.onDestroy();
	}
	
	private void scrollToBottom()
	{
	    mScrollView.post(new Runnable()
	    { 
	        public void run()
	        { 
	            //mScrollView.smoothScrollTo(0, mTextStatus.getBottom());
	            mScrollView.fullScroll(View.FOCUS_DOWN);
	        } 
	    });
	}
	
	
	
}
