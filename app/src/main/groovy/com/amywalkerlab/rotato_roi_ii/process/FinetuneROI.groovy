package com.amywalkerlab.rotato_roi_ii.process;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.util.Tools;
import ij.measure.Calibration;

public class FinetuneROI implements DialogListener {
	private static double width, height, xRoi, yRoi;
	private static double lockWidth, lockHeight;
	private final static int X_ROI = 0, Y_ROI = 1, WIDTH = 2, HEIGHT = 3;	//sequence of NumericFields
	private int iSlice;
	private ImagePlus imp;
    private boolean dialogWasCanceled;
    private Vector fields;
	private int stackSize;

	public void run(String arg) {
        dialogWasCanceled = false
		imp = IJ.getImage();
		if (!imp.okToDeleteRoi())
			return;
		stackSize = imp.getStackSize();
		Roi roi = imp.getRoi();
		Calibration cal = imp.getCalibration();
		if (roi!=null) {
			Rectangle r = roi.getBounds();
			width = r.width;
			height = r.height;
			xRoi = r.x;
			yRoi = r.y;
		} else if (!validDialogValues()) {
			width = imp.getWidth()/2;
			height = imp.getHeight()/2;
			xRoi = width/2;
			yRoi = height/2;
		}
		iSlice = imp.getCurrentSlice();
        lockWidth = width;
        lockHeight = height;
		showDialog();
	}

    public boolean wasCanceled(){
        return dialogWasCanceled;
    }

	boolean validDialogValues() {
		Calibration cal = imp.getCalibration();
		double pw=cal.pixelWidth, ph=cal.pixelHeight;
		if (width/pw<1 || height/ph<1)
			return false;
		if (xRoi/pw>imp.getWidth() || yRoi/ph>imp.getHeight())
			return false;
		return true;
	}

	void showDialog() {
		Calibration cal = imp.getCalibration();
		int digits = 0;
		Roi roi = imp.getRoi();
		if (roi==null)
			drawRoi();
		GenericDialog gd = new GenericDialog("Finetune Crop");
		gd.addNumericField("X coordinate:", xRoi, digits);
		gd.addNumericField("Y coordinate:", yRoi, digits);
		//gd.addNumericField("Width (Readonly):", width, digits);
		//gd.addNumericField("Height (Readonly):", height, digits);
		if (stackSize>1)
			gd.addNumericField("Slice:", iSlice, 0);
		fields = gd.getNumericFields();
		gd.addDialogListener(this);

		gd.showDialog();
        if (gd.wasCanceled()) {
            dialogWasCanceled = true
            if (roi==null)
                imp.deleteRoi();
            else // *ALWAYS* restore initial ROI when cancelled
                imp.setRoi(roi);
        }
		
	}
	
	void drawRoi() {
		double xPxl = xRoi;
		double yPxl = yRoi;
		double widthPxl = width;
		double heightPxl = height;
		Calibration cal = imp.getCalibration();
		Roi roi;
		roi = new Roi(xPxl, yPxl, widthPxl, heightPxl);
		imp.setRoi(roi);
	}

    @Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		if (IJ.isMacOSX()) IJ.wait(50);
		Calibration cal = imp.getCalibration();
		xRoi = gd.getNextNumber();	
		yRoi = gd.getNextNumber();
		//width = gd.getNextNumber();
		//height = gd.getNextNumber();
		if (stackSize>1)	
			iSlice = (int) gd.getNextNumber(); 
		if (gd.invalidNumber() || width<=0 || height<=0)
			return false;
		boolean newWidth = false, newHeight = false, newXY = false;
		//Vector numFields = gd.getNumericFields();
		//int digits = 0;
		// if (width!=lockWidth)
		// 	((TextField)(numFields.get(WIDTH))).setText(IJ.d2s(lockWidth, digits));
		// if (height!=lockHeight)
		// 	((TextField)(numFields.get(HEIGHT))).setText(IJ.d2s(lockHeight, digits));

		if (stackSize>1 && iSlice>0 && iSlice<=stackSize)
			imp.setSlice(iSlice);
		if (!newWidth && !newHeight	 && !newXY)	 
			drawRoi();
		return true;
	}

}