package com.xwell.usbcan.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.xwell.usbcan.R;
import com.xwell.usbcan.service.USBCANMonitor;
import com.xwell.usbcan.usb.HidBridge;
import com.xwell.usbcan.usb.IUsbConnectionHandler;

import android.text.TextUtils;
import android.util.Log;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.media.MediaRouter.RouteInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import android.graphics.Color;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	SharedPreferences prefs;
	/** Messenger for communicating with service. */
	private Messenger mService = null;
	/**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
	private AudioManager mgr;
	private TextView txtServiceStatus;
	private TextView txtUSBStatus;
	private Thread timerThread;
	//VID2235(08BB) PID 9988 TI AUdio AMP
	  //VID5118(13FE) Patriot USB stick PID20480 USB storage
	  //VID5824(16C0) Teensy PID1158=TeensyHID
	private int USB_AUDIO_PID = 9988;
	private int USB_TEENSY_PID = 1158;
	private int USB_STORAGE_PID = 20480;
	private int USB_STORAGE2_PID = 21888;
	private boolean USB_AUDIO_PRESENT = false;
	private boolean USB_TEENSY_PRESENT = false;
	private boolean USB_STORAGE_PRESENT = false;
	
	//Declare a preference change listener
	SharedPreferences.OnSharedPreferenceChangeListener prefListener = 
	        new SharedPreferences.OnSharedPreferenceChangeListener() {
	    public void onSharedPreferenceChanged(SharedPreferences prefs,String key) {
	    	try {
		        if (key.equals("balance")) {
		        	Log.d(TAG, key + " changed: " + prefs.getInt(key, 0));
		        	Message msg = Message.obtain(null,USBCANMonitor.MSG_BALANCE_SET);
		            msg.replyTo = mMessenger;
		            msg.arg1 =  prefs.getInt(key, 0);
		            if(mService != null)
							mService.send(msg);
		        }
		        if (key.equals("fade")) {
		        	Log.d(TAG, key + " changed: " + prefs.getInt(key, 0));
		        	Message msg = Message.obtain(null,USBCANMonitor.MSG_FADE_SET);
		            msg.replyTo = mMessenger;
		            msg.arg1 =  prefs.getInt(key, 0);
		            if(mService != null)
							mService.send(msg);
		        }
		        if (key.equals("bass")) {
		        	Log.d(TAG, key + " changed: " + prefs.getInt(key, 0));
		        	Message msg = Message.obtain(null,USBCANMonitor.MSG_BASS_SET);
		            msg.replyTo = mMessenger;
		            msg.arg1 =  prefs.getInt(key, 0);
		            if(mService != null)
							mService.send(msg);
		        }
		        if (key.equals("mid")) {
		        	Log.d(TAG, key + " changed: " + prefs.getInt(key, 0));
		        	Message msg = Message.obtain(null,USBCANMonitor.MSG_MID_SET);
		            msg.replyTo = mMessenger;
		            msg.arg1 =  prefs.getInt(key, 0);
		            if(mService != null)
							mService.send(msg);
		        }
		        if (key.equals("treble")) {
		        	Log.d(TAG, key + " changed: " + prefs.getInt(key, 0));
		        	Message msg = Message.obtain(null,USBCANMonitor.MSG_TREBLE_SET);
		            msg.replyTo = mMessenger;
		            msg.arg1 =  prefs.getInt(key, 0);
		            if(mService != null)
							mService.send(msg);
		        }
	    	} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	};
	
	/**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	//Log.d(TAG, "Received from service: " + msg.what);
            switch (msg.what) {
                case USBCANMonitor.MSG_CANFRAME:
                    
                    break;
                case USBCANMonitor.MSG_STATUS:
                    String status = msg.getData().getString("STATUS");
                    if(status == null)
                    	{ Log.d(TAG, "Service message status was NULL: " + msg.what); break;}
                    txtServiceStatus.setText("Service Status: " + status);
                    break;
                case USBCANMonitor.MSG_FM_SET_CHANNEL:
                    m_radio_channel = msg.arg1;
                	setFreqDisplay(msg.arg1);
                    break;
                case USBCANMonitor.MSG_FM_SET_POWER:
                	m_radio_power = ((msg.arg1 == 0) ? false : true);
                	radioSetGUIPower();
                    break;
                case USBCANMonitor.MSG_FM_GET_STATUS:
					com.xwell.usbcan.usb.USBData data = (com.xwell.usbcan.usb.USBData) msg.obj;
					byte[] bData = data.get_byte();
                	radioSetGUIRSSI(bData[2]);
					setFreqDisplay(bData[5]);
                	// Record Start/Stop:
                    if (bData[3] == 1) {
                      m_iv_stereo.setImageResource (R.drawable.btn_record_press);
                    }
                    else {
                    	m_iv_stereo.setImageResource (R.drawable.btn_record);
                    }
                    break;
                case USBCANMonitor.MSG_TEENSY_GET_STATUS:
                	final int arg1 = msg.arg1;
                    //if (msg.arg2 == 1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()){
                                new AlertDialog.Builder(MainActivity.this)
                                  .setTitle("Teensy ALert!")
                                  .setMessage("Teensy PIN_A8 Changed=" + arg1)
                                  .setCancelable(false)
                                  .setPositiveButton("YES",
                                		    new DialogInterface.OnClickListener() {
                                      public void onClick(DialogInterface dialog, int which) {
                                          // Write your code here to execute after dialog
                                      }
                                  }).create().show();;
                            }
                        }
                    });
                    break;
				case USBCANMonitor.MSG_TEENSY_VOLUME_CHANGE:
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
                Message msg = Message.obtain(null,USBCANMonitor.MSG_REGISTER_CLIENT);
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
        if(!bindService(new Intent(MainActivity.this,USBCANMonitor.class), mConnection, Context.BIND_ABOVE_CLIENT))
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
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//Did we tell ourself to exit our activity?
		if( getIntent().getBooleanExtra("Exit me", false)){
	        finish();
	        return; // add this to prevent from doing unnecessary stuffs
	    }
		
		txtServiceStatus =  (TextView) findViewById(R.id.txtServiceStatus);
		txtUSBStatus =  (TextView) findViewById(R.id.txtUSBStatus);
		// get the prefs object. I do this in the onCreate method of my Activity
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		doBindService();
		
		//Audio service
		mgr=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
		
		//mgr.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE | AudioManager.FLAG_SHOW_UI);
		//mgr.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE | AudioManager.FLAG_SHOW_UI);
		//mgr.setStreamVolume(AudioManager.STREAM_ALARM, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE | AudioManager.FLAG_SHOW_UI);
		//mgr.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE | AudioManager.FLAG_SHOW_UI);
		//mgr.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE | AudioManager.FLAG_SHOW_UI);
		
		//sample mute
		//mgr.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MUTE));
		
		radio_gui_init();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume()");
		super.onResume();
		prefs.registerOnSharedPreferenceChangeListener(prefListener);
		checkInfo(); //enumerate connected USB devices
		txtUSBStatus.setText("USB Status: Audio:" + USB_AUDIO_PRESENT +" CAN:" + USB_TEENSY_PRESENT + " Storage:" + USB_STORAGE_PRESENT );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void Test(){
		/*try {
            Message msg = Message.obtain(null,USBCANMonitor.MSG_VOLUME_UP);
            msg.replyTo = mMessenger;
            if(mService != null)
            	mService.send(msg);
        } catch (RemoteException e) {
            // There is nothing special we need to do if the service
            // has crashed.
        }*/
		MediaRouter mr = (MediaRouter)this.getSystemService(MEDIA_ROUTER_SERVICE);
		if(mr != null)
		{
			int numrouters = mr.getRouteCount();
			for(int i=0;i<numrouters;i++)
			{
				RouteInfo ri = mr.getRouteAt(i);
				if(ri != null)
				{
					
					Log.i(TAG, "Route Name:"+ ri.getName());
					if(ri.getCategory() != null)Log.i(TAG, "Route Cat:"+ ri.getCategory().getName());
					Log.i(TAG, "Route Des:"+ ri.getDescription());
					if(ri.getGroup() != null)Log.i(TAG, "Route Group:"+ ri.getGroup().getName());
					Log.i(TAG, "Route Paybackstream:"+ ri.getPlaybackStream());
					Log.i(TAG, "Route Volume:"+ ri.getVolume());
					Log.i(TAG, "Route Status:"+ ri.getStatus());
					Log.i(TAG, "Route Playbacktype:"+ ri.getPlaybackType());
				}
			}
		}
		
		
		AudioManager am = (AudioManager)this.getSystemService(AUDIO_SERVICE);
		if(am != null)
		{
			Log.i(TAG, "Audio mode:"+ am.getMode());
			Log.i(TAG, "Audio name:"+ am.getParameters("Name"));
			Log.i(TAG, "Audio Sample Rate:"+ am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
			Log.i(TAG, "Audio Frames per buffer:"+ am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
			Log.i(TAG, "Audio isBluetoothA2dpOn:"+ am.isBluetoothA2dpOn());
			Log.i(TAG, "Audio isBluetoothScoAvailableOffCall:"+ am.isBluetoothScoAvailableOffCall());
			Log.i(TAG, "Audio isBluetoothScoOn:"+ am.isBluetoothScoOn());
			Log.i(TAG, "Audio isMicrophoneMute:"+ am.isMicrophoneMute());
			Log.i(TAG, "Audio isMusicActive:"+ am.isMusicActive());
			Log.i(TAG, "Audio isSpeakerphoneOn:"+ am.isSpeakerphoneOn());
			Log.i(TAG, "Audio isWiredHeadsetOn:"+ am.isWiredHeadsetOn());
			
			
		}
		checkInfo();
	}

	private void checkInfo() {
		  UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
		  HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		  Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		  String i = "";
		  while (deviceIterator.hasNext()) {
		   UsbDevice device = deviceIterator.next();
		   i += "\n" +
		    "DeviceID: " + device.getDeviceId() + "\n" +
		    "DeviceName: " + device.getDeviceName() + "\n" +
		    "DeviceClass: " + device.getDeviceClass() + " - " 
		     + translateDeviceClass(device.getDeviceClass()) + "\n" +
		    "DeviceSubClass: " + device.getDeviceSubclass() + "\n" +
		    "VendorID: " + device.getVendorId() + "\n" +
		    "ProductID: " + device.getProductId() + "\n";
		   if(device.getProductId() == USB_AUDIO_PID)
		   {
			   USB_AUDIO_PRESENT = true;
		   } else if(device.getProductId() == USB_TEENSY_PID)
		   {
			   USB_TEENSY_PRESENT= true;
		   } else if(device.getProductId() == USB_STORAGE_PID || device.getProductId() == USB_STORAGE2_PID)
		   {
			   USB_STORAGE_PRESENT = true;
		   }
		  }
		  
		  //textInfo.setText(i);
		  Log.i(TAG, "USB DIAG:"+ i);
		 }
		 
		 private String translateDeviceClass(int deviceClass){
		  switch(deviceClass){
		  case UsbConstants.USB_CLASS_APP_SPEC: 
		   return "Application specific USB class";
		  case UsbConstants.USB_CLASS_AUDIO: 
		   return "USB class for audio devices";
		  case UsbConstants.USB_CLASS_CDC_DATA: 
		   return "USB class for CDC devices (communications device class)";
		  case UsbConstants.USB_CLASS_COMM: 
		   return "USB class for communication devices";
		  case UsbConstants.USB_CLASS_CONTENT_SEC: 
		   return "USB class for content security devices";
		  case UsbConstants.USB_CLASS_CSCID: 
		   return "USB class for content smart card devices";
		  case UsbConstants.USB_CLASS_HID: 
		   return "USB class for human interface devices (for example, mice and keyboards)";
		  case UsbConstants.USB_CLASS_HUB: 
		   return "USB class for USB hubs";
		  case UsbConstants.USB_CLASS_MASS_STORAGE: 
		   return "USB class for mass storage devices";
		  case UsbConstants.USB_CLASS_MISC: 
		   return "USB class for wireless miscellaneous devices";
		  case UsbConstants.USB_CLASS_PER_INTERFACE: 
		   return "USB class indicating that the class is determined on a per-interface basis";
		  case UsbConstants.USB_CLASS_PHYSICA: 
		   return "USB class for physical devices";
		  case UsbConstants.USB_CLASS_PRINTER: 
		   return "USB class for printers";
		  case UsbConstants.USB_CLASS_STILL_IMAGE: 
		   return "USB class for still image devices (digital cameras)";
		  case UsbConstants.USB_CLASS_VENDOR_SPEC: 
		   return "Vendor specific USB class";
		  case UsbConstants.USB_CLASS_VIDEO: 
		   return "USB class for video devices";
		  case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER: 
		   return "USB class for wireless controller devices";
		  default: return "Unknown USB class!";
		  
		  }
		 }
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id)
		{
		case R.id.mnuSettings:
			// Display the fragment as the main content.
	        getFragmentManager().beginTransaction()
	                .add(android.R.id.content, new SettingsFragment())
	                .addToBackStack(null)
	                .commit();
			return true;
		case R.id.mnuExit:
			Intent intent = new Intent(this, MainActivity.class);
		    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    intent.putExtra("Exit me", true);
		    startActivity(intent);
		    finish();
			return true;
		case R.id.mnuStartService:
			Intent service = new Intent(this, USBCANMonitor.class);
			service.setAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
			this.startService(service);
			return true;
		case R.id.mnuStopService:
			Message msg = Message.obtain(null,USBCANMonitor.MSG_STOP);
            msg.replyTo = mMessenger;
            if(mService != null)
				try {
					mService.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			return true;
		case R.id.mnuUSBcandebug: //open debug activity
			Intent debugintent = new Intent(this, DebugActivity.class);
			startActivity(debugintent);
			break;
		case R.id.mnuTest: //open debug activity
			Test();
			break;
		}

		return super.onOptionsItemSelected(item);
	}
    
    @Override
    public void onPause() {
    	Log.i(TAG, "onPause()");
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
    }
	
	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy()");
		doUnbindService();
		super.onDestroy();
	}

	  private boolean		m_radio_power = false;
	  private int			m_radio_channel = 0;
	  private int cur_preset_idx = 0;
	  // Text:
	  private TextView      m_tv_rssi   = null;
	  private TextView      m_tv_svc_count  = null;
	  private TextView      m_tv_svc_phase  = null;
	  private TextView      m_tv_svc_cdown  = null;
	  private TextView      m_tv_pilot  = null;
	  private TextView      m_tv_band   = null;
	  private TextView      m_tv_freq   = null;
	
	  // RDS data:
	  private TextView      m_tv_picl   = null;
	  private TextView      m_tv_ps     = null;
	  private TextView      m_tv_ptyn   = null;
	  private TextView      m_tv_rt     = null;
	
	  // ImageView Buttons:
	  private ImageView     m_iv_seekup = null;
	  private ImageView     m_iv_seekdn = null;
	  private ImageView     m_iv_prev   = null;
	  private ImageView     m_iv_next   = null;
	  private ImageView     m_iv_paupla = null;
	  private ImageView     m_iv_power   = null;
	  private ImageView     m_iv_stereo   = null;
	  
	  // Presets: 16
	  private int           m_presets_curr  = 0;
	  private ImageButton[] m_preset_ib     = {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};   // 16 Preset Image Buttons
	  private TextView   [] m_preset_tv     = {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};   // 16 Preset Text Views
	  private int     [] m_preset_channel   = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};   //FM Channels
	  private String     [] m_preset_name   = {"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""};   //  Names
	  private Handler rssiHandler = new Handler();
	  
	  //Tune radio by Frequency string in Mhz
	  private void radio_freq_tune (String nFreq) {
		    if (TextUtils.isEmpty (nFreq)) {// If an empty string...
		      Log.e(TAG,"nFreq: " + nFreq);
		      return;
		    }
		    Float ffreq = 0f;
		    try {
		      ffreq = Float.valueOf (nFreq);
		      //chan = (freq - 87.5) / 0.2
		      int channel = (int) ((ffreq - 87.5) / 0.2);
		      radio_channel_tune(channel);
		    }
		    catch (Throwable e) {
		    	Log.e(TAG,"ffreq = Float.valueOf (nFreq); failed");
		        e.printStackTrace ();
		    }
	  }

	  //Tune radio by channel number
	  private void radio_channel_tune (int channel) {
		  if(channel < 0) channel = 0;
          if(channel > 102) channel = 102;
		//Send message to USB service which then send data over USB to the teensy
		try {
            Message msg = Message.obtain(null,USBCANMonitor.MSG_FM_SET_CHANNEL);
            msg.replyTo = mMessenger;
            msg.arg1 = channel;
            mService.send(msg);
        } catch (RemoteException e) {
        }
	  }
	  
	  private void setFreqDisplay(int channel)
	  {
		  m_radio_channel = channel;
		  m_tv_freq.setText(radioFMChannelToFreqString(channel));  
		  prefs.edit().putInt("chass_last_chan", channel).apply();
	  }

	  private Runnable RSSIrunnable = new Runnable() {
		   @Override
		   public void run() {
			   if(m_radio_power){
				   try {
			            Message msg = Message.obtain(null,USBCANMonitor.MSG_FM_GET_STATUS);
			            msg.replyTo = mMessenger;
			            mService.send(msg);
			        } catch (RemoteException e) {
			        }
				   rssiHandler.postDelayed(this, 6000);
			   }
		   }
		}; 
		
	void radio_gui_init() {
		m_iv_seekdn = (ImageView) findViewById (R.id.iv_seekdn);
		m_iv_seekdn.setOnClickListener (radio_short_click_lstnr);
		
		m_iv_seekup = (ImageView) findViewById (R.id.iv_seekup);
		m_iv_seekup.setOnClickListener (radio_short_click_lstnr);
		
		m_iv_prev = (ImageView) findViewById (R.id.iv_prev);
		m_iv_prev.setOnClickListener (radio_short_click_lstnr);
		//m_iv_prev.setOnTouchListener(new RepeatListener(true,false, 400,100));
		m_iv_prev.setId (R.id.iv_prev);
		
		m_iv_next = (ImageView) findViewById (R.id.iv_next);
		m_iv_next.setOnClickListener (radio_short_click_lstnr);
		//m_iv_next.setOnTouchListener(new RepeatListener(true,false, 400, 100));
		m_iv_next.setId (R.id.iv_next);
		
		m_tv_rssi = (TextView) findViewById (R.id.tv_rssi);
		
		m_tv_freq = (TextView) findViewById (R.id.tv_freq);
		m_tv_freq.setOnClickListener (radio_short_click_lstnr);
		
		m_iv_paupla = (ImageView) findViewById (R.id.iv_paupla);
		m_iv_paupla.setOnClickListener (radio_short_click_lstnr);
		m_iv_paupla.setId (R.id.iv_paupla);
		
		m_iv_power = (ImageView) findViewById (R.id.iv_power);
		m_iv_power.setOnClickListener (radio_short_click_lstnr);
		m_iv_power.setId (R.id.iv_power);
		
		m_iv_stereo = (ImageView) findViewById (R.id.iv_stereo);

		radio_presets_setup();
	  }
	  
	  private void radio_presets_setup () {// 16 Max Preset Buttons hardcoded
	    // Textviews:
	    m_preset_tv [0] = (TextView) findViewById (R.id.tv_preset_0);
	    m_preset_tv [1] = (TextView) findViewById (R.id.tv_preset_1);
	    m_preset_tv [2] = (TextView) findViewById (R.id.tv_preset_2);
	    m_preset_tv [3] = (TextView) findViewById (R.id.tv_preset_3);
	    m_preset_tv [4] = (TextView) findViewById (R.id.tv_preset_4);
	    m_preset_tv [5] = (TextView) findViewById (R.id.tv_preset_5);
	    m_preset_tv [6] = (TextView) findViewById (R.id.tv_preset_6);
	    m_preset_tv [7] = (TextView) findViewById (R.id.tv_preset_7);
	    m_preset_tv [8] = (TextView) findViewById (R.id.tv_preset_8);
	    m_preset_tv [9] = (TextView) findViewById (R.id.tv_preset_9);
	    m_preset_tv [10]= (TextView) findViewById (R.id.tv_preset_10);
	    m_preset_tv [11]= (TextView) findViewById (R.id.tv_preset_11);
	    m_preset_tv [12]= (TextView) findViewById (R.id.tv_preset_12);
	    m_preset_tv [13]= (TextView) findViewById (R.id.tv_preset_13);
	    m_preset_tv [14]= (TextView) findViewById (R.id.tv_preset_14);
	    m_preset_tv [15]= (TextView) findViewById (R.id.tv_preset_15);

	    // Imagebuttons:
	    m_preset_ib [0] = (ImageButton) findViewById (R.id.ib_preset_0);
	    m_preset_ib [1] = (ImageButton) findViewById (R.id.ib_preset_1);
	    m_preset_ib [2] = (ImageButton) findViewById (R.id.ib_preset_2);
	    m_preset_ib [3] = (ImageButton) findViewById (R.id.ib_preset_3);
	    m_preset_ib [4] = (ImageButton) findViewById (R.id.ib_preset_4);
	    m_preset_ib [5] = (ImageButton) findViewById (R.id.ib_preset_5);
	    m_preset_ib [6] = (ImageButton) findViewById (R.id.ib_preset_6);
	    m_preset_ib [7] = (ImageButton) findViewById (R.id.ib_preset_7);
	    m_preset_ib [8] = (ImageButton) findViewById (R.id.ib_preset_8);
	    m_preset_ib [9] = (ImageButton) findViewById (R.id.ib_preset_9);
	    m_preset_ib [10]= (ImageButton) findViewById (R.id.ib_preset_10);
	    m_preset_ib [11]= (ImageButton) findViewById (R.id.ib_preset_11);
	    m_preset_ib [12]= (ImageButton) findViewById (R.id.ib_preset_12);
	    m_preset_ib [13]= (ImageButton) findViewById (R.id.ib_preset_13);
	    m_preset_ib [14]= (ImageButton) findViewById (R.id.ib_preset_14);
	    m_preset_ib [15]= (ImageButton) findViewById (R.id.ib_preset_15);

	    for (int idx = 0; idx < 16; idx ++) {              // For all presets...
	      m_preset_ib [idx].setOnClickListener     (radio_preset_select_lstnr);   // Set click listener
	      m_preset_ib [idx].setOnLongClickListener (radio_preset_change_lstnr);   // Set long click listener
	    }

	    for (int idx = 0; idx < 16; idx ++) {               // For all presets...
	      String name = prefs.getString("chass_preset_name_" + idx, "");
	      int channel = prefs.getInt("chass_preset_chan_" + idx, 0);
	      if (channel != 0) { 
	        m_presets_curr = idx + 1; // Update number of presets
	        m_preset_channel [idx] = channel;
	        m_preset_name [idx] = name;

	        if (name != null)
	          m_preset_tv [idx].setText (name);
	        else
	          m_preset_tv [idx].setText ("" + radioFMChannelToFreqString(channel));
	        m_preset_ib [idx].setImageResource (R.drawable.transparent);
	        //m_preset_ib [idx].setEnabled (false);
	      }
	      else {
	        //m_presets_curr = idx + 1;   // DISABLED: Update number of presets (For blank ones now too)
	        m_preset_channel [idx] = 0;
	        m_preset_name [idx] = "";
	      }
	    }

	    Log.d(TAG,"m_presets_curr: " + m_presets_curr);
	  }
	  
	  private String radioFMChannelToFreqString(int channel) {
		  return String.valueOf(((channel*0.2)+87.5));
	  }
	  
	  private void radioTogglePower () {
		  m_radio_power = !m_radio_power;
		  try {
	            Message msg = Message.obtain(null,USBCANMonitor.MSG_FM_SET_POWER);
	            msg.replyTo = mMessenger;
	            msg.arg1 = ((m_radio_power == false) ? 0 : 1);
	            mService.send(msg);
	        } catch (RemoteException e) {
	        }
		  radioSetGUIPower();
	  }
	  
	 private void radioSetGUIPower () {
	    if (m_radio_power) {
	      m_iv_power.setImageResource (R.drawable.dial_power_on_250sc);
	      int lastchannel = prefs.getInt("chass_last_chan", 0);
	      //Tune to last channel
	      radio_channel_tune(lastchannel);
	      rssiHandler.postDelayed(RSSIrunnable, 100);
	    }
	    else {
	      m_iv_power.setImageResource (R.drawable.dial_power_off_250sc);
	      //Set all displayable text fields to initial OFF defaults
	      m_tv_freq.setText("");
	    }
	    //m_iv_record.setEnabled  (m_radio_power);
	    m_iv_seekup.setEnabled  (m_radio_power);
	    m_iv_seekdn.setEnabled  (m_radio_power);
	    m_iv_next.setEnabled  (m_radio_power);
	    m_iv_prev.setEnabled  (m_radio_power);
	    m_iv_paupla.setEnabled  (m_radio_power);

	    for (int idx = 0; idx < 16; idx ++)                // For all presets...
	      m_preset_ib [idx].setEnabled (m_radio_power);
	  }
	 
	 private void radioSetGUIRSSI(int rssi) {
		 m_tv_rssi.setText(String.valueOf(rssi));
	 }
	 
	 private View.OnClickListener radio_short_click_lstnr = new View.OnClickListener () {
		    public void onClick (View v) {
		      Log.d(TAG,"view: " + v);
		      if (v == null) {
		    	  Log.d(TAG,"view: " + v);
		      }
		      else if (v == m_iv_paupla)
		        ;//m_com_api.key_set ("audio_state", "Toggle");
		      else if (v == m_iv_power)
		    	  radioTogglePower();
		      else if (v == m_tv_freq) // Frequency direct entry
		        showDialog (RADIO_FREQ_SET_DIALOG);
		      else if (v == m_iv_prev)
		    	  radio_channel_tune(--m_radio_channel);
		      else if (v ==  m_iv_next)
		    	  radio_channel_tune(++m_radio_channel);
		      else if (v == m_iv_seekdn) {  // Seek down
		    	  try {
		              Message msg = Message.obtain(null,USBCANMonitor.MSG_FM_SEEK_DOWN);
		              msg.replyTo = mMessenger;
		              mService.send(msg);
		          } catch (RemoteException e) {
		          }
		      }
		      else if (v == m_iv_seekup) {  // Seek up
		    	  try {
		              Message msg = Message.obtain(null,USBCANMonitor.MSG_FM_SEEK_UP);
		              msg.replyTo = mMessenger;
		              mService.send(msg);
		          } catch (RemoteException e) {
		          }
		      }
		    }
		  };
		  
		  // Presets:
		  private void radio_preset_delete (int idx) {
		    Log.d(TAG,"idx: " + idx);
		    m_preset_tv [idx].setText ("");
		    m_preset_channel [idx] = 0;
		    m_preset_name [idx] = "";
		    m_preset_ib [idx].setImageResource (R.drawable.btn_preset);
		    prefs.edit().putString("chass_preset_name_" + idx, m_preset_name [idx]).putInt("chass_preset_chan_" + idx, m_preset_channel [idx]).apply();
		  }

		  private void radio_preset_rename (int idx, String name) {
		    Log.d(TAG,"idx: " + idx);
		    m_preset_tv [idx].setText (name);
		    m_preset_name [idx] = name;
		    prefs.edit().putString("chass_preset_name_" + idx, m_preset_name [idx]).putInt("chass_preset_chan_" + idx, m_preset_channel [idx]).apply();
		  }

		  private void radio_preset_set (int idx) {
			if(!m_radio_power) return; //radio is not powered on
		    if (idx >= 16) {
		    	Log.e(TAG,"preset_set: Index too high at:" + idx);
		      return;
		    }
		    else if (idx > m_presets_curr) {// If trying to set a preset past the last current one (this avoid blank presets)
		      Log.d(TAG,"idx: " + idx + "  com_api.chass_preset_max: " + 16 + "  m_presets_curr: " + m_presets_curr);
		      boolean set_past_end = true;
		      boolean ignore_past_end = true;
		      if (set_past_end)
		        Log.d(TAG,"set_past_end");
		      else if (ignore_past_end) {
		        Log.d(TAG,"ignore_past_end");
		        return;
		      }
		      else {
		        Log.d(TAG,"set next available");
		        idx = m_presets_curr;                       // Set index to last + 1 = next new one
		      }
		    }      
		    else
		      Log.d(TAG,"idx: " + idx + "  com_api.chass_preset_max: " + 16 + "  m_presets_curr: " + m_presets_curr);

		    if (m_presets_curr < idx + 1)
		      m_presets_curr = idx + 1;                    // Update number of presets

		    m_preset_tv [idx].setText ("" + radioFMChannelToFreqString(m_radio_channel));
		    m_preset_name [idx] = radioFMChannelToFreqString(m_radio_channel);
		    m_preset_channel [idx] = m_radio_channel;
		    prefs.edit().putString("chass_preset_name_" + idx, m_preset_name [idx]).putInt("chass_preset_chan_" + idx, m_preset_channel [idx]).apply();
		    m_preset_ib [idx].setImageResource (R.drawable.transparent);  // R.drawable.btn_preset
		  }

		  private View.OnClickListener radio_preset_select_lstnr = new   // Tap: Tune to preset
		        View.OnClickListener () {
		    public void onClick (View v) {
		      Log.d(TAG,"view: " + v);
		      
		      for (int idx = 0; idx < 16; idx ++) {       // For all presets...
		        if (v == m_preset_ib [idx]) {             // If this preset...
		          Log.d(TAG,"idx: " + idx);
		          try {
		            if (m_preset_channel [idx] == 0)        // If no preset yet...
		              //RE-ENABLED because it's a pain      com_uti.loge ("Must long press to press presets now, to avoid accidental sets");
		              radio_preset_set (idx);                  //Set preset
		            else
		              radio_channel_tune (m_preset_channel [idx]);  // Else change to preset frequency
		          }
		          catch (Throwable e) {
		            e.printStackTrace ();
		          };
		          return;
		        }
		      }
		    }
		  };

		  // Long press/Tap and Hold: Show preset change options
		  private View.OnLongClickListener radio_preset_change_lstnr = new View.OnLongClickListener () {
		    public boolean onLongClick (View v) {
		      Log.d(TAG,"view: " + v);
		      for (int idx = 0; idx < 16; idx ++) {            // For all presets...
		        if (v == m_preset_ib [idx]) {                  // If this preset...
		          cur_preset_idx = idx;
		          Log.d(TAG,"idx: " + idx);
		          if (m_preset_channel [idx] == 0)            // If no preset yet...
		            radio_preset_set (idx);                    // Set preset
		          else
		            showDialog (RADIO_PRESET_CHANGE_DIALOG);    // Else show preset options dialog
		          break;
		        }
		      }
		      return (true);                                        // Consume long click
		    }
		  };
		  
		  // Dialog methods:
		  private static final int RADIO_FREQ_SET_DIALOG        = 1;                    // Frequency set
		  private static final int RADIO_PRESET_CHANGE_DIALOG   = 2;                    // Preset functions
		  private static final int RADIO_PRESET_RENAME_DIALOG   = 3;                    // Preset rename

		  @Override
		  protected Dialog onCreateDialog (int id, Bundle args) {               // Create a dialog by calling specific *_dialog_create function    ; Triggered by showDialog (int id);
			Log.d(TAG,"id: " + id + "  args: " + args);
		    Dialog dlg_ret = null;
		    dlg_ret = dialog_create (id, args);
		    Log.d(TAG,"dlg_ret: " + dlg_ret);
		    return (dlg_ret);
		  }
		  
		  public Dialog dialog_create (int id, Bundle args) {// Create a dialog by calling specific *_dialog_create function    ; Triggered by showDialog (int id);
			Log.d(TAG,"id: " + id + "  args: " + args);
		    Dialog ret = null;                                                  // DialogFragment ret = null;
		    switch (id) {
		      case RADIO_FREQ_SET_DIALOG:
		        ret = radio_freq_set_dialog_create        (id);
		        break;
		      case RADIO_PRESET_CHANGE_DIALOG:
		        ret = radio_preset_change_dialog_create   (id);
		        break;
		      case RADIO_PRESET_RENAME_DIALOG:
		        ret = radio_preset_rename_dialog_create   (id);
		        break;
		    }
		    Log.d(TAG,"dialog: " + ret);
		    return (ret);
		  }
		  
		  private Dialog radio_freq_set_dialog_create (final int id) {                   // Set new frequency
			  Log.d(TAG,"id: " + id);
			    LayoutInflater factory = LayoutInflater.from (this);
			    final View edit_text_view = factory.inflate (R.layout.edit_number, null);
			    AlertDialog.Builder dlg_bldr = new AlertDialog.Builder (this);
			    dlg_bldr.setTitle ("Set Frequency");
			    dlg_bldr.setView (edit_text_view);
			    dlg_bldr.setPositiveButton ("OK", new DialogInterface.OnClickListener () {

			      public void onClick (DialogInterface dialog, int whichButton) {

			        EditText edit_text = (EditText) edit_text_view.findViewById (R.id.edit_number);
			        CharSequence newFreq = edit_text.getEditableText ();
			        String nFreq = String.valueOf (newFreq);  // Get entered text as String
			        radio_freq_tune (nFreq);
			      }
			    });
			    dlg_bldr.setNegativeButton ("Cancel", new DialogInterface.OnClickListener () {
			      public void onClick (DialogInterface dialog, int whichButton) {
			      }
			    });                         
			    return (dlg_bldr.create ());
			  }

			// _PRST
			  private Dialog radio_preset_rename_dialog_create (final int id) {                 // Rename preset
				  Log.d(TAG,"id: " + id);
			    LayoutInflater factory = LayoutInflater.from (this);
			    final View edit_text_view = factory.inflate (R.layout.edit_text, null);
			    AlertDialog.Builder dlg_bldr = new AlertDialog.Builder (this);
			    dlg_bldr.setTitle ("Preset Rename");
			    dlg_bldr.setView (edit_text_view);
			    dlg_bldr.setPositiveButton ("OK", new DialogInterface.OnClickListener () {
			      public void onClick (DialogInterface dialog, int whichButton) {
			        EditText edit_text = (EditText) edit_text_view.findViewById (R.id.edit_text);
			        CharSequence new_name = edit_text.getEditableText ();
			        String name = String.valueOf (new_name);
			        radio_preset_rename (cur_preset_idx, name);
			      }
			    });
			    dlg_bldr.setNegativeButton ("Cancel", new DialogInterface.OnClickListener () {
			      public void onClick (DialogInterface dialog, int whichButton) {
			      }
			    });
			    return (dlg_bldr.create ());
			  }

			  private Dialog radio_preset_change_dialog_create (final int id) {
				  Log.d(TAG,"id: " + id);
			    AlertDialog.Builder dlg_bldr = new AlertDialog.Builder (this);
			    dlg_bldr.setTitle ("Preset");
			    ArrayList <String> array_list = new ArrayList <String> ();
			    //array_list.add ("Tune");
			    array_list.add ("Replace");
			    array_list.add ("Rename");
			    array_list.add ("Delete");

			    dlg_bldr.setOnCancelListener (new DialogInterface.OnCancelListener () {
			      public void onCancel (DialogInterface dialog) {
			      }
			    });
			    String [] items = new String [array_list.size ()];
			    array_list.toArray (items);

			    dlg_bldr.setItems (items, new DialogInterface.OnClickListener () {
			      public void onClick (DialogInterface dialog, int item) {
			        switch (item) {
			          case -1:                                                       // Tune to station
			            Log.d(TAG,"preset_change_dialog_create onClick Tune freq: " + m_preset_channel [cur_preset_idx]);
			            Log.d(TAG,"preset_change_dialog_create onClick Tune name: " + m_preset_name [cur_preset_idx]);
			            radio_channel_tune (m_preset_channel [cur_preset_idx]);             // Change to preset frequency
			            break;
			          case 0:                                                       // Replace preset with currently tuned station
			            Log.d(TAG,"preset_change_dialog_create onClick Replace freq: " + m_preset_channel [cur_preset_idx]);
			            Log.d(TAG,"preset_change_dialog_create onClick Replace name: " + m_preset_name [cur_preset_idx]);
			            radio_preset_set (cur_preset_idx);
			            Log.d(TAG,"preset_change_dialog_create onClick Replace freq: " + m_preset_channel [cur_preset_idx]);
			            Log.d(TAG,"preset_change_dialog_create onClick Replace name: " + m_preset_name [cur_preset_idx]);
			            break;
			          case 1:                                                       // Rename preset
			            Log.d(TAG,"preset_change_dialog_create onClick Rename freq: " + m_preset_channel [cur_preset_idx]);
			            Log.d(TAG,"preset_change_dialog_create onClick Rename name: " + m_preset_name [cur_preset_idx]);
			            showDialog (RADIO_PRESET_RENAME_DIALOG);
			            Log.d(TAG,"preset_change_dialog_create onClick Rename freq: " + m_preset_channel [cur_preset_idx]);
			            Log.d(TAG,"preset_change_dialog_create onClick Rename name: " + m_preset_name [cur_preset_idx]);
			            break;
			          case 2:                                                       // Delete preset   !! Deletes w/ no confirmation
			            Log.d(TAG,"preset_change_dialog_create onClick Delete freq: " + m_preset_channel [cur_preset_idx]);
			            Log.d(TAG,"preset_change_dialog_create onClick Delete name: " + m_preset_name [cur_preset_idx]);
			            radio_preset_delete (cur_preset_idx);
			            Log.d(TAG,"preset_change_dialog_create onClick Delete freq: " + m_preset_channel [cur_preset_idx]);
			            Log.d(TAG,"preset_change_dialog_create onClick Delete name: " + m_preset_name [cur_preset_idx]);
			            break;
			          default:
			            break;
			        }
			    }
			  });

			    return (dlg_bldr.create ());
			  }
}
