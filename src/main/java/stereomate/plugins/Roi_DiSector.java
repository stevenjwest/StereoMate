package stereomate.plugins;


import ij.*;
import ij.process.*;
import imagescience.transform.Scale;
import stereomate.dialog.DialogWindow;
import stereomate.dialog.DialogWindow.FileSelector;
import stereomate.image.ImageWindowWithPanel;
import stereomate.image.ImageWindowWithPanel.ToolPanel;
import ij.gui.*;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.Opener;
import ij.io.RoiEncoder;
import ij.io.SaveDialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.w3c.dom.Document;

import ij.plugin.*;

/**
 * 
 * The ROI Di-Sector Plugin.
 * <p>
 * 
 * 
 * @author stevenwest
 *
 */
public class Roi_DiSector implements PlugIn, StereoMateAlgorithm {
	
	//ImagePlus imp;
	
	DialogWindow dw; //DialogWindow object - to create a dialog to select files for
						//processing.
	FileSelector refFS;
	
	ImageJ ij; //ij instance.
	ImageWindowWithPanel iwp; //the first iwp - for delineating Roi Boundaries.
	ImageWindowWithPanel iwp2; //the second iwp - for selecting Roi Regions.
	
	File file; //file object to store path to File.
	
	ImagePlus impProcessed; //the imp of the image being processed.
	
	//These are no longer required - dealt with in DialogWindow:
	//ArrayList<ArrayList<File>> inputs;
	//ArrayList<File> output;
		
	ImageWindowWithPanel.RoiStore boundaryStore, roiStore; //RoiStore objects to hold ROIs

	/**
	 * The Run Method initialises the DialogWindow:  The DialogWindow provides the data
	 * input for the ROI Di-Sector:
	 * <p>
	 * - The input images, selected on the First FileSelector.
	 * <p>
	 * - Optional Reference Images:  These must match the input images in name, number
	 * and directory structure.  This is optional, and should only become active once
	 * the RefImages checkbox is checked.
	 * 
	 */
	@Override
	public void run(String arg0) {
		
		dw = new DialogWindow("ROI Di-Sector", this);
		
		FileSelector inputFS = dw.addFileSelector("Input Image(s):"); //add FileSelector panel.
		
		JCheckBox refCheckBox = new JCheckBox("Use Reference Image:");
		refCheckBox.setAlignmentX(Component.RIGHT_ALIGNMENT);
		dw.add( refCheckBox );
		
		// Add checkbox and Reference Images FileSelector:
		refFS = dw.addFileSelector("Reference Image(s):");
		
		refFS.setEnabled(false);
		
		refFS.setImageSelected(FileSelector.FILE_IMAGE); // this ensures this FileSelector 
											// will not prevent the Process Button from being active
											// even if no input is selected..
		
		refCheckBox.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(refCheckBox.isSelected()) {
					refFS.setEnabled(true);
					refFS.matchedInput = FileSelector.MATCHED_INPUT;
					// set to matched input to force Process button to match the input
				}
				else {
					refFS.setEnabled(false);
					refFS.matchedInput = FileSelector.ANY_INPUT;
					refFS.setImageSelected(FileSelector.FILE_IMAGE); 
					// this ensures this FileSelector 
					// will not prevent the Process Button from being active
					// even if no input is selected..
				}
			}
			
		});
		
		dw.addActionPanel(); //add Action panel - for Cancel and Process buttons.
		dw.setPlugInSuffix("_ROI");
		dw.layoutAndDisplayFrame(); //display the DialogWindow.
		
	}
	
	
	
	@Override
	public void setup() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * The Process method over-rides the process(imp) method inside the SMA interface.  The DialogWindow
	 * passes each imp derived from the first fileSelector [FileSelector0].  The 'file'
	 * variable is set to the current Output File, and the GUI computations are initialised.
	 * <p>
	 * In order to allow the user to process the images via the GUI in the Event Dispatch
	 * Thread, whilst still allowing computations to occur on the current thread, the current 
	 * thread is paused with each successive call to a GUI method requiring user input (for example
	 * the delineateBoundaries() method and delineateRois() ).  This is achieved by using
	 * the pause() method in the DialogWindow class.
	 * <p>
	 * Note:  The thread is resumed by a resume() method in the DialogWindow class, and this is 
	 * called by a listener object from the EDT to inform the paused thread that all the GUI
	 * processing has been complete.
	 * <p>
	 * After DelineateRois() has been completed, this method is complete - the DialogWindow will pass
	 * any further image files from the first FileSelector if they exist.  For cleanup code post-processing
	 * of all images, use the cleanup() method.
	 * 
	 */
	@Override
	public void process(ImagePlus implus) {
		
		//First, set imp to implus:
		//imp = implus;
		
		//set the file instance variable to output file:
			//This ensures the output is saved properly in saveRois()
		file = dw.getCurrentOutputFile();  //Could just call this method in saveRois()?!
		
		IJ.showMessage("resizeRefImage");
		
		if(refFS.inputOutputFramework.fileArray != null) {
			implus = resizeRefImage(implus);	
		}

		//Do the Computations:
			
		//Delineate boundaries, and pause the process thread:
			//Process Thread is resumed in ActionListener declared in delineateBoundaries GUI:
		delineateBoundaries(implus);
		
		
		dw.pause(); //this pauses the current thread, can be awoken with dw.resume(), which should
					//be called by a GUI object which indicates the end of the computation.
				//This is only necessary for methods which require User GUI input...
			
		//Delineate ROIs on boundaries, and pause process thread:
			//Process Thread is resumed in ActionListener declared in delineateRois GUI:
		delineateRois(implus);
		
		
		dw.pause(); //this pauses the current thread, can be awoken with dw.resume(), which should
					//be called by a GUI object which indicates the end of the computation.
				//This is only necessary for methods which require User GUI input...
				
	} //end process(imp)
	
	
	@Override
	public void cleanup() {
		// Perform any post-image processing steps in this method...
		
		//IJ.log("memory in use: " +IJ.currentMemory());
		
		System.gc();
		System.gc();
		
		//IJ.log("memory in use: " +IJ.currentMemory());
		
	}
	
	public ImagePlus resizeRefImage(ImagePlus implus) {
		
		File refFile = dw.fileSelectors.get(1).inputOutputFramework.fileArray.get( dw.getCurrentFileIndex() );		
		
		ImagePlus refImp = IJ.openImage(refFile.getAbsolutePath());
		
		ImageStack refStack = refImp.getStack();		
		
		ImageStack is = new ImageStack(implus.getWidth(), implus.getHeight() );
		
		for(int a=1; a<refStack.getSize()+1; a++) {
			is.addSlice( refStack.getProcessor(a).resize(implus.getWidth(), implus.getHeight() ) );
		}
		
		ImagePlus imp = new ImagePlus(implus.getTitle(), is);
				
		imp.setDimensions( refImp.getNChannels(), refImp.getNSlices(), refImp.getNFrames() );
		
		return imp;
		
	}
	
	public ImagePlus resizeRefImage2(ImagePlus implus) {
		
		IJ.showMessage("Start");
		
		File refFile = dw.fileSelectors.get(1).inputOutputFramework.fileArray.get( dw.getCurrentFileIndex() );		
		ImagePlus refImp = IJ.openImage(refFile.getAbsolutePath());
		
		// resize imp to match implus size:
		try {
			final imagescience.image.Image input = imagescience.image.Image.wrap(refImp);
			final Scale scaler = new Scale();
			//scaler.messenger.log(TJ_Options.log);
			//scaler.progressor.display(TJ_Options.progress);
			double xf=1, yf=1, zf=1, tf=1, cf=1;
			xf = (double)implus.getWidth() / refImp.getWidth();
			yf = (double)implus.getHeight() / refImp.getHeight();
			zf = Double.parseDouble(""+refImp.getNSlices()); 
			int scheme = Scale.NEAREST;
			//int scheme = Scale.BSPLINE5;
			final imagescience.image.Image output = scaler.run(input,xf,yf,zf,tf,cf,scheme);
			//if (preserve) {
				//final Aspects a = input.aspects();
				//output.aspects(new Aspects(a.x/xf,a.y/yf,a.z/zf,a.t/tf,a.c/cf));
			//}
			//TJ.show(output,image);
			IJ.showMessage("End");
			return output.imageplus();
			
		} catch (OutOfMemoryError e) {
			IJ.error("Not enough memory for this operation");
			
		} catch (UnknownError e) {
			IJ.error("Could not create output image for some reason.\nPossibly there is not enough free memory");
			
		} catch (IllegalArgumentException e) {
			IJ.error("IllegalArgumentException");
			IJ.error(e.getMessage());
			
		} catch (IllegalStateException e) {
			IJ.error("IllegalStateException");
			IJ.error(e.getMessage());
			
		} catch (Throwable e) {
			IJ.error("An unidentified error occurred while running the plugin");
			
		}
		
		return implus;
		
	}
	
	/**
	 * This method is called by the process() method.
	 * This method provides an ImageWindowWithPanel object to display the image on as a projection, 
	 * and provides the tools to delineate the ROI Boundaries - which will be stored in a RoiStore 
	 * object.
	 * @param ImagePlus passedImp: The ImagePlus object referring to the image to be processed.
	 */
	public void delineateBoundaries(ImagePlus passedImp) {
		
		//SETUP THE IMAGE AND PANEL:
		
		//create iwp object:
		Panel p = new Panel(); //create panel for iwp object.
		iwp = new ImageWindowWithPanel(passedImp, p);
		//iwp = new ImageWindowWithPanel(passedImp, p, 
		//				ImageWindowWithPanel.createBlankImageStack(
		//						passedImp.getWidth(), 
		//						passedImp.getHeight(), 
		//						passedImp.getNSlices(),
		//						passedImp.getTitle() ), 
		//			8, true ); //create iwp from file & panel.
													//this iwp will be displayed as a projection.

		// Set active channel to last channel - the one NOT DISPLAYED & which will contain the processed imp:
		//iwp.setActiveChannel( iwp.getChannels() );
		
		iwp.setModal(true); //make the iwp modal - disable the ImageJ window.
		
		// impProcessed = iwp.getImagePlus(); //store a reference to the imp in impProcessed.
		
		//ADD COMPONENTS - Ensure each set of the components are added to a JPanel with a Border:
		
		//Add Refresh Button:
		//iwp.addRefreshButton(); //allows the refreshing of image window layout.
		
		
		//Add Roi Extender & Linker Tools:
		
		ImageWindowWithPanel.RoiExtenderTool ret = iwp.new RoiExtenderTool(iwp);
		
		ImageWindowWithPanel.RoiLinkerTool rlt = iwp.new RoiLinkerTool(iwp);
		
		JLabel roiExtLabel = new JLabel("<html>Extend ROIs<br>to edge:</html>", SwingConstants.CENTER);
		roiExtLabel.setFont( new Font("SansSerif", Font.PLAIN, 10));
		
		JPanel roiExtenderPanel = new JPanel();
		roiExtenderPanel.add(roiExtLabel);
		roiExtenderPanel.add(ret);
		roiExtenderPanel.add(rlt);
		roiExtenderPanel.setBorder(new LineBorder(Color.BLACK,1,false));
		
		iwp.addComponent(roiExtenderPanel);
		
		
		//Add Roi Tool Panel:
		
		JLabel toolLabel = new JLabel("<html>Sketch ROI<br>boundaries:</html>", SwingConstants.CENTER);
		toolLabel.setFont( new Font("SansSerif", Font.PLAIN, 10));
		
		ImageWindowWithPanel.ToolPanel tp = iwp.new ToolPanel();
		tp.addTool(ToolPanel.POLYLINE);
		tp.addTool(ToolPanel.FREELINE);
		
		//set the initial selected tool:
		tp.setTool(ToolPanel.FREELINE);
		
		JPanel toolPanel = new JPanel();
		toolPanel.add(toolLabel);
		toolPanel.add(tp);
		toolPanel.setBorder(new LineBorder(Color.BLACK,1,false));
		
		iwp.addComponent(toolPanel);
		
		
		//Add Roi Store Panel:
		
		boundaryStore = iwp.new RoiStore(true); //boolean true means all rois will be displayed
												//by default.				
		iwp.addComponent(boundaryStore);

		
		//Add Next Step Panel:
		
		JPanel nextStepPanel = new JPanel();
		JLabel nextStepLabel = new JLabel();
		
		nextStepLabel  = new JLabel("<html>Click here<br>when complete:</html>", SwingConstants.CENTER);
		nextStepLabel.setFont( new Font("SansSerif", Font.PLAIN,10));
		
		JButton nextStepButton = new JButton( createImageIcon("/Icons/NextStepButton.png", "Icon for Moving onto the next step of the algorithm") );
		
		nextStepButton.setPreferredSize( new Dimension(30,30));
		nextStepButton.setBorder(new LineBorder(Color.BLACK,1,false));
		nextStepButton.setToolTipText("Enter Button - moves onto the next step in this Plugin");
		
		nextStepButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				//delineateRois(); -> do not call this in the EventDispatchThread!
					//Instead, notify the waiting Thread to continue when the nextStepButton is pressed
					//Do this by calling notify on the impProcessed object.
				dw.resume();	//This functionality is wrapped into the DialogWindow object, by using
							//the methods dw.pause() and dw.resume()
			}
			
		});
		
		nextStepPanel.add(nextStepLabel);
		nextStepPanel.add(nextStepButton);
		nextStepPanel.setBorder(new LineBorder(Color.BLACK,1,false));
		
		iwp.addComponent(nextStepPanel);
		
		
		//Layout iwp Window:
		iwp.layoutWindow();
		
		//deactivate and remove Channels scrollbar:
		//iwp.deactivateChannels();
		//iwp.removeChannelsScrollbar();
		
	}
	
	/** Returns an ImageIcon, or null if the path was invalid. */
	protected ImageIcon createImageIcon(String path,
	                                           String description) {
	    java.net.URL imgURL = getClass().getResource(path);
	    if (imgURL != null) {
	        return new ImageIcon(imgURL, description);
	    } else {
	        System.err.println("Couldn't find file: " + path);
	        return null;
	    }
	}
	
	/**
	 * This method continues to process the image, by allowing the user to select the Regions based on the
	 * divisions set on the image from the previous method. First, a blank Black image of the same width and
	 * height as the original image is created, and the Boundaries drawn onto this surface as pixelated
	 * lines with a value of 1 (i.e are essentially black in colour).  The original image is presented as an
	 * overlay, and can be modified - to display or hide chanels, change projection in Z or T, make more
	 * or less opaque, etc. And finally, the Roi Boundaries obtained before are laid on top.
	 * <p>
	 * The user can select ROIs for adding to the ROI Store using the Wand tool provided. Once all ROIs have been
	 * selected and added to the Store, the User can press the nextStep button to terminate the algorithm (at which
	 * point the ROIs selected are saved to the location of the original image).
	 */
	public void delineateRois(ImagePlus passedImp) {
		
		//derive both sliceValues & activeChannels of current imp
		//for re-assignment later in this method:
		int[] sliceValues = new int[0];
		String activeChannels = "";
		
		if(iwp.getSlices() > 1) {
			sliceValues = iwp.getSliceProjectionValues();
		}
		if(iwp.getChannels() > 1) {
			activeChannels = iwp.getDisplayChannels();
		}
		
		//iwp.shutdownImageWindowWithPanel();
		iwp.getWindow().dispose(); //dispose of the iw - this is set up again in iwp.setUpWindow() !
		iwp = null;  // -> set to null to garbage collect.
		
		// impProcessed = iwp.getOriginalImagePlus();
		
		//create iwp object:
		Panel p = new Panel(); //create panel for iwp object.
		iwp2 = new ImageWindowWithPanel(passedImp, p, 
						ImageWindowWithPanel.createBlankImageStack(
								passedImp.getWidth(), 
								passedImp.getHeight(), 
								passedImp.getNSlices(),
								passedImp.getTitle() ), 
					8, true ); //create iwp from file & panel.
													//this iwp will be displayed as a projection.

		// Set active channel to last channel - the one NOT DISPLAYED & which will contain the processed imp:
		iwp2.setActiveChannel( iwp2.getChannels() );

		//Initially, clear the iwp's panel:
		//iwp2.setNewPanel( p );
		
		//iwp2.addRefreshButton(); //add a refresh button to iwp.
		
		//Add Roi Tool Panel:
		
		JLabel toolLabel = new JLabel("<html>Sketch ROI<br>boundaries:</html>", SwingConstants.CENTER);
		toolLabel.setFont( new Font("SansSerif", Font.PLAIN, 10));
				
		ImageWindowWithPanel.ToolPanel tp = iwp2.new ToolPanel();
		tp.addTool(ToolPanel.WAND);
				
		//set the initial selected tool:
		tp.setTool(ToolPanel.WAND);
				
		JPanel toolPanel = new JPanel();
		toolPanel.add(toolLabel);
		toolPanel.add(tp);
		toolPanel.setBorder(new LineBorder(Color.BLACK,1,false));
				
		iwp2.addComponent(toolPanel);
				
		//Add Roi Store Panel:
		roiStore = iwp2.new RoiStore(false); //boolean false means no rois will be displayed
											//required to allow overlay to be retained
											//whilst ROIs are added to the Store.		
		iwp2.addComponent(roiStore);
				
		//Add Finish Roi Delineation button:
		
		JPanel finishPanel = new JPanel();
		JLabel finishLabel = new JLabel();
		
		finishLabel  = new JLabel("<html>Click here<br>when complete:</html>", SwingConstants.CENTER);
		finishLabel.setFont( new Font("SansSerif", Font.PLAIN,10));
		
		JButton finishButton = new JButton( createImageIcon("/Icons/NextStepButton.png", "Icon for Moving onto the next step of the algorithm") );
		
		finishButton.setPreferredSize( new Dimension(30,30));
		finishButton.setBorder(new LineBorder(Color.BLACK,1,false));
		finishButton.setToolTipText("Enter Button - moves onto the next step in this Plugin");
		
		finishButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(0 == roiStore.numberOfRois) {
					IJ.showMessage("No ROIs Selected."
							+ "\n"
							    + "Please select ROIs.");
				}
				else {
					saveRois();
				}
			}			
		});
		
		finishPanel.add(finishLabel);
		finishPanel.add(finishButton);
		finishPanel.setBorder(new LineBorder(Color.BLACK,1,false));
		
		iwp2.addComponent(finishPanel);
				
		
		//iwp2.setUpWindow(true); //set up fresh iwp with projection - this will use the original imp
								//with full image stack (see setUpWindow() method for further info).
		
		iwp2.setModal(true); //re-set the modal attributes, including the windowListener 
								//to allow ImageJ window re-activation if iwp is closed..
							//MUST be called after setup window - as new iw is generated in that method.
		
		//iwp2.layoutWindow(); //layout the iwp - validate components and maximise window.
		
		//deactivate and remove Channels scrollbar:
		//iwp2.deactivateChannels();
		//iwp2.removeChannelsScrollbar();
		
		//set the active channels BEFORE extra channel is added:
			//ensures only the previous channels are available for alteration..
		//if(iwp2.getChannels() > 1) {
		//	iwp2.setDisplayChannels(activeChannels);
		//}
		
		//Now the new image is set up, the extra channel can be added & drawn on for Roi delineateion:
		
		//First, add an extra blank channel to the iwp ImagePlus:
		//iwp2.addBlankChannel(false);

		//Finally, re-set reference of impProcessed to the new imp ref in iwp:
		//impProcessed.close();
		//impProcessed = iwp2.getImagePlus(); //need to reset impProcessed ref to new imp after 
											//adding blank channel and setting slice projection
		
		//set imp to the last channel (the one just added above - this is where the outlines
			// will be drawn):
		//impProcessed.setC(channels);
				
		//Now, sketch the boundaries derived previously onto this channel:
		
		//set overlay to WHITE:
		boundaryStore.setOverlayColor(Color.WHITE);
		boundaryStore.setDrawLabels(false);
		// boundaryStore.displayAllOverlay( iwp2.getOriginalImagePlus() );

		int channels ;
		ImageStack is;
		int slices;
		//derive the image stack from impProcessed:
			//this contains references to ip's across the entire hyperstack (channels, slices, frames):
		if(iwp2.getSlices() > 1) {
			
			boundaryStore.displayAllOverlay( iwp2.getOriginalImagePlus() );
			
			is = iwp2.getOriginalImagePlus().getStack();
			channels = iwp2.getOriginalImagePlus().getNChannels();
			slices = iwp2.getOriginalImagePlus().getNSlices();
		}
		else {
			
			boundaryStore.displayAllOverlay( iwp2.getImagePlus() );
			
			is = iwp2.getImagePlus().getStack();
			channels = iwp2.getImagePlus().getNChannels();
			slices = 1; // there are no z slices!
		}
		
		//sketch ROI Boundaries from the boundaryStore object onto this channel (all slices and frames):
		//int slices = iwp2.getOriginalImagePlus().getNSlices();
		int numBoundaries = boundaryStore.ROIs.size();
				
		for(int a=1; a<slices+1; a++) {
				//impProcessed.setPosition(channels, a, b);
				int reference = (channels*a);
				ImageProcessor ip = is.getProcessor( reference );
				ip.setLineWidth(1);
				ip.setColor(1.0);
				for(int c=0; c<numBoundaries; c++) {
					ip.draw(boundaryStore.ROIs.get(c));
				}
			
		}
		
		
		//set the slice projection AFTER the extra channel is added & ROIs sketched:
		//ensures correct projection of the new imp is performed..
		if(iwp2.getSlices() > 1) {
			iwp2.setSliceProjection(sliceValues);
		}
		
		iwp2.updateChannelsAndSlices(true);
		
		iwp2.layoutWindow(); //layout the iwp - validate components and maximise window.
		
		//when the image is re-projected, the roi boundaries which have been sketched on are lost.
			//Must find a way to retain these sketched boundaries with further projections
			//Need to ensure the newly added channel is included in any projections...
			// w/o being put as a channel in the channel projector panel!

		
	} //end delineateRois()
	
	
	public ImageStack returnRoiSketchedImageStack(int channels, ImageStack is) {
		
		//sketch ROI Boundaries from the boundaryStore object onto this channel (all slices and frames):
		int slices = impProcessed.getNSlices();
		int numBoundaries = boundaryStore.ROIs.size();

		for(int a=1; a<slices+1; a++) {
			//impProcessed.setPosition(channels, a, b);
			int reference = (channels*a);
			ImageProcessor ip = is.getProcessor( reference );
			ip.setLineWidth(1);
			ip.setColor(1.0);
			for(int c=0; c<numBoundaries; c++) {
				ip.draw(boundaryStore.ROIs.get(c));
			}

		}
		
		return is;
		
	}
	
	/**
	 * This method needs to close the image window & save the ROIs to either the default location
	 * (where the image was opened from), or in a location selected by the user. 
	 */
	public void saveRois() {
		
		iwp2.getWindow().dispose(); //dispose of the iw.
		iwp2.setModal(false); //re-activate the ij window.
		//impProcessed.close();
		iwp2 = null; //set iwp to null to garbage collect this object.
		//imp.close();
		

		Roi[] rois = roiStore.getRoisAsArray();
		
		//declare a string variable to represent a file path:
		String path;
		String name;
		String dir;
		

		//if file does not equal null, 
			//retrieve its filepath & name:
		if(file != null) {
			path = file.getPath();
			name = file.getName();
			if (!(name.endsWith(".zip") || name.endsWith(".ZIP"))) {
				//if the file name does not end in .zip, need to add this extension:
				if( name.length() > 3 && (name.substring(name.length()-4, name.length()-3) == ".") ) {
					//if name has an extension [i.e. the fourth from last 
					//character is a "."], then REMOVE the extension, and add ".zip":
						//Need to make sure the name is longer than three characters!
					name = name.substring(0, name.length()-4) + ".zip";
				}
				
				else {
					//else, the name does not have an extension, so just append ".zip" to the name:"
					name = name + ".zip";
				}
				
			}
			dir = file.getParent();
			path = dir+File.separator+name;
			
		}
		else {
			SaveDialog sd = new SaveDialog("Save ROIs...", impProcessed.getTitle(), ".zip");
			name = sd.getFileName();
			if (name == null)
				return;
			if (!(name.endsWith(".zip") || name.endsWith(".ZIP")))
				name = name + ".zip";
			dir = sd.getDirectory();
			path = dir+name;
		}
		
		DataOutputStream out = null;
		IJ.showStatus("Saving "+rois.length+" ROIs "+" to "+path);
		long t0 = System.currentTimeMillis();
		try {
			ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
			out = new DataOutputStream(new BufferedOutputStream(zos));
			RoiEncoder re = new RoiEncoder(out);
			for (int i=0; i<rois.length; i++) {
				IJ.showProgress(i, rois.length);
				String label = "" + i;
				Roi roi = (Roi)rois[i];
				//if (IJ.debugMode) IJ.log("saveMultiple: "+i+"  "+label+"  "+roi);
				//if (roi==null) continue;
				if (!label.endsWith(".roi")) label += ".roi";
				zos.putNextEntry(new ZipEntry(label));
				re.write(roi);
				out.flush();
			}
			out.close();
		} catch (IOException e) {
			IJ.error("IO Exception: " + e);
			return;
		} finally {
			if (out!=null)
				try {out.close();} catch (IOException e) {}
		}
		double time = (System.currentTimeMillis()-t0)/1000.0;
		IJ.showProgress(1.0);
		IJ.showStatus(IJ.d2s(time,3)+" seconds, "+rois.length+" ROIs, "+path);
		
		//IJ.showMessage(IJ.d2s(time,3)+" seconds, "+rois.length+" ROIs, "+path);
		
		//Resume the Processing Thread after saving the ROI:
		dw.resume();
		
	} //end saveRois()


}
