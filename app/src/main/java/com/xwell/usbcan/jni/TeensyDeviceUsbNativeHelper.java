//=============================================================================
//
// File : TeensyDeviceUsbNativeHelper.java
// Creation date : Mon 26 Nov 2012 03:23:24
// Project : Pragmaware Pegasus Notetaker Android Interface
// Author : Szymon Tomasz Stefanek <s dot stefanek at gmail dot com>
//
//=============================================================================

package com.xwell.usbcan.jni;

public class TeensyDeviceUsbNativeHelper
{
	private int m_iNativeFileDescriptor = -1;
	private int m_iNativeInternalPointer; // do not initialize

	public TeensyDeviceUsbNativeHelper(int iNativeFileDescriptor,int iMaxPacketSize,int iEndpointAddress)
	{
		m_iNativeFileDescriptor = iNativeFileDescriptor;
		nativeInit(m_iNativeFileDescriptor,iMaxPacketSize,iEndpointAddress);
	}

	public void cleanup()
	{
		nativeDone();
	}

	public boolean beginUrbReadLoop()
	{
		return nativeBeginUrbReadLoop() == 0;
	}
	
	public int readUrb(int iTimeout,byte[] aBuffer)
	{
		return nativeReadUrb(iTimeout,aBuffer);
	}
	
	public void endUrbReadLoop()
	{
		nativeEndUrbReadLoop();
	}

    static {
        System.loadLibrary("teensy-jni");
    }
    
    private native int nativeInit(int iNativeFileDescriptor,int iMaxPacketSize,int iEndpointAddress);
    private native int nativeBeginUrbReadLoop();
    private native int nativeReadUrb(int iTimeout,byte[] aBuffer);
    private native int nativeEndUrbReadLoop();
	private native void nativeDone();
}
