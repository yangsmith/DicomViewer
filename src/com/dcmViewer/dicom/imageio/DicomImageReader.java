package com.dcmViewer.dicom.imageio;

import java.io.IOException;

import android.R.integer;

import com.dcmViewer.dicom.data.Tag;
import com.imebra.dicom.CodecFactory;
import com.imebra.dicom.ColorTransformsFactory;
import com.imebra.dicom.DataSet;
import com.imebra.dicom.DrawBitmap;
import com.imebra.dicom.Image;
import com.imebra.dicom.ModalityVOILUT;
import com.imebra.dicom.Stream;
import com.imebra.dicom.StreamReader;
import com.imebra.dicom.TransformsChain;
import com.imebra.dicom.VOILUT;

public class DicomImageReader {

	// 帧数
	private int frames;

	// 图像的宽
	private int width;

	// 图像的高
	private int height;

	// 窗宽窗位
	private int windowCenter;
	private int windowWidth;

	private int mtotalWidthPixels;
	private int mtotalHeightPixels;
	private int mvisibleTopLeftX;
	private int mvisibleTopLeftY;
	private int mvisibleBottomRightX;
	private int mvisibleBottomRightY;

	private DataSet dataSet;

	private ModalityVOILUT modalityVOILUT;

	private VOILUT voilut;

	private TransformsChain transformsChain;

	private Stream stream;

	private DrawBitmap drawBitmap;

	private Image image;

	private int imageBufferSize;

	private int[] imageBuffer;

	public DicomImageReader() {
		// TODO Auto-generated constructor stub
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getwindowCenter() {
		return windowCenter;
	}

	public int getWindowWidth() {
		return windowWidth;
	}

	public DataSet getMedaData() {
		return dataSet;
	}

	public boolean open(String filePath) {

		resetInternalState();

		stream = new Stream();
		stream.openFileRead(filePath);

		readMedaData();

		return (stream != null);
	}

	public void close() {
		resetInternalState();
	}

	// an BGRA buffer of image
	public int[] getBitmap(int destWidth, int destHeight) {

		drawBitmap = new DrawBitmap(image, transformsChain);
		if (drawBitmap != null) {
			mtotalWidthPixels  = destWidth;
			mtotalHeightPixels = destHeight;
			mvisibleBottomRightX = destWidth;
			mvisibleBottomRightY = destHeight;
			
			imageBufferSize = drawBitmap.getBitmap(destWidth, destHeight, 0, 0,
					destWidth, destHeight, new int[1], 0);

			imageBuffer = new int[imageBufferSize];
			drawBitmap.getBitmap(destWidth, destHeight, 0, 0, destWidth,
					destHeight, imageBuffer, imageBufferSize);
		}

		return imageBuffer;

	}

	// 调整窗宽窗位
	public int[] applyWC(int w, int c) {

		voilut.setCenterWidth(c, w);

		drawBitmap.getBitmap(mtotalWidthPixels, mtotalHeightPixels, mvisibleTopLeftX, mvisibleTopLeftY, mvisibleBottomRightX,
				mvisibleBottomRightY, imageBuffer, imageBufferSize);

		return imageBuffer;
	}

	//图像改变
	public int[] resize(int totalWidthPixels, int totalHeightPixels,
			int visibleTopLeftX, int visibleTopLeftY, int visibleBottomRightX,
			int visibleBottomRightY) {

		imageBuffer = null;
		if (drawBitmap != null) {
			mtotalWidthPixels  = totalWidthPixels;
			mtotalHeightPixels = totalHeightPixels;
			mvisibleTopLeftX   = visibleTopLeftX;
			mvisibleTopLeftY   = visibleTopLeftY;
			mvisibleBottomRightX = visibleBottomRightX;
			mvisibleBottomRightY = visibleBottomRightY;
			
			imageBufferSize = drawBitmap.getBitmap(mtotalWidthPixels, mtotalHeightPixels, mvisibleTopLeftX, mvisibleTopLeftY,
					mvisibleBottomRightX, mvisibleBottomRightY, new int[1], 0);

			imageBuffer = new int[imageBufferSize];
			drawBitmap.getBitmap(mtotalWidthPixels, mtotalHeightPixels, mvisibleTopLeftX, mvisibleTopLeftY,
					mvisibleBottomRightX, mvisibleBottomRightY, imageBuffer, imageBufferSize);
		}
		
		return imageBuffer;
	}

	private void resetInternalState() {
		frames = 0;
		width = 0;
		height = 0;
	    dataSet = null;

		if (stream != null) {
			stream.delete();
			stream = null;
		}

		if (transformsChain != null) {
			transformsChain.delete();
			transformsChain = null;
		}

		if (modalityVOILUT != null) {
			modalityVOILUT.delete();
			modalityVOILUT = null;
		}

		if (voilut != null) {
			voilut.delete();
			voilut = null;
		}

		if (drawBitmap != null) {
			drawBitmap.delete();
			drawBitmap = null;
		}

	}

	private void readMedaData() {
		if (dataSet != null)
			return;

		dataSet = CodecFactory.load(new StreamReader(stream), 256);
		image = dataSet.getImage(0);
		if (image != null) {
			width = image.getSizeX();
			height = image.getSizeY();
			frames = dataSet.getUnsignedLong(Tag.NumberOfFrames >> 4, 0,
					Tag.NumberOfFrames & 0x0000FFFF, 0);

			if (ColorTransformsFactory.isMonochrome(image.getColorSpace())) {
				modalityVOILUT = new ModalityVOILUT(dataSet);
				if (!modalityVOILUT.isEmpty()) {
					Image modalityImage = modalityVOILUT.allocateOutputImage(
							image, image.getSizeX(), image.getSizeY());
					modalityVOILUT.runTransform(image, 0, 0, image.getSizeX(),
							image.getSizeY(), modalityImage, 0, 0);
					image = modalityImage;
				}
			}

			transformsChain = new TransformsChain();

			if (ColorTransformsFactory.isMonochrome(image.getColorSpace())) {
				voilut = new VOILUT(dataSet);
				int voilutId = voilut.getVOILUTId(0);
				if (voilutId != 0) {
					voilut.setVOILUT(voilutId);
				} else {
					voilut.applyOptimalVOI(image, 0, 0, image.getSizeX(),
							image.getSizeY());
				}
				transformsChain.addTransform(voilut);

			}

			windowWidth = voilut.getWidth();
			windowCenter = voilut.getCenter();

		}

	}

	private void checkIndex(int frameIndex) {
		if (frames == 0)
			throw new IllegalStateException("Missing Pixel Data");

		if (frameIndex < 0 || frameIndex >= frames)
			throw new IndexOutOfBoundsException("imageIndex: " + frameIndex);
	}

}
