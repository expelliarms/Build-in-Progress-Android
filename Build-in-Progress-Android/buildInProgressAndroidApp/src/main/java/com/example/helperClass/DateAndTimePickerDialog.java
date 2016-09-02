/*
package com.example.helperClass;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import net.simonvt.datepicker.DatePicker;
import net.simonvt.timepicker.TimePicker;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import R;

public class DateAndTimePickerDialog extends Dialog {

	private String stepURL;
	
	*/
/**
	 * Constructor for date and time picker dialog to update the step
	 * @param context activity that called the step
	 * @param prevStep previous step or null if doesn't exists
	 * @param nextStep next step or null if doesn't exists
	 * @param changeStep step to update
	 * @param auth_token string for the auth_token
	 * @param projectID project number for the step
	 * @param setLimits boolean of whether the step needs to have a lower and upper bound
	 *//*

	public DateAndTimePickerDialog
	(final Context context, ParcelStep prevStep, ParcelStep nextStep, final ParcelStep changeStep,
			final String auth_token, final int projectID, final boolean setLimits) {
		super(context, R.style.publish_theme);
		stepURL = Constant.STEP_URL+projectID+"/steps/"+changeStep.getPosition();

		final ParcelStep finalNext;
		final ParcelStep finalPrev;

		final LayoutInflater inflater = ((Activity)context).getLayoutInflater();

		final View setStepPublish = inflater.inflate(
				R.layout.set_step_publish, null);
		this.setContentView(setStepPublish);

		this.getWindow().setGravity(Gravity.CENTER_HORIZONTAL);

		//Getting dialog window buttons
		TextView stepName = (TextView)this.findViewById(R.id.step_publish_step_name);
		Button save = (Button)this.findViewById(R.id.publish_step_save);
		Button cancel = (Button)this.findViewById(R.id.publish_step_cancel);
		RadioGroup dateAndTime = (RadioGroup)this.findViewById(R.id.date_and_time);
		final LinearLayout dateTimeHolder = (LinearLayout)this.findViewById(R.id.time_pickerholder);
		TextView prevStepInfo = (TextView)this.findViewById(R.id.prev_step_date);
		TextView nextStepInfo = (TextView)this.findViewById(R.id.next_step_date);

		stepName.setText(changeStep.getName()+" published on");

		DateFormat formatter = new SimpleDateFormat("MMM dd 'at' h:mm aaa", Locale.US);

		if(prevStep!=null){
			prevStepInfo.setVisibility(View.VISIBLE);
			String text = "<b> <font color=#33b5e5>Previous step:</font> "+prevStep.getName()+"</b>";
			String formattedDate = formatter.format(prevStep.getPublishedDate());
			String allText = text+" <br> published on "+formattedDate;
			prevStepInfo.setText(Html.fromHtml(allText));
		}
		if(nextStep!=null){
			nextStepInfo.setVisibility(View.VISIBLE);
			String formattedDate = formatter.format(nextStep.getPublishedDate());
			String text = "<b> <font color=#33b5e5>Next step:</font> "+nextStep.getName()+"</b>";
			String allText = text+" <br> published on "+formattedDate;
			nextStepInfo.setText(Html.fromHtml(allText));
		}


		//Inflating the views for the date and time pickers to be used when setting date and time
		final View setDate = inflater.inflate(R.layout.date_picker_test, null);
		final DatePicker datePick = (DatePicker)setDate.findViewById(R.id.datePicker_test);
		datePick.setCalendarViewShown(false);

		final View setTime = inflater.inflate(R.layout.time_picker_test, null);
		final TimePicker timePick = (TimePicker)setTime.findViewById(R.id.timePicker_test);


		Calendar minDate = null;
		Calendar maxDate = null;

		maxDate = Calendar.getInstance();
		datePick.setMaxDate(maxDate.getTime().getTime());

		*/
/*
		 * When moving a step, only allow the dates set to be within the
		 * date of the previous step and the date of the next step
		 *//*

		if(setLimits){
			if(prevStep != null){
				if(prevStep.getPosition()!=0){
					minDate = Calendar.getInstance();
					minDate.setTime(prevStep.getPublishedDate());
					datePick.setMinDate(minDate.getTime().getTime());
				}
			}
			if(nextStep != null){
				maxDate = Calendar.getInstance();
				maxDate.setTime(nextStep.getPublishedDate());
				datePick.setMaxDate(maxDate.getTime().getTime());
			}
		}


		if(minDate!=null && maxDate!=null){
			int minYear = minDate.get(Calendar.YEAR);
			int minMonth = minDate.get(Calendar.MONTH);
			int minDay = minDate.get(Calendar.DAY_OF_MONTH);
			int minHour = minDate.get(Calendar.HOUR_OF_DAY);
			int minMinute = minDate.get(Calendar.MINUTE);
			int maxYear = maxDate.get(Calendar.YEAR);
			int maxMonth = maxDate.get(Calendar.MONTH);
			int maxDay = maxDate.get(Calendar.DAY_OF_MONTH);
			int maxHour = maxDate.get(Calendar.HOUR_OF_DAY);
			int maxMinute = maxDate.get(Calendar.MINUTE);
			boolean dateMatch = (minYear==maxYear)&&(minMonth==maxMonth)&&(minDay==maxDay);
			boolean timeMatch = (minHour==maxHour)&&(Math.abs(minMinute-maxMinute)<2);
			if(dateMatch){
				if(timeMatch){
					Toast.makeText(context, "Previous step date and next step date " +
							"too close, please change individual step's published date"
							,  Toast.LENGTH_LONG).show();
					return;
				}
				// set the default date to one minute after the minimum date;
				long minTime = minDate.getTimeInMillis()+60000;
				// long averagedDates = (maxDate.getTime().getTime() + minDate.getTime().getTime())/2;
				Calendar newDate = Calendar.getInstance();
				newDate.setTime(new Date(minTime));
				timePick.setCurrentHour(newDate.get(Calendar.HOUR_OF_DAY));
				timePick.setCurrentMinute(newDate.get(Calendar.MINUTE));

			}


		}	        	

		//Default to the setTime view
		dateTimeHolder.addView(setTime);
		//Step information to be used in the checked change listener
		finalNext = nextStep;
		finalPrev = prevStep;
		//Changing the view for setting time and dates
		dateAndTime.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(RadioGroup radG, int checkId) {
				//HAVE TO REMOVE VIEWS FROM PARENT BEFORE ADDING THEM AGAIN
				if(setTime.getParent()!= null){
					((ViewGroup)setTime.getParent()).removeView(setTime);
				}
				if(setDate.getParent() != null){
					((ViewGroup)setDate.getParent()).removeView(setDate);
				}
				//Removing previous views from the dateTimeSelect and adding set date or set time
				if(checkId == R.id.set_step_date){
					dateTimeHolder.removeAllViews();
					dateTimeHolder.addView(setDate);
				} else {

					int month = datePick.getMonth();
					int year = datePick.getYear();
					int day = datePick.getDayOfMonth();

					//Information about the next step and previous step
					int nextStepYear;
					int nextStepMonth;
					int nextStepDay;
					int prevStepYear;
					int prevStepMonth;
					int prevStepDay;
					
					//Setting max time for time picker when date is set to next step's date
					if(finalNext!=null && finalPrev!= null && finalPrev.getPosition()!=0){
						//Setting dates for both next and previous step
						final Calendar nextCal = Calendar.getInstance();
						nextCal.setTime(finalNext.getPublishedDate());
						nextStepYear = nextCal.get(Calendar.YEAR);
						nextStepMonth = nextCal.get(Calendar.MONTH);
						nextStepDay = nextCal.get(Calendar.DAY_OF_MONTH);

						final Calendar prevCal = Calendar.getInstance();
						prevCal.setTime(finalPrev.getPublishedDate());
						prevStepYear = prevCal.get(Calendar.YEAR);
						prevStepMonth = prevCal.get(Calendar.MONTH);
						prevStepDay = prevCal.get(Calendar.DAY_OF_MONTH);

						int nexthour = nextCal.get(Calendar.HOUR_OF_DAY);
						int nextminute = nextCal.get(Calendar.MINUTE);
						int prevhour = prevCal.get(Calendar.HOUR_OF_DAY);
						int prevminute = prevCal.get(Calendar.MINUTE);

						boolean matchesNext = (day==nextStepDay && month==nextStepMonth && year==nextStepYear);
						boolean matchesPrev = (day==prevStepDay && month==prevStepMonth && year==prevStepYear);

						if(matchesNext && matchesPrev){
							timePick.setCurrentHour(prevhour);
							timePick.setCurrentMinute(prevminute+1);
							timePick.setOnTimeChangedListener(
									new BoundTimeChangeListener(timePick, nexthour, nextminute, prevhour, prevminute));
						}else if(matchesNext){
							timePick.setCurrentHour(nexthour);
							timePick.setCurrentMinute(nextminute-1);
							timePick.setOnTimeChangedListener(
									new BoundTimeChangeListener(timePick, true, nexthour, nextminute));
						}else if(matchesPrev){
							timePick.setCurrentHour(prevhour);
							timePick.setCurrentMinute(prevminute+1);
							timePick.setOnTimeChangedListener(
									new BoundTimeChangeListener(timePick, false, prevhour, prevminute));
						}else{
							timePick.setOnTimeChangedListener(null);
						}
					}else if (finalNext!=null){
						final Calendar nextCal = Calendar.getInstance();
						nextCal.setTime(finalNext.getPublishedDate());
						nextStepYear = nextCal.get(Calendar.YEAR);
						nextStepMonth = nextCal.get(Calendar.MONTH);
						nextStepDay = nextCal.get(Calendar.DAY_OF_MONTH);
						int nexthour = nextCal.get(Calendar.HOUR_OF_DAY);
						int nextminute = nextCal.get(Calendar.MINUTE);
						boolean matchesNext = (day==nextStepDay && month==nextStepMonth && year==nextStepYear);

						if(matchesNext){
							timePick.setOnTimeChangedListener(
									new BoundTimeChangeListener(timePick, true, nexthour, nextminute));
						}else{
							timePick.setOnTimeChangedListener(null);
						}

					}else if (finalPrev!=null && finalPrev.getPosition()!=0){
						final Calendar prevCal = Calendar.getInstance();
						prevCal.setTime(finalPrev.getPublishedDate());
						prevStepYear = prevCal.get(Calendar.YEAR);
						prevStepMonth = prevCal.get(Calendar.MONTH);
						prevStepDay = prevCal.get(Calendar.DAY_OF_MONTH);

						int prevhour = prevCal.get(Calendar.HOUR_OF_DAY);
						int prevminute = prevCal.get(Calendar.MINUTE);

						boolean matchesPrev = (day==prevStepDay && month==prevStepMonth && year==prevStepYear);

						if(matchesPrev){
							timePick.setOnTimeChangedListener(
									new BoundTimeChangeListener(timePick, false, prevhour, prevminute));
						}else{
							timePick.setOnTimeChangedListener(null);
						}

					}else{
						timePick.setOnTimeChangedListener(null);
					}
					dateTimeHolder.removeAllViews();
					dateTimeHolder.addView(setTime);
				}
			}

		});

		//Save on click listener for setting date
		save.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				int year = datePick.getYear();
				int month = datePick.getMonth()+1;
				int day = datePick.getDayOfMonth();
				int hour = timePick.getCurrentHour();
				int minute = timePick.getCurrentMinute();

				Date newPublish = new GregorianCalendar(year, month, day, hour, minute).getTime();
				//Updating the step's publish date
				changeStep.setPublishDate(newPublish);
				String stepPublishedOnDate = String.format(Locale.getDefault(), "%02d/%02d/%d", month, day, year);

				new Method.UpdateStepDateTask(context).execute(stepURL, auth_token, stepPublishedOnDate, hour, minute);

				DateAndTimePickerDialog.this.dismiss();

			}			
		});

		//Cancel on click listener
		cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DateAndTimePickerDialog.this.dismiss();
			}
		});

		//Displaying the dialog
		this.show();
	}
}
*/
