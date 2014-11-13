package cc.openframeworks.exampleICubeXAndroid;

import java.io.IOException;
import java.util.List;

import com.noisepages.nettoyeur.bluetooth.BluetoothSppConnection;
import com.noisepages.nettoyeur.bluetooth.BluetoothSppObserver;
import com.noisepages.nettoyeur.bluetooth.midi.BluetoothMidiDevice;
import com.noisepages.nettoyeur.bluetooth.util.DeviceListActivity;
import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.midi.util.SystemMessageDecoder;
import com.noisepages.nettoyeur.midi.util.SystemMessageReceiver;
import com.noisepages.nettoyeur.usb.ConnectionFailedException;
import com.noisepages.nettoyeur.usb.DeviceNotConnectedException;
import com.noisepages.nettoyeur.usb.InterfaceNotAvailableException;
import com.noisepages.nettoyeur.usb.UsbBroadcastHandler;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice.UsbMidiInput;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice.UsbMidiOutput;
import com.noisepages.nettoyeur.usb.midi.util.UsbMidiInputSelector;
import com.noisepages.nettoyeur.usb.midi.util.UsbMidiOutputSelector;
import com.noisepages.nettoyeur.usb.util.AsyncDeviceInfoLookup;
import com.noisepages.nettoyeur.usb.util.UsbDeviceSelector;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import cc.openframeworks.OFAndroid;
import cc.openframeworks.OFAndroidMidiBridge;
import cc.openframeworks.exampleICubeXAndroid.R;


public class OFActivity extends cc.openframeworks.OFActivity implements cc.openframeworks.OFCustomListener {



	public OFAndroidMidiBridge midiBridge;
	//AndroidMIDI interfaces:
	//BT:
	private BluetoothMidiDevice myBtMidi = null;

	//USB:
	private UsbMidiDevice myUSBMidi = null;
	private MidiReceiver myUSBMidiOut = null;

	//MISC:
	private SystemMessageDecoder mySysExDecoder;

	//NOTE: Midi receiver is declared below
	
	private Toast toast;
	private static final String TAG = "ICubeXTest";
	private static final int CONNECT = 1;

	//these are for testing/dummy data:
	private long jni_calls = 0;


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
			myBtMidi = new BluetoothMidiDevice(observer, myMidiReceiver);
		}
		catch (IOException e) {
			post("MIDI not available!");
			finish();
		}
		installBHandler();


		//requestWindowFeature(Window.FEATURE_NO_TITLE);


		mySysExDecoder = new SystemMessageDecoder(midiSysExReceiver);

		//set up midi bridge
		midiBridge = new OFAndroidMidiBridge();
		midiBridge.addCustomListener((cc.openframeworks.OFCustomListener)this);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (myUSBMidi != null) {
			myUSBMidi.close();
		}
		UsbMidiDevice.uninstallBroadcastHandler(this);
	}

	@Override
	public void onDetachedFromWindow() {
	}

	@Override
	protected void onPause() {
		//close midi ports.
		if (myBtMidi != null) {
			myBtMidi.close();			
		}
		if (myUSBMidi != null) {
			myUSBMidi.close();
			myUSBMidi = null;
		}
		super.onPause();
		ofApp.pause();
		UsbMidiDevice.uninstallBroadcastHandler(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		ofApp.resume();
		installBHandler();
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
			//first, disconnect USB
			if (myUSBMidi != null) {
				Log.v("ICubeX", "closing USB for BT");
				myUSBMidi.close();
				myUSBMidi = null;
			}
			if (myBtMidi.getConnectionState() == BluetoothSppConnection.State.NONE) {
				startActivityForResult(new Intent(this, DeviceListActivity.class), CONNECT);
			} else {
				myBtMidi.close();
			}

		}
		if (item.getItemId() == R.id.menu_conn_usb) {
			//first, lets disconnect BT if its on:
			if (myBtMidi.getConnectionState() == BluetoothSppConnection.State.CONNECTED) {
				Log.v("ICubeX", "closing BT for USB");
				myBtMidi.close();
			}
			if (myUSBMidi == null) {
				Log.v("ICubeX", "select MIDI");
				chooseMidiDevice();
			}
			else {
				myUSBMidi.close();
				myUSBMidi = null;
				Log.v("ICubeX", "close MIDI");
				post("USB MIDI closed");
			}
				
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
			StringBuilder sb = new StringBuilder();
			for (byte b : sysex) {
			    sb.append(String.format("%02X ", b));
			}
			Log.v("sysex: ", sb.toString());
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

	//Same Receiver port for either BT or USB

	private final MidiReceiver myMidiReceiver = new MidiReceiver() {

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
		// This is incoming sysex data from oF C++ App
		// to be sent to the MIDI output
		StringBuilder sb = new StringBuilder();
		for (byte b : data) {
			sb.append(String.format("%02X ", b));
		}
		post("data from ofxICubeX: " + sb.toString());
		if (myBtMidi!=null && myBtMidi.getConnectionState() == BluetoothSppConnection.State.CONNECTED) {
			myBtMidi.getMidiOut().beginBlock();

			for (byte b : data) {
				myBtMidi.getMidiOut().onRawByte(b);
			}
			myBtMidi.getMidiOut().endBlock();
		}
		
		if (myUSBMidi!=null) {
			myUSBMidiOut.beginBlock();
			for (byte b : data) {
				myUSBMidiOut.onRawByte(b);
			}
			myUSBMidiOut.endBlock();
		}
	}

	private void chooseMidiDevice() {
		final List<UsbMidiDevice> devices = UsbMidiDevice.getMidiDevices(this);
		new AsyncDeviceInfoLookup() {

			@Override
			protected void onLookupComplete() {
				new UsbDeviceSelector<UsbMidiDevice>(devices) {

					@Override
					protected void onDeviceSelected(UsbMidiDevice device) {
						myUSBMidi = device;
						post("Selected device: " + device.getCurrentDeviceInfo());
						myUSBMidi.requestPermission(OFActivity.this);

						UsbMidiOutputSelector outputSelector = new UsbMidiOutputSelector(myUSBMidi) {

							@Override
							protected void onOutputSelected(UsbMidiOutput output, UsbMidiDevice device, int iface,
									int index) {
								post("Output selection: Interface " + iface + ", Output " + index);
								try {
									myUSBMidiOut = output.getMidiOut();
								} catch (DeviceNotConnectedException e) {
									post("MIDI device has been disconnected");
								} catch (InterfaceNotAvailableException e) {
									post("MIDI interface is unavailable");
								}
							}

							@Override
							protected void onNoSelection(UsbMidiDevice device) {
								post("No output selected");
							}
						};

						outputSelector.show(getFragmentManager(), "");

					}

					@Override
					protected void onNoSelection() {
						post("No USB MIDI device selected.");
					}
				}.show(getFragmentManager(), null);

			}
		}.execute(devices.toArray(new UsbMidiDevice[devices.size()]));
	}

	private void installBHandler() { 

		UsbMidiDevice.installBroadcastHandler(this, new UsbBroadcastHandler() {

			@Override
			public void onPermissionGranted(UsbDevice device) {
				if (myUSBMidi == null || !myUSBMidi.matches(device)) return;
				try {
					myUSBMidi.open(OFActivity.this);
				} catch (ConnectionFailedException e1) {
					post("\n\nConnection failed.");
					myUSBMidi = null;
					return;
				}
				new UsbMidiInputSelector(myUSBMidi) {

					@Override
					protected void onInputSelected(UsbMidiInput input, UsbMidiDevice device, int iface,
							int index) {
						post("\n\nInput: Interface " + iface + ", Index " + index);
						input.setReceiver(myMidiReceiver);
						try {
							input.start();
						} catch (DeviceNotConnectedException e) {
							post("MIDI device has been disconnected.");
						} catch (InterfaceNotAvailableException e) {
							post("\n\nMIDI interface is unavailable.");
						}
					}

					@Override
					protected void onNoSelection(UsbMidiDevice device) {
						post("\n\nNo inputs available.");
					}
				}.show(getFragmentManager(), null);
			}

			@Override
			public void onPermissionDenied(UsbDevice device) {
				if (myUSBMidi == null || !myUSBMidi.matches(device)) return;
				post("Permission denied for device " + myUSBMidi.getCurrentDeviceInfo() + ".");
				myUSBMidi = null;
			}
			@Override
			public void onDeviceDetached(UsbDevice device) {
				if (myUSBMidi == null || !myUSBMidi.matches(device)) return;
				myUSBMidi.close();
				myUSBMidi = null;
				post("USB MIDI device detached.");
			}
		});
	}
}



