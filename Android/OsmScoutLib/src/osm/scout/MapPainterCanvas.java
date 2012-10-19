/*
  This source is part of the libosmscout library
  Copyright (C) 2010  Tim Teulings

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/

package osm.scout;

import java.util.Enumeration;
import java.util.Vector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;

public class MapPainterCanvas {
	
	private Canvas mCanvas=null;
	private Paint mPaint=null;
	private Rect mCanvasSize=null;
	
	private int mJniMapPainterIndex;
	
	private Vector<Bitmap> mIconArray=null;
	private Vector<Bitmap> mPatternArray=null;

	private Vector<Path> mClippingPaths=null;
	
	private final static float FONT_SCALE=10;
		
	public MapPainterCanvas() {
		
		mJniMapPainterIndex=jniConstructor();
		
		mPaint=new Paint();
		
		mPaint.setAntiAlias(true);
	
		mIconArray=new Vector<Bitmap>();
		mPatternArray=new Vector<Bitmap>();
		
		mClippingPaths=new Vector<Path>();		
	}
	
	protected void finalize() throws Throwable {
		
		try {			
			jniDestructor(mJniMapPainterIndex);
		}
		finally {			
			super.finalize();
		}
	}
	
	public boolean drawMap(StyleConfig styleConfig, MercatorProjection projection,
			MapParameter mapParameter, MapData mapData, Canvas canvas) {
		
		mCanvas=canvas;
		
		mCanvasSize=new Rect(0, 0, mCanvas.getWidth(), mCanvas.getHeight());
		
		return jniDrawMap(mJniMapPainterIndex, styleConfig.getJniObjectIndex(),
                projection.getJniObjectIndex(), mapParameter.getJniObjectIndex(),
                mapData.getJniObjectIndex());
	}
	
	public void drawSymbol(int style, int color, float size, float x, float y) {
		
		mPaint.setColor(color);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setStrokeWidth(1);
		
		switch(style) {
		
		case 1: // Box
			
			mCanvas.drawRect(x-size/2, y-size/2, x+size/2, y+size/2, mPaint);
			break;
			
		case 2: // Circle
			
			mCanvas.drawCircle(x, y, size, mPaint);
			break;
			
		case 3: // Triangle
			
			Path path=new Path();
			path.moveTo(x-size/2, y+size/2);
			path.lineTo(x, y-size/2);
			path.lineTo(x+size/2, y+size/2);
			path.close();
			
			mCanvas.drawPath(path, mPaint);
			break;
			
		default: // None or undefined
			break;
		}
	}
	
	public int loadIconPNG(String iconPath) {
		
		Bitmap bitmap=BitmapFactory.decodeFile(iconPath);
		
		if (bitmap==null) {
			
			// Error loading icon file
			return -1;
		}
		
		mIconArray.add(bitmap);
		
		return mIconArray.size();
	}
	
	public int loadPatternPNG(String patternPath) {
		
		Bitmap bitmap=BitmapFactory.decodeFile(patternPath);
		
		if (bitmap==null) {
			
			// Error loading icon file
			return -1;
		}
		
		mPatternArray.add(bitmap);
		
		return mPatternArray.size();
	}
	
	public void drawIcon(int iconIndex, float x, float y) {
		
		if ((iconIndex<0) || (iconIndex>=mIconArray.size()))
			return;
		
		Bitmap bitmap=mIconArray.elementAt(iconIndex);
		
		mCanvas.drawBitmap(bitmap, x-bitmap.getWidth()/2, y-bitmap.getHeight()/2, mPaint);
	}

	public void drawPath(int color, float width, float[] dash,
			boolean roundedStartCap, boolean roundedEndCap,
			float[] x, float[] y) {
		
		Path path=new Path();
		
		path.moveTo(x[0], y[0]);
		
		int numPoints=x.length;
		
		for(int i=0; i<numPoints; i++) {
			
			path.lineTo(x[i], y[i]);
		}
		
		mPaint.setColor(color);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(width);
		
		if ((dash!=null) && (dash.length>=2)) {
			
			DashPathEffect dashPathEffect=new DashPathEffect(dash, 0);		
			mPaint.setPathEffect(dashPathEffect);
		}
		else {
			
			mPaint.setPathEffect(null);
		}
		
		if ((roundedStartCap) && (roundedEndCap)) {
			
			// Both start and end caps are rounded
			mPaint.setStrokeCap(Paint.Cap.ROUND);
		}
		else {
			mPaint.setStrokeCap(Paint.Cap.BUTT);
		}
		
		mCanvas.drawPath(path, mPaint);
		
		if ((roundedStartCap) && (!roundedEndCap)) {
			
			// Only the start cap is rounded
			Path startPath=new Path();
			
			startPath.moveTo(x[0], y[0]);
			startPath.lineTo(x[0], y[0]);
			
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			
			mCanvas.drawPath(startPath, mPaint);			
		}
		
		if ((!roundedStartCap) && (roundedEndCap)) {
			
			// Only the end cap is rounded
			Path endPath=new Path();
			
			endPath.moveTo(x[numPoints-1], y[numPoints-1]);
			endPath.lineTo(x[numPoints-1], y[numPoints-1]);
			
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			
			mCanvas.drawPath(endPath, mPaint);			
		}
		
		mPaint.setPathEffect(null);
	}
	
	public void addClipArea(float[] x, float[] y) {
		
		Path clipPath=new Path();
		
		clipPath.moveTo(x[0], y[0]);
		
		for(int i=1; i<x.length; i++) {
			
			clipPath.lineTo(x[i], y[i]);
		}
		
		clipPath.close();
		
		mClippingPaths.add(clipPath);
	}
	
	public void drawFilledArea(int fillColor, int borderColor, float borderWidth,
			float[] x, float[] y) {
		
		Path areaPath=new Path();
		
		areaPath.moveTo(x[0], y[0]);
		
		for(int i=1; i<x.length; i++) {
			
			areaPath.lineTo(x[i], y[i]);
		}
		
		areaPath.close();
		
		// Add internal clipping paths (if any) to outer area path
		if (mClippingPaths.size()>0) {
			
			Enumeration<Path> e=mClippingPaths.elements();
					
			while(e.hasMoreElements()) {
						
				areaPath.addPath(e.nextElement());
			}
		
			areaPath.setFillType(Path.FillType.EVEN_ODD);
		}
		
		// Draw area fill
		mPaint.setColor(fillColor);
		mPaint.setStyle(Paint.Style.FILL);
					
		mCanvas.drawPath(areaPath, mPaint);
		
		if (borderWidth>0) {
			
			// Draw area border
			mPaint.setColor(borderColor);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(borderWidth);
							
			mCanvas.drawPath(areaPath, mPaint);
		}
		
		if (mClippingPaths.size()>0) {
			removeClippingPaths();
		}
	}
	
	public void drawPatternArea(int patternId, float[] x, float[] y) {
		
		Path areaPath=new Path();
		
		areaPath.moveTo(x[0], y[0]);
		
		RectF box=new RectF(x[0], y[0], x[0], y[0]);
		
		for(int i=1; i<x.length; i++) {
			
			areaPath.lineTo(x[i], y[i]);
			
			if (x[i]<box.left)
				box.left=x[i];
			
			if (x[i]>box.right)
				box.right=x[i];
			
			if (y[i]<box.top)
				box.top=y[i];
			
			if (y[i]>box.bottom)
				box.bottom=y[i];
		}
		
		areaPath.close();
		
		// Set clipping region
		mCanvas.clipPath(areaPath);
		
		// Add internal clipping paths (if any) to outer area path
		if (mClippingPaths.size()>0) {
					
			Enumeration<Path> e=mClippingPaths.elements();
							
			while(e.hasMoreElements()) {
								
				areaPath.addPath(e.nextElement());
			}
				
			areaPath.setFillType(Path.FillType.EVEN_ODD);
		}
				
		Bitmap pattern=mPatternArray.elementAt(patternId);
		
		for (float xpos=box.left; xpos<=box.right; xpos+=pattern.getWidth()) {
			
			for (float ypos=box.top; ypos<box.bottom; ypos+=pattern.getHeight()) {
				
				mCanvas.drawBitmap(pattern, xpos, ypos, mPaint);
			}	
		}
		
		removeClippingPaths();
	}
	
	private void removeClippingPaths() {
		
		mClippingPaths.removeAllElements();
		
		// Restore full screen clipping region 
		mCanvas.clipRect(mCanvasSize, Region.Op.REPLACE);
	}

	public void drawArea(int fillColor, int borderColor, float borderWidth,
			float[] x, float[] y) {
		
		Path areaPath=new Path();
		
		areaPath.moveTo(x[0], y[0]);
		
		int numPoints=x.length;
		
		for(int i=0; i<numPoints; i++) {
			
			areaPath.lineTo(x[i], y[i]);
		}
		
		areaPath.close();
		
		// Draw area fill
		mPaint.setColor(fillColor);
		mPaint.setStyle(Paint.Style.FILL);
					
		mCanvas.drawPath(areaPath, mPaint);
		
		if (borderWidth>0.0) {
			
			// Draw area border
			mPaint.setColor(borderColor);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(borderWidth);
							
			mCanvas.drawPath(areaPath, mPaint);
		}
	}
	
	public void drawArea(int color, float x, float y, float width, float height) {
		
		RectF rect=new RectF(x, y, width, height);
		
		mPaint.setColor(color);
		mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		
		mCanvas.drawRect(rect, mPaint);
	}
	
	public Rect getTextDimension(String text, float fontSize) {
		
		Rect rect=new Rect();
		
		mPaint.setTextSize(fontSize*FONT_SCALE);
		
		mPaint.getTextBounds(text, 0, text.length(), rect);
		
		return rect;		
	}
	
	public void drawLabel(String text, float fontSize, float x, float y,
			int textColor, int labelStyle) {
		
		mPaint.setTextSize(fontSize*FONT_SCALE);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setTextAlign(Paint.Align.LEFT);
		
		Paint.FontMetrics fontMetrics=mPaint.getFontMetrics();
		
		switch(labelStyle) {
		
		case 1: // Normal
			
			break;
		
		default: // Draw white border 
			
			mPaint.setColor(Color.WHITE);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(1);
			mCanvas.drawText(text, x, y-fontMetrics.ascent, mPaint);
			
			break;
		}
		
		mPaint.setStyle(Paint.Style.FILL);
		
		mPaint.setColor(textColor);
		mCanvas.drawText(text, x, y-fontMetrics.ascent, mPaint);		
	}
	
	public void drawPlateLabel(String text, float fontSize, RectF box,
			int textColor, int bgColor, int borderColor) {
		
		mPaint.setColor(bgColor);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStyle(Paint.Style.FILL);
		
		mCanvas.drawRoundRect(box, 2, 2, mPaint);
		
		// Reduce size of box to draw box border
		box.inset(2, 2);
		
		mPaint.setColor(borderColor);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(1);
		
		mCanvas.drawRoundRect(box, 2, 2, mPaint);
		
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setTextSize(fontSize*FONT_SCALE);
		mPaint.setTextAlign(Paint.Align.LEFT);
		mPaint.setColor(textColor);
		
		mCanvas.drawText(text, box.left+2, box.bottom-3, mPaint);
	}
	
	public void drawContourLabel(String text, int textColor, float fontSize,
								float pathLenght, float[] x, float[] y) {
		
		if (getTextDimension(text, fontSize).width()>pathLenght) {
			
			// Text is longer than path to draw on
			return;
		}
		
		mPaint.setTextSize(fontSize*FONT_SCALE);
		mPaint.setTextAlign(Paint.Align.CENTER);
		mPaint.setColor(textColor);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		
		Path path=new Path();
		
		path.moveTo(x[0], y[0]);
		
		for(int i=1; i<x.length; i++) {
			
			path.lineTo(x[i], y[i]);
		}
		
		Paint.FontMetrics fontMetrics=mPaint.getFontMetrics();
		
		mCanvas.drawTextOnPath(text, path, 0, fontMetrics.descent+1, mPaint);
	}
	
	// Private native methods
	
	private native int jniConstructor();
	private native void jniDestructor(int mapPainterIndex);
	private native boolean jniDrawMap(int mapPainterIndex, int styleConfigIndex,
                           int projectionIndex, int mapParameter, int mapDataIndex);
}
