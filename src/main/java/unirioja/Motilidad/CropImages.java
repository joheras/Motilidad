package unirioja.Motilidad;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.border.Border;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>Motilidad>Crop")
public class CropImages implements Command {

	public void run() {
		// TODO Auto-generated method stub
		
		// Set the measurements that we want to obtain
				IJ.run("Set Measurements...",
						"area mean standard min shape centroid feret's median kurtosis redirect=None decimal=3");

				List<String> files = Utils.searchFiles();
				String dir = files.get(0);
				// Create save directory
				new File(dir + "/images").mkdir();

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
				for (String path : files) {
					//
					/*
					 * Roi r1 = interestingRegion(name); imp = detect(name, r1);// ,r1
					 */

					// 1. We first crop the image.
					imp = cropImage(path);
					String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));

					IJ.saveAs(imp, "JPG", dir + "/images/" + name + "_pred.jpg");
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
					
				} else {
					IJ.setAutoThreshold(imp, "Default");
					IJ.setRawThreshold(imp, 0, 5, null);
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

}
