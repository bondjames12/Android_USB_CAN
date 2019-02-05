package com.xwell.usbcan.ui;

import com.xwell.usbcan.R;
import com.xwell.usbcan.Intents.MyBroadcastReceiver;

import android.app.Activity;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;

public class CreateShortcutVolumeUpActivity extends Activity {
	 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        // The meat of our shortcut
        Intent shortcutIntent = new Intent(this, VolumeActivity.class);
        shortcutIntent.setAction("com.xwell.usbcan.CANVOLUMEUP");
        ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_volume_up);
         
        // The result we are passing back from this activity
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Volume Up");
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
        setResult(RESULT_OK, intent);
         
        finish(); // Must call finish for result to be returned immediately
    }
 
}