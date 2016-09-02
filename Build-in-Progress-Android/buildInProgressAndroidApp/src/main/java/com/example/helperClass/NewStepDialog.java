package com.example.helperClass;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NewStepDialog extends Dialog {

	private String PROJECT_STEPS_URL;

	private int lastStepId;
	private String auth_token;

	//View
	private EditText newStepEditText;

	public NewStepDialog(Context context, int projectID, int lastStepId, String authToken) {
		super(context, com.bip_public_android.R.style.publish_theme);
		setContentView(com.bip_public_android.R.layout.new_step_dialog);

		this.lastStepId = lastStepId;
		this.auth_token = authToken;

		PROJECT_STEPS_URL = Constant.PROJECT_URL+projectID+"/steps";

		setUpViews();
	}

	private void setUpViews(){
		
		newStepEditText = (EditText) findViewById(com.bip_public_android.R.id.new_step_edit);
		Button save = (Button) findViewById(com.bip_public_android.R.id.new_step_ok);
		Button cancel = (Button) findViewById(com.bip_public_android.R.id.new_step_cancel);

		save.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (newStepEditText.getText().toString().length() > 0) {
					Method.AddStepTask newAddStep = new Method.AddStepTask(getContext(), lastStepId);
					newAddStep.execute(PROJECT_STEPS_URL+"?auth_token="+auth_token, newStepEditText.getText().toString());
					NewStepDialog.this.dismiss();
				}else{
					Toast.makeText(getContext(), "Add step name.", Toast.LENGTH_LONG).show();
				}

			}

		});

		cancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				NewStepDialog.this.dismiss();
			}
		});
	}

}
