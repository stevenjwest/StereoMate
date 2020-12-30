package stereomate.plugins;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import cern.colt.Arrays;
import edu.emory.mathcs.restoretools.Enums.OutputType;
import edu.emory.mathcs.restoretools.Enums.PrecisionType;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.ResizingType;
import edu.emory.mathcs.utils.pc.ConcurrencyUtils;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLDoubleIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLFloatIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLOptions;


/**
 * 
 * StereoMateDeconvolutionTest
 * 
 * Tests the deconvolution time and max memory used for a set of input images, using
 * a provided PSF.
 * 
 * @author stevenwest
 *
 */
public class StereoMate_Deconvolution_Test implements PlugIn {
	
	String iters;
	String threads;
	
	ImagePlus impPSF;
	
	ImagePlus blurredImage;
	ImagePlus psfImage;
	String boundaryStr;
	String resizingStr;
	String precisionStr;
	String thresholdStr;
	String maxItersStr;
	String nOfThreadsStr;
	String showIterationsStr;
	String gammaStr;
	String filterXYStr;
	String filterZStr;
	String normalizeStr;
	String logMeanStr;
	String antiRingStr;
	String changeThreshPercentStr;
	String dbStr;
	String detectDivergenceStr;
	
	String logImageProcessing;
    File decLogFile;
	
	FileWriter fw;
	PrintWriter pw;
	
	ThreadController tc ;
	
	@Override
	public void run(String arg) {
		
	     tc = new ThreadController();

		
		// display log in ImageJ during TESTING
		logImageProcessing = "true";
		
		int PSF_SLICE_NUMBER=101;
		//Just need to Log the start of the Deconvolution here:
	    logProcessing("Beginning Deconvolution Test");
		logProcessing("");
		logProcessing("Threads: "+ConcurrencyUtils.getNumberOfProcessors()+"  Iterations: 3" );
		logProcessing(" Channels: "+1+"  PSF Z Slices: "+PSF_SLICE_NUMBER );
		logProcessing("--------------------------------------");

		//Set variables for deconvolveWPL() method call:
		
		//In the Boundary combo-box you can choose from three types of boundary conditions: 
		//Reflexive, Periodic and Zero. The first ones are usually the best choice.
		 boundaryStr = "REFLEXIVE"; //available options: REFLEXIVE, PERIODIC, ZERO
		
		//The Resizing combo-box allows you to specify how the blurred image will be padded 
		//before processing. The Minimal resizing means that the pixel data in each dimension 
		//of a blurred image are padded by the size of the corresponding dimension of a PSF 
		//image. If the Next power of two option is selected, then the pixel data in each 
		//dimension of a blurred image are padded to the next power-of-two size that is 
		//greater or equal to the size of an image obtained by minimal padding. Finally, 
		//the Auto option chooses between the two other options to maximize the performance.
		//String resizingStr = "AUTO"; // available options: AUTO, MINIMAL, NEXT_POWER_OF_TWO
		 resizingStr = "MINIMAL";
		
		//The Output list is used to specify the type of an output (reconstructed image)
		String outputStr = "FLOAT"; // available options: SAME_AS_SOURCE, BYTE, SHORT, FLOAT  

		//in the Precision combo-box you can choose a floating-point precision used in 
		//computations. Practice shows that a single precision is sufficient for most problems.
		 precisionStr = "SINGLE"; //available options: SINGLE, DOUBLE
		
		//When the Threshold option is enabled, then all values in the reconstructed image 
		//that are less than the value specified in the threshold text field are replaced 
		//by zero. However, since WPL is a nonnegatively constrained algorithm, this option 
		//is not very useful and is disabled by default.
		 thresholdStr = "-1"; //if -1, then disabled
		
		//The Max number of iterations text field is used to specify how many iterations a 
		//given method should perform. It is a maximal value, which means that the process 
		//of reconstruction may stop earlier (when the stopping criterion is met).
		 maxItersStr = "3"; // use 3 iters for TESTING
				
		//In the Max number of threads (power of 2) text field you can enter how many 
		//computational threads will be used. By default this value is equal to the number 
		//of CPUs available on your machine.
		 nOfThreadsStr = ""+ConcurrencyUtils.getNumberOfProcessors();
				
		//When the Show iterations check-box is selected, then the reconstructed image will 
		//be displayed after each iteration.
		 showIterationsStr = "false";
		
		//The Wiener filter gamma is a tolerance for the preconditioner. It is intended to speed up 
		//the convergence, but can produce spurious artifacts. Setting this parameter to zero turns 
		//off the preconditioner (Wiener Filter).
			//suggest 0 [<0.0001] to turn off, 0.0001 - 1 as tests
		 gammaStr = "0";
		
		//The Low pass filter xy and z settings, in pixels, provide a way to smooth the results and 
		//accelerate convergence. Choose 0 to disable this function.
			//Suggest 1.0, 0 to turn off.
		 filterXYStr = "1.0";
		 filterZStr = "1.0";
		
		//If Normalize PSF is selected then the point spread function is normalized before processing.
			//NORMALIZE causes resulting PSF to be normalized so Total( psf ) = 1.
		 normalizeStr = "false";
		
		//selecting Log mean pixel value to track convergence effects in displaying the record 
		//of the convergence in a separate Log window.
		 logMeanStr = "false";
		
		//To reduce artifacts from features near the boundary of the imaging volume you should 
		//use the Perform anti-ringing step option.
		 antiRingStr = "true";
		
		//Finally, the Terminate iteration if mean delta less than x% is used as a stopping criterion.
			//Suggest 0.01, 0 to turn off
		 changeThreshPercentStr = "0.01";
		
		//For WPL, the inputs in decibels are permitted (Data (image, psf and result) in dB). 
		//This is uncommon in optical image processing, but is the norm in acoustics.
		 dbStr = "false";
		
		//The Detect divergence property stops the iteration if the changes appear to be increasing. 
		//You may try to increase the low pass filter size if this problem occurs.
		 detectDivergenceStr = "true";
		

		// use 488nm PSF for TESTING:
		String PSF = "02_488nm_ExEm_40x40x100nm.tif";
		//get psf from dw:
		 psfImage = openTiffFromJar(
				File.separator + "PSF" + File.separator + PSF, 
				PSF
				);
		 
		 
		logProcessing("");
		logProcessing("PSF: "+PSF);
		logProcessing("");
		logProcessing("--------------------------------------");
		logProcessing("");
		
		//pathToPsf.show();
		
		//IJ.showMessage("PSF...");
		
		//new results table:
		ResultsTable resTable = new ResultsTable();
		
		long IjMem = IJ.maxMemory();

		
		//Process images 1st time:
		  // length = 100 + (20*a)
		  // calc a:  (length - 100) / 20 
		  // (360 -100) / 20 = 13
		  // 45 - 1000, 95 - 2000, 145 - 3000, 195 - 4000
		//for(int a=0; a<4; a++) {
		for(int a=0; a<145; a++) { //3000
			
			int length = (a*20) + 100;
			//Generate new image:
			ImagePlus blurredImage = IJ.createImage("IMG-"+length+"-"+length+"-"+PSF_SLICE_NUMBER, 
													"16-bit noise", 
													length, length, PSF_SLICE_NUMBER);
			
			
			logProcessing("IMAGE: "+blurredImage.getTitle() );
			logProcessing("");
			// blurredImage = openTiffFromJar(
			//		File.separator + "DEC_TEST" + File.separator + "100x100x55.tif", 
			//		"100x100x55.tif"
			//		);
			 
			
			//blurredImage.show();
			
			//IJ.showMessage("blurredImage...");
			
			MemoryThread memThread = new MemoryThread();
				
			memThread.start();
			
			//Record start time:
			long startTime = System.nanoTime();
			
			//run WPL, return Deconvolved image to imX:				
			//ImagePlus imX = deconvolveWPL(blurredImage, pathToPsf, "",
			//	boundaryStr, resizingStr, outputStr, precisionStr, 
			//	thresholdStr, maxItersStr, nOfThreadsStr, showIterationsStr, gammaStr, 
			//	filterXYStr, filterZStr, normalizeStr, logMeanStr, antiRingStr, 
			//	changeThreshPercentStr, dbStr, detectDivergenceStr);
			
			//DecThread DecThread = new DecThread();
			
			//DecThread.start();
			
			//tc.pause(); // pause the main thread: will resume when DecThread is finished..
			
			
			ImagePlus imX = deconvolveWPL(blurredImage, psfImage, "", 
					boundaryStr, resizingStr, "FLOAT", precisionStr, 
					thresholdStr, maxItersStr, nOfThreadsStr, showIterationsStr, gammaStr, 
					filterXYStr, filterZStr, normalizeStr, logMeanStr, antiRingStr, 
					changeThreshPercentStr, dbStr, detectDivergenceStr);
			
			long maxMem = memThread.stopThread();
			
			long endTime = System.nanoTime();
			
			long excTime = endTime - startTime;
			
			resTable.setValue("TITLE:", a, blurredImage.getTitle() + " AUTO" );
			resTable.setValue("x:", a, length);
			resTable.setValue("y:", a, length);
			resTable.setValue("z:", a, PSF_SLICE_NUMBER);
			resTable.setValue("PSF-TITLE:", a, psfImage.getTitle() );
			resTable.setValue("PSF-x:", a, psfImage.getWidth());
			resTable.setValue("PSF-y:", a, psfImage.getHeight());
			resTable.setValue("PSF-z:", a, psfImage.getNSlices());
			resTable.setValue("MaxMemUsed:", a, maxMem);
			resTable.setValue("Time:", a, excTime);
			resTable.setValue("MaxMemory:", a, IjMem );
			
			resTable.save(IJ.getDirectory("home") + File.separator + "SM_DEC_TEST_Results.csv");
			
			System.gc();
		}
		
		
		resTable.show("Results AUTO");
		
		resTable.save(IJ.getDirectory("home") + File.separator + "SM_DEC_TEST_Results.csv");
		
		
		logProcessing("--------------------------------------");
		logProcessing("");
		logProcessing("saved CSV to: "+IJ.getDirectory("home") + File.separator + "SM_DEC_TEST_Results.csv");
		logProcessing("");
		logProcessing("COMPLETE");
		logProcessing("");
		
		IJ.showStatus("StereoMate Deconvolution: Complete.");		

		
		
	}
	
	public class MemoryThread extends Thread {

		long currentMem;
		long maxMem;
		
		boolean continueThread;
		
		@Override
		public void run() {
			continueThread = true;
			// TODO Auto-generated method stub
			while(continueThread) {
				currentMem = IJ.currentMemory();
				if(currentMem > maxMem) {
					maxMem = currentMem;
				}
				try {
					Thread.sleep(500);
				}
				catch(InterruptedException e) { }
			} //end while
		}//end run
		
		public long stopThread() {
			continueThread = false;
			return maxMem;
		}
		
	}
	
	
	public class DecThread extends Thread {
		
		@Override
		public void run() {
			deconvolveWPL(blurredImage, psfImage, "", 
					boundaryStr, resizingStr, "FLOAT", precisionStr, 
					thresholdStr, maxItersStr, nOfThreadsStr, showIterationsStr, gammaStr, 
					filterXYStr, filterZStr, normalizeStr, logMeanStr, antiRingStr, 
					changeThreshPercentStr, dbStr, detectDivergenceStr);
			
		     tc.resume(); // resume the main thread!
		}
	}
	
	
	
	/**
	 * Deconvolve the image using the WPL algorithm in Parallel Iterative Deconvolution [Piotr Wendykier &
	 * Robert Dougherty]
	 * @param BlurredImage
	 * @param impPsf
	 * @param pathToDeblurredImage
	 * @param boundaryStr
	 * @param resizingStr
	 * @param outputStr
	 * @param precisionStr
	 * @param thresholdStr
	 * @param maxItersStr
	 * @param nOfThreadsStr
	 * @param showIterationsStr
	 * @param gammaStr
	 * @param filterXYStr
	 * @param filterZStr
	 * @param normalizeStr
	 * @param logMeanStr
	 * @param antiRingStr
	 * @param changeThreshPercentStr
	 * @param dbStr
	 * @param detectDivergenceStr
	 * @return
	 */
	
    public ImagePlus deconvolveWPL(ImagePlus BlurredImage, ImagePlus impPsf, String pathToDeblurredImage, String boundaryStr, String resizingStr, String outputStr, String precisionStr, String thresholdStr, String maxItersStr, String nOfThreadsStr, String showIterationsStr, String gammaStr,
            String filterXYStr, String filterZStr, String normalizeStr, String logMeanStr, String antiRingStr, String changeThreshPercentStr, String dbStr, String detectDivergenceStr) {
        boolean showIterations, normalize, logMean, antiRing, db, detectDivergence;
        double threshold, gamma, filterXY, filterZ, changeThreshPercent;
        int maxIters;
        int nOfThreads;
        BoundaryType boundary = null;
        ResizingType resizing = null;
        OutputType output = null;
        PrecisionType precision = null;
        ImagePlus imX = null;
        ImagePlus imB = BlurredImage;
        if (imB == null) {
            IJ.error("Cannot open image " + BlurredImage);
            return null;
        }
        ImagePlus imPSF = impPsf;
        if (imPSF == null) {
        	IJ.error("Cannot open image " + impPsf);
            return null;
        }
        ImageProcessor ipB = imB.getProcessor();
        if (ipB instanceof ColorProcessor) {
        	IJ.error("RGB images are not currently supported");
            return null;
        }
        if (imB.getStackSize() == 1) {
        	IJ.error("For 2D images use Parallel Iterative Deconvolution 2D");
            return null;
        }
        ImageProcessor ipPSF = imPSF.getProcessor();
        if (ipPSF instanceof ColorProcessor) {
        	IJ.error("RGB images are not currently supported");
            return null;
        }
        if (imPSF.getStackSize() == 1) {
        	IJ.error("For 2D images use Parallel Iterative Deconvolution 2D");
            return null;
        }
        try {
            maxIters = Integer.parseInt(maxItersStr);
        } catch (Exception ex) {
        	IJ.error("maxIters must be a positive integer");
            return null;
        }
        if (maxIters < 1) {
        	IJ.error("maxIters must be a positive integer");
            return null;
        }
        for (BoundaryType elem : BoundaryType.values()) {
            if (elem.toString().equals(boundaryStr)) {
                boundary = elem;
                break;
            }
        }
        if (boundary == null) {
        	IJ.error("boundary must be in " + Arrays.toString(BoundaryType.values()));
            return null;
        }
        for (ResizingType elem : ResizingType.values()) {
            if (elem.toString().equals(resizingStr)) {
                resizing = elem;
                break;
            }
        }
        if (resizing == null) {
        	IJ.error("resizing must be in " + Arrays.toString(ResizingType.values()));
            return null;
        }
        for (OutputType elem : OutputType.values()) {
            if (elem.toString().equals(outputStr)) {
                output = elem;
                break;
            }
        }
        if (output == null) {
        	IJ.error("output must be in " + Arrays.toString(OutputType.values()));
            return null;
        }
        for (PrecisionType elem : PrecisionType.values()) {
            if (elem.toString().equals(precisionStr)) {
                precision = elem;
                break;
            }
        }
        if (precision == null) {
        	IJ.error("precision must be in " + Arrays.toString(PrecisionType.values()));
            return null;
        }
        try {
            threshold = Double.parseDouble(thresholdStr);
        } catch (Exception ex) {
        	IJ.error("threshold must be a nonnegative number or -1 to disable");
            return null;
        }
        if ((threshold != -1) && (threshold < 0)) {
        	IJ.error("threshold must be a nonnegative number or -1 to disable");
            return null;
        }
        try {
            nOfThreads = Integer.parseInt(nOfThreadsStr);
        } catch (Exception ex) {
        	IJ.error("nOfThreads must be power of 2 - no parse int");
            return null;
        }
        if (nOfThreads < 1) {
        	IJ.error("nOfThreads must be power of 2 - < 1");
            return null;
        }
        if (!ConcurrencyUtils.isPowerOf2(nOfThreads)) {
        	IJ.error("nOfThreads must be power of 2 < not Pow2");
            return null;
        }
        try {
            showIterations = Boolean.parseBoolean(showIterationsStr);
        } catch (Exception ex) {
        	IJ.error("showItrations must be a boolean value (true or false)");
            return null;
        }
        try {
            gamma = Double.parseDouble(gammaStr);
        } catch (Exception ex) {
        	IJ.error("gamma must be a nonnegative value");
            return null;
        }
        if (gamma < 0.0) {
        	IJ.error("gamma must be a nonnegative value");
            return null;
        }

        try {
            filterXY = Double.parseDouble(filterXYStr);
        } catch (Exception ex) {
        	IJ.error("filterXY must be a nonnegative value");
            return null;
        }
        if (filterXY < 0.0) {
        	IJ.error("filterXY must be a nonnegative value");
            return null;
        }

        try {
            filterZ = Double.parseDouble(filterZStr);
        } catch (Exception ex) {
        	IJ.error("filterZ must be a nonnegative value");
            return null;
        }
        if (filterZ < 0.0) {
        	IJ.error("filterZ must be a nonnegative value");
            return null;
        }
        try {
            normalize = Boolean.parseBoolean(normalizeStr);
        } catch (Exception ex) {
        	IJ.error("normalize must be a boolean value (true or false)");
            return null;
        }
        try {
            logMean = Boolean.parseBoolean(logMeanStr);
        } catch (Exception ex) {
        	IJ.error("logMean must be a boolean value (true or false)");
            return null;
        }
        try {
            antiRing = Boolean.parseBoolean(antiRingStr);
        } catch (Exception ex) {
        	IJ.error("antiRing must be a boolean value (true or false)");
            return null;
        }
        try {
            db = Boolean.parseBoolean(dbStr);
        } catch (Exception ex) {
        	IJ.error("db must be a boolean value (true or false)");
            return null;
        }
        try {
            detectDivergence = Boolean.parseBoolean(detectDivergenceStr);
        } catch (Exception ex) {
        	IJ.error("detectDivergence must be a boolean value (true or false)");
            return null;
        }
        try {
            changeThreshPercent = Double.parseDouble(changeThreshPercentStr);
        } catch (Exception ex) {
        	IJ.error("changeThreshPercent must be a nonnegative value");
            return null;
        }
        if (changeThreshPercent < 0.0) {
            IJ.error("changeThreshPercent must be a nonnegative value");
        }
        ConcurrencyUtils.setNumberOfThreads(nOfThreads);
        WPLOptions options = new WPLOptions(gamma, filterXY, filterZ, normalize, logMean, antiRing, changeThreshPercent, db, detectDivergence, (threshold == -1) ? false : true, threshold);
        switch (precision) {
        case DOUBLE:
            WPLDoubleIterativeDeconvolver3D dwpl = new WPLDoubleIterativeDeconvolver3D(imB, imPSF, boundary, resizing, output, maxIters, showIterations, options);
            imX = dwpl.deconvolve();
            break;
        case SINGLE:
            WPLFloatIterativeDeconvolver3D fwpl = new WPLFloatIterativeDeconvolver3D(imB, imPSF, boundary, resizing, output, maxIters, showIterations, options);
            imX = fwpl.deconvolve();
            break;
        }
        
        return imX;
    }

    
    /**  
     * Loads and opens a TIFF from within a JAR file using getResourceAsStream().
     * 
     * @param path
     * @param title
     * @return
     */
    public ImagePlus openTiffFromJar(String path, String title) {
           InputStream is = getClass().getResourceAsStream(path);
           //System.out.println("input stream: "+is);
           if (is!=null) {
               Opener opener = new Opener();
               //System.out.println("opener: "+opener);
               ImagePlus imp = opener.openTiff(is, title);
               //System.out.println("imp: "+imp);
               try {
				is.close();
               	} catch (IOException e) {
               		// TODO Auto-generated catch block
               		e.printStackTrace();
               	}
               return imp;
           }
           else {
        	   return null;
           }
    }
	
	
	/**
	 * Add a String to the log entry to display current processing in this algorithm,
	 * if logEntry is TRUE.
	 * <p>
	 * Also, write processing to a log file in the output directory.
	 * @param logEntry
	 */
	protected void logProcessing(String logEntry) {
		if(logImageProcessing.equalsIgnoreCase("true")) {
			IJ.log(logEntry);
		}
		
	}
	
	
	
	public class ThreadController {
		
		
		/**
		 * Constructor - keep it blank!
		 */
		public void ThreadController() {
			
		}
		
	/**
	 * This method pauses the thread which calls it.  This is used to pause the Processing Thread
	 * (ProcessThread) which is used to execute the process() method in a SM Algorithm.  Calling this
	 * method will pause this thread, which is required when setting up a GUI interface for a user
	 * and waiting for their input before moving on to the next processing step.
	 * <p>
	 * This method allows the programmer to make the processing thread wait while the user adjusts a
	 * GUI interface for indicate appropriate input for subsequent steps in the algorithm.  One of the
	 * steps in the GUI interface needs to indicate the user have finished their input, and this Listener
	 * Object should call the corresponding method to this one, which is resume().
	 * <p>
	 * This should only be called in the process() method of the SM algorithm, so ensure it only pauses
	 * the Processing Thread.
	 * <p>
	 * Details: This calls wait() on this object, DialogWindow. If called from the process() method, it will
	 * pause the processing thread on the DialogWindow object.  This thread will awake and continue executing
	 * the process() method when resume() is called, which calls notify on this object to awake the thread.
	 */
	public void pause() {
		synchronized(this) {
			try {
				wait();
			}
			catch(InterruptedException e) {
				
			}
		}
	}
	
	
	/**
	 * This method resumes the paused Processing Thread (ProcessThread) which execute the process() method
	 * in the SM Algorithm.  If the SM Algorithm requires user input during its execution, this is typically
	 * performed with a GUI interface for the user.  However, this is set up with GUI components which have
	 * Listener objects assigned to them -> processing which occurs on the EDT.
	 * <p>
	 * In order to stop the SM Algorithm from setting up the GUI and attempting to plough on with image processing,
	 * when the User still has to give input, the Programmer can pause the processing thread [with the method pause()],
	 * and can resume the thread once the user has put the appropriate input.  Resuming the thread is achieved with
	 * this method.
	 * <p>
	 * This method should only be called inside a Listener Objects's method to indicate user input is complete, and
	 * only in a situation where it will be called AFTER pause() has been executed (which the first condition satisfies).
	 * <p>
	 * Details: This calls notify() on this object, DialogWindow.  Since the wait() method was also called on this object,
	 * notify will awake that thread -> which will be the processing thread.
	 */
	public void resume() {
		synchronized(this) {
			notify();
		}
	}
	
	}
	
}
