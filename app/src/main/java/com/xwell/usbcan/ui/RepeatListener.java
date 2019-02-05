package com.xwell.usbcan.ui;

import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

/**
 * A class that can be used as a TouchListener on any view (e.g. a Button).
 * It either calls performClick once, or performLongClick repeatedly on an interval.
 * The performClick can be fired either immediately or on ACTION_UP if no clicks have
 * fired.  The performLongClick is fired once after initialInterval and then repeatedly
 * after normalInterval.
 *
 * <p>Interval is scheduled after the onClick completes, so it has to run fast.
 * If it runs slow, it does not generate skipped onClicks.
 *
 * Based on http://stackoverflow.com/a/12795551/642160
 * Usage:
 * Button button = new Button(context);
button.setOnTouchListener(new RepeatListener(400, 100, new OnClickListener() {
  @Override
  public void onClick(View view) {
    // the code to execute repeatedly
  }
}));
 */
public class RepeatListener implements OnTouchListener {

    private Handler handler = new Handler();
    private static final String TAG = "RepeatListener";
    private final boolean immediateClick;
    private final boolean longClick;
    private final int initialInterval;
    private final int normalInterval;
    private boolean haveClicked;
    

    private Runnable handlerRunnable = new Runnable() {
        @Override
        public void run() {
            haveClicked = true;
            handler.postDelayed(this, normalInterval);
            if(longClick) 
            	downView.performLongClick();
            else
            	 downView.performClick();
        }
    };

    private View downView;

    /**
     * @param immediateClick Whether to call onClick immediately, or only on ACTION_UP
     * @param initialInterval The interval after first click event
     * @param normalInterval The interval after second and subsequent click
     *       events
     * @param clickListener The OnClickListener, that will be called
     *       periodically
     */
    public RepeatListener(
        boolean immediateClick, 
        boolean longClick,
        int initialInterval,
        int normalInterval)
    {
        if (initialInterval < 0 || normalInterval < 0)
            throw new IllegalArgumentException("negative interval");

        this.immediateClick = immediateClick;
        this.longClick = longClick;
        this.initialInterval = initialInterval;
        this.normalInterval = normalInterval;
    }

    /**
     * Constructs a repeat-listener with the system standard long press time
     * for both intervals, and no immediate click.
     */
    public RepeatListener()
    {
        immediateClick = false;
        longClick = true;
        initialInterval = android.view.ViewConfiguration.getLongPressTimeout();
        normalInterval = initialInterval;
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
    	//Log.d(TAG,"onTouch: " + motionEvent.toString());
        switch (motionEvent.getAction()) {
        case MotionEvent.ACTION_DOWN:
            handler.removeCallbacks(handlerRunnable);
            handler.postDelayed(handlerRunnable, initialInterval);
            downView = view;
            if (immediateClick)
                downView.performClick();
            haveClicked = immediateClick;
            return true;
        case MotionEvent.ACTION_UP:
            // If we haven't clicked yet, click now
            if (!haveClicked)
                downView.performClick();
            // Fall through
        case MotionEvent.ACTION_CANCEL:
            handler.removeCallbacks(handlerRunnable);
            downView = null;
            return true;
        }

        return false;
    }

}