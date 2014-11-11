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
import cc.openframeworks.exampleICubeXAndroid.R;


public class OFActivity extends cc.openframeworks.OFActivity{
	
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
	private void post(final String msg) {
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

	private Runnable runnable = new Runnable() {
		//this class deals with generating periodic events,
		// to emulate the incoming "midi messages" from device
		// at a given rate
		@Override
		public void run() {
			Random r = new Random();
			byte r_b = (byte) r.nextInt(127);
			//what looks like sensor data here:
			// sensor 0, value = random (0-127)
			byte data[] = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte)0x00,
					(byte) 0x00, (byte) r_b, (byte) 0xF7};
			OFAndroid.passArray(data);

			jni_calls++;
			if (clickCount % 2 == 1) {
				//repeat if we have odd clickcount
				// silly way to trigger sending of dummy data...
				handler.postDelayed(runnable, SAMPLE_INTERVAL_MS);
			}
			// display call count once in a while...
			if (jni_calls % 1000 == 0) {
				String msg = "JniCnt=" + jni_calls;
				Toast t = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
				t.setGravity(Gravity.TOP|Gravity.CENTER, 0, 0);
				t.show();
				Log.v("JNI_CNT", msg);
			}
		}
	};

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

		//we send dummy data to make interface think sensor 0 is on:
		//TODO: will be replaced with actual sysex message!
        //PORT IS NOT OPEN YET!!!
		//byte data[] = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte)0x00,
		//		(byte) 0x01, (byte) 0x40, (byte) 0xF7};
		//OFAndroid.passArray(data);

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
			// use this to send an stream echo message
			//byte data[] = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte)0x00,
			//		(byte) 0x01, (byte) 0x40, (byte) 0xF7};
			//OFAndroid.passArray(data);
			
			//host mode
    		byte sysex_cmd[] = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x5A, (byte) 0x00, (byte) 0xF7 };
    		myBtMidi.getMidiOut().beginBlock();

    		for (int i=0; i<sysex_cmd.length; i++) {
    			myBtMidi.getMidiOut().onRawByte(sysex_cmd[i]);
    		}
    		myBtMidi.getMidiOut().endBlock();
			
			post("sending stream echo for sensor 0");
			
			//set interval to 20 (0x14) ms
			sysex_cmd = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x03, (byte) 0x00,(byte) 0x14, (byte) 0xF7 };
    		myBtMidi.getMidiOut().beginBlock();
    		for (int i=0; i<sysex_cmd.length; i++) {
    			myBtMidi.getMidiOut().onRawByte(sysex_cmd[i]);
    		}
    		myBtMidi.getMidiOut().endBlock();
    		//start stream port 0 (0x40 == on, port 0)
    		sysex_cmd = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x01, (byte) 0x40, (byte) 0xF7 };
    		myBtMidi.getMidiOut().beginBlock();
    		for (int i=0; i<sysex_cmd.length; i++) {
    			myBtMidi.getMidiOut().onRawByte(sysex_cmd[i]);
    		}
    		myBtMidi.getMidiOut().endBlock();
    		    		
    		//start stream port 7 (0x47 == on, port 7)
    		sysex_cmd = new byte[] { (byte) 0xF0, (byte) 0x7D, (byte) 0x00, (byte) 0x01, (byte) 0x47, (byte) 0xF7 };
    		myBtMidi.getMidiOut().beginBlock();
    		for (int i=0; i<sysex_cmd.length; i++) {
    			myBtMidi.getMidiOut().onRawByte(sysex_cmd[i]);
    		}
    		myBtMidi.getMidiOut().endBlock();
			
		}

		//give oF a chance
		// This passes the menu option string to OF
		// you can add additional behavior from java modifying this method
		// but keep the call to OFAndroid so OF is notified of menu events
		if(OFAndroid.menuItemSelected(item.getItemId())) {
			String msg = "back from C++ code!";
			post(msg);
		}
		//byte data[] = new byte[] { (byte) 0xF0, (byte) 0xFF, (byte)0x0A};
		//data[2]+=clickCount;
		//OFAndroid.passArray(data);
		OFAndroid.onCustom();
		OFAndroid.passInt(clickCount);
		if (clickCount % 2 == 1) {
			//handler.postDelayed(runnable, SAMPLE_INTERVAL_MS);
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

			@Override
			public void onSystemExclusive(byte[] sysex) {
				// TODO Auto-generated method stub
				StringBuilder sb = new StringBuilder();
			    for (byte b : sysex) {
			        sb.append(String.format("%02X ", b));
			    }
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

}



