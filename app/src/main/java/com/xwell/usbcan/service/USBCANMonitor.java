package com.xwell.usbcan.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import com.xwell.usbcan.R;
import com.xwell.usbcan.can.CAN_Frame;
import com.xwell.usbcan.ui.MainActivity;
import com.xwell.usbcan.usb.HidBridge;
import com.xwell.usbcan.usb.IUsbConnectionHandler;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

/**
 * @author Colin
 * It is possible to assign services the same priority as foreground activities. 
 * In this case it is required to have a visible notification active for the related service. 
 * It is frequently used for services which play videos or music.
 */
public class USBCANMonitor extends Service implements IUsbConnectionHandler {
	public static final String LOG_TAG = "USBCAN Monitor";
	//Constants to use for Message Commands from client apps
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_CANFRAME = 3;
	public static final int MSG_STATUS = 4;
	public static final int MSG_VOLUME_UP = 5;
	public static final int MSG_VOLUME_DOWN = 6;
	public static final int MSG_BALANCE_UP = 7;
	public static final int MSG_BALANCE_DOWN = 8;
	public static final int MSG_FADE_UP = 9;
	public static final int MSG_FADE_DOWN = 10;
	public static final int MSG_BASS_UP = 11;
	public static final int MSG_BASS_DOWN = 12;
	public static final int MSG_MID_UP = 13;
	public static final int MSG_MID_DOWN = 14;
	public static final int MSG_TREBLE_UP = 15;
	public static final int MSG_TREBLE_DOWN = 16;
	public static final int MSG_VOLUME_SET = 17;
	public static final int MSG_BALANCE_SET = 18;
	public static final int MSG_FADE_SET = 19;
	public static final int MSG_BASS_SET = 20;
	public static final int MSG_MID_SET = 21;
	public static final int MSG_TREBLE_SET = 22;
	public static final int MSG_STOP = 23;
	public static final int MSG_REGISTER_CLIENT_DEBUG = 24;
	public static final int MSG_USB_RAW_BYTES = 25;
	public static final int MSG_FM_SEEK_UP = 26;
	public static final int MSG_FM_SEEK_DOWN = 27;
	public static final int MSG_FM_SET_CHANNEL = 28;
	public static final int MSG_FM_SET_VOLUME = 29;
	public static final int MSG_FM_GET_RDS = 30;
	public static final int MSG_FM_GET_STATUS = 31;
	public static final int MSG_FM_SET_POWER = 32;
	public static final int MSG_TEENSY_GET_STATUS = 33;
	//Commands constants to use when sending/receiving data to USB arduino device
	public static final byte USB_CANFRAME = 0x1;
	public static final byte USB_RESET = 0x2;
	public static final byte USB_VOLUME = 0x3;
	public static final byte USB_BALANCE = 0x4;
	public static final byte USB_FADE = 0x5;
	public static final byte USB_BASS = 0x6;
	public static final byte USB_MID = 0x7;
	public static final byte USB_TREBLE = 0x8;
	public static final byte USB_AMP_SET = 0x9;
	public static final byte USB_FM_COMMAND = 0x0A;
	public static final byte USB_FM_SEEK_UP = 0x01;
	public static final byte USB_FM_SEEK_DOWN = 0x02;
	public static final byte USB_FM_SET_VOLUME = 0x03;
	public static final byte USB_FM_SET_CHANNEL = 0x04;
	public static final byte USB_FM_GET_RDS = 0x05;
	public static final byte USB_FM_GET_RSSI = 0x06;
	public static final byte USB_FM_SET_POWER = 0x07;
	public static final byte USB_FM_GET_POWER = 0x08;
	//USB Receive COMMAND TYPE CONSTANTS
	public static final byte USBRECEIVE_CAN = 0x01;
	public static final byte USBRECEIVE_AMP = 0x02;
	public static final byte USBRECEIVE_FM_CHANNEL = 0x03;
	public static final byte USBRECEIVE_FM_RDS = 0x04;
	public static final byte USBRECEIVE_FM_POWER = 0x05;
	public static final byte USBRECEIVE_FM_STATUS = 0x06;
	public static final byte USBRECEIVE_TEENSY_STATUS = 0x07;
	
	private static HidBridge sHidBridge;
	/** Keeps track of all current registered clients. */
	private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	private Messenger mClientDebug;
	private byte prevcmd;
	// The queue that contains the read data.
	//private Queue<byte[]> _receivedQueue;
	//Service Notification
	Builder notificationBuilder;

	/**
	 * This class gets loaded and started from MyBroadcastReceiver which is subscribed to ON_BOOT intent
	 */
	public USBCANMonitor() {
		Log.d(LOG_TAG, "Running: Constructor");
		sHidBridge = new HidBridge(this,this, 0x0486, 0x16C0);
	}

	/**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case MSG_REGISTER_CLIENT_DEBUG:
            	mClientDebug = msg.replyTo;
                break;
            case MSG_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            case MSG_VOLUME_UP:
            	Log.d(LOG_TAG, "Running: MSG_VOLUME_UP");
            	sHidBridge.sendUSBCommand(USB_VOLUME, (byte) 1);
                break;
            case MSG_VOLUME_DOWN:
            	Log.d(LOG_TAG, "Running: MSG_VOLUME_DOWN");
            	sHidBridge.sendUSBCommand(USB_VOLUME, (byte) -1);
                break;
            case MSG_VOLUME_SET:
            	Log.d(LOG_TAG, "Running: MSG_VOLUME_SET");
            	sHidBridge.sendUSBCommand(USB_AMP_SET, USB_VOLUME,(byte) msg.arg1);
                break;
			case MSG_BALANCE_UP:
				Log.d(LOG_TAG, "Running: MSG_BALANCE_UP");
				sHidBridge.sendUSBCommand(USB_BALANCE, (byte) 1);
				break;
			case MSG_BALANCE_DOWN:
				Log.d(LOG_TAG, "Running: MSG_BALANCE_DOWN");
				sHidBridge.sendUSBCommand(USB_BALANCE, (byte) -1);
				break;
			case MSG_BALANCE_SET:
				Log.d(LOG_TAG, "Running: MSG_BALANCE_SET");
				sHidBridge.sendUSBCommand(USB_AMP_SET, USB_BALANCE,(byte) (msg.arg1+10));
				break;
			case MSG_FADE_UP:
				Log.d(LOG_TAG, "Running: MSG_FADE_UP");
				sHidBridge.sendUSBCommand(USB_FADE, (byte) 1);
				break;
			case MSG_FADE_DOWN:
				Log.d(LOG_TAG, "Running: MSG_FADE_DOWN");
				sHidBridge.sendUSBCommand(USB_FADE, (byte) -1);
				break;
			case MSG_FADE_SET:
				Log.d(LOG_TAG, "Running: MSG_FADE_SET");
				sHidBridge.sendUSBCommand(USB_AMP_SET, USB_FADE,(byte) (msg.arg1+10));
				break;
			case MSG_BASS_UP:
				Log.d(LOG_TAG, "Running: MSG_BASS_UP");
				sHidBridge.sendUSBCommand(USB_BASS, (byte) 1);
				break;
			case MSG_BASS_DOWN:
				Log.d(LOG_TAG, "Running: MSG_BASS_DOWN");
				sHidBridge.sendUSBCommand(USB_BASS, (byte) -1);
				break;
			case MSG_BASS_SET:
				Log.d(LOG_TAG, "Running: MSG_BASS_SET");
				sHidBridge.sendUSBCommand(USB_AMP_SET, USB_BASS,(byte) (msg.arg1+10));
				break;
			case MSG_MID_UP:
				Log.d(LOG_TAG, "Running: MSG_MID_UP");
				sHidBridge.sendUSBCommand(USB_MID, (byte) 1);
				break;
			case MSG_MID_DOWN:
				Log.d(LOG_TAG, "Running: MSG_MID_DOWN");
				sHidBridge.sendUSBCommand(USB_MID, (byte) -1);
				break;
			case MSG_MID_SET:
				Log.d(LOG_TAG, "Running: MSG_MID_SET");
				sHidBridge.sendUSBCommand(USB_AMP_SET, USB_MID,(byte) (msg.arg1+10));
				break;
			case MSG_TREBLE_UP:
				Log.d(LOG_TAG, "Running: MSG_TREBLE_UP");
				sHidBridge.sendUSBCommand(USB_TREBLE, (byte) 1);
				break;
			case MSG_TREBLE_DOWN:
				Log.d(LOG_TAG, "Running: MSG_TREBLE_DOWN");
				sHidBridge.sendUSBCommand(USB_TREBLE, (byte) -1);
				break;
			case MSG_TREBLE_SET:
				Log.d(LOG_TAG, "Running: MSG_TREBLE_SET");
				sHidBridge.sendUSBCommand(USB_AMP_SET, USB_TREBLE,(byte) (msg.arg1+10));
				break;
			case MSG_STOP: //STOP THIS SERVICE!
				Log.d(LOG_TAG, "Running: MSG_STOP");
				sHidBridge.StopReadingThread();
				break;
			case MSG_FM_SEEK_UP: 
				Log.d(LOG_TAG, "Running: MSG_FM_SEEK_UP");
				sHidBridge.sendUSBCommand(USB_FM_COMMAND, USB_FM_SEEK_UP);
				break;
			case MSG_FM_SEEK_DOWN: 
				Log.d(LOG_TAG, "Running: MSG_FM_SEEK_DOWN");
				sHidBridge.sendUSBCommand(USB_FM_COMMAND, USB_FM_SEEK_DOWN);
				break;
			case MSG_FM_SET_CHANNEL: 
				Log.d(LOG_TAG, "Running: MSG_FM_SET_CHANNEL");
				sHidBridge.sendUSBCommand(USB_FM_COMMAND, USB_FM_SET_CHANNEL,(byte) (msg.arg1));
				break;
			case MSG_FM_SET_VOLUME: 
				Log.d(LOG_TAG, "Running: MSG_FM_SET_VOLUME");
				sHidBridge.sendUSBCommand(USB_FM_COMMAND, USB_FM_SET_VOLUME,(byte) (msg.arg1));
				break;
			case MSG_FM_GET_RDS: 
				Log.d(LOG_TAG, "Running: MSG_FM_GET_RDS");
				sHidBridge.sendUSBCommand(USB_FM_COMMAND, USB_FM_GET_RDS);
				break;
			case MSG_FM_GET_STATUS: 
				Log.d(LOG_TAG, "Running: MSG_FM_GET_STATUS");
				sHidBridge.sendUSBCommand(USB_FM_COMMAND, USB_FM_GET_RSSI);
				break;
			case MSG_FM_SET_POWER: 
				Log.d(LOG_TAG, "Running: MSG_FM_SET_POWER");
				sHidBridge.sendUSBCommand(USB_FM_COMMAND, USB_FM_SET_POWER,(byte) (msg.arg1));
				break;
            default:
                super.handleMessage(msg);
            }
        }
    }

	 @Override
	public void onCreate() {
		Log.d(LOG_TAG, "Running: onCreate");
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		dumpIntent(intent);
		Log.d(LOG_TAG, "Running: onStartCommand");
		String action = null;
		if(intent != null)	action = intent.getAction();
		if(action != null)
		{
			//CHECK FOR BroadcastReciver intents from USB attach or detach
	        if(intent.getAction() == "android.hardware.usb.action.USB_DEVICE_ATTACHED")
	        {
	        	//no need to do anything here. read loop started below will take care of it
	        	setNotification("USBCAN Service:Teensy Attached");
	        	//start a listen thread
	    		sHidBridge.StartReadingThread();
	    		return Service.START_STICKY;
	        }
	        if(intent.getAction() == "android.hardware.usb.action.USB_DEVICE_DETACHED")
	        {
	        	setNotification("USBCAN Service:Teensy DEtached");
	        	//we should notify our USB read thread that the device is gone now
	        	sHidBridge.StopReadingThread();
	        	return Service.START_STICKY;
	        }
			
			//Check for intent action that might be sent from our widget etc to send USB commands
			if(intent.getAction() == this.getString(R.string.VOLUME_SET))
			{ //got intent extras that wants us to change the volume
				int newVol = intent.getIntExtra(this.getString(R.string.VOL_EXTRA), -1);
				Log.d(LOG_TAG, "VOLUME_SET: " + newVol);
				sHidBridge.sendUSBCommand(USB_AMP_SET,USB_VOLUME, (byte)newVol);
			} else if (intent.getAction() == this.getString(R.string.VOLUME_UP)) 
			{
				Log.d(LOG_TAG, "USB_VOLUME UP: " + 1);
				sHidBridge.sendUSBCommand(USB_VOLUME, (byte)1);
			} else if (intent.getAction() == this.getString(R.string.VOLUME_DOWN))
			{
				Log.d(LOG_TAG, "USB_VOLUME DOWN: " + -1);
				sHidBridge.sendUSBCommand(USB_VOLUME, (byte)-1);
			}
		}
		return Service.START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Log.i(LOG_TAG, "onDestroy()");
		sHidBridge.StopReadingThread();
		super.onDestroy();
	}
	
	public static void dumpIntent(Intent i){
		if(i == null) return;
		Log.e(LOG_TAG,"Dumping Intent start");
		if(i.getAction() == null)
			Log.e(LOG_TAG,"Intent Action:NULL");
		else
			Log.e(LOG_TAG,"Intent Action:" + i.getAction());
	    Bundle bundle = i.getExtras();
	    if (bundle != null) {
	        Set<String> keys = bundle.keySet();
	        Iterator<String> it = keys.iterator();
	        Log.e(LOG_TAG,"Dumping Intent extras start");
	        while (it.hasNext()) {
	            String key = it.next();
	            Log.e(LOG_TAG,"[" + key + "=" + bundle.get(key)+"]");
	        }
	        Log.e(LOG_TAG,"Dumping Intent end");
	    } else {
	    	Log.e(LOG_TAG,"Intent Extras:NULL");
	    }
	}

	 /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(LOG_TAG, "Running: USBCANMonitor onBind");
		return mMessenger.getBinder();
	}
	
	public void setNotification(String msg)
	{
		//This first use we store the notification builder. Any calls after just update the same builder
		if(notificationBuilder == null)
		{
			//prepare intent which is triggered if the notification is selected (START USBCAN APP)
			Intent notificationintent = new Intent(this, MainActivity.class);
			notificationintent.setAction("android.intent.action.MAIN");
			notificationintent.addCategory("android.intent.category.LAUNCHER");
			PendingIntent pIntent = PendingIntent.getActivity(this, 0, notificationintent, 0);
			// build notification
			notificationBuilder  = new Notification.Builder(this)
		        .setContentTitle("USBCAN Service")
		        .setContentText(msg)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentIntent(pIntent)
		        .setAutoCancel(true);
		        //.addAction(R.drawable.icon, "Call", pIntent)
		        //.addAction(R.drawable.icon, "More", pIntent)
		        //.addAction(R.drawable.icon, "And more",pIntent)
		} else
		{
			notificationBuilder.setContentText(msg);
		}
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(0, notificationBuilder.build()); 
	}
	
	public void sendStatusMessage(String msg)
	{
		Message msgdata = new Message();
		msgdata.what = MSG_STATUS;
		Bundle b = new Bundle();
		b.putString("STATUS", msg);
		msgdata.setData(b);
		sendMessageToAllClients(msgdata);
	}
	
	protected void sendMessageToAllClients(Message msg)
	{
		for (int i=mClients.size()-1; i>=0; i--) {
            try {
	            mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
	}
	
	@Override
	public void onUsbStopped() {
		Log.d(LOG_TAG,"Running: onUsbStopped()");
		setNotification("USB CAN Device stopped.");
		sendStatusMessage("USB CAN Device stopped.");
	}
	
	@Override
	public void onUSBDeviceClaimed() {
		setNotification("USB Device was found and claimed.");
		sendStatusMessage("USB CAN Device was found and claimed.");
		sHidBridge.sendUSBCommand(USB_FM_COMMAND, USB_FM_GET_POWER);
	}
	
	@Override
	public void onDeviceNotFound() {
		setNotification("USB CAN Device was not found.");
		sendStatusMessage("USB CAN Device was not found.");
	}

	@Override
	public void onUSBRecieve() {
		byte[] data = sHidBridge.GetReadQueueData();
		if(data == null) return;
		if(mClientDebug != null) //send everything recieved on USB to the debug screen
		{
			try {
	        	Message msgdata = new Message();
	        	msgdata.what = MSG_USB_RAW_BYTES;
	            Bundle b = new Bundle();
	            b.putByteArray("DATA", data);
	            msgdata.setData(b);
	            mClientDebug.send(msgdata);
	        } catch (RemoteException e) {
	        }
		}
		//TODO:its byte 1 because byte 0 is the USB message length
		switch (data[1])
		{
		case USBRECEIVE_CAN:
		{
			//Send CAN frame to any registered client APPS
        	Message msgdata = new Message();
        	msgdata.what = MSG_CANFRAME;
            Bundle b = new Bundle();
            b.putByteArray("CANFRAME", data);
            msgdata.setData(b);
            sendMessageToAllClients(msgdata);
			parseCANFrameFromUSB(data); //parse this can frame
			break;
		}
		case USBRECEIVE_AMP: //A USB AMP settings update from teensy
		{
			//Log.d(LOG_TAG, CAN_Frame.bytesToHexString(data));
			
			//send intent broadcast to volume widget
			final Intent setIntent = new Intent(this.getString(R.string.VOLUME_SET));
			setIntent.putExtra(this.getString(R.string.STREAM_EXTRA), 99);
			setIntent.putExtra("CANUSB-Volume",(int)data[2]);
			setIntent.putExtra("CANUSB-Mode",(int)data[8]);
		    sendBroadcast(setIntent); 
		    
		    byte cmd = data[9];
		    if(cmd != prevcmd) //don't process steering wheel buttons if the last button was the same
		    {
				if ((cmd & 0x00000004) != 0) 
				{
					//volume down HANDLED ONLY ON TEENSY DEVICE
				}
				else if ((cmd & 0x00000002) != 0) 
				{
					//volume up HANDLED ONLY ON TEENSY DEVICE
				}
				else if ((cmd & 0x00000010) != 0) //left down steering wheel button
				{
					AudioManager am = (AudioManager) this.getSystemService(AUDIO_SERVICE);
					if(am != null)
					{
						am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
						am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
					}
					/*simulate pressing the media previous key
					Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
					i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
					sendOrderedBroadcast(i, null);
					
					i = new Intent(Intent.ACTION_MEDIA_BUTTON);
					i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
					sendOrderedBroadcast(i, null);
					*/
					Log.d(LOG_TAG, "Previous Track!");
				}
				else if ((cmd & 0x00000008) != 0) //left up steering wheel button
				{
					AudioManager am = (AudioManager) this.getSystemService(AUDIO_SERVICE);
					if(am != null)
					{
						am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
						am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
					}
					/*
					//simulate pressing the media next key
					Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
					i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
					sendOrderedBroadcast(i, null);
					
					i = new Intent(Intent.ACTION_MEDIA_BUTTON);
					i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
					sendOrderedBroadcast(i, null);
					*/
					Log.d(LOG_TAG, "Next Track!");
				}	
				else if ((cmd & 0x00000001) != 0) //right middle button)
				{
					//we should pause
					AudioManager am = (AudioManager) this.getSystemService(AUDIO_SERVICE);
					if(am != null)
					{
						am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
						am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
					}
				}
				else if ((cmd & 0x00000020) != 0) //left middle button)
				{
				}
		    }
		    prevcmd = cmd;
			break;
		}
		case USBRECEIVE_FM_CHANNEL: //A USB FM channel update means we tuned the FM radio to new channel
		{
			int channel = (int) data[2];
			Log.d(LOG_TAG, "FM Channel Changed: " + channel);
			//Freq (MHz) = 0.2 x Channel + 87.5 MHz.
			//Send FM channel to any registered client APPS
        	 Message msg = Message.obtain(null,USBCANMonitor.MSG_FM_SET_CHANNEL);
             msg.replyTo = mMessenger;
             msg.arg1 = channel;
			sendMessageToAllClients(msg);
			break;
		}
		case USBRECEIVE_FM_RDS: //A USB FM RDS data block was decoded
		{
			byte[] rds = new byte[10];
			System.arraycopy(data, 2, rds, 0, 10);
			Log.d(LOG_TAG, "FM RDS Received: " + rds);
			//TODO Send this to GUI/registered client apps
			break;
		}
		case USBRECEIVE_FM_POWER: //A USB FM power status
		{
			int power = (int) data[2];
			Log.d(LOG_TAG, "FM power Received: " + power);
			Message msg = Message.obtain(null,USBCANMonitor.MSG_FM_SET_POWER);
            msg.replyTo = mMessenger;
            msg.arg1 = power;
			sendMessageToAllClients(msg);
			break;
		}
		case USBRECEIVE_FM_STATUS: //A USB FM rssi signal level
		{
			byte rssi = data[2];
			byte stereo = data[3];
			byte rdsready = data[4];
			byte channel = data[5];
			Log.d(LOG_TAG, "FM Status Received RSSI: " + rssi + " Stereo:" + stereo + " RDS Ready:" + rdsready+ " Channel:" + channel + "Freq=" + ((channel*0.2)+87.5));
			Message msg = Message.obtain(null,USBCANMonitor.MSG_FM_GET_STATUS);
            msg.replyTo = mMessenger;
            msg.arg1 = rssi;
            msg.arg2 = stereo;
			sendMessageToAllClients(msg);
			break;
		}
		case USBRECEIVE_TEENSY_STATUS: //A USB Teensy status update
		{
			byte A8_State = data[2];
			Log.d(LOG_TAG, "Teensy PIN_A8 Changed State: " + A8_State);
			Message msg = Message.obtain(null,USBCANMonitor.MSG_TEENSY_GET_STATUS);
            msg.replyTo = mMessenger;
            msg.arg1 = A8_State;
			sendMessageToAllClients(msg);
			break;
		}
		}
		
																		
	}
	
	public void parseCANFrameFromUSB(byte[] data)
	{
		//Store CAN byte array in a CAN_Frame class object and parse
		CAN_Frame canmsg = new CAN_Frame(data);
		switch (canmsg.mID)
		{
		case 0x3A0: //Don't parse these they are handled on the Teensy device and updates are sent over usb in a different format
			//Log.d(LOG_TAG, "Parsing 3A0 CAN Frame:");
			
			break;
		}
	}

	@Override
	public void onUSBPermissionError() {
		sendStatusMessage("USB CAN Device Permission Error.");
	}
}
