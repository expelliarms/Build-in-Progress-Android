package com.example.helperClass;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.bip_public_android.MediaPreview;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	
    private static final String TAG = "CameraPreview";
    private Camera mCamera;
	private SurfaceHolder mHolder;
	private MediaRecorder recorder;
	
	private Size mPreviewSize;
		
	/**Auto focus call back used whenever the preview is clicked*/
	private AutoFocusCallback focusCallback = new AutoFocusCallback() {
		
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			//TODO: Maybe add some extra stuff for when the camera focuses
//			if(!success){
//				Toast.makeText(getContext(), "Error focusing", Toast.LENGTH_SHORT).show();
//			}
		}
	};
	
	public CameraPreview(Context context) {
        super(context);
        Log.d(TAG, "custom camera!!!!!!!!");
        
        MediaPreview.log();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        
        //Setting auto focus feature on the 
        this.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(mCamera != null && event.getAction() == MotionEvent.ACTION_DOWN){
					mCamera.autoFocus(focusCallback);
//					placeboSquare = new HollowSquare(getContext(), event.getX(), event.getY());
				}
				return true;
			}        	
        });

    }
    
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "Destroying surface view");
		if(mCamera!=null){
			mCamera.stopPreview();
		}
		if(recorder != null){
			recorder.release();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	    Log.d(TAG, "surfaceChanged");
	    
	    if(mCamera!=null){
	    	Log.d(TAG, "camera not null, getting optimal preview size");
	        getOptimalPreviewSize(mCamera.getParameters().getSupportedPreviewSizes(), width, height);
	    }
	    Log.d(TAG, "setting camera display orientation");
		setCameraDisplayOrientation(holder);
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		setCameraDisplayOrientation(holder);
	}
    
    /**
     * Method used to keep to adjust the camera's display orientation according to
     * the phone's current orientation
     */
    private void setCameraDisplayOrientation(SurfaceHolder holder) {
    	if(mCamera != null){
			//Stopping preview for older version in order to set orientation
//			if(android.os.Build.VERSION.SDK_INT <14){
				mCamera.stopPreview();
//			}
				
				
			Log.d(TAG, "SurfaceChanged");
			try {
				Camera.Parameters parameters = mCamera.getParameters();
				if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					Log.d(TAG, "Setting orientation to portrait");
					// This is an undocumented although widely known feature
					parameters.set("orientation", "portrait");
					// For Android 2.2 and above
					mCamera.setDisplayOrientation(90);

					// Uncomment for Android 2.0 and above
					 parameters.setRotation(90);
					 
				} else {
					Log.d(TAG, "Setting orientation to landscape");

					// This is an undocumented although widely known feature
					parameters.set("orientation", "landscape");
					// For Android 2.2 and above
					mCamera.setDisplayOrientation(0);
					// Uncomment for Android 2.0 and above
					 parameters.setRotation(0);
					 
				}
				if(mPreviewSize != null){
					Log.d(TAG, "Set parameter's preview size");
			        //parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);	
					parameters.setPreviewSize(mPreviewSize.height, mPreviewSize.width);
				}
				
				mCamera.setParameters(parameters);
				//Resuming preview for older versions
//				if(android.os.Build.VERSION.SDK_INT <14){
					mCamera.startPreview();
//				}
				mCamera.setPreviewDisplay(holder);
			} catch (IOException exception) {
				mCamera.release();
			}
		}else{
			Log.d(TAG, "Surface created with null camera");
		}
 	 }
    
	
	public void setCamera(Camera camera) {
	    if (mCamera == camera) { return; }
	    
	    mCamera = camera;
	    
	    if (mCamera != null) {
	        requestLayout();
	        mCamera.getParameters().getSupportedPreviewSizes();
	      
	        try {
	            mCamera.setPreviewDisplay(mHolder);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	/**
	 * Method used to find the optimal aspect ratio for previewing
	 * a camera
	 * @param sizes list of available sizes
	 * @param w width
	 * @param h height
	 * @return optimal size
	 */
	public static Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
	    final double ASPECT_TOLERANCE = 0.05;
	    double targetRatio = (double) w/h;
	    Log.d(TAG, "target size = " + w + " " + h + ", target ratio = " + targetRatio);
	    if (sizes==null) return null;

	    Size optimalSize = null;

	    double minDiff = Double.MAX_VALUE;
	    
	    int targetHeight = h;
	   
	    // Find size
	    for (Size size : sizes) {
	        double ratio = (double) size.width / size.height;
	        if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
	        if (Math.abs(size.height - targetHeight) < minDiff) {
	            optimalSize = size;
	            minDiff = Math.abs(size.height - targetHeight);
	        }
	    }

	    if (optimalSize == null) {
	        minDiff = Double.MAX_VALUE;
	        for (Size size : sizes) {
	            if (Math.abs(size.height - targetHeight) < minDiff) {
	                optimalSize = size;
	                minDiff = Math.abs(size.height - targetHeight);
	            }
	        }
	    }
	    return optimalSize;
	}
	
	public void setRecorder(MediaRecorder recorder){
		this.recorder = recorder;
	}
	
	public int getDeviceDefaultOrientation() {

		WindowManager windowManager =  (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

	    Configuration config = getResources().getConfiguration();

	    int rotation = windowManager.getDefaultDisplay().getRotation();

	    if ( ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
	            config.orientation == Configuration.ORIENTATION_LANDSCAPE)
	        || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&    
	            config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
	      return Configuration.ORIENTATION_LANDSCAPE;
	    }
	    else {
	      return Configuration.ORIENTATION_PORTRAIT;
	    }
	}
}