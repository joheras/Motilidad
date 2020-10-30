package unirioja.Motilidad;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.ImageCalculator;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;

public class OldCode {


	private ImagePlus detectImage(ImagePlus imp1) {
		OvalRoi r = new OvalRoi(55, 45, 920, 920);

		ImagePlus imp2 = imp1.duplicate();
		imp2.hide();
		ImagePlus imp = imp2.duplicate();

		ImagePlus imp3 = imp2.duplicate();
		ImagePlus imp4 = imp2.duplicate();
		imp3.setRoi(3, 4, 93, 71);
		ImageStatistics is = imp3.getStatistics();
		imp3.deleteRoi();

		ImageCalculator ic = new ImageCalculator();

		if (is.mean < 2000) {
			IJ.setAutoThreshold(imp3, "MaxEntropy dark");
			IJ.run(imp3, "Convert to Mask", "");
			imp3.setRoi(r);
			IJ.setBackgroundColor(0, 0, 0);
			IJ.run(imp3, "Clear Outside", "");

			imp3.deleteRoi();
			RoiManager rm = RoiManager.getInstance();
			rm.setVisible(false);
			// rm.select(0);
			// rm.runCommand(imp3, "Delete");
		} else {
			IJ.setAutoThreshold(imp3, "MaxEntropy");
			IJ.setAutoThreshold(imp4, "Default");
			IJ.run(imp3, "Convert to Mask", "");
			IJ.run(imp4, "Convert to Mask", "");
			imp3.setRoi(r);
			IJ.setBackgroundColor(0, 0, 0);
			IJ.run(imp3, "Clear Outside", "");
			imp4.setRoi(r);
			IJ.run(imp4, "Clear Outside", "");

			imp3.deleteRoi();
			imp4.deleteRoi();
			RoiManager rm = RoiManager.getInstance();
			rm.setVisible(false);
			// rm.select(0);
			// rm.runCommand(imp3, "Delete");
			// rm.runCommand(imp4, "Delete");
			imp3 = ic.run("OR create", imp3, imp4);

		}
		/// Using the variance
		// imp2.setRoi(r);
		// imp2.deleteRoi();
		IJ.run(imp2, "Variance...", "radius=19");
		IJ.setAutoThreshold(imp2, "MinError dark");// MinError
		IJ.run(imp2, "Convert to Mask", "");

		// OvalRoi r1 = new OvalRoi(r.getXBase() + 20, r.getYBase() + 20,
		// r.getFloatWidth() - 40, r.getFloatHeight() - 40);

		imp2.setRoi(r);
		IJ.setBackgroundColor(0, 0, 0);
		IJ.run(imp2, "Clear Outside", "");

		imp2.deleteRoi();
		/*
		 * rm = RoiManager.getInstance(); rm.select(0); rm.runCommand(imp2, "Delete");
		 */

		imp4 = ic.run("OR create", imp2, imp3);
		IJ.run(imp4, "Fill Holes", "");
		IJ.run(imp4, "Options...", "iterations=10 count=1 black do=Erode");

		IJ.run(imp4, "Analyze Particles...", "  circularity=0.0-1.00 add");
		imp3.show();
		imp2.show();
		imp4.changes = false;
		imp4.close();
		imp2.changes = false;
		imp2.close();
		imp3.changes = false;
		imp3.close();
		return imp;

	}

	private ImagePlus detect(String path, Roi r) {//
		ImagePlus imp = IJ.openImage(path);

		ImagePlus imp2 = IJ.getImage();
		imp2.hide();
		imp = imp2.duplicate();

		ImagePlus imp3 = imp2.duplicate();
		ImagePlus imp4 = imp2.duplicate();
		imp3.setRoi(3, 4, 93, 71);
		ImageStatistics is = imp3.getStatistics();
		imp3.deleteRoi();

		ImageCalculator ic = new ImageCalculator();

		if (is.mean < 2000) {
			IJ.setAutoThreshold(imp3, "MaxEntropy dark");
			IJ.run(imp3, "Convert to Mask", "");
			imp3.setRoi(r);
			IJ.setBackgroundColor(0, 0, 0);
			IJ.run(imp3, "Clear Outside", "");

			imp3.deleteRoi();
			RoiManager rm = RoiManager.getInstance();
			rm.setVisible(false);
			rm.select(0);
			rm.runCommand(imp3, "Delete");
		} else {
			IJ.setAutoThreshold(imp3, "MaxEntropy");
			IJ.setAutoThreshold(imp4, "Default");
			IJ.run(imp3, "Convert to Mask", "");
			IJ.run(imp4, "Convert to Mask", "");
			imp3.setRoi(r);
			IJ.setBackgroundColor(0, 0, 0);
			IJ.run(imp3, "Clear Outside", "");
			imp4.setRoi(r);
			IJ.run(imp4, "Clear Outside", "");

			imp3.deleteRoi();
			imp4.deleteRoi();
			RoiManager rm = RoiManager.getInstance();
			rm.setVisible(false);
			rm.select(0);
			rm.runCommand(imp3, "Delete");
			// rm.runCommand(imp4, "Delete");
			imp3 = ic.run("OR create", imp3, imp4);

		}

		/// Using the variance
		// imp2.setRoi(r);
		imp2.deleteRoi();
		IJ.run(imp2, "Variance...", "radius=19");
		IJ.setAutoThreshold(imp2, "MinError dark");// MinError
		IJ.run(imp2, "Convert to Mask", "");

		OvalRoi r1 = new OvalRoi(r.getXBase() + 20, r.getYBase() + 20, r.getFloatWidth() - 40, r.getFloatHeight() - 40);

		imp2.setRoi(r1);
		IJ.setBackgroundColor(0, 0, 0);
		IJ.run(imp2, "Clear Outside", "");

		imp2.deleteRoi();
		/*
		 * rm = RoiManager.getInstance(); rm.select(0); rm.runCommand(imp2, "Delete");
		 */

		imp4 = ic.run("OR create", imp2, imp3);
		IJ.run(imp4, "Fill Holes", "");
		IJ.run(imp4, "Options...", "iterations=10 count=1 black do=Erode");

		IJ.run(imp4, "Analyze Particles...", "  circularity=0.0-1.00 add");
		imp3.show();
		imp2.show();
		imp4.changes = false;
		imp4.close();
		imp2.changes = false;
		imp2.close();
		imp3.changes = false;
		imp3.close();
		return imp;
	}

	private boolean isDark = false;
	
	private Roi interestingRegion(String path) {
		ImagePlus imp = IJ.openImage(path);

		imp = IJ.getImage();
		imp.hide();
		// ImagePlus imp2 = imp.duplicate();
		imp.setRoi(3, 4, 93, 71);
		ImageStatistics is = imp.getStatistics();
		imp.deleteRoi();

		if (is.mean < 2000) {
			IJ.setAutoThreshold(imp, "MaxEntropy dark");
			isDark = true;
		} else {
			IJ.setAutoThreshold(imp, "Default");
			IJ.setRawThreshold(imp, 0, 100, null);
			isDark = false;
		}
		IJ.run(imp, "Convert to Mask", "");
		IJ.run(imp, "Fit Circle to Image", "threshold=5");
		IJ.setBackgroundColor(0, 0, 0);
		IJ.run(imp, "Clear", "slice");
		IJ.run(imp, "Fit Circle to Image", "threshold=5");
		while (Utils.getArea(imp.getRoi().getPolygon()) < 650000) {
			IJ.setBackgroundColor(0, 0, 0);
			IJ.run(imp, "Clear", "slice");
			IJ.run(imp, "Fit Circle to Image", "threshold=5");
		}

		OvalRoi r = (OvalRoi) imp.getRoi();
		OvalRoi r1 = new OvalRoi(r.getXBase() + 25, r.getYBase() + 25, r.getFloatWidth() - 50, r.getFloatHeight() - 50);
		// imp.setRoi(r1);
		RoiManager rm = RoiManager.getRoiManager();
		if (rm == null) {
			rm = new RoiManager();
		}
		rm.setVisible(false);
		rm.addRoi(r1);
		imp.changes = false;
		imp.close();
		return r1;

	}
	

	
	
	
}
