package com.example.helperClass;

import android.view.animation.AlphaAnimation;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Constant {
	public static final String TAG = "Methods";
	public static final String IMAGE_INTENT = "image";
	public static final String PREVIEW_INTENT = "preview";
	public static final String GALLERY_INTENT = "gallery";
	public static final String PROJECT_INTENT = "Project";
	public static final String STEP_NAME = "stepName";
	public static final String STEP_DETAIL ="stepDetail";
	public static final String STEP_IMAGE ="stepImage";
	public static final String STEP_POSITION = "stepPosition";
	public static final String STEP_DELETE = "stepDelete";
	public static final String PROJECT_TITLE_INTENT = "projectTitle";
	public static final String IS_NEW_STEP = "isNewStep";
	public static final String IS_PROJECT_OVERVIEW = "projectOverView";
	public static final float UPLOADING_ALPHA = (float).5;
	public static final float UPLOADED_ALPHA = 1;
	public static final int REQUEST_CODE_ADD_STEP = 10;
	public static final int REQUEST_CODE_ADD_PIC = 11;
	public static final int REQUEST_CODE_PROJECT = 12;
	public static final int REQUEST_CODE_PREVIEW = 15;
	public static final int REQUEST_CODE_STEP = 16;
	public static final int REQUEST_CODE_DELETE_STEP = 17;
	public static final int REQUEST_CODE_EDITED_STEP = 19;
	public static final int NOTIFICATION_UPLOADING_ID = 99;
	public static final int NOTIFICATION_UPLOADED_ID = 100;

	public static final ArrayList<String> REGULAR_STEP_PROMPTS = new ArrayList<String>(){/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	{
		add("Did you face any interesting challenges at this step?"); 
		add("What materials did you use, and why did you chose them (sturdier, cheaper, etc.)?");
		add("What types of tools did you use?");
		add("Is there any advice you'd give to someone building this?");
	}};

	public static final ArrayList<String> BUILT_STEP_PROMPTS = new ArrayList<String>(){/**
 * 
 */
	private static final long serialVersionUID = 1L;

	{
		add("If you were to make this project again, what would you do differently?"); 
		add("What do you wish you knew at the start of this project?");
		add("What are ways other people could remix this project?");
		add("What skills would be necessary for someone making your project?");
	}};

	public static AtomicInteger currentlyUploading = new AtomicInteger(0);
	public static AtomicInteger totalUploadedImages = new AtomicInteger(0);

	public static String SITE_URL = ""; // http://your_bip_web_app.com/

	public final static String LOGIN_API_ENDPOINT_URL = SITE_URL + "sessions.json";
	public static final String TASKS_URL = SITE_URL + "tasks.json";
	public static final String LOGOUT_URL = SITE_URL + "sessions.json";

	public static final String USER_PROFILE_URL = SITE_URL + "users";
	public static final String NEW_PROJECT_URL = SITE_URL + "projects/new";
	public static final String PROJECT_URL = SITE_URL + "projects/";
	public static final String IMAGE_URL = SITE_URL + "images/";
	public static final String VIDEO_URL = SITE_URL + "videos/";
	public static final String STEP_URL = SITE_URL + "steps/";
	public static final String REGISTER_URL = SITE_URL + "users/";

	public static final String AWS_IMAGE_URL = ""; //https://your_aws_bucket_name.s3.amazonaws.com/image/image_path/

	public static AlphaAnimation uploadingAnimation = new AlphaAnimation(1, (float) .5);
	public static AlphaAnimation uploadedAnimation = new AlphaAnimation((float).5, 1);


}
