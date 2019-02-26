package com.xwell.usbcan.usb;

import java.lang.Thread.State;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
 








import com.xwell.usbcan.can.CAN_Frame;
import com.xwell.usbcan.jni.TeensyDeviceUsbNativeHelper;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.IBinder;
import android.util.Log;
 
/**
 * This class is used for talking to hid of the dongle, connecting, disconnencting and enumerating the devices.
 * @author gai
 */
public class HidBridge {
	private Context _context;
	private final IUsbConnectionHandler _connectionHandler;
	private int _productId;
	private int _vendorId;
	
	// Can be used for debugging.
	@SuppressWarnings("unused")
	//private HidBridgeLogSupporter _logSupporter = new HidBridgeLogSupporter();
	private static final String ACTION_USB_PERMISSION =
		    "com.example.company.app.testhid.USB_PERMISSION";
	
	// Locker object that is responsible for locking read/write thread.
	private Object _locker = new Object();
	private Object _writelocker = new Object();
	private Thread _readingThread = null;
	private volatile boolean interfaceclaimed = false;
	private volatile boolean _shouldstop = false;
	private String _deviceName;
	
	private UsbManager _usbManager;
	private UsbDevice _usbDevice;
	
	// The data queues that contain the read and write data.
	private Queue<byte[]> _receivedQueue;
	private Queue<byte[]> _writeQueue;
	
	/**
	 * Creates a hid bridge to the dongle. Should be created once.
	 * @param context is the UI context of Android.
	 * @param productId of the device.
	 * @param vendorId of the device.
	 */
	public HidBridge(Context context,IUsbConnectionHandler connectionHandler, int productId, int vendorId) {
		_context = context;
		_connectionHandler = connectionHandler;
		_productId = productId;
		_vendorId = vendorId;
		_receivedQueue = new LinkedList<byte[]>();
		_writeQueue = new LinkedList<byte[]>();
	}
	
	/**
	 * Searches for the device and opens it if successful
	 * @return true, if connection was successful
	 */
	public boolean FindDevice() {		   
		_usbManager = (UsbManager) _context.getSystemService(Context.USB_SERVICE);
		
		HashMap<String, UsbDevice> deviceList = _usbManager.getDeviceList();
 
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		_usbDevice = null;
		
		// Iterate all the available devices and find ours.
		while(deviceIterator.hasNext()){
		    UsbDevice device = deviceIterator.next();
		    if (device.getProductId() == _productId && device.getVendorId() == _vendorId) {
		    	_usbDevice = device;
		    	_deviceName = _usbDevice.getDeviceName();
		    }
		}
		
		if (_usbDevice == null) {
			Log("Cannot find the device. Did you forgot to plug it?");
			Log(String.format("\t I search for VendorId: %s and ProductId: %s", _vendorId, _productId));
			_connectionHandler.onDeviceNotFound();
			return false;
		}
		
		// Create and intent and request a permission.
		PendingIntent mPermissionIntent = PendingIntent.getBroadcast(_context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		_context.registerReceiver(mUsbReceiver, filter);
 
		_usbManager.requestPermission(_usbDevice, mPermissionIntent);
		Log("Found the device and requested permission.");
		return true;
	}
	
	/**
	 * Starts the thread that continuously reads the data from the device. 
	 * Should be called in order to be able to talk with the device.
	 */
	public void StartReadingThread() {
		if (_readingThread == null) {
			FindDevice();
			Log("Starting reading thread.");
			_shouldstop = false;
			_readingThread = new Thread(readerReceiverIntr);
			_readingThread.start();
		} else {
			Log("Reading thread already created");
			Log("Reading thread isAlive=" + _readingThread.isAlive());
			Log("Reading thread getState=" + _readingThread.getState());
			if(_readingThread.getState() == State.TERMINATED)
			{ //Thread previous completed we can just start it back up!
				FindDevice();
				Log("Starting reading thread.");
				_shouldstop = false;
				_readingThread = new Thread(readerReceiverIntr);
				_readingThread.start();
			}
		}
	}
	
	/**
	 * Tells the thread that continuously reads the data from the device to stop!
	 * If it is stopped - talking to the device would be impossible.
	 */
	public void StopReadingThread() {
		if (_readingThread != null) {
			_shouldstop = true;
			
		} else {
			Log("No reading thread to stop");
		}
	}
	
	/**
	 * @return true if there are any data in the queue to be read.
	 */
	public boolean IsThereAnyReceivedData() {
		synchronized(_locker) {
			return !_receivedQueue.isEmpty();
		}
	}
	
	/**
	 * UnQueue(poll) the data from the read queue.
	 * @return queued data.
	 */
	public byte[] GetReadQueueData() {
		synchronized(_locker) {
			return _receivedQueue.poll();
		}
	}
	
	/**
	 * Get the size of the Data Queue
	 * @return queue size.
	 */
	public int GetReadQueueSize() {
		synchronized(_locker) {
			return _receivedQueue.size();
		}
	}
	
	/**
	 * Queue the data into the write queue.
	 */
	public void AddWriteQueueData(byte[] buf) {
		if(!interfaceclaimed) return; //abort this if USB is not even opened and working
		synchronized(_writelocker) {
			_writeQueue.add(buf);
		}
	}
	
	/**
	 * Get the size of the Data Queue
	 * @return queue size.
	 */
	public int GetWriteQueueSize() {
		synchronized(_writelocker) {
			return _writeQueue.size();
		}
	}
	
	
	
	// The thread that continuously receives data from the dongle and put it to the queue.
	private Runnable readerReceiverIntr = new Runnable() {
	    public void run() {
	    	Log("USB Thread started");
 
	    	UsbEndpoint readEp = null;
	    	UsbDeviceConnection usbConnection = null;
	    	UsbInterface Intf = null;
	    	UsbInterface Intf0 = null;
	    	UsbEndpoint readEp0 = null;
	    	UsbEndpoint writeEp0 = null;
	    	byte[] readbuffer = new byte[64];
	        
	        Log("Entering Read loop");
	        interfaceclaimed = false;
	        while (!_shouldstop) {
	        	if (_usbDevice == null) {
	        		Log("No device. Open device and Recheck in 1 sec...");
	        		interfaceclaimed = false;
					FindDevice();
					Sleep(10000);
					continue;
				}
		        if(!interfaceclaimed)
		        {
		        	Log("Read loop Aquiring Interface 0 and Endpoint 1");
			    	Intf = _usbDevice.getInterface(1);
					readEp = Intf.getEndpoint(0);
					Log("Read loop Aquiring Interface 0 and Endpoint 0");
			    	Intf0 = _usbDevice.getInterface(0);
					readEp0 = Intf0.getEndpoint(0);
					writeEp0 = Intf0.getEndpoint(1);
					if (!_usbManager.getDeviceList().containsKey(_deviceName)) {
						Log("Failed to connect to the device. Retrying to acquire it.");
						FindDevice();
						if (!_usbManager.getDeviceList().containsKey(_deviceName)) {
							Log("No device. Recheking in 10 sec...");
							_connectionHandler.onDeviceNotFound();
							Sleep(10000);
							continue;
						}
					}
					
					try
					{
						
						usbConnection = _usbManager.openDevice(_usbDevice); 
						
						if (usbConnection == null) {
							Log("Cannot start reader because the user didn't gave me permissions or the device is not present. Retrying in 2 sec...");
							_connectionHandler.onUSBPermissionError();
							Sleep(2000);
							continue;
						}
						
						Log("USB loop claiming interface");
						// Claim and lock the interface in the android system.
						if(usbConnection.claimInterface(Intf, true)  && usbConnection.claimInterface(Intf0, true))
						{
							Log("USB loop Interface claimed successful");
							interfaceclaimed = true;
							_connectionHandler.onUSBDeviceClaimed();
						}
					}
					catch (SecurityException e) {
						Log("Cannot start reader because the user didn't gave me permissions. Retrying in 2 sec...");
						interfaceclaimed = false;
						Sleep(2000);
						continue;
					}
		        }
		        /*
		        int filed = readConnection.getFileDescriptor();
		        int epaddress = readEp.getAddress();
		        TeensyDeviceUsbNativeHelper nativehelper = new TeensyDeviceUsbNativeHelper(filed,64,epaddress);
		        nativehelper.beginUrbReadLoop();
		        byte[] buffer2 = new byte[64];
		        nativehelper.readUrb(1000, buffer2);
		        //_receivedQueue.add(buffer2);
            	Log(String.format("Message received of lengths %s and content: %s", 64, composeString(buffer2)));
		        */
            	synchronized(_writelocker) {
		        	//Queue a USBRequest to USB if any data is in queue
		        	if(_writeQueue.size() > 0)
		        	{
		        		byte[] writebuffer = _writeQueue.poll();
		        		Log(String.format("Writing to USB: %s", CAN_Frame.bytesToHexString(writebuffer)));
		        		//Log("USB loop sending bulk write request");
		        		if(usbConnection.bulkTransfer(writeEp0,writebuffer, 64, 100) < 0)
		        		{
		        			//failed to transfer
		        			Log("USB loop sending bulk write request FAILED");
		        		} //else Log("USB loop sending bulk write request SUCCEEDED");
		        	}
		        }
		
        		synchronized(_locker) {
		            //Log("Read loop Waiting for requst to complete");
					if(usbConnection.bulkTransfer(readEp0, readbuffer, 64, 100) >= 0) {
						byte[] readbuffertrimmed = new byte[readbuffer[0]];
						System.arraycopy( readbuffer, 0, readbuffertrimmed, 0, readbuffer[0] );
						_receivedQueue.add(readbuffertrimmed);
		            	Log(String.format("Message received: %s", CAN_Frame.bytesToHexString(readbuffer)));
		            	_connectionHandler.onUSBRecieve();
		            }
		        }
		        // Sleep for 10 ms to pause, so other thread can write data or anything. 
				// As both read and write data methods lock each other - they cannot be run in parallel.
				// Looks like Android is not so smart in planning the threads, so we need to give it a small time
				// to switch the thread context.
				//Sleep(10);
	        }
	        if(usbConnection != null)
	        {
		        usbConnection.releaseInterface(Intf);
		        usbConnection.releaseInterface(Intf0);
		        interfaceclaimed = false;
		        usbConnection.close();
		        _connectionHandler.onUsbStopped();
		        Log("USB Thread ENDED!");
	        }
	    }
	};
	
	private void Sleep(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
 
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        Log.d("USBHID", "Recieved Permission Intent");
	        if (ACTION_USB_PERMISSION.equals(action)) {
	            synchronized (this) {
	                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
	                
	                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
	                    if(device != null){
	                    	Log.d("USBHID", "permission granted for the device " + device);
	                    	//call method to set up device communication
	                    	//not needed since device is opened in the read thread
	                   }
	                } 
	                else {
	                    Log.d("USBHID", "permission denied for the device " + device);
	                }
	            }
	        }
	    }
	};
	
	/**
	 * Logs the message from HidBridge.
	 * @param message to log.
	 */
	private void Log(String message) {
		Log.d("USBHID",message);
		//LogHandler logHandler = LogHandler.getInstance();
		//logHandler.WriteMessage("HidBridge: " + message, LogHandler.GetNormalColor());
	}
	
	/**
	 * Composes a string from byte array.
	 */
	private String composeString(byte[] bytes) {
		StringBuilder builder = new StringBuilder();
		for (byte b: bytes) {
			builder.append(b);
			builder.append(" ");
		}
		
		return builder.toString();
	}

	public void sendUSBCommand(byte[] payload)
	{
		AddWriteQueueData(payload);
	}

	public void sendUSBCommand(byte command, byte[] payload)
	{
		byte data[] = new byte[64];
		data[0] = command;
		for (int i=1; i<payload.length; i++)
		{
			if(i>=64) break;
			data[i] = payload[i];
		}
		AddWriteQueueData(data);
	}

	public void sendUSBCommand(byte command, byte payload1, byte[] payload2)
	{
		byte data[] = new byte[64];
		data[0] = command;
		for (int i=1; i<payload2.length; i++)
		{
			if(i>=64) break;
			data[i] = payload2[i];
		}
		AddWriteQueueData(data);
	}

	public void sendUSBCommand(byte command, byte payload)
	{
		byte data[] = new byte[64];
		data[0] = command;
		data[1]= payload;
		AddWriteQueueData(data);
	}
	
	public void sendUSBCommand(byte command, byte payload1, byte payload2)
	{
		byte data[] = new byte[64];
		data[0] = command;
		data[1]= payload1;
		data[2]= payload2;
		AddWriteQueueData(data);
	}
}