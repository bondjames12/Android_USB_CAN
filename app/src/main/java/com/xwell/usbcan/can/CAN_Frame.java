package com.xwell.usbcan.can;

public class CAN_Frame {
	public int mID;
	public int mDataLength;
	public byte[] mData;
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHexString(byte[] bytes) {
	    char[] hexChars = new char[(bytes.length * 3)+1];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 3] = hexArray[v >>> 4];
	        hexChars[j * 3 + 1] = hexArray[v & 0x0F];
	        hexChars[j * 3 + 2] = ' ';
	    }
	    hexChars[bytes.length * 3] = '\n';
	    return new String(hexChars);
	}
	
	
	
	public CAN_Frame() {
		mID = 0;
		mDataLength = 0;
		mData = new byte[8];
	}
	
	public CAN_Frame(byte[] data) {
		if(data[1] == 0x01)
		{
			mID = ((data[2] & 0xff) << 8) | (data[3] & 0xff); //combine 2nd and 3rd byte into the ID (was split to be sent over USB)
			mDataLength = data[4];
			mData = new byte[mDataLength];
			for(int i=0;i<mDataLength;i++)
			{
				mData[i] = data[i+5];
			}
		}
	}
	
	public CAN_Frame(int id, int length, byte[] data) {
		mID = id;
		mDataLength = length;
		mData = data;
	}
	
	public CAN_Frame(int id, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7, byte b8) {
		mID = id;
		mDataLength = 8;
		mData = new byte[]  {b1,b2,b3,b4,b5,b6,b7,b8};
	}
	
	public CAN_Frame(int id, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
		mID = id;
		mDataLength = 7;
		mData = new byte[]  {b1,b2,b3,b4,b5,b6,b7};
	}
	
	public CAN_Frame(int id, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6) {
		mID = id;
		mDataLength = 6;
		mData = new byte[]  {b1,b2,b3,b4,b5,b6};
	}
	
	public CAN_Frame(int id, byte b1, byte b2, byte b3, byte b4, byte b5) {
		mID = id;
		mDataLength = 5;
		mData = new byte[]  {b1,b2,b3,b4,b5};
	}
	
	public CAN_Frame(int id, byte b1, byte b2, byte b3, byte b4) {
		mID = id;
		mDataLength = 4;
		mData = new byte[]  {b1,b2,b3,b4};
	}
	
	public CAN_Frame(int id, byte b1, byte b2, byte b3) {
		mID = id;
		mDataLength = 3;
		mData = new byte[]  {b1,b2,b3};
	}
	
	public CAN_Frame(int id, byte b1, byte b2) {
		mID = id;
		mDataLength = 2;
		mData = new byte[]  {b1,b2};
	}
	
	public CAN_Frame(int id, byte b1) {
		mID = id;
		mDataLength = 1;
		mData = new byte[]  {b1};
	}

}
