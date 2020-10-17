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
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>Motilidad")
public class MotilidadJ_ implements Command {

	private boolean isDark = false;

	public void run() {

		List<String> files = searchFiles();
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
		// For each file in the folder we detect the esferoid on it.
		for (String name : files) {
			//
			Roi r1 = interestingRegion(name);
			imp = detect(name, r1);// ,r1
			processOutput(imp, dir, name, r1);//
			n++;
			progressBar.setValue(n);
			// IJ.showMessage("...");

		}
		frame.setVisible(false);
		frame.dispose();
		RoiManager rm = RoiManager.getRoiManager();
		rm.setVisible(true);
		rm.close();
		IJ.showMessage("Process finished");

	}

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
			IJ.setRawThreshold(imp, 0, 1000, null);
			isDark = false;
		}
		IJ.run(imp, "Convert to Mask", "");
		IJ.run(imp, "Fit Circle to Image", "threshold=5");
		IJ.setBackgroundColor(0, 0, 0);
		IJ.run(imp, "Clear", "slice");
		IJ.run(imp, "Fit Circle to Image", "threshold=5");
		while (getArea(imp.getRoi().getPolygon()) < 650000) {
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

	private static List<String> searchFiles() {

		List<String> result = new ArrayList<String>();

		// We ask the user for a directory with nd2 images.
		DirectoryChooser dc = new DirectoryChooser("Select the folder containing the jpg images");
		String dir = dc.getDirectory();

		// We store the list of tiff files in the result list.
		File folder = new File(dir);

		search(".*1sc", folder, result);

		Collections.sort(result);
		result.add(0, dir);
		return result;

	}

	public static void search(final String pattern, final File folder, List<String> result) {
		for (final File f : folder.listFiles()) {

			if (f.isDirectory()) {
				search(pattern, f, result);
			}

			if (f.isFile()) {
				if (f.getName().matches(pattern) && !f.getName().contains("pred")) {
					result.add(f.getAbsolutePath());
				}
			}

		}
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

			keepBiggestROI(rm);
			imp2.setRoi(rm.getRoi(0));
			IJ.run(imp2, "Fit Spline", "");
			/*
			 * getCentroid(imp2.getProcessor(),rm.getRoi(0)); double x1 = xCentroid; double
			 * y1 = yCentroid;
			 */
			rm.addRoi(imp2.getRoi());
			rm.select(0);
			rm.runCommand(imp2, "Delete");

			rm.addRoi(
					new OvalRoi(r.getXBase() - 40, r.getYBase() - 40, r.getFloatWidth() + 80, r.getFloatHeight() + 80));
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
					ImagePlus imp3 = imp2.duplicate();
					
					// Performing some cleaning
					Roi initRoi = rm.getRoi(0);
					rm.select(0);
					imp3.setRoi(initRoi);
					IJ.run(imp3, "Select Bounding Box", "");
					Roi box = imp3.getRoi();
					IJ.run(imp3, "Median...", "radius=5");
					IJ.run(imp3, "Enhance Contrast", "saturated=0.35");
					IJ.setAutoThreshold(imp3, "MinError");
					IJ.run(imp3, "Convert to Mask", "");
					imp3.setRoi(box);
					IJ.setBackgroundColor(0, 0, 0);
					IJ.run(imp3, "Clear Outside", "");
					rm.select(0);
					rm.runCommand(imp3, "Delete");
					IJ.run(imp3, "Analyze Particles...", "add");
					keepBiggestROI(rm);
					rm.addRoi(initRoi);
					rm.setSelectedIndexes(new int[] { 0, 1 });
					rm.runCommand(imp3, "AND");
					rm.addRoi(imp3.getRoi());
					rm.setSelectedIndexes(new int[] { 0, 1 });
					rm.runCommand(imp3, "Delete");
					
				}
			}else {
				System.out.println(is.mean);
			}
			
			
			rm.runCommand(imp2, "Show All without labels");
			rm.runCommand(imp2, "Draw");
			rm.runCommand("Save", dir + "/preds/" + name + ".zip");
			rm.runCommand(imp2, "Delete");
			IJ.saveAs(imp2, "JPG", dir + "/preds/" + name + "_pred.jpg");

		}
		// imp2.close();

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

	protected static void keepBiggestROI(RoiManager rm) {
		if (rm != null) {
			Roi[] rois = rm.getRoisAsArray();

			if (rois.length >= 1) {
				rm.runCommand("Select All");
				rm.runCommand("Delete");

				Roi biggestROI = rois[0];

				for (int i = 1; i < rois.length; i++) {

					if (getArea(biggestROI.getPolygon()) < getArea(rois[i].getPolygon())) {

						biggestROI = rois[i];
					}

				}
//					IJ.showMessage(""+getArea(biggestROI.getPolygon()));
				rm.addRoi(biggestROI);

			}

		}
	}

	protected static final double getArea(Polygon p) {
		if (p == null)
			return Double.NaN;
		int carea = 0;
		int iminus1;
		for (int i = 0; i < p.npoints; i++) {
			iminus1 = i - 1;
			if (iminus1 < 0)
				iminus1 = p.npoints - 1;
			carea += (p.xpoints[i] + p.xpoints[iminus1]) * (p.ypoints[i] - p.ypoints[iminus1]);
		}
		return (Math.abs(carea / 2.0));
	}

}
