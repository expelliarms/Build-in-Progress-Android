package com.example.helperClass;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

public class Timer{
	
	private int seconds;
	private int minutes;
	private AsyncTask<Void, Void, Void> timer;
	private Handler updater = new Handler();
	private boolean running = true;
	

	public Timer(final TextView timerView) {
		timer = new AsyncTask<Void, Void, Void>(){

			@Override
			protected Void doInBackground(Void... arg0) {
				while(running){
					Log.d("TIMER", "time is ticking");
					//Update the view
					updater.post(new Runnable(){
						@Override
						public void run() {
							String time = String.format("%02d:%02d", minutes, seconds);
							timerView.setText(time);
						}
					});
					SystemClock.sleep(1000);
					seconds+=1;
					if(seconds>59){
						minutes+=1;
						seconds = 0;
					}
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result){
				//Resetting the timerView whenever the timer is stopped
				timerView.setText("00:00");
			}
		};
	}
	
	public void startTimer(){
		timer.execute();
	}
	
	public void stopTimer(){
		running = false;
		seconds = 0;
	}
	
	public int getTime(){
		return seconds+60*minutes;
	}

}
