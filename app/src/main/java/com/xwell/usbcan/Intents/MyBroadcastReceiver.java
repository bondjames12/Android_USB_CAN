package com.xwell.usbcan.Intents;

import java.util.HashMap;
import java.util.Iterator;

import com.xwell.usbcan.service.USBCANMonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;

//Subscribed to android.intent.action.BOOT_COMPLETED
//check manifest file
public class MyBroadcastReceiver extends BroadcastReceiver {
	private static final String LOG_TAG = "USBHID-Broadcast Receiver";

	public MyBroadcastReceiver() {
		Log.d(LOG_TAG, "Running: MyBroadcastReceiver Constructor");
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LOG_TAG, "Running: MyBroadcastReceiver onReceive");
        Log.d(LOG_TAG, "intent: " + intent);
        Intent service = new Intent(context, USBCANMonitor.class);
        String action = intent.getAction();
        if(action == "android.hardware.usb.action.USB_DEVICE_ATTACHED")
        {
        	Log.d(LOG_TAG, "Our USB device was attached");
        	service.setAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
	        //Get the USBDevice from the Intent
        	//TRY TO GIVE US AUTOMATIC USB PERMISSION ---NEEDS TO BE SYSTEM APP----
	        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
	        try
	        {
	            PackageManager pm = context.getPackageManager();
	            ApplicationInfo ai = pm.getApplicationInfo( "com.xwell.usbcan", 0 );
	            if( ai != null )
	            {
	                UsbManager manager = (UsbManager) context.getSystemService( Context.USB_SERVICE );
	                IBinder b = ServiceManager.getService( Context.USB_SERVICE );
	                IUsbManager usbservice = IUsbManager.Stub.asInterface( b );
	
	                HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
	                Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
	                while( deviceIterator.hasNext() )
	                {
	                        UsbDevice usbdevice = deviceIterator.next();
	                        if( usbdevice.getVendorId() == 0x16C0 )
	                        {
	                        	usbservice.grantDevicePermission( usbdevice, ai.uid );
	                        	usbservice.setDevicePackage( usbdevice, "com.xwell.usbcan", ai.uid);
	                       }
	                }
	            }
	        }
	        catch( Exception e )
	        {
	        	Log.i(LOG_TAG, "Failed to grant automatic permission" + e);
				e.printStackTrace();
	        }
		}//end android.hardware.usb.action.USB_DEVICE_ATTACHED
        else if(action == "android.hardware.usb.action.USB_DEVICE_DETACHED")
        {
        	Log.d(LOG_TAG, "Our USB device was DEtached");
        	service.setAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        }
        Log.d(LOG_TAG, "Sending USBCANMonitor Service Intent with extras"); 
		//Copy intent extras from intent broadcast to our intent we are using to start our service
		service.putExtras(intent);
        context.startService(service);
	}

}
