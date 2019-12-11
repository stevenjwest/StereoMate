package stereomate.plugins;

import org.w3c.dom.Document;

import ij.ImagePlus;

/**
 * This interface declares one method - which is used in StereoMate
 * Algorithms to write its XML file.  This is required for the
 * StereoMate Input-Output Framework's writeStereoMateInfo() method.
 * <p>
 * Note: This class extends ImageJAlgorithm, and so any class implementing
 * this interface must also implement the method 'process(imp)' from the
 * ImageJAlgorithm interface.
 * @author stevenwest
 *
 */
public interface StereoMateAlgorithm {

	/**
	 * 
	 */
	public void setup();
	
	/**
	 * This method is called by the DialogWindow class once the DialogWindow has finished setting inputs,
	 * outputs, and settings for the ImageJ Algorithm being implemented.  This method is also called by 
	 * the DialogWindow through a number of methods:
	 * <p>
	 * processNextImp();
	 * <p>
	 * saveAndProcessNextImp();
	 * <p>
	 * Calling of these methods should be performed at the END of the process method, or the end of the thread
	 * stream which leads to the output of the algorithm.  If the current imp needs to be saved, the
	 * saveAndProcessNextImp() method [perhaps saveAndProcessNextImp(imp)] should be called, otherwise the data
	 * is saved appropriately by the programmer, which is followed by a call to processNextImp -> both methods
	 * ultimately will retrieve the next imp, and call process() again, if there are any more imps to process.
	 * If not, the algorithm ends as the processing is complete.
	 * @param imp The ImagePlus to process.
	 */
	public void process(ImagePlus imp);
	
	/**
	 * This method is called AFTER the while loop in the processImages() method of the DialogWindow class.
	 * It allows the programmer of the StereoMateAlgorithm to run any processes AFTER all images have been
	 * processed.
	 */
	public void cleanup();
	
}
