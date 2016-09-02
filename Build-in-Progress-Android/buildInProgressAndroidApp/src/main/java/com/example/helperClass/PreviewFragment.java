package com.example.helperClass;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;

import com.bip_public_android.DeveloperKey;
import com.bip_public_android.HTML5WebView;
import com.bip_public_android.MediaPreview;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;

/**
 * Fragment of image or video that is created when the user swipes left or right
 * in MediaPreview.
 * 
 * @author eunice
 *
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PreviewFragment extends Fragment implements
		TextureView.SurfaceTextureListener,
		YouTubeThumbnailView.OnInitializedListener,
		MediaPlayer.OnBufferingUpdateListener,
		MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
		MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnInfoListener,
		MediaController.MediaPlayerControl {
	private static final String TAG = "PreviewFragment";
	public static final String IMAGE_DATA_EXTRA = "resPos";
	public static final String IMAGE_DATA_IS_VIDEO = "isVideo";
	public static final String INITIAL_SELECTION = "initialSelection";
	public static final String BOTTOM_BUTTON_HEIGHT = "bottomButtonHeight";
	private int resPos; // resource position
	public ImageVideoResource resource;
	private int initialPosition;

	private ImageView mImageView;

	public Context mContext;
	public FrameLayout mediaHolder;

	// Youtube Player
	private FrameLayout youtubeHolder;
	private YouTubeThumbnailView ytThumbnailView;
	private YouTubeThumbnailLoader ytThumbnailLoader;
	private boolean isYoutube = false;
	private String ytID; // the id of the youtube video
	public YTPlayerFragment playerFragment; // for holding youtube video
	public ImageView videoIcon;

	// Video Player
	public MediaPlayer s3VideoPlayer;
	private TextureView s3TextureView;
	private int textureWidth;
	private int textureHeight;
	private Surface mSurface;
	private int videoRotation = 0;
	private boolean isAmazon = false;
	public boolean isVideo;
	public MediaController videoController;
	boolean videoControllerExists = false;
	public AsyncTask playVideo;
	private int bottomButtonsHeight;

	// Vimeo Player
	private boolean isVimeo = false;
	public HTML5WebView vimeoPlayer;

	// don't call the async function if the items all already exist
	private boolean alreadyLoaded = false;

	public PreviewFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = getArguments();
		if (bundle != null) {
			resPos = bundle.getInt(IMAGE_DATA_EXTRA);
			resource = MediaPreview.mediaRes.get(resPos);
			videoRotation = resource.getRotation();
			isVideo = bundle.getBoolean(IMAGE_DATA_IS_VIDEO);
			initialPosition = bundle.getInt(INITIAL_SELECTION);
			bottomButtonsHeight = bundle.getInt(BOTTOM_BUTTON_HEIGHT);
		} else {
			resPos = -1;
			isVideo = bundle.getBoolean(IMAGE_DATA_IS_VIDEO);
			initialPosition = bundle.getInt(INITIAL_SELECTION);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View v = inflater.inflate(com.bip_public_android.R.layout.preview_fragment, container,
				false);
		// vcj: need a switch here that picks which one to use

		mContext = container.getContext();// need to create this earlier so
		// it's not null on post execute in MediaPreview
		mediaHolder = (FrameLayout) v.findViewById(com.bip_public_android.R.id.fragment_frame_layout);
		youtubeHolder = (FrameLayout) v.findViewById(com.bip_public_android.R.id.youtube_holder);
		ytThumbnailView = (YouTubeThumbnailView) v
				.findViewById(com.bip_public_android.R.id.ytThumbnailview);

		videoIcon = (ImageView) v.findViewById(com.bip_public_android.R.id.video_icon2);
		videoIcon.setVisibility(View.GONE);

		// set up textureview
		s3TextureView = (TextureView) v.findViewById(com.bip_public_android.R.id.s3_video);

		s3TextureView.setSurfaceTextureListener(this);

		mImageView = (ImageView) v.findViewById(com.bip_public_android.R.id.preview_image);
		/*getActivity().findViewById(R.id.media_progress)
				.setVisibility(View.GONE);*/

		// Switch to show and hide proper views
		if (isVideo) {
			//Log.d(TAG, "VCJ onCreateView: Video switch resPos: " + resPos);
			s3TextureView.setVisibility(View.VISIBLE);
			mImageView.setVisibility(View.GONE);
		} else {
			mImageView.setVisibility(View.VISIBLE);
			s3TextureView.setVisibility(View.GONE);
		}
		String urlPath = resource.getVideoURL().toLowerCase();
		if (urlPath.contains("youtube")) {
			isYoutube = true;
			hideProgressBar();
			String path = resource.getVideoURL(); // yt IDs are case sensitive
			ytID = path.substring(path.lastIndexOf("/") + 1, path.length());
		} else if (urlPath.contains("vimeo")) {
			isVimeo = true;
		} else {
			isAmazon = true;
		}

		/*Log.d(TAG, "VCJ onCreateView: isVideo: " + isVideo + " resPos: "
				+ resPos + " mediaRes item: " + resource + " videoURL item: "
				+ urlPath);*/

		s3TextureView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isAmazon && s3VideoPlayer != null) {
					if (s3VideoPlayer.isPlaying()) {
						s3VideoPlayer.pause();
					} else {
						s3VideoPlayer.start();
					}
					if (videoControllerExists) {
						videoController.show();
					}
				}
			}
		});

		// if they click the black area, it should react the same as touching
		// the video image
		mediaHolder.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isAmazon && s3VideoPlayer != null) {
					if (s3VideoPlayer.isPlaying()) {
						s3VideoPlayer.pause();
					} else {
						s3VideoPlayer.start();
					}
					if (videoControllerExists) {
						videoController.show();
					}
				}
			}
		});

		return v;
	}

	private void adjustAspectRatio(int videoWidth, int videoHeight, boolean flip) {
		// For some reason, I can't use the TextureView size because it keeps
		// changing
		// int viewWidth = s3TextureView.getWidth();
		// int viewHeight = s3TextureView.getHeight(); // the height is changed
		// somewhere

		int viewWidth = mediaHolder.getWidth();
		int viewHeight = mediaHolder.getHeight();
		double aspectRatio = (double) videoHeight / videoWidth;
		
		double temp1 = (double) videoHeight / videoWidth;
		double temp2 = (double) videoWidth / videoHeight;
		
		// HACK: For some reason, height and width are coming out scaled and flipped on some devices
		
		// This should not affect devices that are displaying properly, but it will correct for 
		// devices that have the proper scale, but are flipped incorrectly
		if ((videoRotation ==90)||(videoRotation==270)) {
			// Portrait on one device wxh = 360x640, on another : 360x202
			// the ratio is correct for portrait if you use the larger number over the smaller one
			aspectRatio = Math.max(temp1, temp2); 
			// The height should be larger than the width, so the correct ratio (h/w) should be bigger than 1
			
		} else {
			// For landscape, the width should be larger, so the correct ratio (h/w) should be less than 1
			aspectRatio = Math.min(temp1, temp2);
		}

		int newWidth, newHeight;
		if (viewHeight > (int) (viewWidth * aspectRatio)) {
			// limited by narrow width; restrict height
			newWidth = viewWidth;
			newHeight = (int) (viewWidth * aspectRatio);
		} else {
			// limited by short height; restrict width
			newWidth = (int) (viewHeight / aspectRatio);
			newHeight = viewHeight;
		}

		/*Log.d(TAG, " view width: " + viewWidth + " view height: " + viewHeight
				+ " video=" + videoWidth + "x" + videoHeight
				+ " aspect ratio: " + aspectRatio + " newWidth: " + newWidth
				+ " newHeight: " + newHeight + " flip: " + flip);*/

		FrameLayout.LayoutParams videoLayoutParams;

		if (flip) {
			// Since you set the texture View rotation (so the video orientation
			// is correct),
			// the Texture View width should be set to the new height for
			// portrait and inverted portrait
			videoLayoutParams = new FrameLayout.LayoutParams(newHeight,
					newWidth);
		} else {
			videoLayoutParams = new FrameLayout.LayoutParams(newWidth,
					newHeight);
		}

		videoLayoutParams.gravity = Gravity.CENTER;
		s3TextureView.setLayoutParams(videoLayoutParams);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		/*Log.d(TAG, "VCJ onActivityCreated: isVideo: " + isVideo + " resPos: "
				+ resPos + " mediaRes item: " + resource + " videoURL item: "
				+ resource.getVideoURL());*/
		// Double check to make sure you have the right type
		if (isVideo
				&& resource.getResourceType().equals(ImageVideoResource.VIDEO)) {
			if (isYoutube) {
				loadYoutubeVideo();
			} else if (isVimeo) {
				playVimeoVideo();
			}
		} else if (resource.getResourceType().equals(ImageVideoResource.IMAGE)) {
			TaggedBitmapDrawable drawable;
			drawable = resource.getImageDrawable();
			mImageView.setImageDrawable(drawable);
			hideProgressBar();
		}
	}

	private void hideProgressBar() {
		getActivity().findViewById(com.bip_public_android.R.id.media_progress).setVisibility(View.GONE);
	}

	private void showProgressBar() {
		getActivity().findViewById(com.bip_public_android.R.id.media_progress).setVisibility(View.VISIBLE);
	}

	private void loadYoutubeVideo() {
		int newID = Method.generateViewId();
		youtubeHolder.setId(newID);
		ytThumbnailView.initialize(DeveloperKey.DEVELOPER_KEY, this);
		// start if this is the initial selection
		if (initialPosition == resPos) {
			videoIcon.setVisibility(View.GONE);
			playYoutubeVideo();
		} else {
			videoIcon.setVisibility(View.VISIBLE);
		}

		ytThumbnailView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				playYoutubeVideo();
			}
		});
	}

	private void playYoutubeVideo() {
		playerFragment = YTPlayerFragment.newInstance(ytID);
		getFragmentManager().beginTransaction()
				.add(youtubeHolder.getId(), playerFragment).commit();
	}

	private void playVimeoVideo() {
		vimeoPlayer = new HTML5WebView(mContext, mediaHolder);

		// some videoURL have http in front and some don't
		if (resource.getVideoURL().indexOf("http") < 0) {
			vimeoPlayer.loadUrl("https:" + resource.getVideoURL());
		} else {
			vimeoPlayer.loadUrl(resource.getVideoURL());
		}
		hideProgressBar();
		s3TextureView.setVisibility(View.GONE);
		mImageView.setVisibility(View.GONE);
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
			int height) {
		mSurface = new Surface(surface);
		//showProgressBar();
		/*Log.d(TAG, "surfaceTextureAvailable and mSurface: " + mSurface
				+ " holder: " + mediaHolder.getId() + " context: " + mContext);*/

		if (isAmazon && !alreadyLoaded) {
			// set scale for textureview
			//Log.d(TAG, "setting video rotation..");
			/*Log.d(TAG,
					" Texture View before rotate : width : "
							+ s3TextureView.getWidth() + " height : "
							+ s3TextureView.getHeight());*/

			s3TextureView.setRotation(Math.abs(360 - videoRotation));
			//Log.d(TAG, "video URL : " + resource.getVideoURL());
			s3VideoPlayer = MediaPlayer.create(mContext,
					Uri.parse(resource.getVideoURL())); // TODO vcj: possible move to Media Preview so video is loaded earlier
			// need to set it early for so the listener is attached before prepare is completed
			// Media player's create calls prepare as the final step
			s3VideoPlayer.setOnPreparedListener(this);
			playVideo = new playUploadedVideo(mediaHolder, mContext)
					.execute(resource.getVideoURL());
		}
		//Log.d(TAG, "done onSurfaceAvailable");
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
			int width, int height) {
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
		/*Log.d(TAG, "destroying surface, trying to cancel play video: "
				+ playVideo);*/
		if (playVideo != null) {
			playVideo.cancel(true);
		}
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
//		Log.d(TAG,
//				" Texture View updated : widthxHeight : "
//						+ s3TextureView.getWidth() + " x "
//						+ s3TextureView.getHeight() + " mediaHolder: "
//						+ mediaHolder.getWidth() + " x "
//						+ mediaHolder.getHeight());

	}

	@Override
	public void onStop() {
		if (isVimeo) {
			vimeoPlayer.stopLoading();
			vimeoPlayer.destroy();
		}
		if (isAmazon) {
			if (s3VideoPlayer != null) {
				//Log.d(TAG, "STOPPING S3 VIDEO");
				if (s3VideoPlayer.isPlaying()) {
					s3VideoPlayer.pause();
				}
				/*s3VideoPlayer.reset();
				s3VideoPlayer.release();*/
			}
			if (videoController != null) {
				// the problem is the controller wants to show for 3 seconds and
				// you're stopping it before it can
				//Log.d(TAG, "Hiding controller");
				videoController.hide();
			}
		}
		//Log.d(TAG, "ONSTOP!!!!!!!! ");
		super.onStop();

	}

	@Override
	public void onPause() {
		// when okay button, or check is hit quickly, this isn't called for some
		// reason (just onStop is called)
		if (isAmazon) {
			if (s3VideoPlayer != null) {
				//Log.d(TAG, "pausing S3 VIDEO");
				if (videoControllerExists) {
					videoController.hide();
				}
			}
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		if (isAmazon) {
			if (s3VideoPlayer != null) {
				//Log.d(TAG, "resuming S3 VIDEO");
			}
		}
		super.onResume();
	}

	@Override
	public void onDestroy() {
		if (s3VideoPlayer != null) {
			//Log.d(TAG, "DESTROYING S3 VIDEO");
			s3VideoPlayer.reset();
			s3VideoPlayer.release();
			super.onDestroy();
		}
		if (ytThumbnailLoader != null) {
			ytThumbnailLoader.release();
		}
		super.onDestroy();
	}

	@Override
	public void onInitializationSuccess(
			YouTubeThumbnailView youTubeThumbnailView,
			YouTubeThumbnailLoader youTubeThumbnailLoader) {
		ytThumbnailLoader = youTubeThumbnailLoader;
		youTubeThumbnailLoader
				.setOnThumbnailLoadedListener(new YouTubeThumbnailLoader.OnThumbnailLoadedListener() {
					@Override
					public void onThumbnailLoaded(
							YouTubeThumbnailView youTubeThumbnailView, String s) {
					}

					@Override
					public void onThumbnailError(
							YouTubeThumbnailView youTubeThumbnailView,
							YouTubeThumbnailLoader.ErrorReason errorReason) {
					}
				});
		ytThumbnailLoader.setVideo(ytID);

		if (initialPosition != resPos) {
			videoIcon.setVisibility(View.VISIBLE);
			videoIcon.bringToFront();
		}
	}

	@Override
	public void onInitializationFailure(
			YouTubeThumbnailView youTubeThumbnailView,
			YouTubeInitializationResult youTubeInitializationResult) {
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getBufferPercentage() {
		return 0;
	}

	@Override
	public int getCurrentPosition() {
		return s3VideoPlayer.getCurrentPosition();
	}

	@Override
	public int getDuration() {
		return s3VideoPlayer.getDuration();
	}

	@Override
	public boolean isPlaying() {
		return s3VideoPlayer.isPlaying();
	}

	@Override
	public void pause() {
		s3VideoPlayer.pause();
	}

	@Override
	public void seekTo(int pos) {
		s3VideoPlayer.seekTo(pos);
	}

	@Override
	public void start() {
		s3VideoPlayer.start();
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		// TODO Auto-generated method stub
		//Log.d(TAG, "Video size changed: WxH : " + width + "x" + height);
		boolean flip = false;
		if ((videoRotation == 90) || (videoRotation == 270)) {
			// portrait or inverted portrait : switch because they are saved
			// sideways
			flip = true;
		}
		adjustAspectRatio(s3VideoPlayer.getVideoWidth(),
				s3VideoPlayer.getVideoHeight(), flip);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// loads the "thumbnail" image on some devices
		mp.seekTo(0);
		//Log.d(TAG, "prepared : "+mp.getVideoHeight()+" and width: "+mp.getVideoWidth());

		if (resPos == initialPosition) {
			// Play the video automatically, if the video was the
			// initial selection
			//Log.d(TAG, " starting video ");
			s3VideoPlayer.start();
		} else {
			// Start the video so there is a "thumbnail" image (TODO: doesn't work on some devices. Unclear why)
			PreviewFragment.this.start();
			// If the video isn't the initially selected video, pause it
			PreviewFragment.this.pause();
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
		//Log.d(TAG, "Info listener i: "+i+" i1: "+i1);
		return false;
	}

	public class playUploadedVideo extends AsyncTask<String, Void, Boolean> {
		private final String TAG = "playUploadedVideo";
		private FrameLayout mediaHolder;
		private Context context;
		private PreviewFragment fragment;

		public playUploadedVideo(FrameLayout mediaHolder, Context context) {
			this.fragment = PreviewFragment.this;
			this.mediaHolder = mediaHolder;
			this.context = context;
		}

		@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		@Override
		protected Boolean doInBackground(String... arg0) {
			// Setting up video view using Media Player
			try {
				/*Log.d(TAG, "attempting to load video " + Uri.parse(arg0[0])
						+ " and surface : " + mSurface);
				Log.d(TAG, "video player : " + s3VideoPlayer);
				Log.d(TAG,
						" Video Player : width : "
								+ s3VideoPlayer.getVideoWidth() + " height : "
								+ s3VideoPlayer.getVideoHeight());*/
				s3VideoPlayer.setSurface(mSurface);
				//s3VideoPlayer.setOnPreparedListener(fragment);
				s3VideoPlayer.setOnBufferingUpdateListener(fragment);
				s3VideoPlayer.setOnVideoSizeChangedListener(fragment);
				s3VideoPlayer.setOnInfoListener(fragment);
				s3VideoPlayer.setOnCompletionListener(fragment);
				s3VideoPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				return true;
			} catch (Exception e) {
				//Log.d(TAG, "Media failed: " + e.getMessage());
				e.printStackTrace();
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean loaded) {
			if (loaded) {
				// add mediaController
				videoController = new MediaController(context) {
					public boolean dispatchKeyEvent(KeyEvent event) {
						if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
							((Activity) getContext()).finish();

						return super.dispatchKeyEvent(event);
					}
				};
				videoControllerExists = true;
				//Log.d(TAG, "SETTING UP VIDEO CONTROLLER");
				videoController.setAnchorView(mediaHolder);
				videoController.setMediaPlayer(fragment);
				videoController.setEnabled(true);
				videoController.requestFocus();

				hideProgressBar();
				if (getUserVisibleHint()) {
					videoController.show();
				}
				alreadyLoaded = true;
			}
		}

		@Override
		protected void onCancelled(Boolean loaded) {
			// if cancelled in do in background, will not execute onPostExecute
			if (isAmazon) {
				if (s3VideoPlayer != null) {
					//Log.d(TAG, "CANCEL/STOPPING S3 VIDEO");
					//s3VideoPlayer.stop(); // Illegal state exception if not initialized
					s3VideoPlayer.reset();
					s3VideoPlayer.release();
				}
				if (videoController != null) {
					videoController = null;
				}
			}
			//Log.d(TAG, "CANCELLED LOADING VIDEO");
		}
	}
	}
