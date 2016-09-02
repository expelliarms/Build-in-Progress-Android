package com.example.helperClass;

import java.io.Serializable;

/**
 * Multimedia class used to keep track of all the different multimedia added to a step.
 * Multimedia will 
 * @author jan
 *
 */
public class Multimedia implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String TAG = "Multimedia";
	/**
	 * Id of the multimedia. Used to delete from the website.
	 */
	private int id;
	/**
	 * Position of the multimedia. Used to sort in a step between all other media
	 */
	private int position;
	/**
	 * Boolean to know whether the multimedia has a video or not.
	 * Defaults to false unless a video is specified.
	 */
	private boolean isFromVideo = false;
	/**
	 * Boolean to check if the video is on the amazon s3 server or on youtube/vimeo
	 */
	private boolean isEmbeddedVideo = false;
	
	//Keeping track of all of the different image paths
	private String path;
	private String previewPath;
	private String thumbnailPath;
	private String sqThumbnailPath;
	
	//If the media is from a video that is hosted on the amazon s3 server
	//Keep track of all the different video paths & video rotation
	private int videoRotation;
	private String videoPath;
	//private String webmPath;
	
	/**
	 * Constructor called when the multimedia is from an image
	 * @param id
	 * @param path url path to image
	 * @param previewPath url path to the preview image
	 * @param thumbnailPath url path to the thumbnail image
	 * @param sqThumbnailPath url path to different thumbnail image
	 */
	public Multimedia(int id, int position, String path, String previewPath, String thumbnailPath, String sqThumbnailPath){
		this.id = id;
		this.position = position;
		this.path = path;
		this.previewPath = previewPath;
		this.thumbnailPath = thumbnailPath;
		this.sqThumbnailPath = sqThumbnailPath;
	}
	
	/**
	 * Constructor called when multimedia is from a non-embedded video
	 * @param id
	 * @param path
	 * @param previewPath url path
	 * @param thumbnailPath url path to thumbnail
	 * @param sqThumbnailPath url path to different thumbnail
	 * @param videoPath url path to video
	 * @param videoRotation video rotation
	 */
	public Multimedia(int id, int position, String path, String previewPath, String thumbnailPath, String sqThumbnailPath,
			String videoPath, int videoRotation){
		
		//Calling image constructor
		this(id, position, path, previewPath, thumbnailPath, sqThumbnailPath);
		
		this.isFromVideo = true;
		this.videoPath = videoPath;
		this.videoRotation = videoRotation;
	}
	
	/**
	 * Constructor for an embedded video
	 * @param id
	 * @param position
	 * @param path url path
	 * @param previewPath url path to preview
	 * @param thumbnailPath url path to thumbnail
	 * @param sqThumbnailPath url path to different thumbnail
	 * @param videoPath url path to video
	 */
	public Multimedia(int id, int position, String path, String previewPath, String thumbnailPath, String sqThumbnailPath,
			String videoPath){
		//Calling image constructor
		this(id, position, path, previewPath, thumbnailPath, sqThumbnailPath);
		
		this.isFromVideo = true;
		this.isEmbeddedVideo = true;
		
		this.videoPath = videoPath;
		
	}
	

	/**
	 * Getters
	 */
	public String getPath() {
		return path;
	}

	public String getPreviewPath() {
		return previewPath;
	}

	public String getThumbnailPath() {
		return thumbnailPath;
	}

	public String getSqThumbnailPath() {
		return sqThumbnailPath;
	}

	public int getId() {
		return id;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
	
	public boolean isFromVideo(){
		return this.isFromVideo;
	}

	public String getVideoPath() {
		return videoPath;
	}


	public int getVideoRotation(){
	    return videoRotation;
	}

}
