package com.example.helperClass;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.bip_public_android.MediaPreview;

/**
 * Generates the fragment views for the MediaPreview image viewer.
 * @author eunice
 *
 */

public class MediaPreviewAdapter extends FragmentStatePagerAdapter{
	
	private final int mSize;
	private final int mSelectedIndex;
	
	public MediaPreviewAdapter(FragmentManager fm, int size, int position){
		super(fm);
		mSize = size;
		//Give information to the fragments so that the proper fragment knows to start
		mSelectedIndex = position; // give the mediaAdapter the selectedIndex, so that it can give that information to the fragments
	}
	
	@Override
	public int getCount() {
		return mSize;
	}
	
	@Override
	public Fragment getItem(int position) {
		final PreviewFragment f = new PreviewFragment();
		final Bundle args = new Bundle();
		args.putInt(PreviewFragment.IMAGE_DATA_EXTRA, position);
		//vcj: additional information - image or video
		Boolean nIsVideo = MediaPreview.mediaRes.get(position).getResourceType().equals(ImageVideoResource.VIDEO);
		args.putBoolean(PreviewFragment.IMAGE_DATA_IS_VIDEO, nIsVideo);
		args.putInt(PreviewFragment.BOTTOM_BUTTON_HEIGHT, MediaPreview.bottomButtonHeight);
		args.putInt(PreviewFragment.INITIAL_SELECTION, mSelectedIndex); // each fragment gets the initial index
		f.setArguments(args);
		return f;
	}
}
