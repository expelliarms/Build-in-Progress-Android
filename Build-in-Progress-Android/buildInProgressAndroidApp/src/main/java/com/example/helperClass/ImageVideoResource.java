package com.example.helperClass;

/**
 * Class designed to fill mediaRes is MediaPreview.java with one kind of object that works for images and videos
 *
 * Previously, mediaRes was filled with TaggedBitmapDrawable items that are useful for displaying images, but don't work
 * for displaying videos.
 *
 * Videos can be displayed by setting the Video Path of a TextureView to the videoURL in MediaPreview.java
 * Thus, the videoURL is saved in this Object and given to the Preview Fragment.
 *
 * The rotation of the video is also saved so the Texture View can rotate the video to the appropriate orientation
 *
 * Created by victoria on 6/18/2015.
 */
public class ImageVideoResource {
    /**if video, video url. else empty string**/
    private String videoURL;
    /**Bitmap for image or frame from video**/
    private TaggedBitmapDrawable imageDrawable;
    /**Video rotation. Set to 0 if an image (or if video rotation is 0)**/
    private int rotation;
    /**Based on videoURL, set to ImageVideoResource.IMAGE or ImageVideoResource.VIDEO**/
    public final String resourceType;
    public static final String IMAGE = "IMAGE";
    public static final String VIDEO = "VIDEO";

    /**
     * @param videoURL          set to the videoUrl if media is a video. Set to the empty string ("") if an image
     * @param videoRotation     set to the video rotation if the media is a video. Set to 0 if an image (or if video rotation is 0)
     * @param bm                images and videos generate bitmaps
     *                          (if it's a video, bm should be ignored)
     **/
    public ImageVideoResource(String videoURL, int videoRotation, TaggedBitmapDrawable bm) {
        this.videoURL = videoURL;
        this.imageDrawable = bm;
        this.rotation = videoRotation;
        if (videoURL.isEmpty()) {
            resourceType = IMAGE;
        } else {
            resourceType = VIDEO;
        }
    }

    public String getVideoURL() {return videoURL;}

    public int getRotation() {return rotation;}

    public TaggedBitmapDrawable getImageDrawable() {return imageDrawable;}

    public String getResourceType() {return resourceType;}

}
