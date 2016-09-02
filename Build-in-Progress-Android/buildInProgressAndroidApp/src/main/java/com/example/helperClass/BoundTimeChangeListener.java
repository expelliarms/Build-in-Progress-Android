/*
package com.example.helperClass;

import net.simonvt.timepicker.TimePicker;
import net.simonvt.timepicker.TimePicker.OnTimeChangedListener;

public class BoundTimeChangeListener implements OnTimeChangedListener {
	
	//Booleans to keep track of whether we want to upperbound or lowerbound time
	boolean upperbound;
	boolean lowerbound;
	
	int maxHour;
	int maxMinute;
	int minHour;
	int minMinute;
	 
	TimePicker timePick;
	
	*/
/**
	 * Constructor when needed to both upper and lower bound time
	 * @param maxHour
	 * @param maxMinute
	 * @param minHour
	 * @param minMinute
	 *//*

	public BoundTimeChangeListener(TimePicker timePick, int maxHour, int maxMinute, int minHour, int minMinute){
		this.upperbound = true;
		this.lowerbound = true;
		this.maxHour = maxHour;
		this.maxMinute = maxMinute;
		this.minHour = minHour;
		this.minMinute = minMinute;
		this.timePick = timePick;
	}
	
	*/
/**
	 * Constructor setting either the max or min time to be bound
	 * @param max boolean representing whether to be upper bound
	 * @param hour min or max hour, depending on max boolean
	 * @param minute min or max minute, depending on max boolean
	 *//*

	public BoundTimeChangeListener(TimePicker timePick ,boolean upperbound, int hour, int minute){
		this.timePick = timePick;
		this.upperbound = upperbound;
		this.lowerbound = !upperbound;
		if(upperbound){
			this.maxHour = hour;
			this.maxMinute = minute;
		}else{
			this.minHour = hour;
			this.minMinute = minute;
		}
	}
	@Override
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		boolean over = false;
		boolean under = false;
		int currentHour = 0;
		int currentMinute = 0;
		boolean validTime = true;
		if(upperbound){
			if (hourOfDay  > maxHour || (hourOfDay == maxHour && minute >= maxMinute)){
				over = true;				
		        validTime = false;
		    }
		}
		if (lowerbound){
			if (hourOfDay < minHour || (hourOfDay == minHour && minute <= minMinute)){
				under = true;
		        validTime = false;
		    }
		}
		
		if (validTime) {
	        currentHour = hourOfDay;
	        currentMinute = minute;
	    }else{
	    	if(over){
	    		currentHour = maxHour;
	    		currentMinute = maxMinute -1;
	    		if(currentMinute <0){
	    			currentHour = (currentHour-1)%24;
	    			currentMinute =0;
	    		}
	    	}else if(under){
	    		currentHour = minHour;
	    		currentMinute = minMinute +1;
	    		if(currentMinute>59){
	    			currentHour = (currentHour+1)%24;
	    			currentMinute = 0;
	    		}
	    	}
	    }
		
		timePick.setCurrentHour(currentHour);
		timePick.setCurrentMinute(currentMinute);
	}

}
*/
