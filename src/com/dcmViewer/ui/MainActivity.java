package com.dcmViewer.ui;

import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.IllegalFormatCodePointException;

import com.dicomViewer.R;
import com.dcmViewer.GestureDetector.MoveGestureDetector;
import com.dcmViewer.GestureDetector.RotateGestureDetector;
import com.dcmViewer.dicom.imageio.DicomImageReader;
import com.dcmViewer.widget.DicomView;
import com.imebra.dicom.*;

import android.R.bool;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
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
	private float currentScale = 0.f;
	private int offestX;
	private int offestY;
	private Rect mScreenRect =  new Rect();
	private Rect mImageRect  =  new Rect();
	private Rect mImageROIRect=  new Rect();

	private DicomImageReader mDicomImageReader;
	private int[] imageBuffer;

	private boolean isAdjustWL = false;
	
	private boolean isMove = false;

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
			currentScale = Scale;

			mImageWidth = (int) (mDicomImageReader.getWidth() * Scale);
			mImageHeight = (int) (mDicomImageReader.getHeight() * Scale);

			imageBuffer = mDicomImageReader
					.getBitmap(mImageWidth, mImageHeight);

			int left = mImageWidth >= mScreenWidth ? 0
					: (mScreenWidth - mImageWidth) / 2;
			int top = mImageHeight >= mScreenHeight ? 0
					: (mScreenHeight - mImageHeight) / 2;
			mImageRect.set(left, top, left + mImageWidth, top + mImageHeight);
			mImageROIRect.set(mImageRect);

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

		imageView.refreshColors(imageBuffer);

		displayWLS(windowWidth, windowCenter, Scale);
	}

	/*
	 * 移动
	 */
	private boolean move(int nX, int nY) {

		if (mImageROIRect.top + nY > mScreenHeight
				|| mImageROIRect.bottom + nY < 0
				|| mImageROIRect.left + nX > mScreenWidth
				|| mImageROIRect.right + nX < 0)
			return false; // 边界检测

		mImageRect.top += nY;
		mImageRect.bottom += nY;
		mImageRect.left += nX;
		mImageRect.right += nX;

		if (mImageRect.top < 0) {
			mImageROIRect.top = 0;
		} else {
			mImageROIRect.top += nY;
		}

		if (mImageRect.bottom > mScreenHeight) {
			mImageROIRect.bottom = mScreenHeight;
		} else {
			mImageROIRect.bottom += nY;
		}

		if (mImageRect.left < 0) {
			mImageROIRect.left = 0;
		} else {
			mImageROIRect.left += nX;
		}

		if (mImageRect.right > mScreenWidth) {
			mImageROIRect.right = mScreenWidth;
		} else {
			mImageROIRect.right += nX;
		}

		return true;
	}
	
	
	/*
	 * 缩放
	 */
	private boolean scale(float s){
		if (currentScale == s)
			return false;
		
		float difscale = currentScale-s;
		float halfX = mImageRect.width()*difscale/2;
		float halfY = mImageRect.height()*difscale/2;
		
		mImageRect.top     += halfY;
		mImageRect.bottom  -= halfY;
		mImageRect.left    += halfX;
		mImageRect.right   -= halfX;
		
		if (mImageRect.top < 0) {
			mImageROIRect.top = 0;
		}else {
			mImageROIRect.top += halfY;
		}
		
		if (mImageRect.bottom > mScreenHeight) {
			mImageROIRect.bottom = mScreenHeight;
		}else {
			mImageROIRect.bottom -= halfY;
		}
		
		if (mImageRect.left < 0) {
			mImageROIRect.left = 0;
		}else {
			mImageROIRect.left += halfX;
		}
		
		if (mImageRect.right> mScreenWidth) {
			mImageROIRect.right = mScreenWidth;
		}else {
			mImageROIRect.right -= halfX;
		}
		currentScale = s;
		return true;
	}
	

	/*
	 * 刷新图像
	 */
	private void refreshBitmap() {
		if (mScreenRect.contains(mImageROIRect)) {
			imageView.setDrawRect(mImageROIRect.top, mImageROIRect.left,
					mImageROIRect.width(), mImageROIRect.height());
		} else {
			Rect realImageROIRect = screenToimage(mImageRect, mImageROIRect);
			imageBuffer = mDicomImageReader.resize(mImageRect.width(),
					mImageRect.height(), realImageROIRect.left,
					realImageROIRect.top, realImageROIRect.right,
					realImageROIRect.bottom);
			
			imageView.setColors(imageBuffer);
			imageView.setDrawRect(mImageROIRect.top, mImageROIRect.left, mImageROIRect.width(), mImageROIRect.height());
		}
	}

	// 屏幕坐标映射到实际图像坐标
	private Rect screenToimage(Rect imageRect, Rect imageROIRect) {

		Rect rc = new Rect(0, 0, 0, 0);

		rc.top = (int) (imageROIRect.top - imageRect.top);
		rc.left = (int) (imageROIRect.left - imageRect.left);
		rc.bottom = (int) (imageROIRect.bottom - imageRect.top);
		rc.right = (int) (imageROIRect.right - imageRect.left);

		return rc;

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		ImageView view = (ImageView) v;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			currentX = event.getX();
			currentY = event.getY();
			isMove = true;
			break;
		case MotionEvent.ACTION_POINTER_2_DOWN:
			if (event.getPointerCount() == 2) {
				lastFingerdis = distanceBetweenFingers(event);
			}
            isMove = false;
			break;
		case MotionEvent.ACTION_MOVE:

			if (event.getPointerCount() == 1) {
				if (currentY == event.getY() && currentX == event.getX())
					return true;

				if (isAdjustWL) {
					setWL((int) (event.getX() - currentX),
							(int) (event.getY() - currentY));
				} else {
					if(isMove && move((int) (event.getX() - currentX),
							(int) (event.getY() - currentY)))
					refreshBitmap();
				}

				currentX = event.getX();
				currentY = event.getY();
			} else if (event.getPointerCount() == 2) {

				currentFingerdis = distanceBetweenFingers(event);
				Scale = currentFingerdis > lastFingerdis ? Scale + 0.01f
						: Scale - 0.01f;
				if (Scale > 5) {
					Scale = 5.00f;
				} else if (Scale < 0.01) {
					Scale = 0.01f;
				}
				if (scale(Scale)) {
					refreshBitmap();
				}
				displayWLS(windowWidth, windowCenter, Scale);

			}

			break;
		case MotionEvent.ACTION_UP:
			isMove = false;
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
