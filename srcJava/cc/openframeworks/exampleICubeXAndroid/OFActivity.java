package cc.openframeworks.exampleICubeXAndroid;

import java.io.IOException;
import java.util.Random;

import com.noisepages.nettoyeur.bluetooth.BluetoothSppConnection;
import com.noisepages.nettoyeur.bluetooth.BluetoothSppObserver;
import com.noisepages.nettoyeur.bluetooth.midi.BluetoothMidiDevice;
import com.noisepages.nettoyeur.bluetooth.util.DeviceListActivity;
import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.midi.util.SystemMessageDecoder;
import com.noisepages.nettoyeur.midi.util.SystemMessageReceiver;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;
import cc.openframeworks.OFAndroid;
import cc.openframeworks.OFAndroid.*;
import cc.openframeworks.OFAndroidMidiBridge;
import cc.openframeworks.exampleICubeXAndroid.R;


public class OFActivity extends cc.openframeworks.OFActivity implements cc.openframeworks.OFCustomListener {

	public OFAndroidMidiBridge midiBridge;
	private BluetoothMidiDevice myBtMidi = null;
	private SystemMessageDecoder mySysExDecoder;
	private Toast toast;
	private static final String TAG = "ICubeXTest";
	private static final int CONNECT = 1;

	//these are for testing/dummy data:
	private long jni_calls = 0;
	private int clickCount = 1;
	private Handler handler = new Handler();
	private final int SAMPLE_INTERVAL_MS = 25; // dummy data generation interval in ms
	private final int SAMPLE_DATA_SIZE = 8; //dummy data length


	//simple helper method for data display+logging to LogCat
	public void post(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.v(TAG, msg);
				if (toast == null) {
					toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
				}
				toast.setText(TAG + ": " + msg);
				toast.show();
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{ 
		super.onCreate(savedInstanceState);
		String packageName = getPackageName();

		ofApp = new OFAndroid(packageName,this);

		//init midi objects

		try {
			myBtMidi = new BluetoothMidiDevice(observer, receiver);
		}
		catch (IOException e) {
			post("MIDI not available!");
			finish();
		}

		//requestWindowFeature(Window.FEATURE_NO_TITLE);


		mySysExDecoder = new SystemMessageDecoder(midiSysExReceiver);

		//set up midi bridge
		midiBridge = new OFAndroidMidiBridge();
		midiBridge.addCustomListener((cc.openframeworks.OFCustomListener)this);

	}

	@Override
	public void onDetachedFromWindow() {
	}

	@Override
	protected void onPause() {
		if (myBtMidi != null)
			myBtMidi.close();
		super.onPause();
		ofApp.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		ofApp.resume();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (OFAndroid.keyDown(keyCode, event)) {
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (OFAndroid.keyUp(keyCode, event)) {
			return true;
		} else {
			return super.onKeyUp(keyCode, event);
		}
	}


	OFAndroid ofApp;

	// Menus
	// http://developer.android.com/guide/topics/ui/menus.html
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Create settings menu options from here, one by one or infalting an xml
		MenuInflater inflator = getMenuInflater();
		inflator.inflate(R.menu.main_layout, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		//handle within java
		if (item.getItemId() == R.id.menu_conn_bt) {
			if (myBtMidi.getConnectionState() == BluetoothSppConnection.State.NONE) {
				startActivityForResult(new Intent(this, DeviceListActivity.class), CONNECT);
			} else {
				myBtMidi.close();
			}

		}
		if (item.getItemId() == R.id.menu_settings) {
		}

		//give oF a chance: pass to ofApp (C++ code)
		if(OFAndroid.menuItemSelected(item.getItemId())) {
		}

		return super.onOptionsItemSelected(item);
	}


	@Override
	public boolean onPrepareOptionsMenu (Menu menu){
		// This method is called every time the menu is opened
		//  you can add or remove menu options from here
		return  super.onPrepareOptionsMenu(menu);
	}

	//BT observer class for showing connection status
	private final BluetoothSppObserver observer = new BluetoothSppObserver() {
		@Override
		public void onDeviceConnected(BluetoothDevice device) {
			post("device connected: " + device);
		}

		@Override
		public void onConnectionLost() {
			post("connection lost");
		}

		@Override
		public void onConnectionFailed() {
			post("connection failed");
		}

	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case CONNECT:
			if (resultCode == Activity.RESULT_OK) {
				String address = data.getExtras().getString(DeviceListActivity.DEVICE_ADDRESS);
				try {
					myBtMidi.connect(address);

				} catch (IOException e) {
					post(e.getMessage());
				}
			}
			break;
		}
	}
	//sys ex receiver
	private final SystemMessageReceiver midiSysExReceiver = new SystemMessageReceiver() {

		//This is the function that passes the received sysex data to
		// the C++ code via JNI!
		@Override
		public void onSystemExclusive(byte[] sysex) {
			//StringBuilder sb = new StringBuilder();
			//for (byte b : sysex) {
			//    sb.append(String.format("%02X ", b));
			//}
			//Log.v("sysex: ", sb.toString());
			OFAndroid.passArray(sysex);
			//Log.v("USBMIDI", "sysex");
		}

		@Override
		public void onTimeCode(int value) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onSongPosition(int pointer) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onSongSelect(int index) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onTuneRequest() {
			// TODO Auto-generated method stub
		}

		@Override
		public void onTimingClock() {
			// TODO Auto-generated method stub
		}

		@Override
		public void onStart() {
			// TODO Auto-generated method stub
		}

		@Override
		public void onContinue() {
			// TODO Auto-generated method stub
		}

		@Override
		public void onStop() {
			// TODO Auto-generated method stub
		}

		@Override
		public void onActiveSensing() {
			// TODO Auto-generated method stub
		}

		@Override
		public void onSystemReset() {
			// TODO Auto-generated method stub
		}

	};

	//BT Receiver Port

	private final MidiReceiver receiver = new MidiReceiver() {

		@Override
		public void onNoteOff(int channel, int key, int velocity) {
			post("note off: " + channel + ", " + key + ", " + velocity);
		}

		@Override
		public void onNoteOn(int channel, int key, int velocity) {
			post("note on: " + channel + ", " + key + ", " + velocity);
		}

		@Override
		public void onAftertouch(int channel, int velocity) {
			post("aftertouch: " + channel + ", " + velocity);
		}

		@Override
		public void onControlChange(int channel, int controller, int value) {
			post("control change: " + channel + ", " + controller + ", " + value);
		}

		@Override
		public void onPitchBend(int channel, int value) {
			post("pitch bend: " + channel + ", " + value);
		}

		@Override
		public void onPolyAftertouch(int channel, int key, int velocity) {
			post("polyphonic aftertouch: " + channel + ", " + key + ", " + velocity);
		}

		@Override
		public void onProgramChange(int channel, int program) {
			post("program change: " + channel + ", " + program);
		}

		@Override
		public void onRawByte(byte value) {
			if (mySysExDecoder != null) {
				mySysExDecoder.decodeByte(value);
				//Integer v = (int)value;
				//Log.v("rawbyte", v.toString());
			}
		}

		@Override
		public boolean beginBlock() {
			return false;
		}

		@Override
		public void endBlock() {}
	};


	@Override
	public void onEvent(byte[] data) {
		// calling from the custom listener
		StringBuilder sb = new StringBuilder();
		for (byte b : data) {
			sb.append(String.format("%02X ", b));
		}
		post("data from ofxICubeX: " + sb.toString());
		if (myBtMidi!=null) {
			myBtMidi.getMidiOut().beginBlock();

			for (byte b : data) {
				myBtMidi.getMidiOut().onRawByte(b);
			}
			myBtMidi.getMidiOut().endBlock();
		}
	}
}



