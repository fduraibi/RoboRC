/**
 * Created by Fahad Alduraibi on 12/4/14.
 * fadvisor.net
 */

package net.fadvisor.roborc;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    public static volatile Context myContext;

    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    public static MySeekBar sb1;
    public static MySeekBar sb2;
    private static ToggleButton btConnect;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    // byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    // String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    // byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    // String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // show the connected device's name
                    String mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(myContext, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                    // Turn connect button on
                    btConnect.setEnabled(true);
                    btConnect.setChecked(true);

                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(myContext, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    btConnect.setEnabled(true);
                    break;
            }
        }
    };
    // String buffer for outgoing messages
//    private StringBuffer mOutStringBuffer; // I will use it later to send info about battery power (Low battery?)
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the bluetooth services
    private BluetoothService btService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myContext = getApplicationContext();
        setContentView(R.layout.activity_main);
        final View rl2 = findViewById(R.id.rl2);

        sb1 = (MySeekBar) findViewById(R.id.sb1);
        sb2 = (MySeekBar) findViewById(R.id.sb2);

        btConnect = (ToggleButton) findViewById(R.id.btConnect);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.btNotAvailable), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // When the layout rl2 is created rotate it and swap height and width values
        rl2.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {

                int w = rl2.getWidth();
                int h = rl2.getHeight();

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(h, w);
                rl2.setLayoutParams(params);
                if (android.os.Build.VERSION.SDK_INT >= 11) {
                    rl2.setRotation(270.0f);
                    rl2.setTranslationX((w - h) / 2);
                    rl2.setTranslationY((h - w) / 2);
                }

                if (android.os.Build.VERSION.SDK_INT >= 16)
                    rl2.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                else
                    rl2.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });

        // Bring seekbars to front to make sure they won't be covered by Ads in devices with small screen
        sb1.bringToFront();
        sb2.bringToFront();
    }

    public static void ResetSeekBar(final View v) {
        final MySeekBar tempsb;
        if (v.getId() == R.id.sb1) {
            tempsb = sb1;

        } else {
            tempsb = sb2;
        }
        if (Build.VERSION.SDK_INT >= 11) {
            ValueAnimator anim = ValueAnimator.ofInt(tempsb.getProgress(), 50);
            anim.setDuration(100);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int animProgress = (Integer) animation.getAnimatedValue();
                    tempsb.setProgress(animProgress);
                }
            });
            anim.start();
        } else {
            tempsb.setProgress(50);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (btService == null) btService = new BluetoothService(this, mHandler);
        }
    }

    public void btConnectClick(View v) {
        if (btConnect.isChecked()) {
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            btConnect.setChecked(false);
            btConnect.setEnabled(false);
        } else {
            btService.stop();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    btService.connect(device);
                } else {
                    btConnect.setChecked(false);
                    btConnect.setEnabled(true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    btService = new BluetoothService(this, mHandler);
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, R.string.btNotEnabledFinish, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        FullscreenUI();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (btService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (btService.getState() == BluetoothService.STATE_CONNECTED) {
                btConnect.setChecked(true);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth services
        if (btService != null) btService.stop();
    }

    public void FullscreenUI() {
        int newUiOptions = 0;

        // Navigation bar hiding:  Backwards compatible to ICS.
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        // Immersive mode: Backward compatible to KitKat.
        if (Build.VERSION.SDK_INT >= 19) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        if (Build.VERSION.SDK_INT >= 11) {
            this.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
        }
    }
}
