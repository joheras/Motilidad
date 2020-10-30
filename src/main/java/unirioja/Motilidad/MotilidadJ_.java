package unirioja.Motilidad;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.border.Border;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>Motilidad>Main")
public class MotilidadJ_ implements Command {

	private boolean isDark = false;

	public void run() {

		// Set the measurements that we want to obtain
		IJ.run("Set Measurements...",
				"area mean standard min shape centroid feret's median kurtosis redirect=None decimal=3");

		List<String> files = Utils.searchFiles();
		String dir = files.get(0);
		// Create save directory
		new File(dir + "/preds").mkdir();

		files.remove(0);
		JFrame frame = new JFrame("Work in progress");
		JProgressBar progressBar = new JProgressBar();
		int n = 0;
		progressBar.setValue(0);
		progressBar.setString("");
		progressBar.setStringPainted(true);
		progressBar.setMaximum(files.size());
		Border border = BorderFactory.createTitledBorder("Processing...");
		progressBar.setBorder(border);
		Container content = frame.getContentPane();
		content.add(progressBar, BorderLayout.NORTH);
		frame.setSize(300, 100);
		frame.setVisible(true);
		ImagePlus imp;
		RoiManager rm = RoiManager.getRoiManager();
		if (rm == null) {
			rm = new RoiManager();
		}
		rm.setVisible(false);
		// For each file in the folder we detect the esferoid on it.
		for (String name : files) {
			//
			/*
			 * Roi r1 = interestingRegion(name); imp = detect(name, r1);// ,r1
			 */

			// 1. We first crop the image.
			imp = cropImage(name);
			imp = detectImageSimple(imp);
			OvalRoi r1 = new OvalRoi(55, 45, 910, 910);

			processOutput(imp, dir, name, r1);//
			n++;
			progressBar.setValue(n);
			// IJ.showMessage("...");

		}
		frame.setVisible(false);
		frame.dispose();
		rm = RoiManager.getRoiManager();
		rm.setVisible(true);
		rm.close();
		IJ.showMessage("Process finished");

	}

	private ImagePlus cropImage(String path) {
		ImagePlus imp = IJ.openImage(path);

		imp = IJ.getImage();
		ImagePlus imp2 = imp.duplicate();

		imp.hide();
		// ImagePlus imp2 = imp.duplicate();
		imp.setRoi(3, 4, 93, 71);
		ImageStatistics is = imp.getStatistics();
		imp.deleteRoi();
		IJ.run(imp, "Median...", "radius=3");
		double m = is.mean;
		if (m < 2000) {
			IJ.run(imp, "8-bit", "");
			IJ.setAutoThreshold(imp, "Default");
			IJ.setRawThreshold(imp, 255, 255, null);
			isDark = true;
		} else {
			IJ.setAutoThreshold(imp, "Default");
			IJ.setRawThreshold(imp, 0, 5, null);
			isDark = false;
		}
		IJ.run(imp, "Convert to Mask", "");

		IJ.run(imp, "Select Bounding Box", "");
		Roi r = imp.getRoi();
		Roi r1 = new Roi(r.getXBase(), 0, r.getFloatWidth(), imp.getHeight());
		imp.changes = false;
		imp.close();
		imp2.setRoi(r1);
		imp2 = imp2.crop();
		ImagePlus imp3 = imp2.duplicate();

		IJ.run(imp2, "Median...", "radius=5");
		IJ.run(imp2, "8-bit", "");
		if (m < 2000) {
			IJ.run(imp2, "Auto Local Threshold", "method=Bernsen radius=15 parameter_1=0 parameter_2=0 white");
		} else {
			IJ.run(imp2, "Auto Local Threshold", "method=Bernsen radius=15 parameter_1=0 parameter_2=0");
		}
		IJ.run(imp2, "Select Bounding Box", "");
		imp3.setRoi(imp2.getRoi());
		imp3 = imp3.crop();
		imp2.close();

		return imp3;

	}

	private void processOutput(ImagePlus imp2, String dir, String path, Roi r) {
		String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));

		// ImagePlus imp = imp2.duplicate();
		// Processing the output
		RoiManager rm = RoiManager.getInstance();
		if (rm != null) {
			rm.setVisible(false);

			// double imageMean = Utils.mean(imp2.getProcessor().getHistogram());
			// System.out.println(imageMean);
			// System.out.println("----------------------");

			ImageStatistics is;

			Utils.keepBiggestROI(rm);
			if (rm.getRoisAsArray().length > 0) {
				imp2.setRoi(rm.getRoi(0));
				IJ.run(imp2, "Fit Spline", "");
				/*
				 * getCentroid(imp2.getProcessor(),rm.getRoi(0)); double x1 = xCentroid; double
				 * y1 = yCentroid;
				 */
				rm.addRoi(imp2.getRoi());
				rm.select(0);
				rm.runCommand(imp2, "Delete");

				rm.addRoi(r);
				rm.setSelectedIndexes(new int[] { 0, 1 });
				rm.runCommand(imp2, "XOR");

				is = imp2.getStatistics();
				rm.select(-1);

				if (!isDark) {

					if (is.mean < 1900) {
						rm.select(0);
						rm.runCommand(imp2, "Delete");
					} else {
						rm.select(1);
						rm.runCommand(imp2, "Delete");
					}
				} else {
					rm.select(1);
					rm.runCommand(imp2, "Delete");
				}
			} else {
				rm.addRoi(r);
			}
			/*
			 * rm.select(1); rm.runCommand(imp2, "Delete");
			 */

			rm.runCommand(imp2, "Show All without labels");
			rm.runCommand(imp2, "Draw");
			rm.runCommand("Save", dir + "/preds/" + name + ".zip");
			rm.runCommand(imp2, "Delete");
			IJ.saveAs(imp2, "JPG", dir + "/preds/" + name + "_pred.jpg");

		}
		// imp2.close();

	}

	private ImagePlus detectImageSimple(ImagePlus imp1) {
		OvalRoi r = new OvalRoi(55, 45, 910, 910);

		ImagePlus imp2 = imp1.duplicate();
		imp2.hide();
		ImagePlus imp = imp2.duplicate();
		imp2.setRoi(r);

		IJ.run(imp2, "Median...", "radius=13");

		ImagePlus imp3 = imp2.duplicate();
		// IJ.run(imp2, "Variance...", "radius=1");
		IJ.run(imp2, "Find Edges", "");

		IJ.run(imp2, "8-bit", "");
		IJ.setAutoThreshold(imp2, "MinError dark");

		int i = 6;
		boolean detected = false;
		double s = 0;
		RoiManager rm = null;
		ImagePlus impT;
		ResultsTable rt = null;
		double x = 0, y = 0;
		while (i >= 3 && !detected) {
			impT = imp2.duplicate();

			IJ.setRawThreshold(impT, i, 255, null);

			IJ.run(impT, "Convert to Mask", "");

			impT.setRoi(r);
			IJ.run(impT, "Clear Outside", "");
			IJ.run(impT, "Fill Holes", "");
			IJ.run(impT, "Erode", "");
			IJ.run(impT, "Analyze Particles...", "  circularity=0.0-1.00 add");
			rm = RoiManager.getInstance();
			Utils.keepBiggestROI(rm);

			rm.select(0);
			rm.runCommand(impT, "Measure");
			rt = ResultsTable.getResultsTable();
			int n = rt.size();

			if (n != 1) {

				
				imp2.changes = false;
				imp2.close();
				imp3.changes = false;
				imp3.close();
				return imp;
			}

			s = rt.getValue("Solidity", 0);
			x = rt.getValue("X", 0);
			y = rt.getValue("Y", 0);

			rt.reset();
			if (WindowManager.getFrame("Results") != null) {
				IJ.selectWindow("Results");
				IJ.run("Close");
			}

			// In case nothing is detected, we directly apply a threshold mechanism
			impT.changes = false;
			impT.close();
			if (s > 0.5 && ((x > 350) && (x < 650) && (y > 350) && (y < 650))) {
				detected = true;
			} else {
				i--;
			}

		}

		if (s < 0.5) {
			rm.runCommand("Delete");
			if (isDark) {
				IJ.setAutoThreshold(imp3, "Default dark");
			} else {
				IJ.setAutoThreshold(imp3, "Default");
			}
			IJ.run(imp3, "Convert to Mask", "");

			imp3.setRoi(r);
			IJ.run(imp3, "Clear Outside", "");
			IJ.run(imp3, "Fill Holes", "");
			IJ.run(imp3, "Erode", "");
			IJ.run(imp3, "Analyze Particles...", "  circularity=0.0-1.00 add");

		}

		rm = RoiManager.getInstance();
		Utils.keepBiggestROI(rm);

		rm.select(0);
		rm.runCommand(imp3, "Measure");
		rt = ResultsTable.getResultsTable();
		int n = rt.size();

		if (n != 1) {
			imp2.changes = false;
			imp2.close();
			imp3.changes = false;
			imp3.close();
			return imp;
		}

		x = rt.getValue("X", 0);
		y = rt.getValue("Y", 0);
		rt.reset();
		if (WindowManager.getFrame("Results") != null) {
			IJ.selectWindow("Results");
			IJ.run("Close");
		}
		if (!((x > 350) && (x < 650) && (y > 350) && (y < 650))) {

			rm.runCommand("Delete");
			// rm.addRoi(r);
		}

		imp3.changes = false;
		imp3.close();

		return imp;

	}

}
