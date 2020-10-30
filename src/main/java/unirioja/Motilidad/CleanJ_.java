package unirioja.Motilidad;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
import ij.io.DirectoryChooser;
import ij.plugin.frame.RoiManager;

//@Plugin(type = Command.class, headless = true, menuPath = "Plugins>Motilidad>Clean")
public class CleanJ_ implements Command {

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
		for (String path : files) {
			imp = IJ.openImage(path);
			String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));

			imp = IJ.getImage();
			imp.hide();
			ImagePlus imp2 = imp.duplicate();
			IJ.setAutoThreshold(imp, "Default");
			IJ.setRawThreshold(imp, 0, 10, null);
			IJ.run(imp, "Convert to Mask", "");
			IJ.run(imp, "Select Bounding Box", "");
			Roi r = imp.getRoi();
			Roi r1 = new Roi(r.getXBase(), 0, r.getFloatWidth(), imp.getHeight());
			imp.close();
			imp2.setRoi(r1);
			imp2=imp2.crop();
			ImagePlus imp3= imp2.duplicate();
			IJ.run(imp2, "Median...", "radius=5");
			IJ.run(imp2, "8-bit", "");
			IJ.run(imp2, "Auto Local Threshold", "method=Bernsen radius=15 parameter_1=0 parameter_2=0");
			IJ.run(imp2, "Select Bounding Box", "");
			imp3.setRoi(imp2.getRoi());
			imp3 = imp3.crop();
			OvalRoi or = new OvalRoi(55,45,920,920);
			RoiManager rm = RoiManager.getRoiManager();
			if (rm == null) {
				rm = new RoiManager();
			}
			rm.setVisible(false);
			rm.addRoi(or);
			rm.runCommand(imp3, "Show All without labels");
			rm.runCommand(imp3, "Draw");
			rm.runCommand("Save", dir + "/preds/" + name + ".zip");
			rm.runCommand(imp3, "Delete");
			IJ.saveAs(imp3, "JPG", dir + "/preds/" + name + "_pred.jpg");
			imp2.close();
			imp3.close();
			n++;
			progressBar.setValue(n);

		}
		frame.setVisible(false);
		frame.dispose();
		RoiManager rm = RoiManager.getRoiManager();
		rm.setVisible(true);
		rm.close();
		IJ.showMessage("Process finished");

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

}
