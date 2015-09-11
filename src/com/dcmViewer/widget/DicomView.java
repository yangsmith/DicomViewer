package com.dcmViewer.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

public class DicomView extends ImageView {

	private int top;
	private int left;
	private int imageWidth;
	private int imageHeight;
	private int[] colors;
	
	
	public DicomView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public DicomView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public DicomView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public void setColors(int[] colors) {
		if (this.colors == null) {
			this.colors = colors;
		}else{
			this.colors = colors;
			invalidate();
		}
		
		
	}
	
	public void setDrawRect(int top,int left,int imageWidth,int imageHeight) {
		// TODO Auto-generated method stub
		this.top = top;
		this.left = left;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		invalidate();

	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		if (colors == null) {
			return;
		}
		
		
		canvas.drawBitmap(colors, 0, imageWidth, top, left, imageWidth, imageHeight, false, null);
	}

}
