package com.example.helperClass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

public class HollowSquare extends View {
	
	private int height = 25;
	private int width = 25;
	private float x;
	private float y;
	
	private Rect rect = new Rect();
	private Paint white = new Paint();

	public HollowSquare(Context context, float x, float y) {
		super(context);
		Log.d("SQUARE", "Created rectangle");
		this.x = x;
		this.y = y;
		setWillNotDraw(false);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Log.d("SQUARE", "Drawing rectangle");
		rect.set(width - (int)x, height - (int)y, width + (int)x, height + (int)y);
		
		white.setColor(Color.WHITE);
		white.setStyle(Paint.Style.STROKE);
		white.setStrokeWidth(3);
		
		canvas.drawRect(rect, white);
		
	}

}
