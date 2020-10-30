package unirioja.Motilidad;

import java.awt.Polygon;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.plugin.frame.RoiManager;

public class Utils {
	
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
	
	
	protected static List<String> searchFiles() {

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

	protected static void search(final String pattern, final File folder, List<String> result) {
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

}
