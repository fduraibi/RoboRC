/**
 * Created by Fahad Alduraibi on 12/4/14.
 * fadvisor.net
 */
package net.fadvisor.roborc;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class MySeekBar extends SeekBar {
    public MySeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP) {
            //Log.d("tag", "UP");
            MainActivity.ResetSeekBar(this);
//        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
//            //Log.d("tag", Integer.toString(getProgress()));
//        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            //Log.d("tag", "DOWN ");
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            // When orientation changes reset the SeekBars since they get stock when device
            // is flipped by mistake.
            MainActivity.ResetSeekBar(this);
        }
        return true;
    }
}