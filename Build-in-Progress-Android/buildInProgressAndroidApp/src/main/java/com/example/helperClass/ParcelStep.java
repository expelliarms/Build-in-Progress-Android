package com.example.helperClass;

/**
 *  Step.java - Represents a step of a project
 *  @author Ashley Smith
 */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class maintaining information for steps in a project.
 *
 */
@SuppressLint("UseSparseArrays")
public class ParcelStep implements Parcelable {

    /**
     * Keeping track of the total number of images being uploaded by a step
     */
    private AtomicInteger totalUploading = new AtomicInteger(0);
    
    private ArrayList<Multimedia> multimediaList = new ArrayList<Multimedia>();
    
	private int id;
	private int position;
	private int arrayPosition;
	private String name = "";
    private Date published_on;
    private String description = "";
	private String labelColor=""; //color, if a label

    /**
     * List of extra images that have been locally added to a step, but not yet
     * uploaded
     */
    private ArrayList<String> extraMedia;
    /**
     * List of all the currently uploading images associated with the step
     */
    private ArrayList<String> currentlyUploading = new ArrayList<String>();
    
    /**
     * Constructor for step
     * @param name of step
     * @param id unique integer for step
     * @param position of step in the branch display
     * @param arrayPos of step in the ordered array
     * @param publishedDate string of published date
     */    
    public ParcelStep(String name, int id, int position, int arrayPos, String publishedDate){
    	this.name = name;
    	this.id = id;
    	this.position = position;
    	this.arrayPosition = arrayPos;
    	if(publishedDate!=null){
	    	DateFormat formatter = new SimpleDateFormat("MM/dd/yy KK:mm:ss", Locale.US);
	    	Date date = null;
	    	try {
				date = formatter.parse(publishedDate);
			} catch (ParseException e) {
				e.printStackTrace();
			}
	        this.published_on = date;
    	}else{
    		this.published_on = null;
    	}
    }


    /**
     * Setter methods
     */

	public void setName(String name) {
		this.name = name;
	}
    public void setDescription(String d) {
        description = d;
    }
    
    public void setPublishDate(Date d){
    	published_on = d;
    }
    
    public String getDescription() {
        return description;
    }
    

    
	public String getName() {
		return name;
	}
	
	public int getId(){
		return id;
	}
	
	public int getPosition(){
		return position;
	}
	
	public Date getPublishedDate(){
		return published_on;
	}
	
    /**
     * Returns the list of all the different multimedia associated with a step
     * @return
     */
	public ArrayList<Multimedia> getMultimediaList(){
		return this.multimediaList;
	}
	
	public void setMultimediaList(ArrayList<Multimedia> multimedia){
		this.multimediaList = multimedia;
	}
	
	public void addMultimedia(Multimedia multimedia){
		this.multimediaList.add(multimedia);
	}
	
	public void removeMultimedia(Multimedia deletedMedia){
		
		int deletedPos = deletedMedia.getPosition();
		
		//If an image has been deleted, we have to offset all of the images with a position greater than it
		//by one
		for(Multimedia multimedia: this.getMultimediaList()){
			int mediaPos = multimedia.getPosition();
			if(mediaPos > deletedPos){
				multimedia.setPosition(mediaPos - 1);
			}
		}
		
		//Removing the deleted multimedia from the step after updating all the other media
		this.multimediaList.remove(deletedMedia);
	}

	public void setLabelColor(String color) {
		this.labelColor = color;
	}

	public String getLabelColor() {
		return this.labelColor;
	}
	
	
	public ArrayList<String> getExtraImages(){
		return this.extraMedia;
	}
	
	public void addExtraImage(String path){
		if(this.extraMedia!=null){
			this.extraMedia.add(path);
		}else{
			this.extraMedia = new ArrayList<String>();
			this.extraMedia.add(path);
		}
	}
	
	public void setExtraImages(ArrayList<String> extraImg){
		this.extraMedia = extraImg;
	}
	
	public int getArrayPosition(){
		return this.arrayPosition;
	}
	
	public void setArrayPosition(int position){
		this.arrayPosition = position;
	}
	
	public void setPosition(int position){
		this.position = position;
	}


	public int incTotalUploading() {
		return totalUploading.incrementAndGet();
	}


	public int decTotalUploading() {
		return this.totalUploading.decrementAndGet();
	}
	
	public int getTotalUploading(){
		return this.totalUploading.get();
	}
	
	public void addCurrentlyUploading(String path){
		this.currentlyUploading.add(path);
	}
	
	public boolean removeCurrentlyUploading(String path){
		return this.currentlyUploading.remove(path);
	}
	
	public ArrayList<String> getCurrentlyUploading(){
		return this.currentlyUploading;
	}
	
	/**
	 * Finds the specific multimedia in the steps list of multimedia given
	 * a specific id
	 * @param id of multimedia
	 * @return The Multimedia with the given id, or null if not found
	 */
	public Multimedia findMultimediaById(int id){
		
		for(Multimedia media: this.multimediaList){
			if(media.getId() == id){
				return media;
			}
		}
		return null;
	}

    protected ParcelStep(Parcel in) {
        totalUploading = (AtomicInteger)in.readValue(null);
        id = in.readInt();
        position = in.readInt();
        arrayPosition = in.readInt();
        name = in.readString();
        long tmpPublished_on = in.readLong();
        published_on = tmpPublished_on != -1 ? new Date(tmpPublished_on) : null;
        description = in.readString();
        extraMedia = new ArrayList<String>();
        in.readList(extraMedia, null);
        currentlyUploading = new ArrayList<String>();
        in.readList(currentlyUploading, null);
        in.readList(multimediaList, null);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(totalUploading);
        dest.writeInt(id);
        dest.writeInt(position);
        dest.writeInt(arrayPosition);
        dest.writeString(name);
        dest.writeLong(published_on != null ? published_on.getTime() : -1L);
        dest.writeString(description);
        dest.writeList(extraMedia);
        dest.writeList(currentlyUploading);
        dest.writeList(multimediaList);
    }

    public static final Creator<ParcelStep> CREATOR = new Parcelable.Creator<ParcelStep>() {
        public ParcelStep createFromParcel(Parcel in) {
            return new ParcelStep(in);
        }

        public ParcelStep[] newArray(int size) {
            return new ParcelStep[size];
        }
    };
}