package com.example.helperClass;

/**
 *  Project.java - Represents a project created by a user
 *  @author Ashley Smith
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.graphics.Bitmap;

public class Project implements Serializable{

	private static final long serialVersionUID = -8564171545740507368L;
	private String name;
	private int id;
	private boolean built;
	private boolean collaborative = false;
	private Bitmap projectImage;
	private List<ParcelStep> steps;
	private String description;
	private Date date;
	private String category;
	private String imagePath;
	private int projectRanking;

	public Project() {
		date = new Date();
		category = "science";
		description = "";
		name = "";
		this.projectRanking = 0;
	}
	
	public Project(String name,String imagePath, int id, boolean built) {
		this.name = name;
		this.imagePath = imagePath;
		this.id = id;
		this.built = built;
		date = new Date();
		category = null;
		this.description = null;
		this.projectRanking = 0;
	}


	public Project(String name,  String desc, String category) {
		this.name = name;
		steps = new ArrayList<ParcelStep>();
		description = desc;
		date = new Date();
		this.category = category;
		this.projectRanking = 0;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Bitmap getProjectImage(){
		return projectImage;
	}

	public void setSteps(List<ParcelStep> steps) {
		this.steps = steps;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public List<ParcelStep> getSteps() {
		return steps;
	}
	public Bitmap getProfilePic(){
	    return projectImage;
	}
	public void addStep(ParcelStep s) {
		steps.add(s);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
	
	public void setDescription( String description){
		this.description = description;
	}
	
	public void setBuilt(boolean built){
		this.built = built;
	}
	
	public boolean getBuilt(){
		return built;
	}
	
	public Date getDate() {
		return date;
	}

	public String getCategory() {
		return category;
	}
	
	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String image) {
		this.imagePath = image;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getProjectRanking() {
		return projectRanking;
	}

	public void setProjectRanking(int projectRanking) {
		this.projectRanking = projectRanking;
	}

	public boolean isCollaborative() {
		return collaborative;
	}

	public void setCollaborative(boolean collaborative) {
		this.collaborative = collaborative;
	}

}
