#pragma once

#include "ofMain.h"
#include "ofxAndroid.h"
#include "ofxXmlSettings.h"
#include "ofxICubeX.h"

class ofApp : public ofxAndroidApp{
	
	public:
		
		void setup();
		void update();
		void draw();
		
		void keyPressed(int key);
		void keyReleased(int key);
		void windowResized(int w, int h);

		void touchDown(int x, int y, int id);
		void touchMoved(int x, int y, int id);
		void touchUp(int x, int y, int id);
		void touchDoubleTap(int x, int y, int id);
		void touchCancelled(int x, int y, int id);
		void swipe(ofxAndroidSwipeDir swipeDir, int id);

		bool menuItemSelected(char* menu_id);

		void pause();
		void stop();
		void resume();
		void reloadTextures();

		bool backPressed();
		void okPressed();
		void cancelPressed();

		void onCustom();
		void onArray(char* data, int len);
		void onInt(int i);

		string output_msg;
		string last_locked;
		unsigned long int jni_count;
		char* recv_data;
		int data_len;

		long last_count;

		ofMutex mylock;

		bool lockedout;

		ofxICubeX myICube;

		ofVideoPlayer myVidPlayer1;
		ofVideoPlayer myVidPlayer2;

		int curr_state;
		int prev_state;

		int sensor_val;
};
