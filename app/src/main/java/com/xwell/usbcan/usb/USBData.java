package com.xwell.usbcan.usb;

import android.os.Parcel;
import android.os.Parcelable;

public class USBData implements Parcelable
{    private byte[] _byte;

    public USBData() {
    }

    public USBData(Parcel in) {
        readFromParcel(in);
    }


    public byte[] get_byte() {
        return _byte;
    }

    public void set_byte(byte[] _byte) {
        this._byte = _byte;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(_byte);
    }

    public void readFromParcel(Parcel in) {
        _byte = in.createByteArray();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public USBData createFromParcel(Parcel in) {
            return new USBData(in);
        }

        public USBData[] newArray(int size) {
            return new USBData[size];
        }
    };

}