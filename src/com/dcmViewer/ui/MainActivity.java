package com.dcmViewer.ui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.dicomViewer.R;
import com.dcmViewer.GestureDetector.MoveGestureDetector;
import com.dcmViewer.GestureDetector.RotateGestureDetector;
import com.dcmViewer.dicom.imageio.DicomImageReader;
import com.dcmViewer.widget.DicomView;
import com.imebra.dicom.*;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnTouchListener,
		OnClickListener {

	private int mImageHeight, mImageWidth;
	private int mScreenWidth, mScreenHeight;

	private float currentX;
	private float currentY;
	
	private double lastFingerdis;
	private double currentFingerdis;

	private int windowCenter;
	private int windowWidth;
	private float Scale = 0.f;
	private int offestX;
	private int offestY;
    private Rect mScreenRect;
	private Rect mImageRect;
	private Rect mImageROIRect;

	private DicomImageReader mDicomImageReader;
	private int[] imageBuffer;

	private boolean isAdjustWL = false;

	DicomView imageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mainactivity);

		// Set this class as touchListener to the ImageView
		imageView = (DicomView) findViewById(R.id.dcmview);
		imageView.setOnTouchListener(this);

		System.loadLibrary("imebra_lib");

		Button buttonload = (Button) findViewById(R.id.load);
		buttonload.setOnClickListener(this);
		Button buttonadjustWL = (Button) findViewById(R.id.adjustWL);
		buttonadjustWL.setOnClickListener(this);

		mDicomImageReader = new DicomImageReader();

	}

	// Load an image
	public void LoadDicomImage(String fileName) {

		mScreenWidth = imageView.getWidth();
		mScreenHeight = imageView.getHeight();
		mScreenRect.set(0, 0, mImageWidth, mScreenHeight);

		if (mDicomImageReader.open(fileName)) {

			windowCenter = mDicomImageReader.getwindowCenter();
			windowWidth = mDicomImageReader.getWindowWidth();

			float ScaleX = (float) mScreenWidth / mDicomImageReader.getWidth();
			float ScaleY = (float) mScreenHeight
					/ mDicomImageReader.getHeight();
			Scale = ScaleX > ScaleY ? ScaleY : ScaleX;

			mImageWidth = (int) (mDicomImageReader.getWidth() * Scale);
			mImageHeight = (int) (mDicomImageReader.getHeight() * Scale);

			imageBuffer = mDicomImageReader
					.getBitmap(mImageWidth, mImageHeight);
			
			int top  = mImageWidth >= mScreenWidth ? 0 : (mScreenWidth-mImageWidth)/2; 
			int left = mImageHeight >= mScreenHeight ? 0 : (mScreenHeight-mImageHeight)/2; 
			mImageRect.set(left, top, left + mImageWidth, top + mImageHeight);
		

	        imageView.setColors(imageBuffer);
	        imageView.setDrawRect(top, left, mImageWidth, mImageHeight);
	        
	     

			

			displayWLS(windowWidth, windowCenter, Scale);
		}

	}

	// set windowns/center
	private void setWL(int nX, int nY) {
		windowWidth += nX;
		windowCenter += nY;

		imageBuffer = mDicomImageReader.applyWC(windowWidth, windowCenter);
	
		imageView.setColors(imageBuffer);

		displayWLS(windowWidth, windowCenter, Scale);
	}
	
	private void move(int nX, int nY){
		mImageRect.top     += nY;
		mImageRect.bottom  += nY;
		mImageRect.left    += nX;
		mImageRect.right   += nX;
		
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		ImageView view = (ImageView) v;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			currentX = event.getX();
			currentY = event.getY();
		break;
		case MotionEvent.ACTION_POINTER_2_DOWN:
			if (event.getPointerCount() == 2) {
				lastFingerdis = distanceBetweenFingers(event);
			}

			break;
		case MotionEvent.ACTION_MOVE:
			
			if (event.getPointerCount() == 1) {
				if (currentY == event.getY() && currentX == event.getX())
					return true;

				if(isAdjustWL){
					setWL((int) (event.getX() - currentX),
							(int) (event.getY() - currentY));
				}else {
					move((int)(event.getX()-currentX),(int)(event.getY()-currentY));
				}
				
	            currentX = event.getX();
	            currentY = event.getY();
			}else if (event.getPointerCount() == 2) {
				
				currentFingerdis = distanceBetweenFingers(event);
				Scale = currentFingerdis > lastFingerdis ? Scale+0.01f:Scale-0.01f;
				if (Scale > 5) {
					Scale = 5.00f;
				}else if(Scale < 0.01){
					Scale = 0.01f;
				}
				displayWLS(windowWidth, windowCenter, Scale);
				
			}
			
			
			
			break;

		default:
			break;
		}

		return true;
	}
	
	/**
	 * 计算两个手指之间的距离。
	 * 
	 * @param event
	 * @return 两个手指之间的距离
	 */
	private double distanceBetweenFingers(MotionEvent event) {
		float disX = Math.abs(event.getX(0) - event.getX(1));
		float disY = Math.abs(event.getY(0) - event.getY(1));
		return Math.sqrt(disX * disX + disY * disY);
	}

	private void displayWLS(int w, int c, float s) {
		String string = String.format("W/L:%d/%d    Scale:%.2f", w, c, s);
		((TextView) findViewById(R.id.WLValue)).setText(string);
	}


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.adjustWL:
			isAdjustWL = !isAdjustWL;
			((Button) findViewById(R.id.adjustWL))
					.setText(isAdjustWL ? "cancel W/L" : "adjust W/L");
			break;

		case R.id.load: {
			String testFileName = this.getCacheDir().getAbsolutePath()
					+ "/test2.dcm";
			File file = new File(testFileName);
			if (file.exists()) {
				file.delete();
			}

			InputStream is;
			try {
				is = getAssets().open("test2.dcm");
				copyFile(is, file);
				LoadDicomImage(testFileName);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
			break;

		default:
			break;
		}

	}

	void copyFile(InputStream is, File dstFile) {
		try {

			BufferedInputStream bis = new BufferedInputStream(is);
			BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(dstFile), 1024);
			byte buf[] = new byte[1024];
			int c = 0;
			c = bis.read(buf);
			while (c > 0) {
				bos.write(buf, 0, c);
				c = bis.read(buf);
			}
			bis.close();
			bos.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		if (mDicomImageReader != null) {
			mDicomImageReader.close();
			mDicomImageReader = null;
		}
	}

}
