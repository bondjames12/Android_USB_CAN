package com.xwell.usbcan.can;

public interface ICANDecoder {
	byte onVolumeChanged();
	byte onBalanceChanged();
	byte onFadeChanged();
	byte onBassChanged();
	byte onMidChanged();
	byte onTrebleChanged();
}
