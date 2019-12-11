package stereomate.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ij.CompositeImage;
import ij.Executer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Menus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.FreehandRoi;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageLayout;
import ij.gui.ImageRoi;
import ij.gui.ImageWindow;
import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ScrollbarWithLabel;
import ij.gui.StackWindow;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.macro.Interpreter;
import ij.plugin.ChannelSplitter;
import ij.plugin.CompositeConverter;
import ij.plugin.GelAnalyzer;
import ij.plugin.HyperStackConverter;
import ij.plugin.HyperStackMaker;
import ij.plugin.MacroInstaller;
import ij.plugin.Orthogonal_Views;
import ij.plugin.OverlayLabels;
import ij.plugin.RGBStackMerge;
import ij.plugin.WandToolOptions;
import ij.plugin.ZProjector;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;
import ij.plugin.tool.PlugInTool;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.LUT;

/**
 * This class represents an image window with a Panel located at the bottom of the window.  
 * <p>
 * This class assumes no frames are present on the image - an assumption which is assured by
 * the DialogWindow class. [Note, the set up for dealing with frames is coded here, but not currently
 * supported]
 * <p>
 * This class deals with the placement and layout of the window
 * <p>
 *  	- including ImagePlus, ImageCanvas, any Scrollbars & a panel on the bottom.
 * <p>
 * The image is always presented as a PROJECTION -> channels are shown as composite, and slices
 * are shown as a MIP.  Tools are provided by default to alter the channels and slices.
 * <p>
 * Pixel data presented represents what the programmer wants the user to work with, and does not
 * reflect what is seen necessarily.
 * <p>
 * This class does not handle images with Frames -> must split these with the Frame Splitter.
 * <p>
 * It also provides functionality such as: 
 * <p>
 * - adding components to this Panel, 
 * <p>
 * - adding the standard Tools from the IJ Toolbar, including cross-compatibility with all 
 * standard tools on the toolbar (i.e clicking a toolbar icon will change the selected icon 
 * on the window, and clicking an icon on the window will change the selected tool).  
 * <p>
 * Window layout will maximise the imageplus, and to fit any other components onto the window, 
 * is performed in the layoutWindow() method, and should be called LAST, AFTER the addition of
 * other components to a ImageWindowWithPanel's Panel.
 * 
 * @author stevenwest
 *
 */
public class ImageWindowWithPanel {

	/**
	 * reference to the ImagePlus object in this ImageWindowWithPanel.  This is the PROJECTED IMAGE.
	 */
	protected ImagePlus imp;
	
	/**
	 * The ORIGINAL Title of the imp.
	 */
	protected String title;
	
	/**
	 * Width and Height of imp.
	 */
	protected int w, h;
	
	/**
	 * variables to represent the number of channels, slices & frames on the original imp.
	 */
	protected int c, s, sOrig, f;
	
	/**
	 * The channel which is active in the current image - channel which tools access during image manipulation.
	 */
	protected int activeChannel;
	
	/**
	 * filepath to the image to be opened.
	 */
	protected File filePath;
	
	/**
	 * the Panel to be inserted below the image on the window.
	 */
	protected Panel panel;
	
	/**
	 * boolean values which encode whether Channel and Slice Projections are in use.
	 */
	protected boolean chProj;
	protected boolean slProj;
	
	/**
	 * ImageWindow reference.
	 */
	public ImageWindow iw;
	
	/**
	 * CustomCanvas reference - defined in this class.
	 */
	public CustomCanvas cc;
	
	/**
	 * The Panel Height for the CustomCanvas.
	 */
	int panelHeight2;
	
	/**
	 * Boolean to represent whether disinfo is available.
	 */
	boolean displayInfoAvailable;
	
	/**
	 * ImageJ instance - for removing listeners from ImageCanvas & ImageWindow.
	 */
	protected ImageJ ij;
	
	/**
	 * Instance of this ImageWindowWithPanel.
	 */
	//protected static ImageWindowWithPanel instance;
	
	/**
	 * Array to store all tool panels associated with this iwp. Used in setting listeners for
	 * each ToolPanel object to all other ToolPanel objects in the static method setListeners() 
	 * in the ToolPanel class.
	 */
	protected ArrayList<ToolPanel> ToolPanels;
	
	/**
	 * Instance variable to determine if this object is displaying its ImagePlus object as an
	 * overlay on top of a blank ByteProcessor.
	 */
	protected boolean isOverlay; //DEPRECATED.
	
	/**
	 * Dimension indicating the Panel size.
	 */
	Dimension panelSize;
	
	//refresh panel variables:
	protected JPanel refreshPanel;
	protected JButton refreshButton;
	
	//channel projector:
	protected JPanel chProjPanel;
	public ChannelCheckBox[] channels;

	//slice projector:
	protected ImagePlus impOriginal; //reference to the original imp prior to projection.
	protected JPanel slProjPanel; //panel for components.
	protected ZProjector sliceProjector; //ZProjector to project the image.
	JLabel first, last, rate; //Labels for the spinners.
	JSpinner minSlice, maxSlice; //Spinners to set the min (first) slice and max (last) slice of projection.
	public SpinnerModel minModel; //Two models for the Spinners.

	public SpinnerModel maxModel;
	//JSpinner rateSlice; //Spinner for the rate to update the projections when playing the projection.
	//SpinnerModel rateModel;
	//JButton start, stop, //buttons to play and stop projection
	JButton forward, backward; //buttons to move forward or backward through the slices at the current 
									// projection thickness.
	
	//KeyListeners ArrayList -> to keep a record of all keyListeners added to CustomCanvas, and to allow
		// CustomCanvas to add these as a new CustomCanvas is made with any updates to the slice projection:
	
	ArrayList<KeyListener> keyListeners;
	
	//Canvas key listener for deactivating and activating ability to change channels:
	//KeyListener[] canvasKeyListener;
	ScrollbarWithLabel channelScrollbar;
	ScrollbarWithLabel sliceScrollbar;
	ScrollbarWithLabel frameScrollbar;

	/**
	 * Boolean to represent whether activeImp (i.e an added last channel imp) is ON or OFF.
	 */
	boolean activeImpOn;
	

	/**
	 * Constructor: Sets filePath and panel to instance variables, retrieves imp from filePath & 
	 * calls setUpWindow(), which deals with the displaying & layout of the image window.
	 * 
	 * @param filePath File object containing the path to the image (this method assumes this path is
	 * valid).
	 * @param panel A Panel object to insert below the image window.
	 * @param proj A boolean value to set whether the IWP is to display a projected image, with relevant
	 * tools.
	 */
	public ImageWindowWithPanel(File filePath, Panel panel) {
		
		//set parameters to instance variables:
		this.filePath = filePath;
		this.panel = panel;
		
		chProj = false;
		slProj = false;
		
		//retrieve the imp from filePath:
		imp = IJ.openImage( filePath.getPath() );
		
		if(imp.getBitDepth() != 8) {
		//convert image to 8-bit:
			ImageConverter ic = new ImageConverter(imp);
			ic.convertToGray8(); //converts the imp to 8 bits directly..
			ic = null;
		}
		
		//Store ref to imp in impOriginal ???
		impOriginal = imp;
		
		keyListeners = new ArrayList<KeyListener>();
		
		//set up the window:
		setUpWindow();
		
	}

	
	
	/**
	 * Constructor: Sets imp and panel, & calls setUpWindow(), which deals with the displaying 
	 * and layout of the image window.
	 * <p>
	 * Converts imp to 8-bit if not.
	 *
	 * @param imp ImagePlus of the image to be displayed.
	 * @param panel A Panel object to insert below the image window.
	 * @param proj A boolean value to set whether the IWP is to display a projected image, with relevant
	 * tools.
	 */
	public ImageWindowWithPanel(ImagePlus imp, Panel panel) {
		
		//set parameters to instance variables:
		this.imp = imp;
		this.panel = panel;
		
		if(imp.getBitDepth() != 8) {
		//convert image to 8-bit:
			ImageConverter ic = new ImageConverter(imp);
			ic.convertToGray8(); //converts the imp to 8 bits directly..
			ic = null;
		}
		
		//Store ref to imp in impOriginal ???
		impOriginal = imp;
		
		keyListeners = new ArrayList<KeyListener>();
		
		//set up the window
		setUpWindow();
		
		
	}
	
	
	
	
	
	
	
	/**
	 * Constructor: Sets imp and panel, & calls setUpWindow(), which deals with the displaying 
	 * and layout of the image window.
	 * <p>
	 * Note image is converted to bit depth indicated in this constructor - if bitDepth int is NOT 8,
	 * 16 or 32, then NO conversion takes place.
	 *
	 * @param imp ImagePlus of the image to be displayed.
	 * @param panel A Panel object to insert below the image window.
	 * @param channelNumberDisplayed the Int which represents how many channel numbers to display with
	 * channel projection.
	 */
	public void ImageWindowWithPanel2(ImagePlus imp, Panel panel, int channelNumberDisplayed, int bitDepth) {
		
		//set parameters to instance variables:
		this.imp = imp;
		this.panel = panel;
		
		if(bitDepth == 8) {
			if(imp.getBitDepth() != 8) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray8(); //converts the imp to 8 bits directly..
				ic = null;
			}
		}
		if(bitDepth == 16) {
			if(imp.getBitDepth() != 16) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray16(); //converts the imp to 8 bits directly..
				ic = null;
			}
		}
		if(bitDepth == 32) {
			if(imp.getBitDepth() != 32) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray32(); //converts the imp to 8 bits directly..
				ic = null;
			}
		}
		
		//Store ref to imp in impOriginal ???
		impOriginal = imp;
		
		keyListeners = new ArrayList<KeyListener>();
		
		//set up the window
		setUpWindow(channelNumberDisplayed);
		
		
	}
	
	
	
	
	/**
	 * Constructor: Sets imp and panel, & calls setUpWindow(), which deals with the displaying 
	 * and layout of the image window.
	 * <p>
	 * Note image is converted to bit depth indicated in this constructor - if bitDepth int is NOT 8,
	 * 16 or 32, then NO conversion takes place.
	 *
	 * @param imp ImagePlus of the image to be displayed.
	 * @param panel A Panel object to insert below the image window.
	 * @param channelNumberDisplayed the Int which represents how many channel numbers to display with
	 * channel projection.
	 */
	public ImageWindowWithPanel(ImagePlus imp, Panel panel, int channelNumberDup, int bitDepth) {
		
		//set parameters to instance variables:
		this.imp = imp;
		this.panel = panel;
		
		if(bitDepth == 8) {
			if(imp.getBitDepth() != 8) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray8(); //converts the imp to 8 bits directly..
				ic = null;
			}
		}
		if(bitDepth == 16) {
			if(imp.getBitDepth() != 16) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray16(); //converts the imp to 8 bits directly..
				ic = null;
			}
		}
		if(bitDepth == 32) {
			if(imp.getBitDepth() != 32) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray32(); //converts the imp to 8 bits directly..
				ic = null;
			}
		}
		
		//First, get the image stack from the IWP original image:
		ImageStack is = imp.getStack();

		//Next, filter ImageStack to retrieve the stack which represents the active channel:
		//In this setup case, it is the FIRST channel (1)
		is = filterImageStack(is, channelNumberDup, imp.getNChannels() );

		//Finally, generate the activeImp from the new ImageStack:
		//This represents the activeChannel which is to be processed & display the results of the processing
		//to the user.
		//activeImp = new ImagePlus();
		//activeImp.setStack(is);
		//It is the activeImp which will be processed, and its stack will be displayed in the IWP
		//with the originalImp projection.

		//activeImp.setLut(LUT.createLutFromColor(Color.WHITE));

		//Add activeImp's ImageStack into the IWP ImagePlus' ImageStack:

		//adding 1 to the number of channels, as the mergeStacks adds activeImp stack as an extra channel:
		imp.setStack( mergeStacks( imp.getStack(), is ), 
				imp.getNChannels()+1, imp.getNSlices(), imp.getNFrames() );  
		
		
		//Store ref to imp in impOriginal ???
		impOriginal = imp;
		
		keyListeners = new ArrayList<KeyListener>();
		
		//set up the window
		setUpWindow(imp.getNChannels()-1);
		
		
	}
	
	
	
	/**
	 * Constructor: Sets imp and panel, & calls setUpWindow(), which deals with the displaying 
	 * and layout of the image window.
	 * <p>
	 * Note image is converted to bit depth indicated in this constructor - if bitDepth int is NOT 8,
	 * 16 or 32, then NO conversion takes place.
	 *
	 * @param imp ImagePlus of the image to be displayed.
	 * @param panel A Panel object to insert below the image window.
	 * @param extraImp This is added to the END of the first Imp as an extra channel.  Assumes the extraImp
	 * only has one channel, and the Z stack size is the same as imp.
	 * @param bitDepthAn int representing the bitDepth which this image should be converted to.
	 * @param displayNewChannelToggle If treu, will add the new channel's toggle to IWP, and it will automatically
	 * be on when IWP is displayed.
	 */
	public ImageWindowWithPanel(ImagePlus imp, Panel panel, ImagePlus extraImp, int bitDepth, boolean displayNewChannelToggle) {
		
		//set parameters to instance variables:
		this.panel = panel;
		
		//IJ.showMessage("new window open?");
		
		if(bitDepth == 8) {
			if(imp.getBitDepth() != 8) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray8(); //converts the imp to 8 bits directly..
				ic = null;
			}
			if(extraImp.getBitDepth() != 8) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(extraImp);
				ic.convertToGray8(); //converts the imp to 8 bits directly..
				ic = null;
			}
		}
		if(bitDepth == 16) {
			if(imp.getBitDepth() != 16) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray16(); //converts the imp to 16 bits directly..
				ic = null;
			}
			if(extraImp.getBitDepth() != 16) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(extraImp);
				ic.convertToGray16(); //converts the imp to 16 bits directly..
				ic = null;
			}
		}
		if(bitDepth == 32) {
			if(imp.getBitDepth() != 32) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray32(); //converts the imp to 32 bits directly..
				ic = null;
			}
			if(extraImp.getBitDepth() != 32) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(extraImp);
				ic.convertToGray32(); //converts the imp to 32 bits directly..
				ic = null;
			}
		}
		
		//IJ.showMessage("new window open2?");
		
		// Merge the imp and extraImp:
		//adding 1 to the number of channels, as the mergeStacks adds activeImp stack as an extra channel:
		imp = addExtraChannel(imp, extraImp); 
		
		//IJ.showMessage("new window open3?");
		
		
		//Store ref to imp in impOriginal ???
		impOriginal = imp;
		this.imp = imp; // must set refs to imp and impOriginal AFTER extra channel is added!
		
		keyListeners = new ArrayList<KeyListener>();
		
		//set up the window
		if(displayNewChannelToggle == true) {
			setUpWindow(imp.getNChannels()-1);
		}
		else {
			setUpWindow(imp.getNChannels());
		}
		
	}
	
	public static ImagePlus addExtraChannel(ImagePlus imp, ImagePlus extraImp) {
		
		ImagePlus imp2 = new ImagePlus(imp.getTitle(), mergeStacks( imp.getStack(), extraImp.getStack() ) );
		
		imp2.setDimensions( imp.getNChannels()+1, imp.getNSlices(), imp.getNFrames() );
		
		return imp2;
		
		// imp.setStack( mergeStacks( imp.getStack(), extraImp.getStack() ), 
		//		imp.getNChannels()+1, imp.getNSlices(), imp.getNFrames() ); 
	}
	
	
	/**
	 * Returns the ImagePlus at channelNum position in the original Imp.  This IS a 
	 * DUPLICATE of the image at channelNum, and NOT the original.
	 * @param channelNum
	 * @return
	 */
	public ImagePlus returnChannelDuplicate(int channelNum) {
		ImageStack is = new ImageStack(impOriginal.getWidth(), impOriginal.getHeight() );
		
		for(int a=channelNum; a<=impOriginal.getStack().getSize(); a=a+impOriginal.getC()) {
			////IJ.log("filterImageStack loop a: "+a);
			//Duplicate the IP from the ImageStack, and copy into the new ImageStack:
			is.addSlice( impOriginal.getStack().getProcessor(a).duplicate() );
		}
		
		ImagePlus imp = new ImagePlus();
		
		imp.setStack(is);
		
		return imp;
		
	}
	
	/**
	 * Returns the ImagePlus at channelNum position in the originalImp.  The returned Imp contains the
	 * references to each ImageProcessor in the original Imp and is NOT a duplicate.
	 * @param channelNum
	 * @return
	 */
	public ImagePlus returnChannel(int channelNum) {
		ImageStack is = new ImageStack(impOriginal.getWidth(), impOriginal.getHeight() );
		
		for(int a=channelNum; a<=impOriginal.getStack().getSize(); a=a+impOriginal.getC()) {
			////IJ.log("filterImageStack loop a: "+a);
			//Duplicate the IP from the ImageStack, and copy into the new ImageStack:
			is.addSlice( impOriginal.getStack().getProcessor(a) );
		}
		
		ImagePlus imp = new ImagePlus();
		
		imp.setStack(is);
		
		return imp;
		
	}
	
	/**
	 * This method deletes a set of ImageProcessors from the passed ImageStack, according to
	 * the pattern outlined by 'channelToDelete' and 'totalNumberOfChannels'.  Both numbers
	 * are 1 based, and 'channelToDelete' must not be larger than 'totalNumberOfChannels' - 
	 * otherwise no processing takes place (no error is thrown).
	 * <p>
	 * IPs are removed first at the position indicated by 'channelToDelete', and then every
	 * 'channelToDelete + totalNumberOfChannels', until the end of the ImageStack is reached.
	 * 
	 * @param is
	 * @param channelToDelete
	 * @param totalNumberOfChannels
	 */
	public static void deleteImageStack(ImagePlus imp, int channelToDelete, int totalNumberOfChannels) {

		ImageStack is = imp.getStack();
		int ch = imp.getNChannels();
		int sl = imp.getNSlices();
		int fr = imp.getNFrames();
		
		//IJ.showMessage("chDel: "+channelToDelete+" chTot: "+totalNumberOfChannels);
		//IJ.showMessage("impCh: "+ch+"impSl: "+sl+" impFr: "+fr+" stacksize: "+is.getSize() );
		if(channelToDelete <= totalNumberOfChannels) {
			//Loop through the parsed ImageStack - start at channelNumber, go to ImageStack size,
			//and step through the stack as number of channels:
			// This Will ensure each IP is from the designated channel
			// REMEMBER TO REMOVE BACKWARDS:
			// for(int a=channelToDelete; a<=is.getSize(); a=a+totalNumberOfChannels) {
			for(int a=is.getSize()-(totalNumberOfChannels-channelToDelete); a>=channelToDelete; a=a-totalNumberOfChannels) {
				//DELETE the IP from the ImageStack:
				is.deleteSlice(a);
			}
			
			//IJ.showMessage("impCh: "+imp.getNChannels()+"impSl: "+imp.getNSlices()+
				//	" impFr: "+imp.getNFrames()+" stacksize: "+is.getSize() );
			
			//subtract 1 from the number of channels, as the new stack removes one channel:
			imp.setStack( is, ch-1, sl, fr ); 

		}
		
	}
	
	
	
	/**
	 * This method will filter the ImageStack to return a sub-stack consisting of the series of ImageProcessors
	 * which corresponds to the channelNumber, out of the total number of channels.  This method returns an
	 * ImageStack where the ImageProcessors have been DUPLICATED - they are separate images, and not referencing
	 * the original ImageProcessors from the original ImageStack!
	 * <p>
	 * The image stack is simply a series of ImageProcessors.  These are arranged in a nested loop of channels,
	 * slices, frames.  Therefore, to get every slice of the first channel when total number of channels is 3,
	 * the 1st, 4th, 7th etc ImageProcessor must be duplicated.
	 * <p>
	 * This assumes there is no frames in the image - guaranteed when images are parsed through the DialogWindow
	 * class in StereoMate.
	 * 
	 * @param is The ImageStack to extract from.
	 * @param channelNumber The Channel Number to be extracted - must be between 1 and totalNumberOfChannels.
	 * @param totalNumberOfChannels The total number of channels in this ImageStack.
	 * @return
	 */
	public static ImageStack filterImageStack(ImageStack is, int channelNumber, int totalNumberOfChannels ) {
		
		//Generate a new ImageStack to return - with correct width and height!:
			//is must have at least one processor, so use this as ref to get width and height
		ImageStack is2 = new ImageStack( is.getProcessor(1).getWidth(), is.getProcessor(1).getHeight() );

		//Loop through the parsed ImageStack - start at channelNumber, go to ImageStack size,
			//and step through the stack as number of channels:
				//Will ensure each IP is from the designated channel
		for(int a=channelNumber; a<=is.getSize(); a=a+totalNumberOfChannels) {
			////IJ.log("filterImageStack loop a: "+a);
			//Duplicate the IP from the ImageStack, and copy into the new ImageStack:
			////IJ.log("a: "+a+" slice: "+is.getSize());
			is2.addSlice( is.getProcessor(a).duplicate() );
		}
		
		//Return the new ImageStack:
		return is2;
	}
	
	/**
	 * Returns a new BLANK ImageStack containing ImageProcessors (8-bit greyscale) with width
	 * and height specified, and the stack to depth specified.
	 * @param width
	 * @param height
	 * @param depth
	 * @return The new Image Stack
	 */
	public static ImagePlus createBlankImageStack(int width, int height, int depth, String title) {
		
		ImageStack is = new ImageStack(width, height);
		
		for(int a=0; a<depth; a++) {
			is.addSlice( new ByteProcessor(width, height) );
		}
		
		return new ImagePlus(title, is);
				
	}
	
	/**
	 * This method will filter the ImageStack to return a sub-stack consisting of the series of ImageProcessors
	 * which corresponds to the channelNumber, out of the total number of channels.  This method returns an
	 * ImageStack where the ImageProcessors have been REFERENCED - they are NOT separate images, and are referencing
	 * the original ImageProcessors from the original ImageStack!
	 * <p>
	 * The image stack is simply a series of ImageProcessors.  These are arranged in a nested loop of channels,
	 * slices, frames.  Therefore, to get every slice of the first channel when total number of channels is 3,
	 * the 1st, 4th, 7th etc ImageProcessor must be duplicated.
	 * <p>
	 * This assumes there is no frames in the image - guaranteed when images are parsed through the DialogWindow
	 * class in StereoMate.
	 * 
	 * @param is The ImageStack to extract from.
	 * @param channelNumber The Channel Number to be extracted - must be between 1 and totalNumberOfChannels.
	 * @param totalNumberOfChannels The total number of channels in this ImageStack.
	 * @return
	 */
	public static ImageStack filterImageStackNoDup(ImageStack is, int channelNumber, int totalNumberOfChannels ) {
		
		//Generate a new ImageStack to return - with correct width and height!:
			//is must have at least one processor, so use this as ref to get width and height
		ImageStack is2 = new ImageStack( is.getProcessor(1).getWidth(), is.getProcessor(1).getHeight() );

		//Loop through the parsed ImageStack - start at channelNumber, go to ImageStack size,
			//and step through the stack as number of channels:
				//Will ensure each IP is from the designated channel
		for(int a=channelNumber; a<=is.getSize(); a=a+totalNumberOfChannels) {
			////IJ.log("filterImageStack loop a: "+a);
			//Duplicate the IP from the ImageStack, and copy into the new ImageStack:
			////IJ.log("a: "+a+" slice: "+is.getSize());
			is2.addSlice( is.getProcessor(a) );
		}
		
		//Return the new ImageStack:
		return is2;
	}
	
	
	
	/**
	 * Merges two stacks to form a 2+ channel image.  
	 * It will put the second imagestack at the end of the first imagestack to form a new channel.
	 * It assumes the second imageStack is a single channel.
	 */
	public static ImageStack mergeStacks( ImageStack iwpStack, ImageStack thresholdedStack ) {
		
		//create new image stack to fill with the two stacks to merge:
		ImageStack is2 = new ImageStack(iwpStack.getProcessor(1).getWidth(), iwpStack.getProcessor(1).getHeight() );
		
		//Get the difference in size between IWP and thresholded stacks:
			//this is used to determine when to insert the thresholdedStack IPs in loop below:
		int diff = iwpStack.getSize() / thresholdedStack.getSize();
		
		////IJ.log("merge stacks - diff: "+diff);
		
		for(int a=1; a<=iwpStack.getSize(); a++) {
			//always add the iwpStack IP - and add it BEFORe the thresholded stack IP
			is2.addSlice(iwpStack.getProcessor(a));
			if(a % diff == 0) {
				//if a / diff remainder is 0, then add the thresholded stack IP:
					//if stacks are same size, diff is 1, then a % diff is always 0.
					//if iwp stack is 2x thresholded stack, a % diff only 0 for EVEN numbers (modulus is 0
						//when divisible by 2), etc.
				int b = a / diff;
				is2.addSlice(thresholdedStack.getProcessor(b));
			}
			
		}
		
		//Now, the is2 stack should contain the references to the IPs from both the IWP and thresholded stack
			//and these are presented in the order:  channels : slices.
		
		//return the new ImageStack:
		return is2;
		
	}
	
	
	
	/**
	 * replaces last channel in iwpStack with thresholdedStack.  
	 * It will put the second imagestack in place of last channel of the first imagestack.
	 * It assumes the second imageStack is a single channel.
	 */
	public ImageStack replaceStacks( ImageStack iwpStack, ImageStack thresholdedStack ) {
		
		////IJ.log("replace stacks");
		//create new image stack to fill with the two stacks to merge:
		ImageStack is2 = new ImageStack(iwpStack.getProcessor(1).getWidth(), iwpStack.getProcessor(1).getHeight() );
		
		//Get the difference in size between IWP and thresholded stacks:
			//this is used to determine when to insert the thresholdedStack IPs in loop below:
		int diff = iwpStack.getSize() / thresholdedStack.getSize();
		
		//IJ.log("iwp stack size: "+iwpStack.getSize() );
		//IJ.log("thresholdedStack size: "+thresholdedStack.getSize() );
		//IJ.log("Diff: "+diff);
		//IJ.log("a max: "+iwpStack.getSize() );
		
		for(int a=1; a<=iwpStack.getSize(); a++) {
			//always add the iwpStack IP - and add it BEFORe the thresholded stack IP
			if(a % diff != 0) {
				//IJ.log("a: "+a+" a % diff: "+(a%diff) );
				//only add from iwpStack if a % diff does not equal 0 - i.e. IP is not from the 
					//channel being replaced!
				is2.addSlice(iwpStack.getProcessor(a));
			}
			if(a % diff == 0) {
				//IJ.log("a: "+a+" a % diff: "+(a%diff) );
				//if a / diff remainder is 0, then add the thresholded stack IP:
					//if stacks are same size, diff is 1, then a % diff is always 0.
					//if iwp stack is 2x thresholded stack, a % diff only 0 for EVEN numbers (modulus is 0
						//when divisible by 2), etc.
				int b = a / diff;
				is2.addSlice(thresholdedStack.getProcessor(b));
			}
			
		}
		
		//Now, the is2 stack should contain the references to the IPs from both the IWP and thresholded stack
			//and these are presented in the order:  channels : slices.
		
		//return the new ImageStack:
		return is2;
		
	}
	
	
	/**
	 * converts the passed ImagePlus to the desired bit depth -> 8, 16, 32.  If bitDepth is not 8, 16 or 32
	 * or if the imp is already the same bitDepth, this method does nothing to the ImagePlus.
	 * @param imp
	 * @param bitDepth
	 */
	public static void convertBitDepth(ImagePlus imp, int bitDepth) {
		
		if(bitDepth == 8) {
			if(imp.getBitDepth() != 8) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray8(); //converts the imp to 8 bits directly..
				ic = null;
			}
		}
		if(bitDepth == 16) {
			if(imp.getBitDepth() != 16) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray16(); //converts the imp to 8 bits directly..
				ic = null;
			}
		}
		if(bitDepth == 32) {
			if(imp.getBitDepth() != 32) {
				//convert image to 8-bit:
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray32(); //converts the imp to 8 bits directly..
				ic = null;
			}
		}
				
	}
	
	/**
	 * Constructor: Sets imp and panel, & calls setUpWindow(), which deals with the displaying 
	 * and layout of the image window.
	 *
	 * @param imp ImagePlus of the image to be displayed.
	 * @param panel A Panel object to insert below the image window.
	 * @param proj A boolean value to set whether the IWP is to display a projected image, with relevant
	 * tools.
	 */
	public ImageWindowWithPanel(ImagePlus imp, Panel panel, CustomCanvas cc2) {
		
		//set parameters to instance variables:
		this.imp = imp;
		this.panel = panel;
		
		if(imp.getBitDepth() != 8) {
		//convert image to 8-bit:
			ImageConverter ic = new ImageConverter(imp);
			ic.convertToGray8(); //converts the imp to 8 bits directly..
			ic = null;
		}
		
		//set imageWindow to null - allow new window to accept imp.
		//imp.setWindow(null);
		
		//Store ref to imp in impOriginal ???
		impOriginal = imp;
		
		//set the imp to the customCanvas:
		//cc2.updateImage(imp);
		
		//set up the window
		setUpWindow(cc2);
		
		//set the imp to the customCanvas:
		//cc2.updateImage(imp);
		
		//imp.setWindow(iw);
		
	}
	
	
	
	
	
	
	/**
	 * Constructor: Sets imp and panel, & calls setUpWindow(), which deals with the displaying 
	 * and layout of the image window.
	 *
	 * @param imp ImagePlus of the image to be displayed.
	 * @param panel A Panel object to insert below the image window.
	 * @param proj A boolean value to set whether the IWP is to display a projected image, with relevant
	 * tools.
	 */
	public ImageWindowWithPanel(ImagePlus imp, Panel panel, CustomCanvas cc2, int channelNumber) {
		
		//set parameters to instance variables:
		this.imp = imp;
		this.panel = panel;
		
		if(imp.getBitDepth() != 8) {
		//convert image to 8-bit:
			ImageConverter ic = new ImageConverter(imp);
			ic.convertToGray8(); //converts the imp to 8 bits directly..
			ic = null;
		}
		
		//set imageWindow to null - allow new window to accept imp.
		//imp.setWindow(null);
		
		//Store ref to imp in impOriginal ???
		impOriginal = imp;
		
		//set the imp to the customCanvas:
		//cc2.updateImage(imp);
		
		//set up the window
		setUpWindow(cc2, channelNumber);
		
		//set the imp to the customCanvas:
		//cc2.updateImage(imp);
		
		//imp.setWindow(iw);
		
	}
	
	
	
	
	
	
	/**
	 * This method detects the imp from the image path and generates an appropriate
	 * image window containing the imp.  The panel is added to the image window, and 
	 * if channel and slice projectors are required, these are added to the Panel.
	 * <p>
	 * When adding a new blank channel, want to re-project here, but also maintain the original
	 * projection information - channel and slice projectors - so when the image is re-displayed,
	 * it LOOKS the same as before, but it now HAS an extra blank channel.
	 * <p>
	 * New method - resetUpWindow() -> to set everything up.
	 * Hoq does this link with the manipulations done in the Roi_DiSector class, where it sets the active channel
	 * to the new blank channel, and it draws on this blank channel to produce the ROI outlines???
	 * @param proj A boolean value to set whether the IWP is to display a projected image, with relevant
	 * tools.
	 */
	public void setUpWindow() {
		
		
		imp = impOriginal;  //set imp to impOriginal - required for re-calls to this method when
							//using a slice projection (since slice projection overwrites imp with
							//a projected imp - and thus slices have been removed from it!)
		
		//get an instance of ImageJ window:
		ij = IJ.getInstance();
		
		//Get image characteristics:
		title = imp.getTitle();
		
		w = imp.getWidth();
		h = imp.getHeight();
		
		//identify number of channels and slices in imp:
		c = imp.getNChannels();
		s = sOrig = imp.getNSlices();
		f = imp.getNFrames();
		
		
		//By default set activeChannel to the first channel, 1:
		
		activeChannel = 1;

		
		//IJ.showMessage("c: "+c);
		//IJ.showMessage("s: "+s);
		//IJ.showMessage("f: "+f);
		
		//if(proj == true) {
			//Have removed the proj component of the IWP -> the images only work as projections!
			
			//if image has more than one channel, set up channel projector 
			//tool, and make image a composite:
			
			if(c > 1) {
				
				//IJ.log("setUpWindow:  setupChProj");
				
				chProj = true;
				
				setupChannelProjector();
				
				//CompositeImage ci = (CompositeImage)imp;
				CompositeImage ci = new CompositeImage(imp, CompositeImage.COMPOSITE);
				imp = ci;
				impOriginal = imp;
				//ci.setMode(IJ.COMPOSITE);
				ci.updateAndDraw();
				
			}
			
			//if image has more than one slice, set up slice projector
			//tool, and make image project the entire stack.
			
			if(s > 1) {
				
				//IJ.log("setUpWindow:  setupSlProj");
				
				slProj = true;
			
				setupSliceProjector();
				
				s = 1;//setupSliceProjector() eliminates slices in imp, so set s to 1.
			
			}	

		//}//end if proj == true
		
		
		//Create a new CustomCanvas object with the imp:
		cc = new CustomCanvas(imp);
		//cc = new CustomCanvas(imp, panelHeight);
			//This must be done AFTER setup of SliceProjector:
				//Slice Projector generates a new imp (a projection of the original), whose reference is set to imp.
				//Therefore need to make sure the CustomCanvas is made with the up-to-date imp!
		
		
		if(imp.isHyperStack() ) {
			
			// No hyperstacks can survive the current processing layout:
				//DialogWindow removes any images with frames within them.
				//This algorithm will project and remove any images with slices in them.
					//Have kept this code in case the class is modified in future...
			
			//IJ.log("setUpWindow: Hyperstack");
			
			iw = new StackWindowPublicScrollBars(imp, cc);
			
			StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
			//obtain object ref. to scrollbars for channels frame & slices
				//StackWindowPublicScrollBars is an extension of the StackWindow class,
				//which makes the scrollbar object references available to this class.
			
			if(c > 1) {
				channelScrollbar = sw.getcSelector();
			}
			
			if(f > 1) {
				frameScrollbar = sw.gettSelector();
			}
			
			if(s > 1) {
				sliceScrollbar = sw.getzSelector();
			}
			
		}
		else if(imp.getImageStackSize() > 1) {
			
			//if image is not a hyperstack, but stack size is greater than 1, then only
			//one dimension is greater than one - either channel, slice, or frame.
			//depending on which is greater than one will depend on the operation:
				//NOTE: on images with one dimension, the scrollbar ref is always in zSelector.
			
			//ALL IMAGES passed through DialogWindow will be this, or not a stack:
				//All images with slices in will be projected, removing any slices.
				//All images containing Frames will not be passed by the DialogWindow.
				//All Channels will still be present - making the image stack the no. of channels.
					
			
			if(c > 1) { //if stack is due to channels:
				
				//This is the only possible section to be called in this block with current layout of code:
					//Only channels survive the filtering by DialogWindow and this class..
				
				//IJ.log("setUpWindow: Channels");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				channelScrollbar = sw.getzSelector();
				
				if(channelScrollbar == null) {
					//get channelScrollbar from cSelector if zSelector returns null..
					channelScrollbar = sw.getcSelector();
				}
				
			}
			else if( s > 1) { //if stack is due to slices:
				
				//This cannot be reached with current layout of code:
					//Slices will always be projected, yielding an image with one slice.
				
					//Have kept this code in case the class is modified in future...
				
				//IJ.log("setUpWindow: Slices");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				sliceScrollbar = sw.getzSelector();
			
			}
			else if(f > 1) { //if stack is due to frames:
				
				//This cannot be reached with current layout of code:
					//Images parsed by the DialogWindow will not parse an image with frames.
				
					//Have kept this code in case the class is modified in future...
				
				//IJ.log("setUpWindow: Frames");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				frameScrollbar = sw.getzSelector();
			
			}
		
		}
		else {
			
			//This is called if the image is a simple 2D image with only one channel, or if its a
				//zStack with one channel.
			
			//IJ.log("setUpWindow: No Stack - CustomWindow");
			
			iw = new CustomWindow(imp, cc);
			
			imp.setLut( LUT.createLutFromColor(Color.RED) ); 
		
		}
				
		panel.setLayout( new FlowLayout() );
		iw.add(panel);
		
		//instance = this;
		
		//Get the canvasKeyListener -> to allow activation and deactivation of key listener for channel
			//scrollbar:
		
		//canvasKeyListener = imp.getCanvas().getKeyListeners();
		
		//IJ.log(" ");
		
		
		//if the slice projector has run, however, need to ADD THE KEYLISTENER to the customcanvas:
		
		if(slProj == true) {
			
			// add the key listener that the IWP implements to the CustomCanvas:
						// use the addKeyListener method in this class.
						// The keyPressed() method will call moveProjectionForward() or moveProjectionBackward()
							// if the RIGHT and LEFT arrow keys are pressed.
			addKeyListener( new SliceProjectorKeyListener() );
			
			// Also add this keylistener to the array of keylisteners -> ensures CustomCanvas adds this listener
				// to itself when regenerated when a new projection is made!
			
			
		}
		
		
		//Finally, if the chProj is true -> remove and inactivate channels & scrollbar from iw:
		if(chProj == true) {
			deactivateChannels();
			removeChannelsScrollbar();
			
			// also add keylistener to allow user to turn channels on and off by pressing the number keys on the
				// keyboard:
			
			addKeyListener( new ChannelProjectorKeyListener() );
			
		}
		
	} //end setUpWindow()
	
	
	
	
	
	
	
	/**
	 * This method detects the imp from the image path and generates an appropriate
	 * image window containing the imp.  The panel is added to the image window, and 
	 * if channel and slice projectors are required, these are added to the Panel.
	 * <p>
	 * When adding a new blank channel, want to re-project here, but also maintain the original
	 * projection information - channel and slice projectors - so when the image is re-displayed,
	 * it LOOKS the same as before, but it now HAS an extra blank channel.
	 * <p>
	 * New method - resetUpWindow() -> to set everything up.
	 * Hoq does this link with the manipulations done in the Roi_DiSector class, where it sets the active channel
	 * to the new blank channel, and it draws on this blank channel to produce the ROI outlines???
	 * @param channelNumberDisplayed An int of the number of channels to DISPLAY.
	 * 
	 */
	public void setUpWindow(int channelNumberDisplayed) {


		imp = impOriginal;  //set imp to impOriginal - required for re-calls to this method when
		//using a slice projection (since slice projection overwrites imp with
		//a projected imp - and thus slices have been removed from it!)

		//get an instance of ImageJ window:
		ij = IJ.getInstance();

		//Get image characteristics:
		title = imp.getTitle();

		w = imp.getWidth();
		h = imp.getHeight();

		//identify number of channels and slices in imp:
		c = imp.getNChannels();  //c will be the TOTAL number of channels, independe of how many are displayed.
		s = sOrig = imp.getNSlices();
		f = imp.getNFrames();


		//By default set activeChannel to the first channel, 1:
		activeChannel = 1;

		if(c > 1) {

			chProj = true;

			setupChannelProjector(channelNumberDisplayed);

			//CompositeImage ci = (CompositeImage)imp;
			// instead of casting the imp -> construct it as a composite image:
			CompositeImage ci = new CompositeImage(imp);
			// AND set imp to this ci - to ensure the reference in imp is correct!
			imp = ci;
			ci.setMode(IJ.COMPOSITE);
			ci.setActiveChannels( getDisplayChannels() );
			//Need to do this to shut off the correct channels as detailed in channelNumberDisplayed..
			ci.updateAndDraw();

		}

		//if image has more than one slice, set up slice projector
		//tool, and make image project the entire stack.

		if(s > 1) {

			// IJ.log("setUpWindow:  setupSlProj");

			slProj = true;

			setupSliceProjector();

			s = 1;//setupSliceProjector() eliminates slices in imp, so set s to 1.

		}	

		//}//end if proj == true


		//Create a new CustomCanvas object with the imp:
		cc = new CustomCanvas(imp);
		//cc = new CustomCanvas(imp, panelHeight);
		//This must be done AFTER setup of SliceProjector:
		//Slice Projector generates a new imp (a projection of the original), whose reference is set to imp.
		//Therefore need to make sure the CustomCanvas is made with the up-to-date imp!


		if(imp.isHyperStack() ) {

			// No hyperstacks can survive the current processing layout:
			//DialogWindow removes any images with frames within them.
			//This algorithm will project and remove any images with slices in them.
			//Have kept this code in case the class is modified in future...

			//IJ.log("setUpWindow: Hyperstack");

			iw = new StackWindowPublicScrollBars(imp, cc);
			updateChannelsAndSlices(true);

			StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
			//obtain object ref. to scrollbars for channels frame & slices
			//StackWindowPublicScrollBars is an extension of the StackWindow class,
			//which makes the scrollbar object references available to this class.

			if(c > 1) {
				channelScrollbar = sw.getcSelector();
			}

			if(f > 1) {
				frameScrollbar = sw.gettSelector();
			}

			if(s > 1) {
				sliceScrollbar = sw.getzSelector();
			}

		}
		else if(imp.getImageStackSize() > 1) {

			//if image is not a hyperstack, but stack size is greater than 1, then only
			//one dimension is greater than one - either channel, slice, or frame.
			//depending on which is greater than one will depend on the operation:
			//NOTE: on images with one dimension, the scrollbar ref is always in zSelector.

			//ALL IMAGES passed through DialogWindow will be this, or not a stack:
			//All images with slices in will be projected, removing any slices.
			//All images containing Frames will not be passed by the DialogWindow.
			//All Channels will still be present - making the image stack the no. of channels.


			if(c > 1) { //if stack is due to channels:

				//This is the only possible section to be called in this block with current layout of code:
				//Only channels survive the filtering by DialogWindow and this class..

				//IJ.log("setUpWindow: Channels");

				iw = new StackWindowPublicScrollBars(imp, cc);
				updateChannelsAndSlices(true);

				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;

				//obtain object ref. to scrollbar for channels:
				channelScrollbar = sw.getzSelector();

				if(channelScrollbar == null) {
					//get channelScrollbar from cSelector if zSelector returns null..
					channelScrollbar = sw.getcSelector();
				}

			}
			else if( s > 1) { //if stack is due to slices:

				//This cannot be reached with current layout of code:
				//Slices will always be projected, yielding an image with one slice.

				//Have kept this code in case the class is modified in future...

				//IJ.log("setUpWindow: Slices");

				iw = new StackWindowPublicScrollBars(imp, cc);
				updateChannelsAndSlices(true);

				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;

				//obtain object ref. to scrollbar for channels:
				sliceScrollbar = sw.getzSelector();

			}
			else if(f > 1) { //if stack is due to frames:

				//This cannot be reached with current layout of code:
				//Images parsed by the DialogWindow will not parse an image with frames.

				//Have kept this code in case the class is modified in future...

				//IJ.log("setUpWindow: Frames");

				iw = new StackWindowPublicScrollBars(imp, cc);
				updateChannelsAndSlices(true);

				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;

				//obtain object ref. to scrollbar for channels:
				frameScrollbar = sw.getzSelector();

			}

		}
		else {

			//This is called if the image is a simple 2D image with only one channel, or if its a
			//zStack with one channel.

			//IJ.log("setUpWindow: No Stack - CustomWindow");

			iw = new CustomWindow(imp, cc);
			updateChannelsAndSlices(true);

			imp.setLut( LUT.createLutFromColor(Color.RED) ); 

		}

		panel.setLayout( new FlowLayout() );
		iw.add(panel);

		//instance = this;

		//Get the canvasKeyListener -> to allow activation and deactivation of key listener for channel
		//scrollbar:

		//canvasKeyListener = imp.getCanvas().getKeyListeners();

		//if the slice projector has run, however, need to ADD THE KEYLISTENER to the customcanvas:

		if(slProj == true) {

			// add the key listener that the IWP implements to the CustomCanvas:
			// use the addKeyListener method in this class.
			// The keyPressed() method will call moveProjectionForward() or moveProjectionBackward()
			// if the RIGHT and LEFT arrow keys are pressed.
			addKeyListener( new SliceProjectorKeyListener() );

			// Also add this keylistener to the array of keylisteners -> ensures CustomCanvas adds this listener
			// to itself when regenerated when a new projection is made!


		}

		//IJ.log(" ");

		// if the chProj is true -> remove and inactivate channels & scrollbar from iw:
		if(chProj == true) {
			deactivateChannels();
			removeChannelsScrollbar();

			// also add keylistener to allow user to turn channels on and off by pressing the number keys on the
			// keyboard:

			addKeyListener( new ChannelProjectorKeyListener() );

		}

	} //end setUpWindow()
	
	/**
	 * This method detects the imp from the image path and generates an appropriate
	 * image window containing the imp.  The panel is added to the image window, and 
	 * if channel and slice projectors are required, these are added to the Panel.
	 * <p>
	 * When adding a new blank channel, want to re-project here, but also maintain the original
	 * projection information - channel and slice projectors - so when the image is re-displayed,
	 * it LOOKS the same as before, but it now HAS an extra blank channel.
	 * <p>
	 * New method - resetUpWindow() -> to set everything up.
	 * Hoq does this link with the manipulations done in the Roi_DiSector class, where it sets the active channel
	 * to the new blank channel, and it draws on this blank channel to produce the ROI outlines???
	 * @param proj A boolean value to set whether the IWP is to display a projected image, with relevant
	 * tools.
	 */
	public void setUpWindow(CustomCanvas cc2) {
		
		
		imp = impOriginal;  //set imp to impOriginal - required for re-calls to this method when
							//using a slice projection (since slice projection overwrites imp with
							//a projected imp - and thus slices have been removed from it!)
		
		//get an instance of ImageJ window:
		ij = IJ.getInstance();
		
		title = imp.getTitle();
		
		w = imp.getWidth();
		h = imp.getHeight();
		
		//identify number of channels and slices in imp:
		c = imp.getNChannels();
		s = sOrig = imp.getNSlices();
		f = imp.getNFrames();

		
		//IJ.showMessage("c: "+c);
		//IJ.showMessage("s: "+s);
		//IJ.showMessage("f: "+f);
		
		//if(proj == true) {
			//Have removed the proj component of the IWP -> the images only work as projections!
			
			//if image has more than one channel, set up channel projector 
			//tool, and make image a composite:
			
			if(c > 1) {
				
				//IJ.log("setUpWindow:  setupChProj");
				
				chProj = true;
				
				setupChannelProjector();
				
				CompositeImage ci = (CompositeImage)imp;
				ci.setMode(IJ.COMPOSITE);
				ci.updateAndDraw();
				
			}
			
			//if image has more than one slice, set up slice projector
			//tool, and make image project the entire stack.
			
			if(s > 1) {
				
				//IJ.log("setUpWindow:  setupSlProj");
				
				slProj = true;
			
				setupSliceProjector();
				
				s = 1;//setupSliceProjector() eliminates slices, so set s to 1.
			
			}	

		//}//end if proj == true
		
		
		//Create a new CustomCanvas object with the imp:
		cc = cc2;
			//This must be done AFTER setup of SliceProjector:
				//Slice Projector generates a new imp, whose reference is set to imp.
				//Therefore need to make sure the CustomCanvas is made with the up-to-date imp!
		
		
		if(imp.isHyperStack() ) {
			
			// No hyperstacks can survive the current processing layout:
				//DialogWindow removes any images with frames within them.
				//This algorithm will project and remove any images with slices in them.
			
			//IJ.log("setUpWindow: Hyperstack");
			
			iw = new StackWindowPublicScrollBars(imp, cc);
			
			StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
			//obtain object ref. to scrollbars for channels frame & slices
				//StackWindowPublicScrollBars is an extension of the StackWindow class,
				//which makes the scrollbar object references available to this class.
			
			if(c > 1) {
				channelScrollbar = sw.getcSelector();
			}
			
			if(f > 1) {
				frameScrollbar = sw.gettSelector();
			}
			
			if(s > 1) {
				sliceScrollbar = sw.getzSelector();
			}
			
		}
		else if(imp.getImageStackSize() > 1) {
			
			//if image is not a hyperstack, but stack size is greater than 1, then only
			//one dimension is greater than one - either channel, slice, or frame.
			//depending on which is greater than one will depend on the operation:
				//NOTE: on images with one dimension, the scrollbar ref is always in zSelector.
			
			//ALL IMAGES passed through DialogWindow will be this, or not a stack:
				//All images with slices in will be projected, removign slices.
				//All images containing Frames will not be passed by the DialogWindow.
				//All Channels will still be present - making the image stack the no. of channels
					
			
			if(c > 1) { //if stack is due to channels:
				
				//This is the only possible section to be called in this block with current layout of code:
					//Only channels survive the filtering by DialogWindow and this class..
				
				//IJ.log("setUpWindow: Channels");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				channelScrollbar = sw.getzSelector();
				
			}
			else if( s > 1) { //if stack is due to slices:
				
				//This cannot be reached with current layout of code:
					//Slices will always be projected, yielding an image with one slice.
				
				//IJ.log("setUpWindow: Slices");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				sliceScrollbar = sw.getzSelector();
			
			}
			else if(f > 1) { //if stack is due to frames:
				
				//This cannot be reached with current layout of code:
					//Images parsed by the DialogWindow will not parse an image with frames.
				
				//IJ.log("setUpWindow: Frames");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				frameScrollbar = sw.getzSelector();
			
			}
		
		}
		else {
			
			//This is called if the image is a simple 2D image with only one channel, or if its a
				//zStack with one channel.
			
			//IJ.log("setUpWindow: No Stack - CustomWindow");
			
			iw = new CustomWindow(imp, cc);
		
		}
				
		panel.setLayout( new FlowLayout() );
		iw.add(panel);
		
		//instance = this;
		
		//Get the canvasKeyListener -> to allow activation and deactivation of key listener for channel
			//scrollbar:
		
		//canvasKeyListener = imp.getCanvas().getKeyListeners();
		
		//IJ.log(" ");
		
		//if the slice projector has run, however, need to ADD THE KEYLISTENER to the customcanvas:
		
		if(slProj == true) {
			
			// add the key listener that the IWP implements to the CustomCanvas:
						// use the addKeyListener method in this class.
						// The keyPressed() method will call moveProjectionForward() or moveProjectionBackward()
							// if the RIGHT and LEFT arrow keys are pressed.
			addKeyListener( new SliceProjectorKeyListener() );
			
			// Also add this keylistener to the array of keylisteners -> ensures CustomCanvas adds this listener
				// to itself when regenerated when a new projection is made!
			
			
		}
		
		//Finally, if the chProj is true -> remove and inactivate channels & scrollbar:
		if(chProj == true) {
			deactivateChannels();
			removeChannelsScrollbar();

			// also add keylistener to allow user to turn channels on and off by pressing the number keys on the
				// keyboard:
			
			addKeyListener( new ChannelProjectorKeyListener() );
			
		}
		
	} //end setUpWindow()
	
	
	
	
	
	/**
	 * This method detects the imp from the image path and generates an appropriate
	 * image window containing the imp.  The panel is added to the image window, and 
	 * if channel and slice projectors are required, these are added to the Panel.
	 * <p>
	 * When adding a new blank channel, want to re-project here, but also maintain the original
	 * projection information - channel and slice projectors - so when the image is re-displayed,
	 * it LOOKS the same as before, but it now HAS an extra blank channel.
	 * <p>
	 * New method - resetUpWindow() -> to set everything up.
	 * Hoq does this link with the manipulations done in the Roi_DiSector class, where it sets the active channel
	 * to the new blank channel, and it draws on this blank channel to produce the ROI outlines???
	 * @param proj A boolean value to set whether the IWP is to display a projected image, with relevant
	 * tools.
	 */
	public void setUpWindow(CustomCanvas cc2, int channelNumber) {
		
		
		imp = impOriginal;  //set imp to impOriginal - required for re-calls to this method when
							//using a slice projection (since slice projection overwrites imp with
							//a projected imp - and thus slices have been removed from it!)
		
		//get an instance of ImageJ window:
		ij = IJ.getInstance();
		
		title = imp.getTitle();
		
		w = imp.getWidth();
		h = imp.getHeight();
		
		//identify number of channels and slices in imp:
		//c = channelNumber;
		c = imp.getNChannels();
		s = sOrig = imp.getNSlices();
		f = imp.getNFrames();

		
		//IJ.showMessage("c: "+c);
		//IJ.showMessage("s: "+s);
		//IJ.showMessage("f: "+f);
		
		//if(proj == true) {
			//Have removed the proj component of the IWP -> the images only work as projections!
			
			//if image has more than one channel, set up channel projector 
			//tool, and make image a composite:
			
			//if(c > 1) {
			if(channelNumber > 1) {
				
				//IJ.log("setUpWindow:  setupChProj");
				
				chProj = true;
				
				setupChannelProjector(channelNumber);
				//setupChannelProjector();

				
				CompositeImage ci = (CompositeImage)imp;
				ci.setMode(IJ.COMPOSITE);
				ci.updateAndDraw();
				
			}
			
			//if image has more than one slice, set up slice projector
			//tool, and make image project the entire stack.
			
			if(s > 1) {
				
				//IJ.log("setUpWindow:  setupSlProj");
				
				slProj = true;
			
				setupSliceProjector();
				
				s = 1;//setupSliceProjector() eliminates slices, so set s to 1.
			
			}	

		//}//end if proj == true
		
		
		//Create a new CustomCanvas object with the imp:
		cc = cc2;
			//This must be done AFTER setup of SliceProjector:
				//Slice Projector generates a new imp, whose reference is set to imp.
				//Therefore need to make sure the CustomCanvas is made with the up-to-date imp!
		
		
		if(imp.isHyperStack() ) {
			
			// No hyperstacks can survive the current processing layout:
				//DialogWindow removes any images with frames within them.
				//This algorithm will project and remove any images with slices in them.
			
			//IJ.log("setUpWindow: Hyperstack");
			
			iw = new StackWindowPublicScrollBars(imp, cc);
			
			StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
			//obtain object ref. to scrollbars for channels frame & slices
				//StackWindowPublicScrollBars is an extension of the StackWindow class,
				//which makes the scrollbar object references available to this class.
			
			if(c > 1) {
				channelScrollbar = sw.getcSelector();
			}
			
			if(f > 1) {
				frameScrollbar = sw.gettSelector();
			}
			
			if(s > 1) {
				sliceScrollbar = sw.getzSelector();
			}
			
		}
		else if(imp.getImageStackSize() > 1) {
			
			//if image is not a hyperstack, but stack size is greater than 1, then only
			//one dimension is greater than one - either channel, slice, or frame.
			//depending on which is greater than one will depend on the operation:
				//NOTE: on images with one dimension, the scrollbar ref is always in zSelector.
			
			//ALL IMAGES passed through DialogWindow will be this, or not a stack:
				//All images with slices in will be projected, removign slices.
				//All images containing Frames will not be passed by the DialogWindow.
				//All Channels will still be present - making the image stack the no. of channels
					
			
			if(c > 1) { //if stack is due to channels:
				
				//This is the only possible section to be called in this block with current layout of code:
					//Only channels survive the filtering by DialogWindow and this class..
				
				//IJ.log("setUpWindow: Channels");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				channelScrollbar = sw.getzSelector();
				
			}
			else if( s > 1) { //if stack is due to slices:
				
				//This cannot be reached with current layout of code:
					//Slices will always be projected, yielding an image with one slice.
				
				//IJ.log("setUpWindow: Slices");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				sliceScrollbar = sw.getzSelector();
			
			}
			else if(f > 1) { //if stack is due to frames:
				
				//This cannot be reached with current layout of code:
					//Images parsed by the DialogWindow will not parse an image with frames.
				
				//IJ.log("setUpWindow: Frames");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				frameScrollbar = sw.getzSelector();
			
			}
		
		}
		else {
			
			//This is called if the image is a simple 2D image with only one channel, or if its a
				//zStack with one channel.
			
			//IJ.log("setUpWindow: No Stack - CustomWindow");
			
			iw = new CustomWindow(imp, cc);
		
		}
				
		panel.setLayout( new FlowLayout() );
		iw.add(panel);
		
		//instance = this;
		
		//Get the canvasKeyListener -> to allow activation and deactivation of key listener for channel
			//scrollbar:
		
		//canvasKeyListener = imp.getCanvas().getKeyListeners();
		
		//IJ.log(" ");
		
		//if the slice projector has run, however, need to ADD THE KEYLISTENER to the customcanvas:
		
		if(slProj == true) {
			
			// add the key listener that the IWP implements to the CustomCanvas:
						// use the addKeyListener method in this class.
						// The keyPressed() method will call moveProjectionForward() or moveProjectionBackward()
							// if the RIGHT and LEFT arrow keys are pressed.
			addKeyListener( new SliceProjectorKeyListener() );
			
			// Also add this keylistener to the array of keylisteners -> ensures CustomCanvas adds this listener
				// to itself when regenerated when a new projection is made!
			
			
		}
		
		//Finally, if the chProj is true -> remove and inactivate channels & scrollbar:
		if(chProj == true) {
			deactivateChannels();
			removeChannelsScrollbar();

			// also add keylistener to allow user to turn channels on and off by pressing the number keys on the
				// keyboard:
			
			addKeyListener( new ChannelProjectorKeyListener() );
			
		}
		
	} //end setUpWindow()
	
	
	
	
	

	
	/**
	 * Called if projection is true, and the image has more than one channel.
	 * <p>
	 * This method sets up the channel projector - it creates a JPanel for the 
	 * channel checkboxes to exist on, and initialises the ChannelCheckBox array.
	 * ChannelCheckBox objects are created in ascending integers, and the behaviour
	 * of the checkboxes themselves is added - which consists of changing the state
	 * of the state String variable in ChannelCheckBox (by calling changeState() in the
	 * ChannelCheckBox class), and calling updateChannels().
	 * <p>
	 * All ChannelCheckBox objects in the array are added to the panel, which is added
	 * to the ImageWindowWithPanel's Panel.
	 * 
	 */
	public void setupChannelProjector() {
		chProjPanel = new JPanel();
		chProjPanel.setLayout(new GridLayout(2,0));
		
		channels = new ChannelCheckBox[c];
		
		for(int a=0; a< c; a++) {
			final int aFinal = a;
			int aChannel = a + 1;
			String boxNo = ""+aChannel;
			channels[a] = new ChannelCheckBox(boxNo, true);
			//add listener to implement behaviour.
			channels[a].cb.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					channels[aFinal].changeState();
					ImageWindowWithPanel.this.updateChannels();
				}
				
			});
			chProjPanel.add(channels[a].cb);
		}

		panel.add(chProjPanel);
		
	}
	
	
	
	
	
	/**
	 * Called if projection is true, and the image has more than one channel.
	 * <p>
	 * This method sets up the channel projector - it creates a JPanel for the 
	 * channel checkboxes to exist on, and initialises the ChannelCheckBox array.
	 * ChannelCheckBox objects are created in ascending integers, and the behaviour
	 * of the checkboxes themselves is added - which consists of changing the state
	 * of the state String variable in ChannelCheckBox (by calling changeState() in the
	 * ChannelCheckBox class), and calling updateChannels().
	 * <p>
	 * All ChannelCheckBox objects in the array are added to the panel, which is added
	 * to the ImageWindowWithPanel's Panel.
	 * 
	 */
	public void setupChannelProjector(String activeChannels) {
		chProjPanel = new JPanel();
		chProjPanel.setLayout(new GridLayout(2,0));
		
		channels = new ChannelCheckBox[c];
		
		for(int a=0; a< c; a++) {
			final int aFinal = a;
			int aChannel = a + 1;
			String boxNo = ""+aChannel;
			
			if(activeChannels != null) {
				if(activeChannels.substring(a,a+1).equals("1") ) {
					//IJ.log("setupChannelProjector: 1");
					channels[a] = new ChannelCheckBox(boxNo, true);
				}
				else {
					//IJ.log("setupChannelProjector: 0");
					channels[a] = new ChannelCheckBox(boxNo, false);
				}
			}
			else {
				channels[a] = new ChannelCheckBox(boxNo, true);
			}
			
			
			//add listener to implement behaviour.
			channels[a].cb.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					channels[aFinal].changeState();
					ImageWindowWithPanel.this.updateChannels();
				}
				
			});
			chProjPanel.add(channels[a].cb);
		}

		panel.add(chProjPanel);
		
		CompositeImage ci = (CompositeImage)imp;
		ci.setMode(IJ.COMPOSITE);
		//ci.updateAndDraw();
		ci.updateImage();
		
		updateChannels();
		
	}
	
	
	
	
	
	
	/**
	 * Called if projection is true, and the image has more than one channel.
	 * <p>
	 * This method sets up the channel projector - it creates a JPanel for the 
	 * channel checkboxes to exist on, and initialises the ChannelCheckBox array.
	 * ChannelCheckBox objects are created in ascending integers, and the behaviour
	 * of the checkboxes themselves is added - which consists of changing the state
	 * of the state String variable in ChannelCheckBox (by calling changeState() in the
	 * ChannelCheckBox class), and calling updateChannels().
	 * <p>
	 * All ChannelCheckBox objects in the array are added to the panel, which is added
	 * to the ImageWindowWithPanel's Panel.
	 * 
	 */
	public void setupChannelProjector(int channelNumberDisplayed) {
		
		chProjPanel = new JPanel();
		chProjPanel.setLayout(new GridBagLayout() );

		GridBagConstraints gbc = new GridBagConstraints();
		
		// Add buttons to Panel:
		// set fill and weights for all:
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;
		
		channels = new ChannelCheckBox[c];
		
		for(int a=0; a< c; a++) {
			final int aFinal = a;
			int aChannel = a + 1;
			String boxNo = ""+aChannel;
			if(a<channelNumberDisplayed) {
				//Add the ChannelCheckBox as displayed, and add checkbox to chProjPanel:
				channels[a] = new ChannelCheckBox(boxNo, true);
				//add listener to implement behaviour.
				channels[a].cb.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						// TODO Auto-generated method stub
						channels[aFinal].changeState();
						ImageWindowWithPanel.this.updateChannels();
					}
				
				});
				gbc.gridx = a/4;
				gbc.gridy = a%4;
				chProjPanel.add(channels[a].cb, gbc);
				// chProjPanel.add(channels[a].cb);
			}
			else if(a>=channelNumberDisplayed) {
				//the ChannelCheckBox as not displayed, and do not add checkbox to chProjPanel:
				channels[a] = new ChannelCheckBox(boxNo, false);
				//add listener to implement behaviour.
				channels[a].cb.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						// TODO Auto-generated method stub
						channels[aFinal].changeState();
						ImageWindowWithPanel.this.updateChannels();
					}
				
				});
				
			}
			
		}

		//Finally, add chProjPanel to the panel:
		panel.add(chProjPanel);
		
	}
	
	
	
	
	
	
	/**
	 * Adds a Slice Projector panel to the iwp, and adds appropriate functionality, including
	 * ability to set the imp to various projections, and reverting the projected imp back to
	 * slice view.
	 */
	public void setupSliceProjector() {
		
		//create JPanel to hold Slice Projector objects:
		slProjPanel = new JPanel();
		
		// Make the layout a GridLayout with 2 columns:
		slProjPanel.setLayout(new GridBagLayout() );

		GridBagConstraints gbc = new GridBagConstraints();
		
		// Add buttons to Panel:
		// set fill and weights for all:
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;
		
		// create labels for spinners:
		first = new JLabel("First:");
		last = new JLabel("Last:");
		
		//rate = new JLabel("Rate:");
		
		
		//Spinners for first (min) and last (max) slices:
		
		minModel = new SpinnerNumberModel(1,1,s,1); //minModel has initial value of 1.
		maxModel = new SpinnerNumberModel(s,1,s,1); //maxModel has initial value of s.
		
		minSlice = new JSpinner(minModel); //create min Spinner object with minModel.
		maxSlice = new JSpinner(maxModel); //create max Spinner object with maxModel.
		
		// Make sure these components cannot receive the focus - keep the focus on the Image Canvas!
		//minSlice.setFocusable(false);
		//maxSlice.setFocusable(false);
		
		//Component[] comps = minSlice.getEditor().getComponents();
		//for (Component component : comps) {
		  //  component.setFocusable(false);
		//}
		
		//comps = maxSlice.getEditor().getComponents();
		//for (Component component : comps) {
		  //  component.setFocusable(false);
		//}
		
		// Spinner for rate:
		//rateModel = new SpinnerNumberModel(1,1,50,1);
		
		//rateSlice = new JSpinner(rateModel);
	
		// Create new sliceProjector from imp:
			//use zProjector.class in ij.plugin to implement slice projector behaviour
			//on the iwp's imp:
		sliceProjector = new ZProjector(imp);
		
		//Project current image slices with ZProjector:
		sliceProjector.setMethod(ZProjector.MAX_METHOD);
		sliceProjector.setStartSlice( (int)minModel.getValue() );
		sliceProjector.setStopSlice( (int)maxModel.getValue() );
		
		//updateSlices();
		
		// IJ.showMessage("setupSliceProj: Imp prior to projection:");
		//IJ.log("setupSliceProj: IMP slices: "+imp.getNSlices() );
		//IJ.log(" ");
		
		if(imp.isHyperStack() ) {
			sliceProjector.doHyperStackProjection(true);
		}
		else if (imp.getType()==ImagePlus.COLOR_RGB){
			sliceProjector.doRGBProjection(true);
		}
		else { 
			sliceProjector.doProjection(true); 
		}
		
		// IJ.showMessage("setupSliceProj: Imp post-projection:");
		//IJ.log("setupSliceProj: IMP slices: "+imp.getNSlices() );
		//IJ.log(" ");
	
		impOriginal = imp;
		
		//impProjected = sliceProjector.getProjection();
		
		//impProjected.setTitle(title);
		
		
		imp = sliceProjector.getProjection();
		
		imp.setTitle(title);
				
		//IJ.log("setupSliceProj: Imp post-set projection:");
		//IJ.log("setupSliceProj: IMP slices: "+imp.getNSlices() );
		
		//IJ.log("setupSliceProj: ImpOriginal post-set projection:");
		//IJ.log("setupSliceProj: IMPOriginal slices: "+impOriginal.getNSlices() );
		
		//IJ.log(" ");
		
		minSlice.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// Derive min and max spinner values:
				int minVal = (int)minModel.getValue();
				int maxVal = (int)maxModel.getValue();
				//if min spinner value exceeds max spinner val, set
				//max spinner value to min spinner value:
				if(minVal > maxVal) {
					maxVal = minVal;
					maxSlice.setValue(maxVal);
				}
				//produce a projection of the current imp using the zProjector
				//with the current minVal and maxVal:
				sliceProjector.setStartSlice( (int)minModel.getValue() );
				sliceProjector.setStopSlice( (int)maxModel.getValue() );
				updateSlices(true);
			}
		});
		
		maxSlice.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// Derive min and max spinenr values:
				int minVal = (int)minModel.getValue();
				int maxVal = (int)maxModel.getValue();
				//if max spinner value falls below min spinner val, set
				//min spinner value to max spinner value:
				if(maxVal < minVal) {
					minVal = maxVal;
					minSlice.setValue(minVal);
				}
				//produce a projection of the current imp using the zProjector
				//with the current minVal and maxVal:
				sliceProjector.setStartSlice( (int)minModel.getValue() );
				sliceProjector.setStopSlice( (int)maxModel.getValue() );
				updateSlices(true);
			}
		});
		
		
		// Instantiate Buttons - forward and backward (for moving through the slices at the given projection thickness)
		
		forward = new JButton( createImageIcon("/Icons/Next 100x100.png", "Icon Toggle", 20, 20) );
		backward = new JButton( createImageIcon("/Icons/Previous 100x100.png", "Icon Toggle", 20, 20) );
		
		// Make sure these components cannot receive the focus - keep the focus on the Image Canvas!
		forward.setFocusable(false);
		backward.setFocusable(false);
		
		// Add ActionListeners to these buttons:
		
		forward.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Implement behaviour in a separate method
					// So it can be called by the KeyListener as well!
				
				moveProjectionForward();
				
			}
			
		});
		
		backward.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Implement behaviour in a separate method
					// So it can be called by the KeyListener as well!
			
				moveProjectionBackward();
				
			}
			
		});
		
		// First, add the labels to the slProjPanel:
		gbc.gridx = 0;
		gbc.gridy = 0;
		slProjPanel.add(first, gbc);
		// slProjPanel.add(first);
		gbc.gridx = 1;
		gbc.gridy = 0;
		slProjPanel.add(last, gbc);
		// slProjPanel.add(last);
		
		//add spinners to slProjPanel:
		gbc.gridx = 0;
		gbc.gridy = 1;
		slProjPanel.add(minSlice, gbc);
		//slProjPanel.add(minSlice);
		gbc.gridx = 1;
		gbc.gridy = 1;
		slProjPanel.add(maxSlice, gbc);
		//slProjPanel.add(maxSlice);
		
		// finally, add the buttons to move through the image:
		gbc.gridx = 0;
		gbc.gridy = 2;
		slProjPanel.add(backward, gbc);
		//slProjPanel.add(backward);
		gbc.gridx = 1;
		gbc.gridy = 2;
		slProjPanel.add(forward, gbc);
		//slProjPanel.add(forward);
		
		//add slProjPanel to iwp's panel:
		panel.add(slProjPanel);
				
	}
	
	
	
	
	
	
	
	/**
	 * Adds a Slice Projector panel to the iwp, and adds appropriate functionality, including
	 * ability to set the imp to various projections, and reverting the projected imp back to
	 * slice view.
	 * @param sliceValues:  This is a int[] array of length two, the first value is the minVal
	 * projection value, the second value is the maxVal projection value.
	 */
	public void setupSliceProjector(int[] sliceValues) {
		
		
		int minVal = sliceValues[0];
		int maxVal = sliceValues[1];
		
		//create JPanel to hold Slice Projector objects:
		slProjPanel = new JPanel();
		
		//create two "spinner" objects to handle slice projections:
		
		minModel = new SpinnerNumberModel(minVal,1,s,1); //minModel has initial value of 1.
		maxModel = new SpinnerNumberModel(maxVal,1,s,1); //maxModel has initial value of s.
		
		minSlice = new JSpinner(minModel); //create min Spinner object with minModel.
		maxSlice = new JSpinner(maxModel); //create max Spinner object with maxModel.
	
		//use zProjector.class in ij.plugin to implement slice projector behaviour
		//on the iwp's imp:
		sliceProjector = new ZProjector(imp);
		
		//Project current image slices with ZProjector:
		sliceProjector.setMethod(ZProjector.MAX_METHOD);
		sliceProjector.setStartSlice( (int)minModel.getValue() );
		sliceProjector.setStopSlice( (int)maxModel.getValue() );
		
		//IJ.log("setupSliceProjector(sliceValues): stopSlice: "+(int)maxModel.getValue() );
		//IJ.log("setupSliceProjector(sliceValues): imp.getNSlice(): "+ imp.getNSlices() );
		//IJ.log("setupSliceProjector(sliceValues): sliceProj stopSlice: "+imp.getStackSize() );
		
		//updateSlices();
		
		IJ.log("setupSliceProj: Imp prior to projection:");
		//IJ.log("setupSliceProj: IMP slices: "+imp.getNSlices() );
		//IJ.log(" ");
		
		if(imp.isHyperStack() ) {
			sliceProjector.doHyperStackProjection(true);
		}
		else if (imp.getType()==ImagePlus.COLOR_RGB){
			sliceProjector.doRGBProjection(true);
		}
		else { 
			sliceProjector.doProjection(true); 
		}
		
		IJ.log("setupSliceProj: Imp post-projection:");
		//IJ.log("setupSliceProj: IMP slices: "+imp.getNSlices() );
		//IJ.log(" ");
	
		impOriginal = imp;
		
		//impProjected = sliceProjector.getProjection();
		
		//impProjected.setTitle(title);
		
		
		imp = sliceProjector.getProjection();
		
		imp.setTitle(title);
				
		//IJ.log("setupSliceProj: Imp post-set projection:");
		//IJ.log("setupSliceProj: IMP slices: "+imp.getNSlices() );
		
		//IJ.log("setupSliceProj: ImpOriginal post-set projection:");
		//IJ.log("setupSliceProj: IMPOriginal slices: "+impOriginal.getNSlices() );
		
		//IJ.log(" ");
		
		minSlice.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// Derive min and max spinner values:
				int minVal = (int)minModel.getValue();
				int maxVal = (int)maxModel.getValue();
				//if min spinner value exceeds max spinner val, set
				//max spinner value to min spinner value:
				if(minVal > maxVal) {
					maxVal = minVal;
					maxSlice.setValue(maxVal);
				}
				//produce a projection of the current imp using the zProjector
				//with the current minVal and maxVal:
				//sliceProjector.setStartSlice( (int)minModel.getValue() );
				//sliceProjector.setStopSlice( (int)maxModel.getValue() );
					// do not need to set start or stop slices - called in updateSlices()
				updateSlices(true);
			}
		});
		
		maxSlice.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// Derive min and max spinenr values:
				int minVal = (int)minModel.getValue();
				int maxVal = (int)maxModel.getValue();
				//if max spinner value falls below min spinner val, set
				//min spinner value to max spinner value:
				if(maxVal < minVal) {
					minVal = maxVal;
					minSlice.setValue(minVal);
				}
				//produce a projection of the current imp using the zProjector
				//with the current minVal and maxVal:
				//sliceProjector.setStartSlice( (int)minModel.getValue() );
				//sliceProjector.setStopSlice( (int)maxModel.getValue() );
					// do not need to set start or stop slices - called in updateSlices()
				updateSlices(true);
			}
		});
		
		//add spinners to slProjPanel:
		slProjPanel.add(minSlice);
		slProjPanel.add(maxSlice);
		
		//add slProjPanel to iwp's panel:
		panel.add(slProjPanel);
				
	}
	
	
	/**
	 * Moves the projection forward one slice.  Maintains the same distance between the first and
	 * last slice.  Allows the user to move through an image at a given projection thickness.
	 */
	public void moveProjectionForward() {
		// Derive min and max spinner values:
		//int minVal = (int)minModel.getValue();
		//int maxVal = (int)maxModel.getValue();
		//if min spinner value exceeds max spinner val, set
		//max spinner value to min spinner value:
		//if(minVal > maxVal) {
			//maxVal = minVal;
			//maxSlice.setValue(maxVal);
		//}
		
		// IJ.showMessage("Move Proj Forward:");

		//try to increment both the minModel and maxModel by 1:
		if((int)maxModel.getValue() == sOrig) {
			//if maxVal is already equal to sOrig, then projection has reached the end of the stack!
				//Here want to keep increasing the minSlice value and project, as this allows the user to
					//nudge the projection thickness down when the end of the stack is reached:
			if((int)minModel.getValue() != sOrig) {
				//as long as the minModel value has not reached  the max value, then keep incrementing it 
					//and projecting it:
				minSlice.setValue( ((int)minModel.getValue()+1) );
				
				// IJ.showMessage("UpdateSlices:");
				updateSlices(true);
				
			}
		}
		else {
			// otherwise, the projection can be increased by 1, so do so for both minSlice and maxSlice:
				// Increment the maxSlice/maxModel first - otherwise the changeListener on minSlice will increment
					//maxModel by 1 before its incremented here!!
			maxSlice.setValue( ((int)maxModel.getValue()+1) );
			minSlice.setValue( ((int)minModel.getValue()+1) );
			// and now do the projection and updateSlices:
			//sliceProjector.setStartSlice( (int)minModel.getValue() );
			//sliceProjector.setStopSlice( (int)maxModel.getValue() );
				// do not need to set start or stop slices - called in updateSlices()
			// IJ.showMessage("UpdateSlices:");
			updateSlices(true);
		}

	}
	
	/**
	 * Called by the KeyListener when the SHIFT key is down and the RIGHT arrow key is pressed.
	 */
	public void moveMinForward() {

		if((int)minModel.getValue() != (int)maxModel.getValue() ) {
			//as long as the minModel value has not reached  the max value, then keep incrementing it 
				//and projecting it:
			minSlice.setValue( ((int)minModel.getValue()+1) );
			updateSlices(true);
			
		}
	}
	
	/**
	 * Called by the KeyListener when the ALT key is down and the RIGHT arrow key is pressed.
	 */
	public void moveMaxForward() {

		if((int)maxModel.getValue() != sOrig ) {
			//as long as the maxModel value has not reached  the max value, then keep incrementing it 
				//and projecting it:
			maxSlice.setValue( ((int)maxModel.getValue()+1) );
			updateSlices(true);
			
		}
	}
	
	
	/**
	 * Moves the projection backward one slice.  Maintains the same distance between the first and
	 * last slice.  Allows the user to move through an image at a given projection thickness.
	 */
	public void moveProjectionBackward() {
		// Derive min and max spinner values:
				//int minVal = (int)minModel.getValue();
				//int maxVal = (int)maxModel.getValue();
				//if min spinner value exceeds max spinner val, set
				//max spinner value to min spinner value:
				//if(minVal > maxVal) {
					//maxVal = minVal;
					//maxSlice.setValue(maxVal);
				//}
		
		
				//try to increment both the minModel and maxModel by 1:
		if((int)minModel.getValue() == 1) {
			//if minVal is already equal to 1, then projection has reached the start of the stack!
				//Here want to keep decreasing the maxSlice value and project, as this allows the user to
				//nudge the projection thickness down when the start of the stack is reached:
			if((int)maxModel.getValue() != 1) {
				//as long as the maxModel value has not reached  the min value [1], then keep incrementing it 
					//and projecting it:
				maxSlice.setValue( ((int)maxModel.getValue()-1) );
				updateSlices(true);
			}
		}
		else {
			// otherwise, the projection can be decreased by 1, so do so for both minSlice and maxSlice:
			minSlice.setValue( ((int)minModel.getValue()-1) );
			maxSlice.setValue( ((int)maxModel.getValue()-1) );
			// and now do the projection and updateSlices:
			//sliceProjector.setStartSlice( (int)minModel.getValue() );
			//sliceProjector.setStopSlice( (int)maxModel.getValue() );
				// do not need to set start or stop slices - called in updateSlices()
			updateSlices(true);
		}
	}
	
	
	/**
	 * Called by the KeyListener when the SHIFT key is down and the LEFT arrow key is pressed.
	 */
	public void moveMaxBackward() {
		if((int)maxModel.getValue() != (int)minModel.getValue() ) {
			//as long as the maxModel value has not reached  the min value, then keep decrementing it 
				//and projecting it:
			maxSlice.setValue( ((int)maxModel.getValue()-1) );
			updateSlices(true);
		}
	}
	

	/**
	 * Called by the KeyListener when the ALT key is down and the LEFT arrow key is pressed.
	 */
	public void moveMinBackward() {
		if((int)minModel.getValue() != 1 ) {
			//as long as the minModel value has not reached the min value [1], then keep decrementing it 
				//and projecting it:
			minSlice.setValue( ((int)minModel.getValue()-1) );
			updateSlices(true);
		}
	}

	
	
	class SliceProjectorKeyListener implements KeyListener {	
	
	@Override
	public void keyTyped(KeyEvent e) {	}



	@Override
	public void keyPressed(KeyEvent e) {

		//IJ.showMessage("Key Pressed: "+e.getKeyCode() );
		
		int keyCode = e.getKeyCode();
		IJ.setKeyDown(keyCode);
		boolean hotkey = false;
		//if (keyCode==KeyEvent.VK_CONTROL || keyCode==KeyEvent.VK_SHIFT)
			//return;
		char keyChar = e.getKeyChar();
		int flags = e.getModifiers();
		if (IJ.debugMode) IJ.log("keyPressed: code=" + keyCode + " (" + KeyEvent.getKeyText(keyCode)
			+ "), char=\"" + keyChar + "\" (" + (int)keyChar + "), flags="
			+ KeyEvent.getKeyModifiersText(flags));
		boolean shift = (flags & KeyEvent.SHIFT_MASK) != 0;
		//boolean control = (flags & KeyEvent.CTRL_MASK) != 0;
		boolean alt = (flags & KeyEvent.ALT_MASK) != 0;
		//boolean meta = (flags & KeyEvent.META_MASK) != 0;
		//String cmd = null;
		
		
		// if the left arrow key is pressed, move the projection backwards:
		if(keyCode == KeyEvent.VK_LEFT) {
			if(shift) {
				moveMaxBackward();
			}
			else if(alt) {
				moveMinBackward();
			}
			else {
				moveProjectionBackward();
			}
		}
		
		// if the right arrow key is pressed, move the projection forwards:
		if(keyCode == KeyEvent.VK_RIGHT) {
			if(shift) {
				moveMinForward();
			}
			else if(alt) {
				moveMaxForward();
			}
			else {
				moveProjectionForward();
			}
		}
		
        		
		
		//if (cmd==null) {
			//switch (keyChar) {
				//case '<': case ',': if (isStack) cmd="Previous Slice [<]"; break;
				//case '>': case '.': case ';': if (isStack) cmd="Next Slice [>]"; break;
				//case '+': case '=': cmd="In [+]"; break;
				//case '-': cmd="Out [-]"; break;
				//case '/': cmd="Reslice [/]..."; break;
				//default:
			//}
		//}

		//if (cmd==null) {
			//switch (keyCode) {
				
				//case KeyEvent.VK_BACK_SLASH: cmd=IJ.altKeyDown()?"Animation Options...":"Start Animation"; break;
				//case KeyEvent.VK_EQUALS: cmd="In [+]"; break;
				//case KeyEvent.VK_MINUS: cmd="Out [-]"; break;
				//default: break;
			//}
		//}
		
		//if (cmd!=null && !cmd.equals("")) {
			//String commandName = cmd;
			//if (cmd.equals("Fill")||cmd.equals("Draw"))
				//hotkey = true;
			//if (cmd.charAt(0)==MacroInstaller.commandPrefix)
				//MacroInstaller.runMacroShortcut(cmd);
			//else {
				//ImageJ.doCommand(cmd);
				//new Executer(cmd, null);
				//new CustomExecuter(cmd, null);
				//keyPressedTime = System.currentTimeMillis();
				//lastKeyCommand = cmd;
			//}
		//}
		
	}



	@Override
	public void keyReleased(KeyEvent e) {	}

	}
	
	
	class ChannelProjectorKeyListener implements KeyListener {

		@Override
		public void keyTyped(KeyEvent e) {	}



		@Override
		public void keyPressed(KeyEvent e) {

			//IJ.showMessage("Key Pressed: "+e.getKeyCode() );
			
			int keyCode = e.getKeyCode();
			IJ.setKeyDown(keyCode);
			boolean hotkey = false;
			if (keyCode==KeyEvent.VK_CONTROL || keyCode==KeyEvent.VK_SHIFT)
				return;
			char keyChar = e.getKeyChar();
			int flags = e.getModifiers();
			if (IJ.debugMode) IJ.log("keyPressed: code=" + keyCode + " (" + KeyEvent.getKeyText(keyCode)
				+ "), char=\"" + keyChar + "\" (" + (int)keyChar + "), flags="
				+ KeyEvent.getKeyModifiersText(flags));
			boolean shift = (flags & KeyEvent.SHIFT_MASK) != 0;
			boolean control = (flags & KeyEvent.CTRL_MASK) != 0;
			boolean alt = (flags & KeyEvent.ALT_MASK) != 0;
			boolean meta = (flags & KeyEvent.META_MASK) != 0;
			String cmd = null;
			
			// if the 1 key is pressed, adjust the channels array at index 0 & updateChannels()
			if(keyCode == KeyEvent.VK_1) {
				// IJ.showMessage("Key Pressed: 1");
				//adjust the channel at index 0, only if channels length is AT LEAST 1:
				if(channels.length >= 1) {
					channels[0].setState( ( !channels[0].getStateBool() ) );
					updateChannels();
				}
			}
			
			
			// if the 1 key is pressed, adjust the channels array at index 0 & updateChannels()
						if(keyCode == KeyEvent.VK_2) {
							//adjust the channel at index 0, only if channels length is AT LEAST 1:
							if(channels.length >= 2) {
								channels[1].setState( ( !channels[1].getStateBool() ) );
								updateChannels();
							}
						}
						
						
						// if the 1 key is pressed, adjust the channels array at index 0 & updateChannels()
						if(keyCode == KeyEvent.VK_3) {
							//adjust the channel at index 0, only if channels length is AT LEAST 1:
							if(channels.length >= 3) {
								channels[2].setState( ( !channels[2].getStateBool() ) );
								updateChannels();
							}
						}
						
						
						// if the 1 key is pressed, adjust the channels array at index 0 & updateChannels()
						if(keyCode == KeyEvent.VK_4) {
							//adjust the channel at index 0, only if channels length is AT LEAST 1:
							if(channels.length >= 4) {
								channels[3].setState( ( !channels[3].getStateBool() ) );
								updateChannels();
							}
						}
						
						
						// if the 1 key is pressed, adjust the channels array at index 0 & updateChannels()
						if(keyCode == KeyEvent.VK_5) {
							//adjust the channel at index 0, only if channels length is AT LEAST 1:
							if(channels.length >= 5) {
								channels[4].setState( ( !channels[4].getStateBool() ) );
								updateChannels();
							}
						}
						
						
						// if the 1 key is pressed, adjust the channels array at index 0 & updateChannels()
						if(keyCode == KeyEvent.VK_6) {
							//adjust the channel at index 0, only if channels length is AT LEAST 1:
							if(channels.length >= 6) {
								channels[5].setState( ( !channels[5].getStateBool() ) );
								updateChannels();
							}
						}
						
						
						// if the 1 key is pressed, adjust the channels array at index 0 & updateChannels()
						if(keyCode == KeyEvent.VK_7) {
							//adjust the channel at index 0, only if channels length is AT LEAST 1:
							if(channels.length >= 7) {
								channels[6].setState( ( !channels[6].getStateBool() ) );
								updateChannels();
							}
						}
						
						
						
						// if the 1 key is pressed, adjust the channels array at index 0 & updateChannels()
						if(keyCode == KeyEvent.VK_8) {
							//adjust the channel at index 0, only if channels length is AT LEAST 1:
							if(channels.length >= 8) {
								channels[7].setState( ( !channels[7].getStateBool() ) );
								updateChannels();
							}
						}
						
						
						
						// if the 1 key is pressed, adjust the channels array at index 0 & updateChannels()
						if(keyCode == KeyEvent.VK_9) {
							//adjust the channel at index 0, only if channels length is AT LEAST 1:
							if(channels.length >= 9) {
								channels[8].setState( ( !channels[8].getStateBool() ) );
								updateChannels();
							}
						}
						
						
						

			
	        		
			
			//if (cmd==null) {
				//switch (keyChar) {
					//case '<': case ',': if (isStack) cmd="Previous Slice [<]"; break;
					//case '>': case '.': case ';': if (isStack) cmd="Next Slice [>]"; break;
					//case '+': case '=': cmd="In [+]"; break;
					//case '-': cmd="Out [-]"; break;
					//case '/': cmd="Reslice [/]..."; break;
					//default:
				//}
			//}

			//if (cmd==null) {
				//switch (keyCode) {
					
					//case KeyEvent.VK_BACK_SLASH: cmd=IJ.altKeyDown()?"Animation Options...":"Start Animation"; break;
					//case KeyEvent.VK_EQUALS: cmd="In [+]"; break;
					//case KeyEvent.VK_MINUS: cmd="Out [-]"; break;
					//default: break;
				//}
			//}
			
			//if (cmd!=null && !cmd.equals("")) {
				//String commandName = cmd;
				//if (cmd.equals("Fill")||cmd.equals("Draw"))
					//hotkey = true;
				//if (cmd.charAt(0)==MacroInstaller.commandPrefix)
					//MacroInstaller.runMacroShortcut(cmd);
				//else {
					//ImageJ.doCommand(cmd);
					//new Executer(cmd, null);
					//new CustomExecuter(cmd, null);
					//keyPressedTime = System.currentTimeMillis();
					//lastKeyCommand = cmd;
				//}
			//}
			
		}



		@Override
		public void keyReleased(KeyEvent e) {	}
		
	}
	
	
	
	/**
	 * Called whenever one of the channels[] checkbox
	 * objects has its actionPerformed() method called - i.e whenever one
	 * checkbox has its state changed.
	 * <p>
	 * This method will update the display of image channels according to
	 * the state of the checkboxes in the channels[] array of ChannelCheckBox
	 * objects.
	 */
	public void updateChannels() {
		
		String activeChannels = "";
		for(int a=0; a<c; a++) {
			activeChannels = activeChannels + channels[a].state;
		}
		
		// IJ.showMessage("activeChannels: "+activeChannels);
		
		imp.setActiveChannels(activeChannels);
		
		// FINALLY -> give the custom canvas the focus:
			// Ensures the custom canvas has focus after adjusting the channel or slice projections
		if(imp.isVisible() == true) {
			cc.requestFocusInWindow();
		}

	}
	
	
	
	/**
	 * Get the activeChannels String - sequence of 1s and 0s indicating channel
	 * activation states.  Useful for getting and saving the channel activation state
	 * of a given IWP.
	 * @return String of 1s and 0s indicating channel activation states.
	 */
	public String getDisplayChannels() {
		
		if(chProj == true) {
			
			String displayChannels = "";
			
			for(int a=0; a<c; a++) {
				displayChannels = displayChannels + channels[a].state;
			}
		
			return displayChannels;
		
		}
		
		else {
			
			return null;
			
		}
		
	}
	
	
	
	/**
	 * Set activeChannels String for channel display.  This method is useful for setting 
	 * which channels to display manually in a new IWP.
	 * @param activeChannels
	 */
	public void setDisplayChannels(String activeChannels) {
		//first set the checkbox array to the values in activeChannels:
		for(int a=0; a<c; a++) {
			String state = activeChannels.substring(a,a+1);
			if(state.equals("1") ) {
				//IJ.showMessage("State: _"+state+"_");
				//channels[a].cb.setSelected(true);
				//channels[a].state = state;
				//Do not need to do anything - channels automatically on by default..
			}
			else if(state.equals("0") ) {
				channels[a].cb.setSelected(false);
				channels[a].changeState();
			}
		}
		//then set Active Channels according to the values in activeChannels:
		imp.setActiveChannels(activeChannels);
	}
	
	
	
	public void setC(int c) {
		this.c = c;
	}
	
	
	
	/**
	 * Set the active channel of the image -> the channel which the pixel data actually represents.
	 * @param c Int - which channel to set as the active channel.
	 */
	public void setActiveChannel(int c) {

		// Set the channel to the imp:
		imp.setC(c);
		
		activeChannel = c;
		
		// update&draw window -> 		
			//Sets pixels of the ImagePlus ImageProcessor to that of the active 
				//slice/channel/frame in the stack.
			//Retrieves this imp's ImageWindow & then its canvas, and sets the image as updated &
				//repaints the canvas via the draw() method.
		//repaintWindow ->
			//repaints the window to update text above the canvas.
		imp.updateAndRepaintWindow();
		
	}
	
	
	
	/**
	 * This method updates the display of slices when the first or last projection slice is 
	 * changed by the minSlice or maxSlice JSpinners.  Also updates the channels if necessary.
	 * @param boolean updateAndDraw:  Will update image and repaint the window if set to true.
	 */
	public void updateSlices(boolean updateAndDraw) {
		
		//derive the currently active channel:
		int channel = imp.getC();

		//REBUILD THE SLICEPROJECTOR WITH THE ImpOriginal - to make sure it has the right information!
		
		//sliceProjector = new ZProjector(impOriginal);
			//THIS CAUSES A BIGGER ERROR!
		
		sliceProjector.setStartSlice( (int)minModel.getValue() );
		sliceProjector.setStopSlice( (int)maxModel.getValue() );
		
		
		if(imp.isHyperStack() || f>1 || c>1) { 
			//if imp is a hyperstack, or a stack, need to do HyperStackProjection
			//imp is a stack if f is greater than one
				//(This handles stacks containing slices and frames only)
			//imp is a stack if c is greater than one
				//(This handles stacks containing slices and channels only)
			// IJ.log("updateSlices: HyperStackProjection");
			sliceProjector.doHyperStackProjection(true);
		}
		else if (imp.getType()==ImagePlus.COLOR_RGB){
			sliceProjector.doRGBProjection(true);
		}
		else { 

			//IJ.log("updateSlices: StackProjection");
			sliceProjector.doProjection(true); 
		}
		
		//IJ.log(" ");
		
		//IJ.log("updateSlices: Imp post-projection:");
		//IJ.log("updateSlices: IMP slices: "+imp.getNSlices() );
		//IJ.log(" ");
		
		//set imp to projection, set to window, update, and fit to window:
		imp = sliceProjector.getProjection();
		// imp.setTitle(title); // DO NOT DO THIS: as the image may be renamed by programmer in plugin...
		
		//IJ.log("updateSlices: Imp post-set projection:");
		//IJ.log("updateSlices: IMP slices: "+imp.getNSlices() );
		
		//IJ.log("updateSlices: ImpOriginal post-set projection:");
		//IJ.log("updateSlices: IMPOriginal slices: "+impOriginal.getNSlices() );
		//IJ.log(" ");
		
		iw.setImage(imp);
		
		//if(updateAndDraw == true) {
			//imp.updateAndDraw();		
		//}
		//imp.getCanvas().fitToWindow();
		
		//IJ.log("updateSlices: active channel after projection: "+imp.getC() );
		
		if(c > 1) { //update channels if more than one channel:
			updateChannels();
			//also, set current channel to the channel the imp was initially:
			//imp.setC(channel);
			imp.setC(activeChannel);
		}
		
		if(updateAndDraw == true) {
			//imp.updateAndDraw();	
			imp.updateAndRepaintWindow();
		}
		
		// add the overlay from original imp to imp -> AFTER updateAndRepaintWindow!
		imp.setOverlay( impOriginal.getOverlay() );
		impOriginal.setOverlay( impOriginal.getOverlay() );
		
		// FINALLY -> give the custom canvas the focus:
			// Ensures the custom canvas has focus after adjusting the channel or slice projections
		cc.requestFocusInWindow();
		
	}
	
	
	
	
	
	public int[] getSliceProjectionValues() {
	
		if(slProj == true) {
		
			int minVal = (int)minModel.getValue();
			int maxVal = (int)maxModel.getValue();
			
			int[] values = new int[2];
			
			values[0] = minVal;
			values[1] = maxVal;
			
			return values;
		
		}
		
		else {
			
			return null;
		
		}
	
	}
	
	
	
	
	/**
	 * This method will set the slice projector spinner models to the values (after checking they are valid
	 * values for projection), and sets the sliceProjector start and stop slices, and finally performs
	 * the projection (by calling updateSlices(true) ).
	 * @param values A 2 value int[] array - first int is the minSlice, second int is the maxSlice.
	 */
	public void setSliceProjection(int[] values) {
		
		//Check the values are in range:
		
		//If the minVal is below 1, set it to 1 - the first slice.
		if(values[0] < 1) {
			values[0] = 1;
		}
		
		//if the maxVal is above the number of slices, set it to max number of slices:
		if(values[1] > sOrig) {
			values[1] = sOrig;
		}
		
		//set spinner values:
		minModel.setValue(values[0]);
		maxModel.setValue(values[1]);
		//produce appropriate projection using zProjector with the current minVal and maxVal:
		sliceProjector.setStartSlice( (int)minModel.getValue() );
		sliceProjector.setStopSlice( (int)maxModel.getValue() );
		updateSlices(true);
	}
	
	public void redisplayImage(String activechannels, int[] sliceValues) {
		
	}
	
	/**
	 * Returns the ImageWindow object.
	 * @return ImageWindow of this instance.
	 */
	public ImageWindow getWindow() {
		return iw;
	}
	
	/**
	 * Returns the ImagePlus object.  Note this returns a reference to the currently displayed
	 * imp, which may have been projected.  For the original imp (which will include all slices
	 * and channesl), call getOriginalImagePlus().
	 * @return ImagePlus of this instance.
	 */
	public ImagePlus getImagePlus() {
		return imp;
	}
	/**
	 * Returns the original ImagePlus object - which has not had any slices removed due
	 * to projection.
	 * @return The original ImagePlus of this instance.
	 */
	public ImagePlus getOriginalImagePlus() {
		return impOriginal;
	}
	
	/**
	 * Returns the original image title.
	 * @return
	 */
	public String getImageTitle() {
		return title;
	}
	
	/**
	 * Set a new ImagePlus to this object:
	 */
	public void setImagePlus(ImagePlus imp) {
		this.imp = imp;
		setUpWindow();
		layoutWindow();
	}
	
	/**
	 * Returns the File object of the ImagePlus on the Window. This method will
	 * return 'null' if the ImagePlus was passed directly to this Window without
	 * a File object.
	 * @return the File object which the imp was generated from, otherwise null.
	 */
	public File getImagePath() {
		return filePath;
	}
	
	/**
	 * Returns the panel reference from this ImageWindowWithPanel object.
	 * @return The panel associated with this ImageWindowWithPanel object.
	 */
	public Panel getPanel() {
		return panel;
	}
	
	
	
	/**
	 * This method will clear the panel COMPLETELY including channels and slices controls.
	 * <p>
	 * To clear panel WITHOUT removing channels and slices controls, use:
	 * <p>
	 * clearPanelKeepChSl()
	 */
	public void clearPanel() {
		
		int keptPanels = 0;
		
		//if(chProj == true) {
			//keptPanels = keptPanels + 1;
		//}
		//if(slProj == true) {
			//keptPanels = keptPanels + 1;
		//}
		
		Component[] comps = panel.getComponents();
		
		//IJ.log("clearPanel: Components: "+comps.length);
		
		for(int a = comps.length-1; a>=keptPanels; a--) {
		
			panel.remove(comps[a]);
			//IJ.log("   clearPanel: removed comp: "+comps[a]);
		
		}
		
		//IJ.log(" ");
		
	}
	
	/**
	 * This method will clear the panel except for the panels for channel and slice projection.
	 */
	public void clearPanelKeepChSl() {
		
		int keptPanels = 0;
		
		if(chProj == true) {
			keptPanels = keptPanels + 1;
		}
		if(slProj == true) {
			keptPanels = keptPanels + 1;
		}
		
		Component[] comps = panel.getComponents();
		
		//IJ.log("clearPanel: Components: "+comps.length);
		
		for(int a = comps.length-1; a>=keptPanels; a--) {
		
			panel.remove(comps[a]);
			//IJ.log("   clearPanel: removed comp: "+comps[a]);
		
		}
		
		//IJ.log(" ");
		
	}
	
	/**
	 * Sets a new Panel to this ImageWindowWithPanwl object.
	 * @param panel The new Panel object to be added to this instance.
	 */
	public void setNewPanel(Panel panel) {
		
		iw.remove(this.panel);
		
		this.panel = panel;
		
		panel.setLayout( new FlowLayout() );
		iw.add(panel);
		
		//repaintWindow();
		
	}
	
	/**
	 * Returns the number of channels on the original imp (independent of any projections).
	 * @return Channels on original imp.
	 */
	public int getChannels() {
		return c;
	}
	
	/**
	 * Returns the number of slices on the original imp (independent of any projections).
	 * @return Slice on original imp.
	 */
	public int getSlices() {
		return sOrig;
	}
	
	/**
	 * Returns the number of slices on the projected imp.
	 */
	public int getSlicesProj() {
		return s;
	}
	
	/**
	 * Returns the number of frames on the original imp (independent of any projections).
	 * @return Frames on original imp.
	 */
	public int getFrames() {
		return f;
	}
	
	
	
	
	public CustomCanvas getCanvas() {
		return cc;
	}

	/**
	 * This method maximises the window with panel, validates the components on the frame,
	 * and centres the frame on the screen, all relative to the content of the window and panel.
	 * This method ensures the whole image is displayed as large as possible, and includes all
	 * scrollbars, and all components attached to the Panel on the Frame.  When required, components
	 * on the panel will be shifted to new rows to match panel width to window width.
	 * <p>
	 * This method also calls the addToolPanelListeners() method if the ToolPanels ArrayList contains
	 * more than one ToolPanel object.  This method ensures all ToolPanel objects have mouseListeners
	 * registed with eachother, and ensures the currect current tool is displayed across all ToolPanels.
	 * See addToolPanelListeners() for more information.
	 */
	public void layoutWindow() {
		
		iw.setVisible(true); //need to call this as setVisible(false) is called in IW constructor after super() !!
		
		iw.maximize();
		//maximise(); //calling maximise in this class, as ImageWindow maximise() method
					//not only gets maximum bounds and sets this to the window, but also calls
					//the ImageCanvas fitToWindow() method, and the pack() method.
						// This is not a problem now, as I have sub-classed the ImageCanvas!
		
		//cc.fitToWindow();
				
		//iw.pack();
		//iw.setVisible(true);
		
		//if ToolPanels has more than one ToolPanel reference within it, run the
				//addToolPanelListeners() method in this class:
				if(ToolPanels != null) {
					if(ToolPanels.size() > 1) {
						this.addToolPanelListeners();
					}
				}
						
	}
	
	/**
	 * Maximise the ImageWindowWithPanel.
	 */
	public void maximise() {
		Rectangle rect = iw.getMaximumBounds();
		iw.setBounds(rect.x, rect.y, rect.width, rect.height);
	}
	
	
	
	/**
	 * This method maximises the window with panel, validates the components on the frame,
	 * and centres the frame on the screen, all relative to the content of the window and panel.
	 * This method ensures the whole image is displayed as large as possible, and includes all
	 * scrollbars, and all components attached to the Panel on the Frame.  When required, components
	 * on the panel will be shifted to new rows to match panel width to window width.
	 * <p>
	 * This method also calls the addToolPanelListeners() method if the ToolPanels ArrayList contains
	 * more than one ToolPanel object.  This method ensures all ToolPanel objects have mouseListeners
	 * registed with eachother, and ensures the currect current tool is displayed across all ToolPanels.
	 * See addToolPanelListeners() for more information.
	 */
	public void layoutWindowDisInfo() {
		
		iw.maximize();
		
		//cc.fitToWindow();
				
		
		//if ToolPanels has more than one ToolPanel reference within it, run the
				//addToolPanelListeners() method in this class:
				if(ToolPanels != null) {
					if(ToolPanels.size() > 1) {
						this.addToolPanelListeners();
					}
				}
		
	}
	
	
	
	/**
	 * This method repaints the Canvas and Window to update all components.
	 */
	public void repaintWindow() {
		
		imp.getWindow().pack();
		
		cc.repaint();
		//panel.repaint();
		iw.repaint();
	}
	
	
	
	public void layoutWindow3() {

		//get iw layout and ignore the panel width, to allow correct calculation of
			//panel layout information:
		ImageLayout il = (ImageLayout)iw.getLayout();
		il.ignoreNonImageWidths(false);
		
		//maximise window and use this window size for layout information:
		iw.maximize();
		
		Dimension windowSize = iw.getSize();
		//Dimension windowSize = iw.getSize();
		panelSize = panel.getSize();
		
		if(windowSize.getWidth() < panelSize.getWidth() ) {
			windowSize.setSize( panelSize.getWidth(), windowSize.getHeight() + 10.0 );
			iw.setSize(windowSize);
			iw.repaint();
		}
		
		iw.setResizable(false);
		
		
		//if ToolPanels has more than one ToolPanel reference within it, run the
				//addToolPanelListeners() method in this class:
				if(ToolPanels != null) {
					if(ToolPanels.size() > 1) {
						this.addToolPanelListeners();
					}
				}
		
	}
	
	
	public void layoutWindow2() {

		//get iw layout and ignore the panel width, to allow correct calculation of
			//panel layout information:
		ImageLayout il = (ImageLayout)iw.getLayout();
		il.ignoreNonImageWidths(true);
		
		//maximise window and use this window size for layout information:
		iw.maximize();
		
		Dimension windowSize = iw.getPreferredSize(); 
		//Dimension windowSize = iw.getSize();
		panelSize = panel.getPreferredSize();
		
		
		//modify the panelSize.width and panelSize.height for correct component layout:
		if(panelSize.width > windowSize.width) {

			//set panelSize.width to windowSize.width:
			int ratioToWindowWidth = panelSize.width / windowSize.width;
			panelSize.width = windowSize.width;
			//to calculate panelSize.height, multiply by ratioToWindowWidth, multiply by 2 to account
				//for between component borders, and add 20 pixels for panel border:
			panelSize.height = (panelSize.height * (ratioToWindowWidth +2 ) );
			panel.setPreferredSize(panelSize);
			//validate panel - i.e re-lay out its components!
			panel.validate();
			
			//IJ.showMessage("Ratio panel to win: "+ratioToWindowWidth);
			
			//panelSize = panel.getSize();
			//IJ.showMessage("Panel Width after val: "+panelSize.width);
		}
		
		//maximise window and use this window size for layout information:
		iw.maximize();
		iw.setPreferredSize(windowSize);

		/* LAYING OUT WINDOW */		
		
		//validate the window for layout:
		iw.validate();
		
		//panelSize = panel.getSize();
		//IJ.showMessage("Panel Width after win val: "+panelSize.width);
		
		//if ToolPanels has more than one ToolPanel reference within it, run the
		//addToolPanelListeners() method in this class:
		if(ToolPanels != null) {
			if(ToolPanels.size() > 1) {
				this.addToolPanelListeners();
			}
		}
	}
	
	
	/**
	 * This method makes the ImageWindowWithPanel instance a modal window - thus, functions can
	 * only be invoked on the ImageWindowWithPanel, and the ImageJ window is disabled.  The
	 * ImageJ window is enabled when the ImageWindowWithPanel is closed.
	 * @param setModal A boolean value to indicate whether the IWP is modal or not.
	 */
	public void setModal(boolean setModal) {
		ImageJ ij = IJ.getInstance();
		
		if(setModal) {
			ij.setEnabled(false);
			iw.addWindowListener( new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					ij.setEnabled(true);
					iw.removeWindowListener( this );
				}
			});
		}
		else {
			ij.setEnabled(true);
		}
	}
	
	/**
	 * Calling this method deactivates the channels scrollbar, and prevents the user
	 * from modifying the channel being displayed.
	 */
	public void deactivateChannels() {
		if(iw instanceof StackWindowPublicScrollBars && c > 1) {
			//if scrollbar is not null, then deactivate it:
			if(channelScrollbar!=null) {
				channelScrollbar.setEnabled(false);
			}
			
			//DO NOT NEED TO DO THIS ANYMORE:
				//Have overwritten the KeyListener interface in ImageJ class in the CustomCanvas
				//class -> and this listener is used on the Canvas & Window!
			//for(int a=0; a<canvasKeyListener.length; a++) {
				//imp.getCanvas().removeKeyListener(canvasKeyListener[a]);
			//}
		}
	}
	
	/**
	 * Calling this method reactivates the channels scrollbar, and prevents the user
	 * from modifying the channel being displayed.
	 */
	public void reactivateChannels() {
		if(iw instanceof StackWindowPublicScrollBars) {
			//if scrollbar is not null, then activate it:
			if(channelScrollbar!=null) {
				channelScrollbar.setEnabled(true);
			}
			
			//DO NOT NEED TO DO THIS ANYMORE:
				//Have overwritten the KeyListener interface in ImageJ class in the CustomCanvas
				//class -> and this listener is used on the Canvas & Window!
			//for(int a=0; a<canvasKeyListener.length; a++) {
				//imp.getCanvas().addKeyListener(canvasKeyListener[a]);
			//}
		}
	}
	
	
	
	
	/**
	 * This method will remove the channels scrollbar from the ImageWindow.
	 */
	public void removeChannelsScrollbar() {
		//IJ.showMessage("channelScrollbar: "+channelScrollbar);
		//IJ.showMessage("SliceScrollbar: "+sliceScrollbar);
		if(channelScrollbar!=null) { //only remove channelsScrollbar if it exists:
			iw.remove(channelScrollbar);
		}
	}
	
	
	
	/**
	 * This methof will add the channels scrollbar onto the ImageWindow.
	 */
	public void addChannels() {
		
		if(channelScrollbar!=null) {
			iw.add(channelScrollbar);
			layoutWindow();
		}
		
	}
	
	/**
	 * Calling this method deactivates the slices scrollbar, and prevents the user
	 * from modifying the channel/slice being displayed.
	 * <p>
	 * Only required if the image is not a Projected Stack.
	 */
	public void deactivateSlices() {
		if(iw instanceof StackWindowPublicScrollBars) {
			//typecast to StackWindowPublicScrollBars:
			StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
			//obtain object ref. to scrollbar for channels:
			sliceScrollbar = sw.getzSelector();
			//if scrollbar is not null, then deactivate it:
			if(sliceScrollbar!=null) {
				sliceScrollbar.setEnabled(false);
			}
		}
	}
	
	/**
	 * Calling this method reactivates the slices scrollbar, and prevents the user
	 * from modifying the channel being displayed.
	 */
	public void reactivateSlices() {
		if(iw instanceof StackWindowPublicScrollBars) {
			//typecast to StackWindowPublicScrollBars:
			StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
			//obtain object ref. to scrollbar for channels:
			sliceScrollbar = sw.getzSelector();
			//if scrollbar is not null, then activate it:
			if(sliceScrollbar!=null) {
				sliceScrollbar.setEnabled(true);
			}
		}
	}
	
	/**
	 * Calling this method deactivates the frames scrollbar, and prevents the user
	 * from modifying the channel being displayed.
	 */
	public void deactivateFrames() {
		if(iw instanceof StackWindowPublicScrollBars) {
			//typecast to StackWindowPublicScrollBars:
			StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
			//obtain object ref. to scrollbar for channels:
			frameScrollbar = sw.gettSelector();
			//if scrollbar is not null, then deactivate it:
			if(frameScrollbar!=null) {
				frameScrollbar.setEnabled(false);
			}
		}
	}
	
	/**
	 * Calling this method reactivates the frames scrollbar, and prevents the user
	 * from modifying the channel being displayed.
	 */
	public void reactivateFrames() {
		if(iw instanceof StackWindowPublicScrollBars) {
			//typecast to StackWindowPublicScrollBars:
			StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
			//obtain object ref. to scrollbar for channels:
			frameScrollbar = sw.gettSelector();
			//if scrollbar is not null, then activate it:
			if(frameScrollbar!=null) {
				frameScrollbar.setEnabled(true);
			}
		}
	}
	
	
	
	
	public void refreshWindow() {
		iw.maximize();
	}
	
	
	
	
	/**
	 * disposes of the image window.
	 * <p>
	 * Releases all of the native screen resources used by this Window, its subcomponents, and all of its 
	 * owned children. That is, the resources for these Components will be destroyed, any memory they consume 
	 * will be returned to the OS, and they will be marked as undisplayable.
	 * <p>
	 * The Window and its subcomponents can be made displayable again by rebuilding the native resources with a 
	 * subsequent call to pack or show. The states of the recreated Window and its subcomponents will be identical 
	 * to the states of these objects at the point where the Window was disposed (not accounting for additional 
	 * modifications between those actions).
	 */
	public void dispose() {
		iw.dispose();
		//if(slProj == true) {
			//imp = null; //remove reference to the projected imp.
		//}
		
		//panel = null; //remove reference to panel.
		
	}
	
	
	/**
	 * Re-displays the image window & all of its components.  Used to re-display an IWP after calling dispose().
	 * <p>
	 * The Window and its subcomponents can be made displayable again by rebuilding the native resources with a 
	 * subsequent call to pack or show. The states of the recreated Window and its subcomponents will be identical 
	 * to the states of these objects at the point where the Window was disposed (not accounting for additional 
	 * modifications between those actions).
	 */
	public void show() {
		// iw.show();
		iw.setVisible(true);
	}
	
	/**
	 * Removes the image window & all of its components.
	 */
	public void hide() {
		// iw.show();
		iw.setVisible(false);
	}
	
	
	
	//Below are methods which allow the addition of standard components to a panel which will
	//form part of the standard tools on a StereoMate ImageWindowWithPanel:
		//Standard Panel containing Refresh button (to refresh window layout)
		//Standard panel with ability to contain any of the ImageJ standard tools 
			//(ToolPanel class - as inner class)
		// Roi Store - for storing Roi objects.
		//addComponent() method to allow the addition of any Component to the Panel.
		//Channel & Slice Projectors. This displays an image based on the imp and the setting
			//of various settings on a Channel / Slice Panel.

	/**
	 * This method adds a refresh button to the panel. This will call layoutWindow() when clicked, to
	 * re-layout the image and panel (this will re-display the window maximised, and aligned to ensure
	 * maximum expanse for the Image, with the Panel displayed below - see layoutWindow() ).
	 */
	public void addRefreshButton() {
		refreshPanel = new JPanel();
		refreshButton = new JButton(createImageIcon("/Icons/RefreshIcon 224x224.png", "Icon for Refreshing ImageWindowWithPanel (maximised)"));  
		
		//resize icon image to match JButton preferred size:
		Icon icon = refreshButton.getIcon();
		ImageIcon imgicon = (ImageIcon)icon;
		Image img = imgicon.getImage();
		Image newImg = img.getScaledInstance(30, 30,  java.awt.Image.SCALE_SMOOTH);
		icon = new ImageIcon(newImg);
		refreshButton.setIcon(icon);
		
		//Add border to JButton:
		refreshButton.setBorder(new LineBorder(Color.BLACK,1,false));
		
		//add functionality of refreshButton here:
		refreshButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ImageWindowWithPanel.this.refreshWindow();
			}
		});
		refreshButton.setPreferredSize(new Dimension(30,30));
		refreshPanel.add(refreshButton);
		refreshPanel.setBorder(new LineBorder(Color.BLACK,1,false));
		panel.add(refreshPanel);
	}
	
	
	/** Returns an ImageIcon, or null if the path was invalid. */
	public ImageIcon createImageIcon(String path,
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
	 * Add any component to the panel on the image window.  Note this method does not provide
	 * any functionality to the added component - this must be done by the programmer with an
	 * appropriate listener.
	 * @param comp The component to be added - must be subclass of Component.
	 */
	public void addComponent(Component comp) {
		panel.add(comp);
	}
	
	
	
	
	/**
	 * This method processes the current imp and returns a new imp with one extra channel.  Assumes the image
	 * does not contain FRAMES.
	 * 
	 */
	public void addBlankChannel2() {
	
		imp = impOriginal; //set imp to impOriginal -> to ensure the imp is dealt with as a full stack!

		//  Information on Passed Imp:
			// w  h  s  f  c
		
		ImageStack imageStack = new ImageStack(w,h);
		
		
		//ArrayList of LUTs:
		
		ArrayList<LUT> luts = new ArrayList<LUT>();
		
		
		//First, create a blank Image Processor to add to the image stack:
			//Select correct ImageProcessor to match bit depth of image.
			//IP made to same width and height as the original imp:
		
		ImageProcessor ip;
	
		ip = getBlankImageProcessor(imp);
		
		
		//Collect all the imageProcessors into the ImageStack object:
		
		
		if(c>1) {
			
			if(s>1) {
				
				//IMAGE CONTAINS CHANNELS AND SLICES
				
				
				for(int a=0; a<c; a++) {
					
					imp.setC(a);
					
					luts.add( imp.getProcessor().getLut() );
					
					for(int b=0; b<s; b++) {
						
						//imp.setSlice(b);
						imp.setZ(b);
					
						imageStack.addSlice( imp.getProcessor() );
								
					}
								
				}
				
				
			}
			else {
				
				//IMAGE CONTAINS CHANNELS ONLY
				
				for(int a=0; a<c; a++) {
					
					imp.setSlice(a+1);
					//imp.setC(a);
					
					luts.add( imp.getProcessor().getLut() );
					
					//IJ.log("LUT no: "+a+" lut: "+luts.get(a) );
					
					ImageProcessor ip2 = imp.getProcessor();
					
					//IJ.log("IP Width: "+ip2.getWidth() );
					//IJ.log("IP Height: "+ip2.getHeight() );
					
					imageStack.addSlice( imp.getProcessor() );				
								
				}
				
				
				imageStack.addSlice( ip );	
				
				
				
				//First convert the image stack to an imageplus:
				ImagePlus imp2 = new ImagePlus(imp.getTitle(), imageStack);
				
				//Next, add one to the Channel variable:
				
				c = c + 1;
				
				//IJ.log("Imp2 stack size: "+imp2.getImageStackSize() );
				//IJ.log("C S F: "+c+" "+s+" "+f);
				
					//Generate a new channelcheckbox array:
				
					ChannelCheckBox[] channels2 = new ChannelCheckBox[c];
					
					//copy over all channels objects to channels2:
					for(int a=0; a< channels.length; a++) {
						channels2[a] = channels[a];
					}

					//Add the new blank channel, set to off to not be displayed:
					int aChannel = c;
					String boxNo = ""+aChannel;
					channels2[c-1] = new ChannelCheckBox(boxNo, false);
					
					//Set channels to channels2:
					channels = channels2;

				
				//Finally, set the imp to the new hyperstack generated by the HyperStackConverter:
				
				imp = HyperStackConverter.toHyperStack(imp2, c, s, f);
				
				//Set the correct LUTs to the new Hyperstack:
				
					//Set the LUTs for each channel the original imp to the newly generated Hyperstack:
						//LUTs gathered in above processing -> stored in luts
				for(int a=0; a<(c-1); a++) {
					imp.setSlice(a+1);
					//imp.getProcessor().setLut( luts.get(a) );
					imp.setLut( luts.get(a) );
				}
				
				//And set the new channels LUT to white as default:
				imp.setSlice(c);
				
				//imp.getProcessor().setLut( LUT.createLutFromColor( Color.WHITE ) );
				
				imp.setLut( LUT.createLutFromColor( Color.WHITE ) );
				
				
				
			}
		}
		else {
			if(s>1) {
				
				//IMAGE CONTAINS ONLY SLICES

				for(int a=0; a<s; a++) {
					
					imp.setSlice(a);
					//imp.setZ(a);
					
					imageStack.addSlice( imp.getProcessor() );				
								
				}
				
			}
			else {
				
				//IMAGE IS A 2D IMAGE
				
				imageStack.addSlice( imp.getProcessor() );
				
			}
		}
		
		
		
		//Set the imp to the canvas and window in this class:
				cc.updateImage(imp);
				
				imp.setWindow(iw);
		

	}
	
	
	
	
	
	public void addBlankChannel3() {
		//Add series of images equivalent to a new channel to the imageStack:
		
		//First, create a blank Image Processor to add to the image stack:
			//Select correct ImageProcessor to match bit depth of image.
			//IP made to same width and height as the original imp:
		ImageProcessor ip;
		
		ImageStack imageStack = new ImageStack();
		
		ip = getBlankImageProcessor(imp);
			
		
		if(c>1) {
			
			if(s>1) {
				
				//IMAGE CONTAINS CHANNELS AND SLICES
								
					for(int b=0; b<s; b++) {
					
						imageStack.addSlice( ip );
								
					}	
				
			}
			else {
				
				//IMAGE CONTAINS CHANNELS ONLY
					
					imageStack.addSlice( ip );				
				
			}
		}
		else {
			if(s>1) {
				
				//IMAGE CONTAINS ONLY SLICES

				for(int a=0; a<s; a++) {
					
					imageStack.addSlice( ip );				
								
				}
				
			}
			else {
				
				//IMAGE IS A 2D IMAGE
				
				imageStack.addSlice( ip );
				
			}
		}
		
		
		
		//Convert imageStack to a hyperstack, using appropriate method:
		
		//First convert the image stack to an imageplus:
		ImagePlus imp2 = new ImagePlus(imp.getTitle(), imageStack);
		
		//Next, add one to the Channel variable:
		
		c = c + 1;
		
		//IJ.log("Imp2 stack size: "+imp2.getImageStackSize() );
		//IJ.log("C S F: "+c+" "+s+" "+f);
		
			//Generate a new channelcheckbox array:
		
			ChannelCheckBox[] channels2 = new ChannelCheckBox[c];
			
			//copy over all channels objects to channels2:
			for(int a=0; a< channels.length; a++) {
				channels2[a] = channels[a];
			}

			//Add the new blank channel, set to off to not be displayed:
			int aChannel = c;
			String boxNo = ""+aChannel;
			channels2[c-1] = new ChannelCheckBox(boxNo, false);
			
			//Set channels to channels2:
			channels = channels2;

		
		//Finally, set the imp to the new hyperstack generated by the HyperStackConverter:
		
		imp = HyperStackConverter.toHyperStack(imp2, c, s, f);
		
		//Set the correct LUTs to the new Hyperstack:
		
		ArrayList<LUT> luts = null;
		
			//Set the LUTs for each channel the original imp to the newly generated Hyperstack:
				//LUTs gathered in above processing -> stored in luts
		for(int a=0; a<(c-1); a++) {
			imp.setC(a);
			imp.getProcessor().setLut( luts.get(a) );
		}
		
		//And set the new channels LUT to white as default:
		imp.setC(c);
		imp.getProcessor().setLut( LUT.createLutFromColor( Color.WHITE ) );
		
		
		//Set the imp to the canvas and window in this class:
		cc.updateImage(imp);
		
		imp.setWindow(iw);
		
		
	}
	
	
	
	
	
	
	/**
	 * Returns a blank Image Processor based on the passed Imp.
	 * @param imp This ImagePlus is used to read off the bitDepth, Width and Height for generating the new ImageProcessor.
	 * @return A new ImageProcessor of same bitDepth, width and height of the ImagePlus.
	 */
	public ImageProcessor getBlankImageProcessor(ImagePlus imp) {
		
		ImageProcessor ip;
		
		int bitDepth;
		
		bitDepth = imp.getBitDepth();
		
		ip = generateImageProcessor(bitDepth, imp.getWidth(), imp.getHeight() );
		
		if(ip == null) {
			bitDepth = imp.getProcessor().getBitDepth();
			ip = generateImageProcessor(bitDepth, imp.getWidth(), imp.getHeight() );
		}
		
		return ip;
	}
	
	
	
	/**
	 * Generates a new Blank image processor based on the bitDepth, width and height provided.  If bitDepth
	 * is 0, then the method returns null.
	 * @param bitDepth 8, 16, 24 [RGB], 32 bit depth images.
	 * @param width Width of returned ImageProcessor.
	 * @param height Height of returned ImageProcecessor.
	 * @return A new imageProcessor of appropriate bitDepth, returns null if bitDepth is 0.
	 */
	public ImageProcessor generateImageProcessor(int bitDepth, int width, int height) {
		
		ImageProcessor ip = null;
		
		if(bitDepth == 8) {
			ip = new ByteProcessor(w,h);
		}
		else if(bitDepth == 16) {
			ip = new ByteProcessor(w,h);
		}
		else if(bitDepth == 24) {
			ip = new ByteProcessor(w,h);
		}
		else if(bitDepth == 32) {
			ip = new ByteProcessor(w,h);
		}
		
		return ip;
		
	}
	
	
	
	
	/**
	 * This method processes the current imp and returns a new imp with one extra channel.
	 * <p>
	 * This has to do two tasks:
	 * <p>
	 * 1. Take the original imp -> with all channels and slices - and ADD AN EXTRA CHANNEL.
	 * <p>
	 * This can convert the image from a stack to hyperstack, or from a 2D image to hyperstack (i.e. from
	 * 2D image to a 2D image made of 2 channels). Therefore its important to make sure the projections
	 * are set up correctly for this potentially new type of image.
	 * <p>
	 * 2. Adjust the Channel and Slice projection tools to display the image.
	 * <p>
	 * As the image may now be a different type, the image channel and slice projection tools need to be
	 * re-set up.  The image also need to be displayed as the imp was before - with the correct channels
	 * on, and the correct number of slices projected.
	 * <p>
	 * A boolean value can be used to select between the new blank channel being accessible to the user
	 * via the channel projector tools, or not.
	 * 
	 * 
	 * 
	 * NOTE:  NEED TO FIGURE OUT WHAT CHANNELS ARE BEING DISPLAYED WHEN USING CHANNEL AND SLICE PROJECTOR
	 * TOOLS!
	 * 		IN THE ORIGINAL SETUPWINDOW() METHOD THE DISPLAY OF CHANNELS AND SLICES WORK FINE, BUT
	 * 			NOT CLEAR WHAT ARE THE ACTIVE CHANNEL AND SLICE
	 * 
	 * 		AFTER ADDING BLANK CHANNEL, DISPLAY OF CHANNELS AND SLICES DO NOT WORK, AND STILL UNCLEAR
	 * 		WHAT ARE ACTIVE CHANNEL AND SLICE...
	 * 
	 * THE PROBLEM MUST BE IN SELECTING THE ACTIVE CHANNEL VERSUS SELECTING WHAT THE COMPOSITE IMAGE IS....
	 * 
	 * 
	 */
	public void addBlankChannel(boolean channelAccessible) {
		
		
		// 1. ADD EXTRA CHANNEL TO IMP:
		
		imp = impOriginal; //set imp to impOriginal -> to ensure the imp is dealt with as a full stack!
		
		//gather information on passed imp:
		String title = imp.getTitle();
		int width = imp.getWidth();
		int height = imp.getHeight();
		int slices = imp.getNSlices();
		int frames = imp.getNFrames();		
		int channelNumber = imp.getNChannels();		
		
		//use ChannelSplitter method split() to derive channels from imp:
		ImagePlus[] imps = ChannelSplitter.split(imp);
		
		//create new ImagePlus object with appropriate title:
		int nextChannel = channelNumber +1;
		ImagePlus blankImp = IJ.createImage("C"+nextChannel+"-"+title, "8-bit composite-mode", width, height, 1, slices, frames);

		//create new ImagePlus[] array, and add all images from array, and new blankImp at the end:
		ImagePlus[] imps2 = new ImagePlus[nextChannel];
		for(int a=0; a<channelNumber; a++) {
			imps2[a] = imps[a];
		}		
		imps2[channelNumber] = blankImp; //blankImp is added to the end 
											//[channelNumber refers to last position in array]
		
		//IJ.log("Blank Channel: Imp before merge channels - c: "+imp.getNChannels()+" s: "+imp.getNSlices()+" f: "+imp.getNFrames() );
		//IJ.log("Blank Channel: Imp before merge channels - dim: "+imp.getNDimensions() );
		//IJ.log(" ");
		
		//generate the ImagePlus by using the static method mergeChannels() in RGBStackMerge:
		imp = RGBStackMerge.mergeChannels(imps2, false);
		
		//IJ.log("Blank Channel: Imp post merge channels - c: "+imp.getNChannels()+" s: "+imp.getNSlices()+" f: "+imp.getNFrames() );
		//IJ.log("Blank Channel: Imp post merge channels - dim: "+imp.getNDimensions() );
		//IJ.log(" ");
		
		
		//set impOriginal to the newly created imp:
		impOriginal = imp;
		
		//set imp to window and canvas:
		cc.updateImage(imp);
		imp.setWindow(iw);
		
		
		// 2. ADJUST CHANNEL & SLICE PROJECTION:
		
		if(channelAccessible == true) {
			
			//if true, want to make the new channel accessible in the Channel Projector:
			
			this.title = imp.getTitle();
			
			w = imp.getWidth();
			h = imp.getHeight();
			
			//identify number of channels and slices in imp:
			c = imp.getNChannels();
			s = imp.getNSlices();
			f = imp.getNFrames();

				
				if(c > 1) {
					
					chProj = true;
					
					resetupChannelProjector();
					
					CompositeImage ci = (CompositeImage)imp;
					ci.setMode(IJ.COMPOSITE);
					ci.updateAndDraw();
					
				}
				
				//if image has more than one slice, set up slice projector
				//tool, and make image project the entire stack.
				
				if(s > 1) {
					
					slProj = true;
				
					resetupSliceProjector();
					
					s = 1;//setupSliceProjector() eliminates slices, so set s to 1.
				
				}	
				
				resetUpWindow();
				
		}
		else if(channelAccessible == false) {
			
			//if true, want to make the new channel accessible in the Channel Projector:
			
			this.title = imp.getTitle();
			
			w = imp.getWidth();
			h = imp.getHeight();
			
			//identify number of channels and slices in imp:
			c = imp.getNChannels();
			
			c = c - 1; //remove one from channels variable, which will remove any possibility
				//of setting up a channelProjector including the newly added blank channel.
			
				//this is a temporary fix - as if more than one channel is added, the previous one will
					//be accessible to the user!
			
			s = imp.getNSlices();
			f = imp.getNFrames();

				
				if(c > 1) {
					
					chProj = true;
					
					resetupChannelProjector();
					
					CompositeImage ci = (CompositeImage)imp;
					ci.setMode(IJ.COMPOSITE);
					ci.updateAndDraw();
					
				}
				
				//if image has more than one slice, set up slice projector
				//tool, and make image project the entire stack.
				
				if(s > 1) {
					
					slProj = true;
				
					resetupSliceProjector();
					
					s = 1;//setupSliceProjector() eliminates slices, so set s to 1.
				
				}	
				
				resetUpWindow();
				
		}
		
	}//end addBlankChannel(boolean)
	
	
	
	
	
	/**
	 * Recalled after adding a new blank channel to the image.
	 * <p>
	 * This method sets up the channel projector - it creates a JPanel for the 
	 * channel checkboxes to exist on, and initialises the ChannelCheckBox array.
	 * ChannelCheckBox objects are created in ascending integers, and the behaviour
	 * of the checkboxes themselves is added - which consists of changing the state
	 * of the state String variable in ChannelCheckBox (by calling changeState() in the
	 * ChannelCheckBox class), and calling updateChannels().
	 * <p>
	 * All ChannelCheckBox objects in the array are added to the panel, which is added
	 * to the ImageWindowWithPanel's Panel.
	 * 
	 */
	public void resetupChannelProjector() {
		
		chProjPanel = new JPanel();
		chProjPanel.setLayout(new GridLayout(2,0));
		
		//create a new channelCheckBox array:
		ChannelCheckBox[] channels2 = new ChannelCheckBox[c];
		
		for(int a=0; a< c; a++) {
			int aChannel = a + 1;
			String boxNo = ""+aChannel;
			if(a < channels.length) {
				//create new ChannelCheckBox with same state as in the original channels obj:
				channels2[a] = new ChannelCheckBox(boxNo, channels[a].getState() );
			}
			else {
				//this is the new Blank Channel - set OFF by default:
				channels2[a] = new ChannelCheckBox(boxNo, false );
			}
			
		}
		
		//point channels2 to channels:
		channels = channels2;
		
		for(int a=0; a< c; a++) {
			final int aFinal = a;
			
			//add listener to implement behaviour.
			channels[a].cb.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					channels[aFinal].changeState();
					ImageWindowWithPanel.this.updateChannels();
				}
				
			});
			chProjPanel.add(channels[a].cb);
		}

		//Add Channel Projection to Panel:
		panel.add(chProjPanel);
		
	}
	
	
	
	/**
	 * Adds a Slice Projector panel to the iwp, and adds appropriate functionality, including
	 * ability to set the imp to various projections, and reverting the projected imp back to
	 * slice view.
	 */
	public void resetupSliceProjector() {
		
		//create JPanel to hold Slice Projector objects:
		slProjPanel = new JPanel();
		
		//create two "spinner" objects to handle slice projections:
		
		minModel = new SpinnerNumberModel( (int)minModel.getValue() ,1,s,1); //minModel has initial value of current minSlice.
		maxModel = new SpinnerNumberModel( (int)maxModel.getValue() ,1,s,1); //maxModel has initial value of current maxSlice.
		
		minSlice = new JSpinner(minModel); //create min Spinner object with minModel.
		maxSlice = new JSpinner(maxModel); //create max Spinner object with maxModel.
	
		//use zProjector.class in ij.plugin to implement slice projector behaviour
		//on the iwp's imp:
		sliceProjector = new ZProjector(imp);
		
		//Project current image slices with ZProjector:
		sliceProjector.setMethod(ZProjector.MAX_METHOD);
		sliceProjector.setStartSlice( (int)minModel.getValue() );
		sliceProjector.setStopSlice( (int)maxModel.getValue() );
		
		//updateSlices();
		
		//IJ.log("resetupSliceProj: Imp prior to projection:");
		//IJ.log("resetupSliceProj: IMP slices: "+imp.getNSlices() );
		//IJ.log(" ");
		
		if(imp.isHyperStack() ) {
			sliceProjector.doHyperStackProjection(true);
		}
		else if (imp.getType()==ImagePlus.COLOR_RGB){
			sliceProjector.doRGBProjection(true);
		}
		else { 
			sliceProjector.doProjection(true); 
		}
		
		//IJ.log("resetupSliceProj: Imp post-projection:");
		//IJ.log("resetupSliceProj: IMP slices: "+imp.getNSlices() );
		//IJ.log(" ");
	
		impOriginal = imp;
		
		//impProjected = sliceProjector.getProjection();
		
		//impProjected.setTitle(title);
		
		
		imp = sliceProjector.getProjection();
		
		imp.setTitle(title);
				
		//IJ.log("resetupSliceProj: Imp post-set projection:");
		//IJ.log("resetupSliceProj: IMP slices: "+imp.getNSlices() );
		
		//IJ.log("resetupSliceProj: ImpOriginal post-set projection:");
		//IJ.log("resetupSliceProj: IMPOriginal slices: "+impOriginal.getNSlices() );
		
		//IJ.log(" ");
		// remove these listeners!!
		minSlice.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// Derive min and max spinner values:
				int minVal = (int)minModel.getValue();
				int maxVal = (int)maxModel.getValue();
				//if min spinner value exceeds max spinner val, set
				//max spinner value to min spinner value:
				if(minVal > maxVal) {
					maxVal = minVal;
					maxSlice.setValue(maxVal);
				}
				//produce a projection of the current imp using the zProjector
				//with the current minVal and maxVal:
				sliceProjector.setStartSlice( (int)minModel.getValue() );
				sliceProjector.setStopSlice( (int)maxModel.getValue() );
				updateSlices(true);
			}
		});
		
		maxSlice.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// Derive min and max spinenr values:
				int minVal = (int)minModel.getValue();
				int maxVal = (int)maxModel.getValue();
				//if max spinner value falls below min spinner val, set
				//min spinner value to max spinner value:
				if(maxVal < minVal) {
					minVal = maxVal;
					minSlice.setValue(minVal);
				}
				//produce a projection of the current imp using the zProjector
				//with the current minVal and maxVal:
				sliceProjector.setStartSlice( (int)minModel.getValue() );
				sliceProjector.setStopSlice( (int)maxModel.getValue() );
				updateSlices(true);
			}
		});
		
		//add spinners to slProjPanel:
		slProjPanel.add(minSlice);
		slProjPanel.add(maxSlice);
		
		//add slProjPanel to iwp's panel:
		panel.add(slProjPanel);
				
	}
	
	
	
	/**
	 * This method will re-set up the window for the appropriate image type.  Called after adding 
	 * a blank channel to the image.
	 */
	public void resetUpWindow() {
		
		if(imp.isHyperStack() ) {
			
			// No hyperstacks can survive the current processing layout:
				//DialogWindow removes any images with frames within them.
				//This algorithm will project and remove any images with slices in them.
			
			//IJ.log("resetUpWindow: Hyperstack");
			//IJ.log(" ");
			
			iw = new StackWindowPublicScrollBars(imp, cc);
			
			StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
			//obtain object ref. to scrollbars for channels frame & slices
				//StackWindowPublicScrollBars is an extension of the StackWindow class,
				//which makes the scrollbar object references available to this class.
			
			if(c > 1) {
				channelScrollbar = sw.getcSelector();
			}
			
			if(f > 1) {
				frameScrollbar = sw.gettSelector();
			}
			
			if(s > 1) {
				sliceScrollbar = sw.getzSelector();
			}
			
		}
		else if(imp.getImageStackSize() > 1) {
			
			//if image is not a hyperstack, but stack size is greater than 1, then only
			//one dimension is greater than one - either channel, slice, or frame.
			//depending on which is greater than one will depend on the operation:
				//NOTE: on images with one dimension, the scrollbar ref is always in zSelector.
			
			//ALL IMAGES passed through DialogWindow will be this, or not a stack:
				//All images with slices in will be projected, removign slices.
				//All images containing Frames will not be passed by the DialogWindow.
				//All Channels will still be present - making the image stack the no. of channels
					
			
			if(c > 1) { //if stack is due to channels:
				
				//This is the only possible section to be called in this block with current layout of code:
					//Only channels survive the filtering by DialogWindow and this class..
				
				//IJ.log("resetUpWindow: Channels");
				//IJ.log(" ");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				channelScrollbar = sw.getzSelector();
				
			}
			else if( s > 1) { //if stack is due to slices:
				
				//This cannot be reached with current layout of code:
					//Slices will always be projected, yielding an image with one slice.
				
				//IJ.log("resetUpWindow: Slices");
				//IJ.log(" ");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				sliceScrollbar = sw.getzSelector();
			
			}
			else if(f > 1) { //if stack is due to frames:
				
				//This cannot be reached with current layout of code:
					//Images parsed by the DialogWindow will not parse an image with frames.
				
				//IJ.log("resetUpWindow: Frames");
				//IJ.log(" ");
				
				iw = new StackWindowPublicScrollBars(imp, cc);
				
				StackWindowPublicScrollBars sw = (StackWindowPublicScrollBars) iw;
				
				//obtain object ref. to scrollbar for channels:
				frameScrollbar = sw.getzSelector();
			
			}
		
		}
		else {
			
			//This is called if the image is a simple 2D image with only one channel, or if its a
				//zStack with one channel.
			
			//IJ.log("resetUpWindow: No Stack - CustomWindow");
			//IJ.log(" ");
			
			iw = new CustomWindow(imp, cc);
		
		}
				
		panel.setLayout( new FlowLayout() );
		iw.add(panel);
		
		//instance = this;
		
		//Get the canvasKeyListener -> to allow activation and deactivation of key listener for channel
			//scrollbar:
		
		////IJ.log("imp: "+imp);
		////IJ.log("Canvas: "+imp.getCanvas() );
		
		//canvasKeyListener = imp.getCanvas().getKeyListeners();
		
	}
	
	
	
	
	/**
	 * Will update the channels and slices after adding a new channel, and the image has been sketched on as
	 * required.
	 */
	public void updateChannelsAndSlices(boolean updateAndDraw) {
		
		//IJ.log("updateChannelsAndSlices: Imp after merge channels - c: "+imp.getNChannels()+" s: "+imp.getNSlices()+" f: "+imp.getNFrames() );
		//IJ.log("updateChannelsAndSlices: Imp after merge channels - dim: "+imp.getNDimensions() );
		//IJ.log(" ");
		
		if(sliceProjector == null && c > 1) { //update channels if more than one channel:
			//Only need to do this if there is no slice projection
				//As updateSlices() calls updateChannels()!
			CompositeImage ci = (CompositeImage)imp;
			ci.setMode(IJ.COMPOSITE);
			//ci.updateAndDraw();
			updateChannels();
		}
		//if sliceProjector is not null, re-generate sliceProjector with new imp, 
		// & call updateSlices() to re-do current projection:
		else if(sliceProjector != null) {
			//sliceProjector = new ZProjector(imp);
			//Project current image slices with ZProjector:
			//sliceProjector.setMethod(ZProjector.MAX_METHOD);
			sliceProjector.setStartSlice( (int)minModel.getValue() );
			sliceProjector.setStopSlice( (int)maxModel.getValue() );
			updateSlices(updateAndDraw);
		}
		else if(updateAndDraw){
			
			imp.updateAndRepaintWindow();
			
			// add the overlay from original imp to imp -> AFTER updateAndRepaintWindow!
				// this ensures the overlay is sketched correctly on images with no slices!
			imp.setOverlay( impOriginal.getOverlay() );
			//impOriginal.setOverlay( impOriginal.getOverlay() );
			
			// FINALLY -> give the custom canvas the focus:
				// Ensures the custom canvas has focus after adjusting the channel or slice projections
			cc.requestFocusInWindow();
		}
	}
	
	
	
	
	
	
	
	public void addBlankChannel() {
		
		imp = impOriginal; //set imp to impOriginal -> to ensure the imp is dealt with as a full stack!
		
		//HAVE COMMENTED THIS OUT FOR NOW -> Images with frames are
			//currently removed by the DialogWindow class:
		//derive the current Frame, if frames are implemented on this imp:
			//To allow the frame position to be updated in the new imp!
		//int frameNumber = 0;
		//if(f>1) {
			//if(c>1) { //i.e if image is a hyperstack, get Frame
				//frameNumber = imp.getFrame();
			//}
			//else { //else, frame is the only dimension, which is stored in slice, so get slice.
				//frameNumber = imp.getCurrentSlice();
			//}
		//}
		
		//gather information on passed imp:
		String title = imp.getTitle();
		int width = imp.getWidth();
		int height = imp.getHeight();
		int slices = imp.getNSlices();
		int frames = imp.getNFrames();		
		int channelNumber = imp.getNChannels();		
		
		//use ChannelSplitter method split() to derive channels from imp:
		ImagePlus[] imps = ChannelSplitter.split(imp);
		
		//create new ImagePlus object with appropriate title:
		int nextChannel = channelNumber +1;
		ImagePlus blankImp = IJ.createImage("C"+nextChannel+"-"+title, "8-bit composite-mode", width, height, 1, slices, frames);

		//create new ImagePlus[] array, and add all images from array, and new blankImp at the end:
		ImagePlus[] imps2 = new ImagePlus[nextChannel];
		for(int a=0; a<channelNumber; a++) {
			imps2[a] = imps[a];
		}		
		imps2[channelNumber] = blankImp; //blankImp is added to the end 
											//[channelNumber refers to last position in array]
		
		//IJ.log("Imp before merge channels - c: "+imp.getNChannels()+" s: "+imp.getNSlices()+" f: "+imp.getNFrames() );
		//IJ.log("Imp before merge channels - dim: "+imp.getNDimensions() );
		
		//generate the ImagePlus by using the static method mergeChannels() in RGBStackMerge:
		imp = RGBStackMerge.mergeChannels(imps2, false);
		
		
		//set imp to window and canvas:
		cc.updateImage(imp);
		imp.setWindow(iw);
		//imp.updateAndDraw();		
		//imp.getCanvas().fitToWindow();
		
		//set impOriginal to the newly created imp:
		impOriginal = imp;

		
		//HAVE COMMENTED THIS OUT FOR NOW -> Images with frames are
				//currently removed by the DialogWindow class:
		//set frame back to the current Frame, if frames are implemented on this imp:
		//if(frameNumber > 0) {
			//if(c>1) { //if imp is a hyperstack, set frame:
				//imp.setT(frameNumber);
			//}
			//else { //else, frame is the only dimension, which is stored in slice, so set slice:
				//imp.setSlice(frameNumber);
			//}
		//}
	}
	
	
	
	
	
	
	
	public static ImagePlus addBlankChannel(ImagePlus imp) {
		
		//imp = impOriginal; //set imp to impOriginal -> to ensure the imp is dealt with as a full stack!
		
		//HAVE COMMENTED THIS OUT FOR NOW -> Images with frames are
			//currently removed by the DialogWindow class:
		//derive the current Frame, if frames are implemented on this imp:
			//To allow the frame position to be updated in the new imp!
		//int frameNumber = 0;
		//if(f>1) {
			//if(c>1) { //i.e if image is a hyperstack, get Frame
				//frameNumber = imp.getFrame();
			//}
			//else { //else, frame is the only dimension, which is stored in slice, so get slice.
				//frameNumber = imp.getCurrentSlice();
			//}
		//}
		
		//gather information on passed imp:
		String title = imp.getTitle();
		int width = imp.getWidth();
		int height = imp.getHeight();
		int slices = imp.getNSlices();
		int frames = imp.getNFrames();		
		int channelNumber = imp.getNChannels();		
		
		//use ChannelSplitter method split() to derive channels from imp:
		ImagePlus[] imps = ChannelSplitter.split(imp);
		
		//create new ImagePlus object with appropriate title:
		int nextChannel = channelNumber +1;
		ImagePlus blankImp = IJ.createImage("C"+nextChannel+"-"+title, "8-bit composite-mode", width, height, 1, slices, frames);

		//create new ImagePlus[] array, and add all images from array, and new blankImp at the end:
		ImagePlus[] imps2 = new ImagePlus[nextChannel];
		for(int a=0; a<channelNumber; a++) {
			imps2[a] = imps[a];
		}		
		imps2[channelNumber] = blankImp; //blankImp is added to the end 
											//[channelNumber refers to last position in array]
		
		//IJ.log("Imp before merge channels - c: "+imp.getNChannels()+" s: "+imp.getNSlices()+" f: "+imp.getNFrames() );
		//IJ.log("Imp before merge channels - dim: "+imp.getNDimensions() );
		//IJ.log(" ");
		
		//generate the ImagePlus by using the static method mergeChannels() in RGBStackMerge:
		imp = RGBStackMerge.mergeChannels(imps2, false);
		
		//IJ.log("Imp before merge channels - c: "+imp.getNChannels()+" s: "+imp.getNSlices()+" f: "+imp.getNFrames() );
		//IJ.log("Imp before merge channels - dim: "+imp.getNDimensions() );
		//IJ.log(" ");
		
		
		return imp;
		
		//set imp to window and canvas:
		//cc.updateImage(imp);
		//imp.setWindow(iw);
		//imp.updateAndDraw();		
		//imp.getCanvas().fitToWindow();
		
		//set impOriginal to the newly created imp:
		//impOriginal = imp;

		
		//HAVE COMMENTED THIS OUT FOR NOW -> Images with frames are
				//currently removed by the DialogWindow class:
		//set frame back to the current Frame, if frames are implemented on this imp:
		//if(frameNumber > 0) {
			//if(c>1) { //if imp is a hyperstack, set frame:
				//imp.setT(frameNumber);
			//}
			//else { //else, frame is the only dimension, which is stored in slice, so set slice:
				//imp.setSlice(frameNumber);
			//}
		//}
	}
	
	
	
	
	
	
	
	/**
	 * This method will sketch the boundaries of the supplied Roi Store onto the selected channel, across
	 * all z slices.
	 */
	public static void sketchBoundariesStack(RoiStore roiStore, ImagePlus imp, int channel) {
		
		//set imp to display the last channel (the one just added above - this is where the outlines
		// will be drawn):
		//int channels = imp.getNChannels();
		imp.setC(channel);
			
		//Now, sketch the boundaries derived previously onto this channel:
	
		//set overlay to WHITE:
		//boundaryStore.setOverlayColor(Color.WHITE);
		roiStore.setDrawLabels(false);
		roiStore.displayAllOverlay(imp);


		//derive the image stack from impProcessed:
			//this contains references to ip's across the entire hyperstack (channels, slices, frames):
		ImageStack is = imp.getStack();
	
		//sketch ROI Boundaries from the boundaryStore object onto this channel (all slices and frames):
		int slices = imp.getNSlices();
		int numBoundaries = roiStore.ROIs.size();
	
		for(int a=1; a<slices+1; a++) {
			//impProcessed.setPosition(channels, a, b);
			int reference = (channel*a);
			ImageProcessor ip = is.getProcessor( reference );
			ip.setLineWidth(1);
			ip.setColor(1.0);
			for(int c=0; c<numBoundaries; c++) {
				ip.draw(roiStore.ROIs.get(c));
			}
		
		}
		
	}
	
	
	
	
	
	/**
	 * This method will sketch the boundaries of the supplied Roi Store onto the active channel, across
	 * all z slices.
	 */
	public void sketchBoundariesStack(RoiStore roiStore) {
		//set imp to display the last channel (the one just added above - this is where the outlines
		// will be drawn):
		int channels = impOriginal.getNChannels();
		impOriginal.setC(channels);
			
	//Now, sketch the boundaries derived previously onto this channel:
	
		//set overlay to WHITE:
		//boundaryStore.setOverlayColor(Color.WHITE);
		roiStore.setDrawLabels(false);
		roiStore.displayAllOverlay();


		//derive the image stack from impProcessed:
			//this contains references to ip's across the entire hyperstack (channels, slices, frames):
		ImageStack is = impOriginal.getStack();
	
		//sketch ROI Boundaries from the boundaryStore object onto this channel (all slices and frames):
		int slices = impOriginal.getNSlices();
		int numBoundaries = roiStore.ROIs.size();
	
	for(int a=1; a<slices+1; a++) {
			//impProcessed.setPosition(channels, a, b);
			int reference = (channels*a);
			ImageProcessor ip = is.getProcessor( reference );
			ip.setLineWidth(1);
			ip.setColor(1.0);
			for(int c=0; c<numBoundaries; c++) {
				ip.draw(roiStore.ROIs.get(c));
			}
		
	}
	}
	
	
	
	/**
	 * Second to the memory leak with the mouseListener between the toolbar and the ToolPanel in this class,
	 * the only other memory leak involves a link between the ImageCanvas (present in the imp) and the
	 * ImageWindow - specifically the subclass of this generated in this class - StackWindowPublicScrollBars.
	 * <p>
	 * The memory leak is a result of the multiple listener and manager objects set up between ic and iw. 
	 * Therefore, to all all of these objects to be GC'd, this method helps to remove all links between the
	 * ic and iw.
	 */
	public void shutdownImageWindowWithPanel() {
		
		removeKeyListeners();
		removeMouseListeners();
		
		ActionListener[] als = forward.getActionListeners();
		for(int b=0; b<als.length; b++) {
			forward.removeActionListener(als[b]);
		}
		
		als = backward.getActionListeners();
		for(int b=0; b<als.length; b++) {
			backward.removeActionListener(als[b]);
		}
		
		
		for(int a=0; a<channels.length; a++) {
			als = channels[a].cb.getActionListeners();
			for(int b=0; b<als.length; b++) {
				channels[a].cb.removeActionListener(als[b]);
			}
		}
		
		ChangeListener[] cls = minSlice.getChangeListeners();
		for(int a=0; a<cls.length; a++) {
			minSlice.removeChangeListener(cls[a]);
		}
		
		cls = maxSlice.getChangeListeners();
		for(int a=0; a<cls.length; a++) {
			maxSlice.removeChangeListener(cls[a]);
		}
		
		for(int a=0; a<keyListeners.size(); a++) {
			keyListeners.set(a, null);
		}
		
		
		keyListeners = null; // let go of the keyListener object
		
		imp = null;
		title = null;
		filePath = null;
		
		Component[] comp = panel.getComponents();
		for(int a=0; a<comp.length; a++) {
			if(comp[a] instanceof Container) {
				// iteratively loop through all components in any containers:
				removeComponents( ((Container)comp[a]).getComponents() );
			}
			comp[a] = null;
		}
		panel = null;
		
		//ESSENTIAL to shutdown StackWindowPublicScrollBars!
			//This seems to contain a memory leak where references from here hold onto
			//a set of int[] arrays linked to the PolygonRois, via an ImageCanvas object!
		if(iw instanceof StackWindowPublicScrollBars ) {
			((StackWindowPublicScrollBars) iw).shutdownStackWindowPublicScrollBars();
		}
		else if(iw instanceof CustomWindow) {
			((CustomWindow) iw).shutdownCustomWindow();
		}
		
		iw.close();
		
		iw = null;
		
		cc = null;
		ij = null;
		
		//This is important to remove the memory leak between the toolbar and toolPanel, via the
			//mouseListener set up between them.
		//This is the source of the MAJOR Memory Leak!!
		if(ToolPanels != null) {
		for(int a=0; a<ToolPanels.size(); a++) {
			ToolPanels.get(a).shutdownToolPanel();
			ToolPanels.set(a, null);
		}
		ToolPanels = null;
		}
				
		panelSize = null;
		
		refreshPanel = null;
		refreshButton = null;
		
		chProjPanel = null;
		channels = null;

		impOriginal = null; //reference to the original imp prior to projection.
		slProjPanel = null;
		sliceProjector = null;
		first = null; last = null; rate = null;
		minSlice = null;
		maxSlice = null; //the two Spinners.
		minModel = null;
		maxModel = null; //Two models for the Spinners.
		
		forward = null; backward = null;
		
		
		//if(canvasKeyListener != null) {
		//for(int a=0; a<canvasKeyListener.length; a++) {
			//canvasKeyListener[a] = null;
		//}
		//canvasKeyListener = null;
		//}
		
		channelScrollbar = null;
		sliceScrollbar = null;
		frameScrollbar = null;
		
		System.gc();
		System.gc();
		
	}
	
	public void removeComponents(Component[] comp) {
		for(int a=0; a<comp.length; a++) {
			if(comp[a] instanceof Container) {
				removeComponents( ((Container)comp[a]).getComponents() );
			}
			comp[a] = null;
		}
	}
	
	
	public void removeKeyListeners() {
		
		//Add the keyListener to the custom canvas:
		// cc.addKeyListener(kl);

		//Add the keyListener to the Custom Window too!
		// iw.addKeyListener(kl);

		//AND add the keyListener to the panel!
		// panel.addKeyListener(kl);

		// Also add this keylistener to the array of keylisteners -> ensures CustomCanvas adds this listener
		// to itself when regenerated when a new projection is made!
		// keyListeners.add(kl);
		
		KeyListener[] kls = cc.getKeyListeners();
		for(int a=0; a<kls.length; a++) {
			cc.removeKeyListener(kls[a]);
		}
		
		kls = iw.getKeyListeners();
		for(int a=0; a<kls.length; a++) {
			iw.removeKeyListener(kls[a]);
		}
		
		kls = panel.getKeyListeners();
		for(int a=0; a<kls.length; a++) {
			panel.removeKeyListener(kls[a]);
		}
		
	}
	
	
	public void removeMouseListeners() {
		
		MouseListener[] kls = cc.getMouseListeners();
		for(int a=0; a<kls.length; a++) {
			cc.removeMouseListener(kls[a]);
		}
		
	}
	

	
	
	public void showIwpActiveImp() {
		
		activeImpOn = true;
		
		//set the last channelcheckbox item to 1 - on:
		//IWP.channels[ (IWP.getChannels()-1) ].state = "1";
			//Do not change state by itself!  Use setState() which will modify
				//both the state AND the checkbox.
		
		//set last channelcheckbox item to true (ON):
		this.channels[ (this.getChannels()-1) ].setState(true);
		
		//Make the LUT of this channel White:
		IJ.run(this.getImagePlus(), "Grays", "");
		
		//update the IWP image - this will display all channels and slices:
		this.updateChannelsAndSlices(true);
		
	}
	
	public void hideIwpActiveImp() {
		
		activeImpOn = false;
		
		//set the last channelcheckbox item to 1 - on:
		//IWP.channels[ (IWP.getChannels()-1) ].state = "0";
		//Do not change state by itself!  Use setState() which will modify
			//both the state AND the checkbox.

		//set last channelcheckbox item to false (OFF):
		this.channels[ (this.getChannels()-1) ].setState(false);
		
		//update the IWP image - this will display all channels and slices:
		this.updateChannelsAndSlices(true);
		
	}
	
	
	/**
	 *  * This method will deactivate all Buttons on the JPanel or any JPanels this JPanel contains.
	 * <p>
	 * Will loop through the panel, any JPanels -> recall this method, any objects which are JButtons -> setEnabled(false).
	 * @param mainPanel JPanel to search components on.
	 */
	public void deactivateButtons() {
		
		Component[] cs = panel.getComponents();
		
		for(int a=0; a<cs.length; a++) {
			if(cs[a] instanceof JPanel) {
				deactivateButtons( (JPanel)cs[a] );
			}
			if(cs[a] instanceof JButton) {
				((JButton)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JComboBox<?>) {
				((JComboBox<?>)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JTextField) {
				((JTextField)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JCheckBox) {
				((JCheckBox)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JTextArea) {
				((JTextArea)(cs[a])).setEnabled(false);
			}
		}
		
	}
	
	
	
	
	/**
	 *  * This method will deactivate all Buttons on the JPanel or any JPanels this JPanel contains.
	 * <p>
	 * Will loop through the panel, any JPanels -> recall this method, any objects which are JButtons -> setEnabled(false).
	 * @param mainPanel JPanel to search components on.
	 */
	public void deactivateButtons(JPanel mainPanel) {
		
		Component[] cs = mainPanel.getComponents();
		
		for(int a=0; a<cs.length; a++) {
			if(cs[a] instanceof JPanel) {
				deactivateButtons( (JPanel)cs[a] );
			}
			if(cs[a] instanceof JButton) {
				((JButton)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JComboBox<?>) {
				((JComboBox<?>)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JTextField) {
				((JTextField)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JCheckBox) {
				((JCheckBox)(cs[a])).setEnabled(false);
			}
			if(cs[a] instanceof JTextArea) {
				((JTextArea)(cs[a])).setEnabled(false);
			}
		}
		
	}
	
	
	/**
	 * This method will reactivate all Buttons on the JPanel or any JPanels this JPanel contains.
	 * <p>
	 * Will loop through the panel, any JPanels -> recall this method, any objects which are JButtons -> setEnabled(true).
	 * @param mainPanel JPanel to search components on.
	 */
	public void reactivateButtons() {
		
		Component[] cs = panel.getComponents();
		
		for(int a=0; a<cs.length; a++) {
			if(cs[a] instanceof JPanel) {
				reactivateButtons( (JPanel)cs[a] );
			}
			if(cs[a] instanceof JButton) {
				((JButton)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JComboBox<?>) {
				((JComboBox<?>)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JTextField) {
				((JTextField)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JCheckBox) {
				((JCheckBox)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JTextArea) {
				((JTextArea)(cs[a])).setEnabled(true);
			}
		}
		
	}
	
	
	
	/**
	 * This method will reactivate all Buttons on the JPanel or any JPanels this JPanel contains.
	 * <p>
	 * Will loop through the panel, any JPanels -> recall this method, any objects which are JButtons -> setEnabled(true).
	 * @param mainPanel JPanel to search components on.
	 */
	public void reactivateButtons(JPanel mainPanel) {
		
		Component[] cs = mainPanel.getComponents();
		
		for(int a=0; a<cs.length; a++) {
			if(cs[a] instanceof JPanel) {
				reactivateButtons( (JPanel)cs[a] );
			}
			if(cs[a] instanceof JButton) {
				((JButton)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JComboBox<?>) {
				((JComboBox<?>)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JTextField) {
				((JTextField)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JCheckBox) {
				((JCheckBox)(cs[a])).setEnabled(true);
			}
			if(cs[a] instanceof JTextArea) {
				((JTextArea)(cs[a])).setEnabled(true);
			}
		}
		
	}
	
	
	/**
	 * Allows a new key listener to be added to the custom canvas on this IWP object.
	 * @param kl KeyListener to be added - must implement the KeyListener interface!
	 */
	public void addKeyListener(KeyListener kl) {

		//Add the keyListener to the custom canvas:
		cc.addKeyListener(kl);
		
		//Add the keyListener to the Custom Window too!
		iw.addKeyListener(kl);
		
		//AND add the keyListener to the panel!
		panel.addKeyListener(kl);
		
		// Also add this keylistener to the array of keylisteners -> ensures CustomCanvas adds this listener
		// to itself when regenerated when a new projection is made!
		keyListeners.add(kl);
		
	}

	
	/** Returns an ImageIcon, or null if the path was invalid.  RESCALES the image. */
	public ImageIcon createImageIcon(String path,
	                                           String description,int resizedWidth, int resizedHeight) {
		
	    java.net.URL imgURL = getClass().getResource(path);
	    
	    if (imgURL != null) {
	    	ImageIcon icon = new ImageIcon(imgURL, description);
	    	Image img = icon.getImage();
	        Image resizedImage = img.getScaledInstance(resizedWidth, resizedHeight,  java.awt.Image.SCALE_SMOOTH);  
	        return new ImageIcon(resizedImage, description);
	    } else {
	        System.err.println("Couldn't find file: " + path);
	        return null;
	    }
	}
	
	
	/**
	 * This CustomCanvas extends the ImageCanvas.  Here, it implements the KeyListener interface
	 * and implements keyEvents on the ImageCanvas.  These have been altered from the original 
	 * KeyListener interface found in the ImageJ class, so that zooming into and out of the image is
	 * supported, but no other keyboard shortcuts are allowed.
	 * <p>
	 * How to add extra listeners for key events???  Add a key listener to the CustomCanvas!
	 * <p>
	 * Note, the key listener on the ImageWindow is also removed to ensure this works.
	 * @author stevenwest
	 *
	 */
	public class CustomCanvas extends ImageCanvas implements KeyListener {
		
		protected double minLengthMag;
		protected double maxLengthMag;
		protected int panelHeight;
		
		/**
		 * Constructs the CustomCanvas - sets the panelHeight to default value: 120.
		 * @param imp
		 */
		CustomCanvas(ImagePlus imp) {
            super(imp);		//call the super-class constructor
            
            removeKeyListener(ij);	//remove the original keyListener -> ImageJ class instance.
            addKeyListener(this);	//add the new keyListener class -> this in this case!
            
           // IJ.showMessage("new Custom Canvas");
            
            // also add any keyListeners which may be inside the keyListeners obj:
            if(keyListeners != null) {
            	for(int a=0; a<keyListeners.size(); a++) {
            		//IJ.showMessage("adding listener from keyListeners "+keyListeners.size() );
            		addKeyListener( keyListeners.get(a) );
            	}
            }
            
            //set panel to 120 pixel size:
            // panelHeight = 56;
            panelHeight = 120;
            
        }
		
		
		/**
		 * Constructs the CustomCanvas - sets the panelHeight to the passed parameter.
		 * @param imp
		 * @param panelHeight
		 */
	    CustomCanvas(ImagePlus imp, int panelHeight) {
	            super(imp);		//call the super-class constructor
	            
	            removeKeyListener(ij);	//remove the original keyListener -> ImageJ class instance.
	            addKeyListener(this);	//add the new keyListener class -> this in this case!
	            
	           // IJ.showMessage("new Custom Canvas");
	            
	            // also add any keyListeners which may be inside the keyListeners obj:
	            if(keyListeners != null) {
	            	for(int a=0; a<keyListeners.size(); a++) {
	            		//IJ.showMessage("adding listener from keyListeners "+keyListeners.size() );
	            		addKeyListener( keyListeners.get(a) );
	            	}
	            }
	            
	            //set panel to 120 pixel size:
	            // panelHeight = 56;
	            //panelHeight = 120;
	            this.panelHeight = panelHeight;
	            
	     }
	       
	       
	       
	      /**
	       * Overwritten this method to ensure the magnification set when this is called during the 
	       * ImageWindowWithPanel layoutWindow() method, is set as the initialMagnification:
	       */
	   	public void fitToWindow() {
	   		
	   		////IJ.log("CC FIT TO WINDOW!");
	   		
	   		ImageWindow win = imp.getWindow();
			
	   		if (win==null) return;
			
			Rectangle bounds = win.getBounds();
			Insets insets = win.getInsets();
			
			int sliderHeight = win.getSliderHeight();
			
			double xmag = (double)(bounds.width-(insets.left+insets.right+ImageWindow.HGAP*2))/srcRect.width;
			double ymag = (double)(bounds.height-(ImageWindow.VGAP*2+insets.top+insets.bottom+sliderHeight+(panelHeight-15) ))/srcRect.height;
			
			setMagnification(Math.max(xmag, ymag));
			
			int width=(int)(imageWidth*magnification);
			int height=(int)(imageHeight*magnification);
			
			if (width==dstWidth&&height==dstHeight) return;
			
			srcRect=new Rectangle(0,0,imageWidth, imageHeight);
			
			setSize(width, height);
			
			getParent().doLayout();
	   		
	   		minLengthMag = Math.min(xmag, ymag);
	   		
	   		maxLengthMag = Math.max(xmag, ymag);
	   		
	   	}
	   	
	   	
	   	/**
	   	 * Overriding the updateImage(imp) method to ONLY CHANGE the imp when updating the image.  
	   	 * <P>
	   	 * ONLY USE THIS METHOD TO UPDATE THE CURRENT IMP WITH A NEW PROJECTION OR ALTERATION TO 
	   	 * THE CURRENT IMP.
	   	 * <p>
	   	 * This ensures the same zoom and location are maintained for the updated image.
	   	 * <p>
	   	 * This assumes that when an image is updated, it is of the same size.  This method is only used
	   	 * in this extension to ImageCanvas when a new Projected image is generated with a new image stack
	   	 * projection, therefore this assumption is met.
	   	 * <p>
	   	 * NOTE:  For any future use - if the image is updated with a DIFFERENT IMAGE, this method may need to
	   	 * be replaced with the original in ImageCanvas.
	   	 * @param imp The new ImagePlus.
	   	 */
		void updateImage(ImagePlus imp) {
			
			this.imp = imp;
			
			// DO NOT IMPLEMENT THE REST!
			
			//int width = imp.getWidth();
			//int height = imp.getHeight();
			
			//imageWidth = width;
			//imageHeight = height;
			
			//srcRect = new Rectangle(0, 0, imageWidth, imageHeight);
			
			//setSize(imageWidth, imageHeight);
			
			//magnification = 1.0;
			


		}
		
		
		/**
		 * This method will set the image zoom such that the box (xMin, yMin) to (xMax, yMax) with border
		 * (border) will be displayed.
		 * @param xMin xMin coordinate
		 * @param xMax xMax coordinate.
		 * @param yMin yMin coordinate.
		 * @param yMax yMax coordinate.
		 * @param border Number of pixels forming the border.
		 */
		public void setZoom(int xMin, int xMax, int yMin, int yMax, int border) {
			
			//work out width and height:
			int width = (xMax - xMin) + (border*2);
			int height = (yMax - yMin) + (border*2);
			
			//calculate the magnification:
			double xMag =  ((double)imageWidth)/((double)width);
			double yMag = ((double)imageHeight)/((double)height);
			
			//set the actual magnification to the LOWER number:
			double mag = 0;
			if(xMag < yMag) {
				mag = xMag;
			}
			else {
				mag = yMag;
			}
						
			// Calibrate the mag with the magnification required to fit the image width/height to the
				// image window:
			mag = mag * minLengthMag;
				// This ensures the magnification is calibrated to the image and displays the objects at the correct
					// magnification relative to this value
			
			//and set the zoomTarget in X and Y:
			zoomTargetOX = (int) ( (xMin-border) + (width/2) );
			zoomTargetOY = (int) ( (yMin-(border-1)) + (height/2) ); //use border-1 as the panel takes up some space
																	//at the bottom of the image!
			
			//finally, adjust the source rect:
			adjustSourceRect(mag, zoomTargetOX, zoomTargetOY);
			
			//and repaint the image:
			imp.getWindow().pack();
			repaint();
			
		}

	       
	   	/** Zooms in by making the window bigger. If it can't be made bigger, then makes 
			the source rectangle (srcRect) smaller and centers it on the position in the
			image where the cursor was when zooming has started.
			Note that sx and sy are screen coordinates. 
			<p>
			I have changed this so that the first zoom fits the image height or width (largest) to the canvas.
			Then the NEXT ZOOM fits the OTHER length to the canvas.  And finally, normal zooming occurs with
			further zoom in.
			*/
		public void zoomIn(int sx, int sy) {
			
			//IJ.log("imp: "+imp);
			//IJ.log("impOriginal: "+impOriginal);
			//IJ.log("imp window: "+imp.getWindow() );
			//IJ.log("impOriginal window: "+impOriginal.getWindow() );
			//IJ.log("iw: "+iw);
			
			if (magnification>=32) return;
		    boolean mouseMoved = sqr(sx-lastZoomSX) + sqr(sy-lastZoomSY) > MAX_MOUSEMOVE_ZOOM*MAX_MOUSEMOVE_ZOOM;
			lastZoomSX = sx;
			lastZoomSY = sy;
			if (mouseMoved || zoomTargetOX<0) {
			    boolean cursorInside = sx >= 0 && sy >= 0 && sx < dstWidth && sy < dstHeight;
			    zoomTargetOX = offScreenX(cursorInside ? sx : dstWidth/2); //where to zoom, offscreen (image) coordinates
			    zoomTargetOY = offScreenY(cursorInside ? sy : dstHeight/2);
			}
			
			//set the correct Magnification -> if minLength -> maxLength, 
				//if maxLength or higher -> getHigherZoomLevel:
			double newMag;
			if(magnification < maxLengthMag) {
				if((magnification*2)>= maxLengthMag) {
					newMag = maxLengthMag;
				}
				else {
					//newMag = (magnification*2);
					newMag = ((maxLengthMag+minLengthMag)/2);
				}
			}
			//if(magnification == minLengthMag) {
			//	newMag = maxLengthMag;
			//}
			else {
				newMag = getHigherZoomLevel(magnification);
			}
						
			int newWidth = (int)(imageWidth*newMag);
			int newHeight = (int)(imageHeight*newMag);
			Dimension newSize = canEnlarge(newWidth, newHeight);
			if (newSize!=null) {
				////IJ.log("Enlarging");
				setSize(newSize.width, newSize.height);
				if (newSize.width!=newWidth || newSize.height!=newHeight) {
					//setMagnification(newMag);
					adjustSourceRect(newMag, zoomTargetOX, zoomTargetOY);
					////IJ.log("AdjSrcRect");
				}
				else
					////IJ.log("NOT AdjSrcRect");
					setMagnification(newMag);
				imp.getWindow().pack();
			} else { // can't enlarge window {
				//setMagnification(newMag);
				adjustSourceRect(newMag, zoomTargetOX, zoomTargetOY);
				////IJ.log("NOT Enlarging - AdjSrcRect");
				}
			imp.getWindow().pack();
			repaint();
			if (srcRect.width<imageWidth || srcRect.height<imageHeight) {
				resetMaxBounds();
			}

			
		}
		
		
		/**Zooms out by making the source rectangle (srcRect)  
		larger and centering it on (x,y). If we can't make it larger,  
		then make the window smaller. Note that
		sx and sy are screen coordinates. 
		<p>
		I have altered this in similar fashion to zoom in - zoom out to the smaller width/height to fit canvas,
		then zoom out to the larger width/height to fit canvas.
		*/
		@Override
		public void zoomOut(int sx, int sy) {
			//if magnification is set to the minimum magnification, return:
			if (magnification<= minLengthMag )
				return;
			
		    boolean mouseMoved = sqr(sx-lastZoomSX) + sqr(sy-lastZoomSY) > MAX_MOUSEMOVE_ZOOM*MAX_MOUSEMOVE_ZOOM;
			lastZoomSX = sx;
			lastZoomSY = sy;
			if (mouseMoved || zoomTargetOX<0) {
			    boolean cursorInside = sx >= 0 && sy >= 0 && sx < dstWidth && sy < dstHeight;
			    zoomTargetOX = offScreenX(cursorInside ? sx : dstWidth/2); //where to zoom, offscreen (image) coordinates
			    zoomTargetOY = offScreenY(cursorInside ? sy : dstHeight/2);
			}
			
			
			double oldMag = magnification;

			double newMag;
			if(oldMag <= maxLengthMag) {
				if( (oldMag/2) > minLengthMag) {
					//newMag = (oldMag/2);
					newMag = ((maxLengthMag+minLengthMag)/2);
				}
				else {
					newMag = minLengthMag;
				}
			}
			//if(oldMag == maxLengthMag) {
			//	newMag = minLengthMag;
			//}
			else {
				
				newMag = getLowerZoomLevel(magnification);
				
				if(newMag <= maxLengthMag) {
					newMag = maxLengthMag;
				}
			}
			
			double srcRatio = (double)srcRect.width/srcRect.height;
			double imageRatio = (double)imageWidth/imageHeight;
			double initialMag = imp.getWindow().getInitialMagnification();
			if (Math.abs(srcRatio-imageRatio)>0.05) {
				double scale = oldMag/newMag;
				int newSrcWidth = (int)Math.round(srcRect.width*scale);
				int newSrcHeight = (int)Math.round(srcRect.height*scale);
				if (newSrcWidth>imageWidth) newSrcWidth=imageWidth; 
				if (newSrcHeight>imageHeight) newSrcHeight=imageHeight;
				int newSrcX = srcRect.x - (newSrcWidth - srcRect.width)/2;
				int newSrcY = srcRect.y - (newSrcHeight - srcRect.height)/2;
				if (newSrcX + newSrcWidth > imageWidth) newSrcX = imageWidth - newSrcWidth;
				if (newSrcY + newSrcHeight > imageHeight) newSrcY = imageHeight - newSrcHeight;
				if (newSrcX<0) newSrcX = 0;
				if (newSrcY<0) newSrcY = 0;
				srcRect = new Rectangle(newSrcX, newSrcY, newSrcWidth, newSrcHeight);
	            ////IJ.log(newMag+" "+srcRect+" "+dstWidth+" "+dstHeight);
				int newDstWidth = (int)(srcRect.width*newMag);
				int newDstHeight = (int)(srcRect.height*newMag);
				setMagnification(newMag);
				//setMaxBounds();
	            ////IJ.log(newDstWidth+" "+dstWidth+" "+newDstHeight+" "+dstHeight);

				
				//if (newDstWidth<dstWidth || newDstHeight<dstHeight) {
				if (newMag < maxLengthMag ) {
					//Only need to call this when the canvas size is smaller than window
						//i.e. when mag is the minLengthMag value!
						//This stops image flashing white when going to maxLengthMag when zoomOut() is called
					setSize(newDstWidth, newDstHeight);
					imp.getWindow().pack();
				} else
					repaint();
				return;
			}
			if (imageWidth*newMag>dstWidth) {
				int w = (int)Math.round(dstWidth/newMag);
				if (w*newMag<dstWidth) w++;
				int h = (int)Math.round(dstHeight/newMag);
				if (h*newMag<dstHeight) h++;
				Rectangle r = new Rectangle(zoomTargetOX-w/2, zoomTargetOY-h/2, w, h);
				if (r.x<0) r.x = 0;
				if (r.y<0) r.y = 0;
				if (r.x+w>imageWidth) r.x = imageWidth-w;
				if (r.y+h>imageHeight) r.y = imageHeight-h;
				srcRect = r;
				setMagnification(newMag);
			} else {
				srcRect = new Rectangle(0, 0, imageWidth, imageHeight);
				setSize((int)(imageWidth*newMag), (int)(imageHeight*newMag));
				setMagnification(newMag);
				imp.getWindow().pack();
			}
			//IJ.write(newMag + " " + srcRect.x+" "+srcRect.y+" "+srcRect.width+" "+srcRect.height+" "+dstWidth + " " + dstHeight);
			//IJ.write(srcRect.x + " " + srcRect.width + " " + dstWidth);
			//setMaxBounds();
			repaint();

			
		}
		
		
		/**
		 * This has been overwritten locally to make sure the image is always drawn started X Y at
		 * 0, 0 (have flipped around adjustment of the Rectangle r so r.x, r.y are always minimum 0!
		 */
		/** Centers the viewable area on offscreen (image) coordinates x, y */
		void adjustSourceRect(double newMag, int x, int y) {
			//IJ.showMessage("adjustSourceRect1: " +srcRect+" "+dstWidth+"  "+dstHeight);
			int w = (int)Math.round(dstWidth/newMag);
			if (w*newMag<dstWidth) w++;
			int h = (int)Math.round(dstHeight/newMag);
			if (h*newMag<dstHeight) h++;
			//IJ.showMessage("adjustSourceRect2: "+srcRect+" "+w+"  "+h);
			Rectangle r = new Rectangle(x-w/2, y-h/2, w, h);
			//if (r.x<0) r.x = 0;
			//if (r.y<0) r.y = 0;
			if (r.x+w>imageWidth) r.x = imageWidth-w;
			if (r.y+h>imageHeight) r.y = imageHeight-h;
			if (r.x<0) r.x = 0;
			if (r.y<0) r.y = 0;
			srcRect = r;
			setMagnification(newMag);
			//IJ.showMessage("adjustSourceRect3: "+srcRect+" "+dstWidth+"  "+dstHeight);
		}
		
		
		/**
		 * Overwriting the setMagnification method to allow the magnification to be set higher than 32.
		 * 
		 */
		public void setMagnification(double magnification) {
			setMagnification2(magnification);
		}
			
		/**
		 * Have adjusted this method such that the max magnification can be greater than 32 - so the setZoom()
		 * method has the freedom to set the zoom to levels greater than 32.
		 * @param magnification
		 */
		void setMagnification2(double magnification) {
			//if (magnification>32.0)
				//magnification = 32.0;
			if (magnification<zoomLevels[0])
				magnification = zoomLevels[0];
			this.magnification = magnification;
			imp.setTitle(imp.getTitle());
		}
		
		/**
		 * Added the zoomLevels final double[] array for the setMagnification2() method.
		 */
		final double[] zoomLevels = {
				1/72.0, 1/48.0, 1/32.0, 1/24.0, 1/16.0, 1/12.0, 
				1/8.0, 1/6.0, 1/4.0, 1/3.0, 1/2.0, 0.75, 1.0, 1.5,
				2.0, 3.0, 4.0, 6.0, 8.0, 12.0, 16.0, 24.0, 32.0 };
		
		/**
		 * 
		 * Overriding the scroll method to ensure the image displays correctly when scrolled with
		 * the mouse and spacebar.
		 * <p>
		 * Flipped the if statements to ensure newx and newy are always minimum of 0!
		 */
		@Override
		protected void scroll(int sx, int sy) {
			int ox = xSrcStart + (int)(sx/magnification);  //convert to offscreen coordinates
			int oy = ySrcStart + (int)(sy/magnification);
			////IJ.log("scroll: "+ox+" "+oy+" "+xMouseStart+" "+yMouseStart);
			int newx = xSrcStart + (xMouseStart-ox);
			int newy = ySrcStart + (yMouseStart-oy);
			//if (newx<0) newx = 0;
			//if (newy<0) newy = 0;
			//moved the if statements above to BELOW the two below:
			if ((newx+srcRect.width)>imageWidth) newx = imageWidth-srcRect.width;
			if ((newy+srcRect.height)>imageHeight) newy = imageHeight-srcRect.height;
			//These two if statements below ensure newx and newy are always a minimum of 0!
			if (newx<0) newx = 0;
			if (newy<0) newy = 0;
			srcRect.x = newx;
			srcRect.y = newy;
			////IJ.log(sx+"  "+sy+"  "+newx+"  "+newy+"  "+srcRect);
			imp.draw();
			Thread.yield();
		}
		
		
		
		void resetMaxBounds() {
			
		}
		
		
		
	    int sqr(int x) {
	        return x*x;
	    }
	    
	    
	    

	    @Override   	
	   	public void keyPressed(KeyEvent e) {
			
	    	int keyCode = e.getKeyCode();
			IJ.setKeyDown(keyCode);
			boolean hotkey = false;
			if (keyCode==KeyEvent.VK_CONTROL || keyCode==KeyEvent.VK_SHIFT)
				return;
			char keyChar = e.getKeyChar();
			int flags = e.getModifiers();
			if (IJ.debugMode) IJ.log("keyPressed: code=" + keyCode + " (" + KeyEvent.getKeyText(keyCode)
				+ "), char=\"" + keyChar + "\" (" + (int)keyChar + "), flags="
				+ KeyEvent.getKeyModifiersText(flags));
			boolean shift = (flags & KeyEvent.SHIFT_MASK) != 0;
			boolean control = (flags & KeyEvent.CTRL_MASK) != 0;
			boolean alt = (flags & KeyEvent.ALT_MASK) != 0;
			boolean meta = (flags & KeyEvent.META_MASK) != 0;
			String cmd = null;
			ImagePlus imp = WindowManager.getCurrentImage();
			boolean isStack = (imp!=null) && (imp.getStackSize()>1);
			
			if (imp!=null && !control && ((keyChar>=32 && keyChar<=255) || keyChar=='\b' || keyChar=='\n')) {
				Roi roi = imp.getRoi();
				if (roi instanceof TextRoi) {
					if ((flags & KeyEvent.META_MASK)!=0 && IJ.isMacOSX()) return;
					if (alt) {
						switch (keyChar) {
							case 'u': case 'm': keyChar = IJ.micronSymbol; break;
							case 'A': keyChar = IJ.angstromSymbol; break;
							default:
						}
					}
					((TextRoi)roi).addChar(keyChar);
					return;
				}
			}
	        		
			
			if (cmd==null) {
				switch (keyChar) {
					//case '<': case ',': if (isStack) cmd="Previous Slice [<]"; break;
					//case '>': case '.': case ';': if (isStack) cmd="Next Slice [>]"; break;
					case '+': case '=': cmd="In [+]"; break;
					case '-': cmd="Out [-]"; break;
					//case '/': cmd="Reslice [/]..."; break;
					default:
				}
			}

			if (cmd==null) {
				switch (keyCode) {
					
					//case KeyEvent.VK_BACK_SLASH: cmd=IJ.altKeyDown()?"Animation Options...":"Start Animation"; break;
					case KeyEvent.VK_EQUALS: cmd="In [+]"; break;
					case KeyEvent.VK_MINUS: cmd="Out [-]"; break;
					default: break;
				}
			}
			
			if (cmd!=null && !cmd.equals("")) {
				String commandName = cmd;
				if (cmd.equals("Fill")||cmd.equals("Draw"))
					hotkey = true;
				if (cmd.charAt(0)==MacroInstaller.commandPrefix)
					MacroInstaller.runMacroShortcut(cmd);
				else {
					//ImageJ.doCommand(cmd);
					new Executer(cmd, null);
					//new CustomExecuter(cmd, null);
					//keyPressedTime = System.currentTimeMillis();
					//lastKeyCommand = cmd;
				}
			}
			
			
			
		}


		@Override
		public void keyTyped(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}


		@Override
		public void keyReleased(KeyEvent e) {
			// TODO Auto-generated method stub
			IJ.setKeyUp(e.getKeyCode());
		}
		
		public void shutdownCustomCanvas() {
			imp = null; // set imp to null, to remove link between CustomCanvas and imp.
		}
		
	}
	
	
	
	
	/** This is a custom layout manager that supports resizing of zoomed
	images. It's based on FlowLayout, but with vertical and centered flow. */
	protected class CustomImageLayout implements LayoutManager {

	    int hgap = ImageWindow.HGAP;
	    int vgap = ImageWindow.VGAP;
		ImageCanvas ic;
		boolean ignoreNonImageWidths;

	    /** Creates a new ImageLayout with center alignment. */
	    public CustomImageLayout(ImageCanvas ic) {
	    	this.ic = ic;
	    }

	    /** Not used by this class. */
	    public void addLayoutComponent(String name, Component comp) {
	    }

	    /** Not used by this class. */
	    public void removeLayoutComponent(Component comp) {
	    }

	    /** Returns the preferred dimensions for this layout. */
	    public Dimension preferredLayoutSize(Container target) {
	    	Rectangle rect = ((ImageWindow)target).getMaximumBounds();
	    	Dimension dim2 = new Dimension(rect.width,rect.height);
	    	return dim2;
	    }
	    
	    public Dimension preferredLayoutSize2(Container target) {
			Dimension dim = new Dimension(0,0);
			int nmembers = target.getComponentCount();
			for (int i=0; i<nmembers; i++) {
			    Component m = target.getComponent(i);
				Dimension d = m.getPreferredSize();
				if (i==0 || !ignoreNonImageWidths)
	    			dim.width = Math.max(dim.width, d.width);
				if (i>0) dim.height += vgap;
				dim.height += d.height;
			}
			Insets insets = target.getInsets();
			dim.width += insets.left + insets.right + hgap*2;
			dim.height += insets.top + insets.bottom + vgap*2;
			return dim;
	    }

	    /** Returns the minimum dimensions for this layout. */
	    public Dimension minimumLayoutSize(Container target) {
			return preferredLayoutSize(target);
	    }

	    /** Determines whether to ignore the width of non-image components when calculating
	     *  the preferred width (default false, i.e. the maximum of the widths of all components is used).
	     *  When true, components that do not fit the window will be truncated at the right.
	     *  The width of the 0th component (the ImageCanvas) is always taken into account. */
		public void ignoreNonImageWidths(boolean ignoreNonImageWidths) {
			this.ignoreNonImageWidths = ignoreNonImageWidths;
		}

	    /** Centers the elements in the specified column, if there is any slack.*/
	    private void moveComponents(Container target, int x, int y, int width, int height, int nmembers) {
	    	int x2 = 0;
		    y += height / 2;
			for (int i=0; i<nmembers; i++) {
			    Component m = target.getComponent(i);
			    Dimension d = m.getSize();
			    ////IJ.log("Component: "+i+" height: " + d.height);
			    //if (i==0 || d.height>60)
			    	//x2 = x + (width - d.width)/2;
				m.setLocation(x2, y);
				y += vgap + d.height;
			}
	    }

	    /** Lays out the container and calls ImageCanvas.resizeCanvas()
			to adjust the image canvas size as needed. */
	    public void layoutContainer(Container target) {
			Insets insets = target.getInsets();
			int nmembers = target.getComponentCount();
			Dimension d;
			int extraHeight = 0;
			for (int i=1; i<nmembers; i++) {
				Component m = target.getComponent(i);
				d = m.getPreferredSize();
				extraHeight += d.height+vgap;
			}
			d = target.getSize();
			int preferredImageWidth = d.width - (insets.left + insets.right + hgap*2);
			int preferredImageHeight = d.height - (insets.top + insets.bottom + vgap*2 + extraHeight);
				//ic.resizeCanvas(preferredImageWidth, preferredImageHeight);
			ic.setSize(preferredImageWidth, preferredImageHeight);
				//ic.fitToWindow();
			int maxwidth = d.width - (insets.left + insets.right + hgap*2);
			int maxheight = d.height - (insets.top + insets.bottom + vgap*2);
			Dimension psize = preferredLayoutSize(target);
			int x = 0;
			//int x = insets.left + hgap + (d.width - psize.width)/2;
			int y = 0;
			int colw = 0;
			
			for (int i=0; i<nmembers; i++) {
				Component m = target.getComponent(i);
				d = m.getPreferredSize();
				if ((m instanceof ScrollbarWithLabel) || (m instanceof Scrollbar)) {
					int scrollbarWidth = target.getComponent(0).getPreferredSize().width;
					Dimension minSize = m.getMinimumSize();
					if (scrollbarWidth<minSize.width) scrollbarWidth = minSize.width;
					m.setSize(scrollbarWidth, d.height);
				} else
					m.setSize(d.width, d.height);
				if (y > 0) y += vgap;
				y += d.height;
				if (i==0 || !ignoreNonImageWidths)
					colw = Math.max(colw, d.width);
			}
			moveComponents(target, x, insets.top + vgap, colw, maxheight - y, nmembers);
	    }
	    
	}
	
	
	
	
	/**
	 * This CustomWindow extends ImageWindow.  It serves the purpose of allowing the removal
	 * of the ImageJ keyListener on the ImageWindow, and replacing it with the KeyListener
	 * implementation in the CustomCanvas class.
	 * @author stevenwest
	 *
	 */
	protected class CustomWindow extends ImageWindow {
		
		/**
		 * This constructor calls the ImageWindow constructor, but also sets a new layout manager (the 
		 * CustomImageLayout manager (declared in ImageWindowWithPanel class) to manage the display of the
		 * ImageCanvas, and it also removes the keyListener on this ImageWindow (ij), and instead sets
		 * CustomCanvas as the key listener.
		 * @param imp The imp to be displayed in this ImageWindow.
		 * @param cc The CustomCanvas which this ImageWindow will use for displaying the imp.
		 */
		public CustomWindow(ImagePlus imp, CustomCanvas cc) {
			
			super(imp, cc);
			//Calls to the ImageWindow constructor result in pack() and show() being called.
			//This is a problem as all layout information is not set before this object is constructed.
			//In future, want to modify the ImageWindow and StackWindow methods to NOT layout
				//and display the window during construction...
			
			//This is not a massive issue -> the image just seems to take more time to load...
				// You can also see it before all the layout information has been performed!

			setVisible(false);  //will hide it after it opens!
					// This does cause it to flash up, but it is then hidden while the program lays out its information
			
			
			//Remove the CustomCanvas from the imageWindow -> will re-add it with the new layout below:
			this.remove(cc);
			
			//Set new layout & re-add the CustomCanvas:
			setLayout( new CustomImageLayout(cc) );
			add(cc);
			
			//Remove the ij KeyListener on this component, replace with the CustomCanvas listener:
			removeKeyListener(ij);
			addKeyListener(cc);
			//also add keyListener cc to the panel:
			panel.addKeyListener(cc);
			
			 // also add any keyListeners which may be inside the keyListeners obj:
			if(keyListeners != null) {
				for(int a=0; a<keyListeners.size(); a++) {
					addKeyListener( keyListeners.get(a) );
				}
			}
			
			
		}
		
		
		
		/**
		 * Overriding the setImage method to ensure the ImageCanvas is infact set as a CustomCanvas
		 * object.
		 */
		@Override
		public void setImage(ImagePlus imp2) {
			//ImageCanvas ic = getCanvas();
			CustomCanvas ic = (CustomCanvas)getCanvas();
			if (ic==null || imp2==null)
				return;
			imp = imp2;
			imp.setWindow(this);
			ic.updateImage(imp);
			ic.setImageUpdated();
			ic.repaint();
			repaint();
		}
		
		
		/**
		 * This method overwrites the getMaxBounds method -> makes sure the window is maximised on screen
		 * irrelevant to the size of the image canvas.
		 */
		@Override
		public Rectangle getMaximumBounds() {
			Rectangle maxWindow = GUI.getMaxWindowBounds();
			return maxWindow;
		}
		
		
		
		/**
		 * Shuts down the StackWindowPublicScrollBars.  This ensures that any important
		 * references between this object and any other objects is set to null - to cast
		 * the objects away from GC Roots and allow appropriate Garbage Collection
		 * <p>
		 * Analysis of heapdumps in Java VisualVM has shown that processing large images
		 * with Roi_DiSector results in a memory leak with a GC Root at ImageJ.  Following the
		 * object references from here revealed the StackWindowPublicScrollBars was retaining
		 * a reference to ImageCanvas (ic), which was ultimately holding a large number of references
		 * to an int[] array.  Therefore, explicitly breaking this link when shutting down
		 * an instance of this class should prevent this memory leak.
		 * <p>
		 * Subsequent to this, a number of listeners were found to hold onto the ImageCanvas reference,
		 * and so removal of these has been added to this shutdown method.
		 * <p>
		 * More listeners were found with zSelector specifically, so have sought to remove these by
		 * removing adjustment & key listeners.
		 * <p>
		 * Still more links shown with ic via mouseMotionListener and mouseListener, so have removed
		 * these references. 
		 * <p>
		 * Finally, the layout manager (which links ImageWindow and ImageCanvas) is set to null via this
		 * imageWindow (this.setLayout(null) ), and have reset the doubleBuffer in ImageCanvas, which sets
		 * the offScreenImage to null, which again serves to link ImageCanvas and ImageWindow.
		 */
		public void shutdownCustomWindow() {
	
			//Remove layout manager which links ic to this:
			this.setLayout(null);
			
			//This sets the offScreenImage to null - which links ic to this:
			ic.resetDoubleBuffer();
			
			//Remove mouse listener from ImageCanvas, which may help to keep it alive...?
			ic.removeMouseListener(ic);
			ic.removeMouseMotionListener(ic);
			
			((CustomCanvas)ic).shutdownCustomCanvas();
			
			this.ic = null;
			
			//Remove WindowStateListeners:
			WindowStateListener[] wfl = this.getWindowStateListeners();
			for(int a=0; a<wfl.length; a++) {
				this.removeWindowStateListener(wfl[a]);
			}
			ij.removeWindowListener(this); // also remove this as a window listener from ij!
			
			//Remove WindowListeners:
			WindowListener[] wl = this.getWindowListeners();
			for(int a=0; a<wl.length; a++) {
				this.removeWindowListener(wl[a]);
			}
			
			//Remove MouseWheelListeners:
			MouseWheelListener[] mwl = this.getMouseWheelListeners();
			for(int a=0; a<mwl.length; a++) {
				this.removeMouseWheelListener(mwl[a]);
			}
			
			//Remove FocusListeners:
			FocusListener[] fl = this.getFocusListeners();
			for(int a=0; a<fl.length; a++) {
				this.removeFocusListener(fl[a]);
			}

			ij = null;
			
		} //end shutdownStackWindowPulicScrollBars()
		
		
	}
	
	
	
	
	/**
	 * This Utility Class allows access to (makes  public) the scrollbars inside the StackWindow class,
	 * so the programmer can use these references to remove scrollbars from the ImageWindow.
	 * @author stevenwest
	 *
	 */
	protected class StackWindowPublicScrollBars extends StackWindow {
	
		/**
		 * This constructor calls the StackWindow constructor, but also sets a new layout manager (the 
		 * CustomImageLayout manager (declared in ImageWindowWithPanel class) to manage the display of the
		 * ImageCanvas, and it also removes the keyListener on this ImageWindow (ij), and instead sets
		 * CustomCanvas as the key listener.
		 * @param imp The imp to be displayed in this ImageWindow.
		 * @param cc The CustomCanvas which this ImageWindow will use for displaying the imp.
		 */
		public StackWindowPublicScrollBars(ImagePlus imp, CustomCanvas cc) {
			
			super(imp, cc);
				//Calls to the StackWindow constructor result in pack() and show() being called.
					//This is a problem as all layout information is not set before this object is constructed.
					//In future, want to modify the ImageWindow and StackWindow methods to NOT layout
						//and display the window during construction...
			
				//This is not a massive issue -> the image just seems to take more time to load...
					// You can also see it before all the layout information has been performed!

			setVisible(false);  //will hide it after it opens!
					// This does cause it to flash up, but it is then hidden while the program lays out its information
			
					// Ultimately want to implement the constructor locally, so I can eliminate the opening in the
						// first place!
			
			
			//Remove the CustomCanvas from the imageWindow -> will re-add it with the new layout below:
			this.remove(cc);
			
			//Set new layout & re-add the CustomCanvas:
			setLayout( new CustomImageLayout(cc) );
			add(cc);
			
			//Remove the ij KeyListener on this component, replace with the CustomCanvas listener:
			removeKeyListener(ij);
			addKeyListener(cc);
			//also add keyListener cc to the panel:
			panel.addKeyListener(cc);
			
			 // also add any keyListeners which may be inside the keyListeners obj:
			if(keyListeners != null) {
				for(int a=0; a<keyListeners.size(); a++) {
					addKeyListener( keyListeners.get(a) );
				}
			}
			
		}
		
		
		
		/**
		 * Overriding the setImage method to ensure the ImageCanvas is infact set as a CustomCanvas
		 * object.
		 */
		@Override
		public void setImage(ImagePlus imp2) {
			//ImageCanvas ic = getCanvas();
			CustomCanvas ic = (CustomCanvas)getCanvas();
			if (ic==null || imp2==null)
				return;
			imp = imp2;
			imp.setWindow(this);
			ic.updateImage(imp);
			ic.setImageUpdated();
			ic.repaint();
			repaint();
		}
		
		
		/**
		 * This method overwrites the getMaxBounds method -> makes sure the window is maximised on screen
		 * irrelevant to the size of the image canvas.
		 */
		@Override
		public Rectangle getMaximumBounds() {
			Rectangle maxWindow = GUI.getMaxWindowBounds();
			return maxWindow;
		}
		
		
		
		
		/**
		 * Shuts down the StackWindowPublicScrollBars.  This ensures that any important
		 * references between this object and any other objects is set to null - to cast
		 * the objects away from GC Roots and allow appropriate Garbage Collection
		 * <p>
		 * Analysis of heapdumps in Java VisualVM has shown that processing large images
		 * with Roi_DiSector results in a memory leak with a GC Root at ImageJ.  Following the
		 * object references from here revealed the StackWindowPublicScrollBars was retaining
		 * a reference to ImageCanvas (ic), which was ultimately holding a large number of references
		 * to an int[] array.  Therefore, explicitly breaking this link when shutting down
		 * an instance of this class should prevent this memory leak.
		 * <p>
		 * Subsequent to this, a number of listeners were found to hold onto the ImageCanvas reference,
		 * and so removal of these has been added to this shutdown method.
		 * <p>
		 * More listeners were found with zSelector specifically, so have sought to remove these by
		 * removing adjustment & key listeners.
		 * <p>
		 * Still more links shown with ic via mouseMotionListener and mouseListener, so have removed
		 * these references. 
		 * <p>
		 * Finally, the layout manager (which links ImageWindow and ImageCanvas) is set to null via this
		 * imageWindow (this.setLayout(null) ), and have reset the doubleBuffer in ImageCanvas, which sets
		 * the offScreenImage to null, which again serves to link ImageCanvas and ImageWindow.
		 */
		public void shutdownStackWindowPublicScrollBars() {
	
			//Remove layout manager which links ic to this:
			this.setLayout(null);
			
			//This sets the offScreenImage to null - which links ic to this:
			ic.resetDoubleBuffer();
			
			//Remove mouse listener from ImageCanvas, which may help to keep it alive...?
			ic.removeMouseListener(ic);
			ic.removeMouseMotionListener(ic);
			
			((CustomCanvas)ic).shutdownCustomCanvas();
			
			this.ic = null;
			
			//Remove WindowStateListeners:
			WindowStateListener[] wfl = this.getWindowStateListeners();
			for(int a=0; a<wfl.length; a++) {
				this.removeWindowStateListener(wfl[a]);
			}
			
			//Remove KeyListeners:
			KeyListener[] kl = this.getKeyListeners();
			for(int a=0; a<kl.length; a++) {
				this.removeKeyListener(kl[a]);
			}
			
			//Remove WindowListeners:
			WindowListener[] wl = this.getWindowListeners();
			for(int a=0; a<wl.length; a++) {
				this.removeWindowListener(wl[a]);
			}
			ij.removeWindowListener(this); // also remove this as a window listener from ij!
			
			//Remove MouseWheelListeners:
			MouseWheelListener[] mwl = this.getMouseWheelListeners();
			for(int a=0; a<mwl.length; a++) {
				this.removeMouseWheelListener(mwl[a]);
			}
			
			//Remove FocusListeners:
			FocusListener[] fl = this.getFocusListeners();
			for(int a=0; a<fl.length; a++) {
				this.removeFocusListener(fl[a]);
			}
			
			//Remove adjustment and Key Listeners for all Selectors:
			if(zSelector != null) {
			zSelector.removeAdjustmentListener(this);
			zSelector.removeAdjustmentListener(zSelector);
			zSelector.removeKeyListener(IJ.getInstance()); 
			}
			if(sliceSelector != null) {
				sliceSelector.removeAdjustmentListener(this);
				sliceSelector.removeKeyListener(IJ.getInstance()); 
			}
			if(tSelector != null) {
				tSelector.removeAdjustmentListener(this);
				tSelector.removeAdjustmentListener(tSelector);
				tSelector.removeKeyListener(IJ.getInstance()); 
			} 
			
			//set Selectors to null:
			this.zSelector = null;
			this.sliceSelector = null;
			this.tSelector = null;
			
			ij = null;
						
		} //end shutdownStackWindowPulicScrollBars()
		
		
		
		
		/**
		 * returns the zSelector from this component.
		 * @return
		 */
		public ScrollbarWithLabel getzSelector() {
			if(super.zSelector != null) {
				return super.zSelector;
			}
			else {
				return null;
			}
		}
		
		
		/**
		 * returns the tSelector from this component.
		 * @return
		 */
		public ScrollbarWithLabel gettSelector() {
			if(super.tSelector != null) {
				return super.tSelector;
			}
			else {
				return null;
			}
		}
		
		
		/**
		 * returns the cSelector from this component.
		 * @return
		 */
		public ScrollbarWithLabel getcSelector() {
			if(super.cSelector != null) {
				return super.cSelector;
			}
			else {
				return null;
			}
		}
		
	} //end class StackWindowPublicScrollBars




	/**
	 * This inner class stores the reference to a channel JCheckBox
	 * and the state of this box, which is used to update the channels
	 * displayed on an ImagePlus object.
	 * @author stevenwest
	 *
	 */
	public class ChannelCheckBox {
		
		protected JCheckBox cb;
		protected String state;
		
		/**
		 * Constructor.  Create the JCheckBox object with title and selected
		 * state - and map the state String variable to this state.
		 * @param title String representing the title of the JCheckBox.
		 * @param selected Boolena indicating whether the JCheckBox should
		 * be initially selected, and also sets the state String variable.
		 */
		public ChannelCheckBox(String title, boolean selected) {
			cb = new JCheckBox(title, selected);
			if(selected == true) {
				state = "1";
			}
			else if(selected == false) {
				state = "0";
			}
		}
		
		
		/**
		 * Constructor.  Create the JCheckBox object with title and selected
		 * state - and map the state String variable to this state.
		 * @param title String representing the title of the JCheckBox.
		 * @param selected String indicating whether the JCheckBox should
		 * be initially selected, and also sets the state String variable.  Only accepts "1" or "0".  If
		 * the value is anything else, the state is set to "0" (off).
		 */
		public ChannelCheckBox(String title, String selected) {
			
			boolean selectedState = false;
			
			if(selected == "1") {
				state = selected;
				selectedState = true;
				
			}
			else if(selected == "0") {
				state = selected;
			}
			else {
				state = "0";
			}
			
			cb = new JCheckBox(title, selectedState);
			
		}
		
		
		/**
		 * This method sets both the state and checkbox to the designated boolean value.  Useful
		 * for modifying the ChannelCheckBox object to ensure the state and checkbox remain aligned
		 * (i.e. if state is "1" [true] the checkbox is ON).
		 * @param state
		 */
		public void setState(boolean state) {
			if(state == true) {
				this.state = "1";
			}
			else {
				this.state = "0";
			}
			cb.setSelected(state);
		}
		
		
		/**
		 * Alter the state variable from 1 to 0, or vice versa.
		 */
		public void changeState() {
			if(state == "1") {
				state = "0";
			}
			else if(state == "0") {
				state = "1";
			}
		}
		
		/**
		 * Returns a reference to the JCheckBox in this ChannelCheckbox object.
		 * @return
		 */
		public JCheckBox getCheckBox() {
			return cb;
		}
		
		public String getState() {
			return state;
		}
		
		public boolean getStateBool() {
			if(state.equalsIgnoreCase("1") ) {
				return true;
			}
			else {
				return false;
			}
		}
	
	}//end class ChannelCheckBox.
	
	
	
	/**
	 * This class holds the data on PolygonRois which need storing during the
	 * delineation of different boundaries.  This stores each PolygonRoi, and
	 * provides the functionality in the form of buttons on a panel for the 
	 * following:
	 * <p>
	 * Adding a PolygonRoi to the RoiStore.
	 * <p>
	 * Moving up through and displaying the currently added PolygonRois.
	 * <p>
	 * Moving down through and displaying the currently added PolygonRois.
	 * <p>
	 * Displaying ALL currently added PolygonRois.
	 * <p>
	 * A TextField indicating which number Roi is being added, or displayed.
	 * <p>
	 * Removing the currently selected Roi.
	 * <p>
	 * A button to refresh (i.e re-draw) the currently selected Roi.
	 * <p>
	 * If any of the add buttons are pressed when a non- PolygonRoi is selected, then
	 * no Roi is added to the RoiStore. Rois are only deleted if the Delete button is clicked
	 * (and are not deleted when a user accidently selects an added Roi and clicks to de-select it).
	 * <p>
	 * The buttons and TextField can all be returned on a single panel, to allow 
	 * an easy means of retrieving all of this functionality in one component.
	 * <p>
	 * This class was written for use with the ImageWindowWithPanel, which is required in the constructor. 
	 * It allows the storage of PolygonRois which represent boundaries between different Regions Of 
	 * Interest.  These PolygonRois are then used to draw white lines onto a blank image which has a 
	 * 50% (adjustable) opaque overlay of the original image, where the user can select the desired Regions Of 
	 * Interest with the Wand Tool.
	 * 
	 * @author stevenwest
	 *
	 */
	public class RoiStore extends JPanel  implements MouseListener {

		
		public ArrayList<Roi> ROIs; //ArrayList of Roi objects to store the Roi's as they are added.
		
		public int numberOfRois;	
		int currentRoi;
		
		boolean displayingAll;
		Overlay overlay; //overlay object to display all ROIs
		Color overlayColor;
		boolean drawLabels;

		// JButtons for this Panel:
		JButton addButton;
		JButton removeButton;
		JButton upButton;
		JButton downButton;
		JButton displayAll;
		JButton refreshButton;
			
		// JTextFields for this Panel:
		JTextField roiNumber;
		JTextField roiTotal;
		
		//JLabels for this Panel:
		JLabel roiStoreTitle;
		JLabel roiNumberLabel;
		JLabel roiTotalLabel;
		
		public RoiStore(boolean displayAllRois) {
			super(); //Call super-class constructor - configures the JPanel Object.
			
			ROIs = new ArrayList<Roi>(); //create ROI ArrayList object to store Rois.	
			
			numberOfRois = 0;	
			currentRoi = 0;
			
			displayingAll = displayAllRois;
			
			overlayColor = Color.WHITE;
			drawLabels = true;
			
			//create JButtons with constructor which accepts an Icon as input:
			addButton = new JButton( createImageIcon("/Icons/AddButton.png", "Icon for Adding ROIs to the ROI Store") );
			removeButton = new JButton( createImageIcon("/Icons/RemoveButton.png", "Icon for RoiExtenderTool") );
			upButton = new JButton( createImageIcon("/Icons/UpButton.png", "Icon for RoiExtenderTool") );
			downButton = new JButton( createImageIcon("/Icons/DownButton.png", "Icon for RoiExtenderTool") );
			//displayAll = new JButton( createImageIcon("/Icons/DisplayAll.png", "Icon for RoiExtenderTool") );
			//refreshButton = new JButton( createImageIcon("/Icons/refreshButton.png", "Icon for RoiExtenderTool") );
			
			//set preferred sizes to 28x28 (Icon size).
			 addButton.setPreferredSize( new Dimension(30,30));
			 removeButton.setPreferredSize( new Dimension(30,30));
			 upButton.setPreferredSize( new Dimension(30,30));
			 downButton.setPreferredSize( new Dimension(30,30));
			 //displayAll.setPreferredSize( new Dimension(30,30));
			 //refreshButton.setPreferredSize( new Dimension(30,30));
			
			 //Add a thin Black border to the buttons
			 addButton.setBorder(new LineBorder(Color.BLACK,1,false));
			 removeButton.setBorder(new LineBorder(Color.BLACK,1,false));
			 upButton.setBorder(new LineBorder(Color.BLACK,1,false));
			 downButton.setBorder(new LineBorder(Color.BLACK,1,false));
			 //displayAll.setBorder(new LineBorder(Color.BLACK,1,false));
			 //refreshButton.setBorder(new LineBorder(Color.BLACK,1,false));
			 
			 //Add tool tips to the buttons:
			 addButton.setToolTipText("Add Button - Add ROI to ROI Store");
			 removeButton.setToolTipText("Remove Button - Remove ROI from ROI Store");
			 upButton.setToolTipText("Up Button - Move up to the next ROI in ROI Store");
			 downButton.setToolTipText("Down Button - Move down to the next ROI in ROI Store");
			 //displayAll.setToolTipText("Display All - show all ROIs in ROI Store");
			 //refreshButton.setToolTipText("Refresh Button - Refresh Currently Selected ROI in ROI Store");
			 
			 //Create JTextFields - which are uneditable:
			 
			roiStoreTitle = new JLabel("<html>Roi<br>Store:</html>", SwingConstants.CENTER);
			roiStoreTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
			
			roiNumber = new JTextField(currentRoi + "",2);
			roiNumber.setEditable(false);
			roiNumberLabel = new JLabel("<html>ROI<br>Number:</html>", SwingConstants.CENTER);
			roiNumberLabel.setFont( new Font("SansSerif", Font.PLAIN,10));
			
			roiTotal = new JTextField(numberOfRois + "",2);
			roiTotal.setEditable(false);
			roiTotalLabel = new JLabel("<html>Total<br>ROIs:</html>", SwingConstants.CENTER);
			roiTotalLabel.setFont( new Font("SansSerif", Font.PLAIN,10));

			 
			 //Add this as a mouselistener to each component:
			
			addButton.addMouseListener(this);
			removeButton.addMouseListener(this);
			upButton.addMouseListener(this);
			downButton.addMouseListener(this);
			//displayAll.addMouseListener(this);
			//refreshButton.addMouseListener(this);
				
			roiStoreTitle.addMouseListener(this);
			roiNumber.addMouseListener(this);
			roiTotal.addMouseListener(this);
			 
			 //Add each Component to this Panel:
			 
			this.add(roiStoreTitle);
			this.add(addButton);
			this.add(removeButton);
			this.add(downButton);
			this.add(upButton);
			//this.add(displayAll);
			//this.add(refreshButton);
			this.add(roiNumberLabel);
			this.add(roiNumber);
			this.add(roiTotalLabel);
			this.add(roiTotal);
			
			//Add Border to this JPanel:
			this.setBorder(new LineBorder(Color.BLACK,1,false));
			 
			 //Constructor Complete - this Panel can now be added to an ImageWindowWithPanel object.
			 
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
		 * Add currently selected Polygon ROI to this RoiStore.
		 */
		public void addRoi() {
			Roi thisRoi = imp.getRoi();
			if(thisRoi instanceof Roi) {
				ROIs.add( thisRoi );
				numberOfRois = numberOfRois + 1;
				currentRoi = numberOfRois;
				roiNumber.setText( currentRoi + ""); 
				roiTotal.setText( numberOfRois + "");
				if(displayingAll==true) {
					displayAll();
				}
				repaint(); //repaint JPanel to display text.
			}
			else if(thisRoi == null) {
				IJ.error("No ROI Selected");
			}
			else {
				//IJ.error("Only Polygon ROIs can be added to the RoiStore");
					//RoiStore now supports all ROIs!
			}
		}
		
		/**
		 * Removes the currently selected ROI.  The current Roi number is returned
		 * from the roiNumber JTextField.
		 */
		public void removeRoi() {
			if(numberOfRois > 0) {
				ROIs.remove( currentRoi-1 );
				numberOfRois = numberOfRois - 1;
				if(currentRoi > 1) {
					currentRoi = currentRoi-1;
				}
				else {
					currentRoi = numberOfRois;
				}
				
				if(displayingAll==true) {
					displayAllAfterRemoval();
				}
				else {
					refreshRoi();
				}
				roiNumber.setText( currentRoi + "" );
				roiTotal.setText( numberOfRois + "" );
				repaint(); //repaint JPanel to update text.
			}
			else {
				IJ.error("No ROIs to remove!");
			}
		}
		
		/**
		 * Moves up the ROIs present in the ArrayList<Roi> ROIs. This method displays the
		 * next ROI in the ROIs list.
		 */
		public void moveUp() {
			imp.killRoi(); //remove any ROI currently displayed
			
			if(numberOfRois == 0) {
				IJ.error("No ROIs added yet!");
			}
			else if (currentRoi < numberOfRois) {
				//the ROI number is below the last number, so can just move up the ROIs:
				currentRoi = currentRoi + 1;
				roiNumber.setText(currentRoi + "" ); //set RoiNumber to the next ROI.
				imp.setRoi(ROIs.get(currentRoi-1), true); //sets the ROI and displays it.
			}
			else if(currentRoi == numberOfRois) {
				//The ROI number is the last ROI in ROIs - therefore move to the first ROI:
				currentRoi = 1;
				roiNumber.setText(currentRoi + ""); //resets roiNumber to 1.
				imp.setRoi(ROIs.get(0), true); //sets the first ROI and displays it.
			}
			repaint(); //repaint this JPanel to update text.
		}
		
		/**
		 * This method displays the previous ROI in the ROIs list.
		 */
		public void moveDown() {
			imp.killRoi(); //remove any ROI currently displayed
			
			if(numberOfRois == 0) {
				IJ.error("No ROIs added yet!");
			}
			else if (currentRoi > 1) {
				//the ROI number is below the last number, so can just move up the ROIs:
				currentRoi = currentRoi - 1;
				roiNumber.setText(currentRoi + "" ); //set RoiNumber to the next ROI.
				imp.setRoi(ROIs.get(currentRoi-1), true); //set the ROI and displays it.
			}
			else if(currentRoi == 1) {
				//The ROI number is the last ROI in ROIs - therefore move to the first ROI:
				currentRoi = numberOfRois;
				roiNumber.setText(currentRoi + ""); //resets roiNumber to 1.
				imp.setRoi(ROIs.get(currentRoi-1), true); //set the last ROI and displays it.
			}
			repaint(); //repaint this JPanel to update text.
			
			
		}
		
		/**
		 * Displays all ROIs in the ROIs list as an Overlay.
		 */
		public void displayAll() {
			imp.killRoi();//remove any ROI currently displayed.
			
			imp.setRoi( ROIs.get( ROIs.size()-1 ), true );
			
			overlay = newOverlay();
			for (int i=0; i<ROIs.size(); i++) {
				if( (i+1) != currentRoi) {
				overlay.add(ROIs.get(i));
				}
			}
			if(currentRoi > 0) {
				overlay.add(ROIs.get(currentRoi-1)); //add current roi last, so it is the one highlighted.
			}
			overlay.setStrokeColor(overlayColor);
			setOverlay(imp, overlay);
			displayingAll = true;
			
		}
		
		
		/**
		 * 
		 */
		public void displayAllOverlay() {

			imp.killRoi();//remove any ROI currently displayed.
			
			//imp.setRoi( ROIs.get( ROIs.size()-1 ), true );
			
			overlay = newOverlay();
			for (int i=0; i<ROIs.size(); i++) {
				if( (i+1) != currentRoi) {
				overlay.add(ROIs.get(i));
				}
			}
			if(currentRoi > 0) {
				overlay.add(ROIs.get(currentRoi-1)); //add current roi last, so it is the one highlighted.
			}
			overlay.setStrokeColor(overlayColor);
			setOverlay(imp, overlay);
			displayingAll = true;
		}
		
		
		
		
		/**
		 * 
		 */
		public void displayAllOverlay(ImagePlus imp) {

			imp.killRoi();//remove any ROI currently displayed.
			
			//imp.setRoi( ROIs.get( ROIs.size()-1 ), true );
			
			overlay = newOverlay();
			for (int i=0; i<ROIs.size(); i++) {
				if( (i+1) != currentRoi) {
				overlay.add(ROIs.get(i));
				}
			}
			if(currentRoi > 0) {
				overlay.add(ROIs.get(currentRoi-1)); //add current roi last, so it is the one highlighted.
			}
			
			overlay.setStrokeColor(overlayColor);
			
			setOverlay(imp, overlay);
			displayingAll = true;
			
		}
		
		
		
		
		/**
		 * This method is slightly different to displayAll() as it takes care of the unique case when
		 * all of the ROIs must be re-displayed after one has been removed in the middle.  This method
		 * ensures the numbering of ROIs on the image matches their order.
		 */
		public void displayAllAfterRemoval() {
			imp.killRoi();

			for(int a=0; a<ROIs.size(); a++) {
				imp.setRoi( ROIs.get(a), true );
			}			
			
			Roi[] rois = getRoisAsArray();
			overlay = newOverlay();
			for (int i=0; i<rois.length; i++) {
				overlay.add(rois[i]);
			}
			setOverlay(imp, overlay);
			displayingAll = true;
			currentRoi = numberOfRois;
			
		}
		
		protected Overlay newOverlay() {
			Overlay overlay = OverlayLabels.createOverlay();
			overlay.drawLabels(drawLabels);
			overlay.setLabelColor(Color.white);
			overlay.drawBackgrounds(true);
			overlay.drawNames(Prefs.useNamesAsLabels);
			//overlay.setFillColor(overlayColor);
			//overlay.setStrokeColor(overlayColor);
			return overlay;
		}
		
		public void refreshRoi() {
			//imp.setOverlay(null);
			//imp.setHideOverlay(true);
			//imp.draw();
			imp.getCanvas().setShowAllList(null);
			imp.killRoi();
			if(currentRoi>0) {
				imp.setRoi(ROIs.get(currentRoi-1), true);
			}
			displayingAll = false;
		}
		
		public Roi[] getRoisAsArray(){
			Roi[] roi = new Roi[ROIs.size()];
			for(int a=0; a<ROIs.size(); a++) {
				roi[a] = ROIs.get(a);
			}
			return roi;
		}
		
		public Overlay getOverlay() {
			return overlay;
		}
		
		public void setOverlayColor(Color color) {
			overlayColor = color;
		}
		
		public void setDrawLabels(Boolean value) {
			drawLabels = value;
		}
		
		protected void setOverlay(ImagePlus imp, Overlay overlay) {
			if (imp==null)
				return;
			//ImageCanvas ic = imp.getCanvas();
			//if (ic==null) {
				imp.setOverlay(overlay);
				return;
			//}
			//ic.setShowAllList(overlay);
			//imp.draw();
		}

		@Override
		public void mouseClicked(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			if(e.getSource() == addButton) {
				addRoi();
			}
			else if(e.getSource() == removeButton) {
				removeRoi();
			}
			else if(e.getSource() == upButton) {
				moveUp();
			}
			else if(e.getSource() == downButton) {
				moveDown();
			}
			else if(e.getSource() == displayAll) {
				displayAll();
			}
			else if(e.getSource() == refreshButton) {
				refreshRoi();
			}
		}
		public void mouseReleased(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		
		public void shutdownRoiStore() {
			
			
			addButton.removeMouseListener(this);
			removeButton.removeMouseListener(this);
			upButton.removeMouseListener(this);
			downButton.removeMouseListener(this);
			//displayAll.addMouseListener(this);
			//refreshButton.addMouseListener(this);
				
			roiStoreTitle.removeMouseListener(this);
			roiNumber.removeMouseListener(this);
			roiTotal.removeMouseListener(this);
			
			if(ROIs != null) {
				for(int a=0; a<ROIs.size(); a++) {
					ROIs.get(a).setImage(null);
					ROIs.set(a, null);
				}
			ROIs = null; //ArrayList of Roi objects to store the Roi's as they are added.
			}
			
			//clear overlay - set list to null:
			//overlay.clear();

			overlay = null; //overlay object to display all ROIs
			overlayColor = null;

			// JButtons for this Panel:
			 addButton = null;
			 removeButton = null;
			 upButton = null;
			 downButton = null;
			 displayAll = null;
			 refreshButton = null;
				
			// JTextFields for this Panel:
			 roiNumber = null;
			 roiTotal = null;
			
			//JLabels for this Panel:
			 roiStoreTitle = null;
			 roiNumberLabel = null;
			 roiTotalLabel = null;
			
		}
		
	} //end class RoiStore
	
	/**
	 * This method will add every ToolPanel object as a mouseListener onto
	 * every other ToolPanel object within this ImageWindowWithPanel object. This ensures the 
	 * ToolPanels are aware of the state each one sets the current Tool to, and
	 * therefore ensures the correct current tool is displayed across all
	 * ToolPanel objects.
	 * <p>
	 * This method is automatically called in the layoutWindow() method whenever
	 * more than one ToolPanel object exists on a ImageWindowWithPanel object.  This
	 * is determined from the ArrayList ToolPanels, which has ToolPanel object references
	 * added to it as they are created into any ImageWindowWithPanel object.
	 */
	protected void addToolPanelListeners() {
		int size = ToolPanels.size();
		for(int a=0; a<size; a++) {
			ToolPanel currentTool = ToolPanels.get(a);
			for(int b=0; b<size; b++) {
				if(b!=a) { 
					//if b != a then b refers to another ToolPanel object in ToolPanels
					//Therefore, add a mouse listener to it!
					currentTool.addMouseListener( ToolPanels.get(b) );
				}
			}
		}
	}
	
	
	
	/**
	 * 
	 * The ToolPanel class provides a panel to add IJ tools to the Image Window Panel.  The tools are
	 * provided with their original functionality.
	 * @author stevenwest
	 *
	 */
	public class ToolPanel extends Panel implements MouseListener {
		
		int toolNum; //keeps track of the number of tools inserted into toolPanel.
		ArrayList<Boolean> toolsDown; //stores state of all potential standard tools - 15 in total, so this array 
							 //should be of length 15. Where true, indicates which tool is pressed down.
							//NOTE:  toolsDown represents the state of the tools in the tools array, in the order presented
							//in the tools array NOT in the ascending order of the NAMED CONSTANTS.
		ArrayList<Tools> tools; //An array of Tools objects to store the tools added to the panel, and used to draw
						//the correct tools on the ToolPanel. This can simply be an array of 15 potential 
						//tools, since no more than 15 will ever be required..
		
		Toolbar tb;  //instance variable to capture the Toolbar on the ImageJ main window, used for listening
						//to events that occur on the Toolbar.
		
		String initialTool; //String of the initial tool selected when the ToolPanel is created.
		
		String currentTool; //String of the tool currently selected - for mouseListener implementation.
				
		//NAMED CONSTANTS representing each tool (used in inner class, Tools, and for reference):

				public static final int RECTANGLE = 0;
				public static final int OVAL = 1;
				public static final int POLYGON = 2;
				public static final int FREEROI = 3;
				public static final int LINE = 4;
				public static final int POLYLINE = 5;
				public static final int FREELINE = 6;
				public static final int POINT = 7, CROSSHAIR = 7;
				public static final int WAND = 8;
				public static final int TEXT = 9;
				//public static final int UNUSED = 10 - do not need to use this tool..
				public static final int MAGNIFIER = 11;
				public static final int HAND = 12;
				public static final int DROPPER = 13;
				public static final int ANGLE = 14;
				public static final int ELLIPSE = 15;
				public static final int BRUSH = 16;
				public static final int ROUNDRECT = 17;
				public static final int ARROW = 18;
				public static final int MULTIPOINT = 19;
								
				public static final int ICON_SIZE = 30;
		
		
		/**
		 * Initiates a ToolPanel object - a Panel with relevant data to add and draw tools from the Toolbar
		 * class.
		 */
		public ToolPanel() {
			//call super-class constructor to initiate the Panel:
			super();
			//initiate instance variables for this class (15 tools max, so set arrays to hold 15 variables):
			toolNum = 0;
			toolsDown = new ArrayList<Boolean>();
			tools = new ArrayList<Tools>();
			//When this Panel is created, a MouseListener should be installed onto the toolbar to deal with events
			//on the Toolbar - such as selecting a new tool.
			tb = Toolbar.getInstance();
			initialTool = tb.getToolName();
			tb.addMouseListener(this);
			this.addMouseListener(this);
			
			//add this ToolPanel object to the ToolPanels ArrayList:
			if(ToolPanels == null) {
				ToolPanels = new ArrayList<ToolPanel>();
			}
			ToolPanels.add(this);
		}
		
		
		/**
		 * Performs the vital step of shutting down the tool panel:  Shuts down all tools and sets them
		 * to null.  It removes the MouseListener set between toolbar and this tool panel, which otherwise
		 * retains a large number of int[] and byte[] objects in memory due to link between toolbar and
		 * this toolpanel.
		 */
		public void shutdownToolPanel() {
			
			for(int a=0; a<tools.size(); a++) {
				tools.get(a).shutdownTools();
				tools.set(a, null);
			}
			tools = null; //An array of Tools objects to store the tools added to the panel, and used to draw
			//the correct tools on the ToolPanel. This can simply be an array of 15 potential 
			//tools, since no more than 15 will ever be required..

			//REMOVE THE MOUSE LISTENERS:
			tb.removeMouseListener(this);
				//This is ESSENTIAL - as the Toolbar reference collected is of course going to hang around!!!!
				//By not removing this at the end of the processing, the link between this class and the still
				//active toolbar (via the mouseListener) will remain!
				//This should remove the memory leak...
			
			this.removeMouseListener(this);
				//SAME APPLIES HERE!  Need to remove the mouseListener Reference, as this might keep this class
					//alive to the JVM...
			
			tb = null;  //instance variable to capture the Toolbar on the ImageJ main window, used for listening
			//to events that occur on the Toolbar.

		}

		
		/** 
		 * add tool to the ToolPanel object.  Note, the same tool can be added more than once - take care not
		 * to do this!
		 * @param toolID
		 */
		public void addTool(int toolID) {
			Tools newTool = new Tools(toolID);
			tools.add(newTool);
			if(newTool.getToolString() == initialTool) {
				toolsDown.add(true);
			}
			else {
				toolsDown.add(false);
			}
			toolNum = toolNum + 1;
			resizePanel();
			repaint();
		}
		
		/**
		 * This paint() method takes care of drawing the added tools onto the Panel.
		 */
		public void paint(Graphics g) {
			for(int a=0; a<toolNum; a++) {
				tools.get(a).drawToolIcon(g,  !(toolsDown.get(a)), a*ICON_SIZE, 0);
			}
		}
		
		/**
		 * Resizes the Panel to fit all tools added to the panel. Note all tool icons are displayed on one column.
		 * To allow for multiple columns, use more than one instance of ToolPanel to display separate rows of tools.
		 */
		public void resizePanel() {
			this.setPreferredSize( new Dimension( (toolNum)*ICON_SIZE,ICON_SIZE) );
		}
		
		public void setTool(int tool) {
			//set tool on toolbar:
			Tools t = new Tools(tool);
			tb.setTool(t.getToolString());
			//set currentTool instance variable:
			currentTool = Toolbar.getToolName();
			//then set the toolsDown array to true for this tool,
				//as well as false for all other tools:
			for(int a=0; a<toolNum; a++) {
				if(tools.get(a).getToolString() == currentTool) {
					toolsDown.set(a, true);
				}
				else {
					toolsDown.set(a,  false);
				}
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) { }

		@Override
		public void mousePressed(MouseEvent e) {
			//mousePressed events must respond to both the Panel and Toolbar mousePressed events.
			if(e.getSource() == this) {
				//deal with mouse event which occur on this panel.
				int x = e.getX();
				int y = e.getY(); //retrieve coordinates of mouse event.
				
				int toolIndex = x / ICON_SIZE; // Calculate which tool was pressed.  
											   // Returns an int which is the index of the tool.			
				
				tb.setTool( tools.get(toolIndex).getToolString() ); //set the toolbar tool to the selected tool.
			
			} //end if(ToolPanel)
			
			currentTool = Toolbar.getToolName();
			
			//set the toolsDown array to false:
			for(int a=0; a<toolNum; a++) {
				toolsDown.set(a, false);
			}
			//and if the currentTool is in tools (i.e the id matches the id of one of the tools)
			//set that tool to down (i.e toolsDown to true) at the position in the toolsDown array,
			//otherwise do not modulate the toolsDown array:
			for(int a=0; a<toolNum; a++) {
				if(tools.get(a).getToolString() == currentTool) {
					toolsDown.set(a, true);
				}
			}
			
			//finally, call repaint on this panel and Toolbar instance to update the tool icons:
			this.repaint();
			tb.repaint();
	
		}// end mousePressed()

		@Override
		public void mouseReleased(MouseEvent e) {
	
		}

		@Override
		public void mouseEntered(MouseEvent e) {
	
		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

	
	/**
	 * This inner class represents the tools found in the Toolbar class in ImageJ.  Each tool has
	 * the same NAMED_CONSTANT value as is seen in the Toolbar class.  A Tools object can be initiated
	 * with a NAMED_CONSTANT, and this object will contain a reference to this ID, as well as a String
	 * representation of the Tool.  getTool(), setTool() and getToolString() are all defined.
	 * @author stevenwest
	 *
	 */
	protected class Tools {		
		final int SIZE = 30; //named constant for size of icon.
		int x, y; //coordinates for drawing icons.
		int xOffset, yOffset; //offset for drawing icon.
		Graphics g; //store graphics context for drawing in other methods.
		
		private Color gray = new Color(228,228,228);
		private Color brighter = gray.brighter();
		private Color darker = new Color(180, 180, 180);
		private Color evenDarker = new Color(110, 110, 110);
		private Color triangleColor = new Color(150, 0, 0);
		private Color toolColor = new Color(0, 25, 45);
		
		private Color foregroundColor = Toolbar.getForegroundColor();
		private Color backgroundColor = Toolbar.getBackgroundColor();
		
		int toolID; //this instance variable stores the tool ID.
		String toolName; //this instance variable stores the tool as a String.
		
		
		/**
		 * This constructor generates a Tools object, representing one of the tools from the
		 * toolbar.
		 * @param toolID An int representing one of the Standard Tools on the Toolbar i.e Toolbar.RECTANGLE, etc.
		 * These NAMED CONSTANTS are also present in the Tools class, and can be accessed with Tools.RECTANGLE, etc.
		 * @throws IllegalArgumentException if the toolID is out of the range of legal NAMED CONSTANTS for the
		 * standard tools [0-14 inclusive - recommended to use the NAMED CONSTANTS in this class as argument).
		 */
		
		public Tools(int toolID) {
			
			if(toolID < 0 || toolID > 19) {
				throw new IllegalArgumentException("Invalid toolID");
			}
			
			this.toolID = toolID;
			
			switch(toolID) {
			case 0:
				toolName = "rectangle";
				break;
			case 1:
				toolName = "oval";
				break;
			case 2:
				toolName = "polygon";
				break;
			case 3:
				toolName = "freehand";
				break;
			case 4:
				toolName = "line";
				break;
			case 5:
				toolName = "polyline";
				break;
			case 6:
				toolName = "freeline";
				break;
			case 7:
				toolName = "point";
				break;
			case 8:
				toolName = "wand";
				break;
			case 9:
				toolName = "text";
				break;
			case 11:
				toolName = "zoom";
				break;
			case 12:
				toolName = "hand";
				break;
			case 13:
				toolName = "dropper";
				break;
			case 14:
				toolName = "angle";
				break;
			case 15:
				toolName = "ellipse";
				break;
			case 16:
				toolName = "brush";
				break;
			case 17:
				toolName = "roundrect";
				break;
			case 18:
				toolName = "arrow";
				break;
			case 19:
				toolName = "multipoint";
				break;
			default:
				toolName = null;
				break;
			}

		}
		
		
		public void shutdownTools() {
			
			g = null;
			
			gray = null;
			brighter = null;
			darker = null;
			evenDarker = null;
			triangleColor = null;
			toolColor = null;
			
			foregroundColor = null;
			backgroundColor = null;
			
			toolName = null;
			
		}
		
		/**
		 * This method provides the functionality to draw the tool icons onto a Panel or other drawing surface.
		 * It requires a Graphics Context (passed as a parameter) for drawing tool icons.
		 * @param g Graphics context for drawing the tool icon.
		 * @param isDown Boolean to indicate whether this tool is selected or not.
		 * @param x X position where drawing should initiate from.
		 * @param y Y position where drawing should initiate from.
		 */
		public void drawToolIcon(Graphics g, boolean isDown, int x, int y) {

			//draw background panel, and store g for other methods:
			fill3DRect(g, x, y, SIZE, SIZE-1, isDown);
   			this.g = g;
   	        g.setColor(toolColor);
   			
   			//determine whether rectangle on toolbar is rect or roundRect mode:
   			boolean roundRectMode;
   			int roundRectSize = Toolbar.getRoundRectArcSize();
   			if(roundRectSize == 0) {
   				roundRectMode = false;
   			}
   			else {
   				roundRectMode = true;
   			}
   			
   			//Determine whether Oval on toolbar is BRUSH_ROI or ELLIPSE_ROI:
   			int ovalType = Toolbar.getOvalToolType();
   			
   			//Cannot determine whether Toolbar.LINE is the Arrow tool, but can set this as required.
   				//Therefore have created new constructor to ascertain what setting this should be.
   			
   			//Determine whether POINT is multiPointMode:
   			boolean multiPointMode = Toolbar.getMultiPointMode();
	
   			//this switch statement is based on a similar statement in the drawButton(g,tool) in the Toolbar
   			//class, used to draw all standard tool icons onto the toolbar.
   			
   			//First set x & y to more appropriate figures:
   			x=x+5;
   			y=y+6;
   			switch (toolID) {
				case RECTANGLE:
					xOffset = x; yOffset = y;
					g.drawRect(x, y+1, 17, 13);
					//drawTriangle(16,15);
					return;
				case ROUNDRECT:
					xOffset = x; yOffset = y;
					g.drawRoundRect(x, y+1, 17, 13, 8, 8);
					return;
				case OVAL:
					xOffset = x; yOffset = y;
					g.drawOval(x, y+1, 17, 13);
					//drawTriangle(16,15);
					return;
				case ELLIPSE:
					xOffset = x;
					yOffset = y + 1;
					polyline(11,0,13,0,14,1,15,1,16,2,17,3,17,7,12,12,11,12,10,13,8,13,7,14,4,14,3,13,2,13,1,12,1,11,0,10,0,9,1,8,1,7,6,2,7,2,8,1,10,1,11,0);
					return;
				case BRUSH:
					xOffset = x;
					yOffset = y - 1;
					polyline(6,4,8,2,12,1,15,2,16,4,15,7,12,8,9,11,9,14,6,16,2,16,0,13,1,10,4,9,6,7,6,4);
					return;
				case POLYGON:
					xOffset = x+1; yOffset = y+2;
					polyline(4,0,15,0,15,1,11,5,11,6,14,10,14,11,0,11,0,4,4,0);
					return;
				case FREEROI:
					xOffset = x; yOffset = y+2;
					polyline(2,0,5,0,7,3,10,3,12,0,15,0,17,2,17,5,16,8,13,10,11,11,6,11,4,10,1,8,0,6,0,2,2,0); 
					return;
				case LINE:
					xOffset = x; yOffset = y;
					m(0,12); d(17,3);
					drawDot(0,11); drawDot(17,2);
					//drawTriangle(12,14);
					return;
				case ARROW:
					xOffset = x; yOffset = y;
					m(1,14); d(14,1); m(6,5); d(14,1); m(10,9); d(14,1); m(6,5); d(10,9);
					return;
				case POLYLINE:
					xOffset = x; yOffset = y;
					polyline(15,6,11,2,1,2,1,3,7,9,2,14);
					//drawTriangle(12,14);
					return;
				case FREELINE:
					xOffset = x; yOffset = y;
					polyline(16,4,14,6,12,6,9,3,8,3,6,7,2,11,1,11);
					//drawTriangle(12,14);
					return;
				case POINT:
					xOffset = x; yOffset = y;
					m(1,8); d(6,8); d(6,6); d(10,6); d(10,10); d(6,10); d(6,9);
					m(8,1); d(8,5); m(11,8); d(15,8); m(8,11); d(8,15);
					m(8,8); d(8,8);
					g.setColor(Roi.getColor());
					g.fillRect(x+7, y+7, 3, 3);
					//drawTriangle(14,14);
					return;
				case MULTIPOINT:
					xOffset = x; yOffset = y;
						drawPoint(1,3); drawPoint(9,1); drawPoint(15,5);
						drawPoint(10,11); drawPoint(2,12);
					return;
				case WAND:
					xOffset = x+2; yOffset = y+1;
					dot(4,0);  m(2,0); d(3,1); d(4,2);  m(0,0); d(1,1);
					m(0,2); d(1,3); d(2,4);  dot(0,4); m(3,3); d(13,13);
					g.setColor(Roi.getColor());
					m(1,2); d(3,2); m(2,1); d(2,3);
					return;
				case TEXT:
					xOffset = x+2; yOffset = y+1;
					m(0,13); d(3,13);
					m(1,12); d(7,0); d(12,13);
					m(11,13); d(14,13);
					m(3,8); d(10,8);
					return;
				case MAGNIFIER:
					xOffset = x+2; yOffset = y+2;
					polyline(3,0,3,0,5,0,8,3,8,5,7,6,7,7,6,7,5,8,3,8,0,5,0,3,3,0);
					polyline(8,8,9,8,13,12,13,13,12,13,8,9,8,8);
					return;
				case HAND:
					xOffset = x+1; yOffset = y+1;
					polyline(5,14,2,11,2,10,0,8,0,7,1,6,2,6,4,8,4,6,3,5,3,4,2,3,2,2,3,1,4,1,5,2,5,3);
					polyline(6,5,6,1,7,0,8,0,9,1,9,5,9,1,11,1,12,2,12,6);
					polyline(13,4,14,3,15,4,15,7,14,8,14,10,13,11,13,12,12,13,12,14);
					return;
				case DROPPER:
					xOffset = x; yOffset = y;
					g.setColor(foregroundColor);
					m(12,2); d(14,2);
					m(11,3); d(15,3);
					m(11,4); d(15,4);
					m(8,5); d(15,5);
					m(9,6); d(14,6);
					polyline(10,7,12,7,12,9);
					polyline(8,7,2,13,2,15,4,15,11,8);
					g.setColor(backgroundColor);
					polyline(-1,-1,18,-1,18,17,-1,17,-1,-1);
					return;
				case ANGLE:
					xOffset = x; yOffset = y+2;
					m(0,11); d(11,0); m(0,11); d(15,11); 
					m(10,11); d(10,8); m(9,7); d(9,6); dot(8,5);
					drawDot(11,-1); drawDot(15,10);
					return;
			}
		}
		
		/**
		 * Draws the background square for the tool icon. Requires a Graphics Context to draw this Rectangle.
		 * @param g Graphics context for drawing.
		 * @param x X position where drawing should initiate from.
		 * @param y Y position where drawing should initiate from.
		 * @param width Width of Rectangle to be drawn.
		 * @param height Height of Rectangle to be drawn
		 * @param raised Boolean to indicate if the background is raised (i.e if this tool is selected).
		 */
 		private void fill3DRect(Graphics g, int x, int y, int width, int height, boolean raised) {
			if (null==g) return;
			if (raised)
				g.setColor(gray);
			else
				g.setColor(darker);
			g.fillRect(x+1, y+1, width-2, height-2);
			g.setColor(raised ? brighter : evenDarker);
			g.drawLine(x, y, x, y + height - 1);
			g.drawLine(x + 1, y, x + width - 2, y);
			g.setColor(raised ? evenDarker : brighter);
			g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
			g.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
		} 
 		
 		
 		private void m(int x, int y) {
 			this.x = xOffset+x;
 			this.y = yOffset+y;
 		}

 		private void d(int x, int y) {
 			x += xOffset;
 			y += yOffset;
 			g.drawLine(this.x, this.y, x, y);
 			this.x = x;
 			this.y = y;
 		}
 		
 		private void dot(int x, int y) {
 			g.fillRect(x+xOffset, y+yOffset, 1, 1);
 		}
		
		private void polyline(int... values) {
			Polygon p = new Polygon();
			int n = values.length/2;
			for (int i=0; i<n; i++)
				p.addPoint(values[i*2]+xOffset, values[i*2+1]+yOffset);
			g.drawPolyline(p.xpoints, p.ypoints, p.npoints);
		}
		
		void drawTriangle(int x, int y) {
			g.setColor(triangleColor);
			xOffset+=x; yOffset+=y;
			m(0,0); d(4,0); m(1,1); d(3,1); dot(2,2);
		}
		
		void drawDot(int x, int y) {
			g.fillRect(xOffset+x, yOffset+y, 2, 2);
		}

		void drawPoint(int x, int y) {
			g.setColor(toolColor);
			m(x-2,y); d(x+2,y);
			m(x,y-2); d(x,y+2);
			g.setColor(Roi.getColor());
			dot(x,y);
		}
		
		/**
		 * Returns the toolID value - equal to one of the tool NAMED CONSTANTS defined in this class.
		 * @return Tool ID.
		 */
		public int getToolID() {
			return toolID;
		}
		
		/**
		 * Returns the toolName value - a String representation of the tool.
		 * @return The tool name.
		 */
		public String getToolString() {
			return toolName;
		}
		
		/**
		 * Set the tool to another tool. This must be one of the named constants defined in this class.
		 * @param toolID
		 */
		public void setTool(int toolID) {
			if(toolID < 0 || toolID > 19) {
				throw new IllegalArgumentException("Invalid toolID");
			}
			
			this.toolID = toolID;
			
			switch(toolID) {
			case 0:
				toolName = "rectangle";
				break;
			case 1:
				toolName = "oval";
				break;
			case 2:
				toolName = "polygon";
				break;
			case 3:
				toolName = "freehand";
				break;
			case 4:
				toolName = "line";
				break;
			case 5:
				toolName = "polyline";
				break;
			case 6:
				toolName = "freeline";
				break;
			case 7:
				toolName = "point";
				break;
			case 8:
				toolName = "wand";
				break;
			case 9:
				toolName = "text";
				break;
			case 11:
				toolName = "zoom";
				break;
			case 12:
				toolName = "hand";
				break;
			case 13:
				toolName = "dropper";
				break;
			case 14:
				toolName = "angle";
				break;
			case 15:
				toolName = "ellipse";
				break;
			case 16:
				toolName = "brush";
				break;
			case 17:
				toolName = "roundrect";
				break;
			case 18:
				toolName = "arrow";
				break;
			case 19:
				toolName = "multipoint";
				break;
			default:
				toolName = null;
				break;
			}
		}
	}//end class Tools.

	
	} //end inner class ToolPanel.
	
	
	
	/**
	 * This class describes the RoiExtenderTool which provides the functionality to extend
	 * an Roi on the imp in the ImageWindowWithPanel to the edges of the image window.
	 * @author stevenwest
	 *
	 */
	public class RoiExtenderTool extends JButton implements ActionListener {

		ImageWindowWithPanel iwp;
		ImagePlus imp;
		Roi roi;
		
		
		public RoiExtenderTool(ImageWindowWithPanel iwp) {
			super(); //call JButton constructor - essential to generate the Button object! Note: The blank constructor generates a button with no text of Icon..
			this.setPreferredSize(new Dimension(30, 30)); //set button to desired size.
			this.addActionListener(this);
			this.iwp = iwp;
			ImageIcon icon = createImageIcon("/Icons/RoiExtenderToolInkScape.png", "Icon for RoiExtenderTool");
			this.setIcon(icon);
			this.setBorder(new LineBorder(Color.BLACK,1,false));
			this.setToolTipText("Roi Extender - will extend the current Polygon Selection to the nearest edges of the image");

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


		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			imp = iwp.getImagePlus();
			roi = imp.getRoi();
			if(roi instanceof PolygonRoi) {
				PolygonRoi polyRoi = (PolygonRoi)roi;
				Polygon p = polyRoi.getPolygon(); //alter this polygon to add first and last points, then re-make a polRoi object, remove the original from the imp, and set the new one to the imp.
				
				int width = imp.getWidth();
				int height = imp.getHeight();
				
				int firstX, firstY;
				int lastX, lastY;
				
				int polygonFirstX = p.xpoints[0];
				int polygonFirstY = p.ypoints[0];
				
				int polygonLastX = p.xpoints[p.npoints-1];
				int polygonLastY = p.ypoints[p.npoints-1];
				
				//calculate the firstX and firstY based on polygonFirstX & polygonFirstY:
				int distEndFirstX = width - polygonFirstX;
				int distEndFirstY = height - polygonFirstY;
				
				int smallestFirstX, smallestFirstY;
				
				//which edge of the image is the first X coordinate closest to?
				if(distEndFirstX < polygonFirstX) {
					smallestFirstX = distEndFirstX;
					firstX = width;
				}
				else if(polygonFirstX < distEndFirstX) {
					smallestFirstX = polygonFirstX;
					firstX = 0;
				}
				else {
					smallestFirstX = polygonFirstX;
					firstX = 0;
				}
				
				//which edge of the image is the first Y coordinate closest to?
				if(distEndFirstY < polygonFirstY) {
					smallestFirstY = distEndFirstY; //set firstY to height.
					firstY = height;
				}
				else if(polygonFirstY < distEndFirstY) {
					smallestFirstY = polygonFirstY; //set firstY to 0.
					firstY = 0;
				}
				else {
					smallestFirstY = polygonFirstY;
					firstY = 0;
				}
				
				//which is closer to the edge of the image - closest X or closest Y coordinate?
				if(smallestFirstX < smallestFirstY) {
					firstY = polygonFirstY;
				}
				else {
					firstX = polygonFirstX;
				}
				
				
				//calculate the lastX and lastY based on polygonFirstX & polygonFirstY:
				int distEndLastX = width - polygonLastX;
				int distEndLastY = height - polygonLastY;
				
				int smallestLastX, smallestLastY;
				
				//which edge of the image is the first X coordinate closest to?
				if(distEndLastX < polygonLastX) {
					smallestLastX = distEndLastX;
					lastX = width;
				}
				else if(polygonLastX < distEndLastX) {
					smallestLastX = polygonLastX;
					lastX = 0;
				}
				else {
					smallestLastX = polygonLastX;
					lastX = 0;
				}
				
				//which edge of the image is the Last Y coordinate closest to?
				if(distEndLastY < polygonLastY) {
					smallestLastY = distEndLastY;
					lastY = height;
				}
				else if(polygonLastY < distEndLastY) {
					smallestLastY = polygonLastY;
					lastY = 0;
				}
				else {
					smallestLastY = polygonLastY;
					lastY = 0;
				}
				
				//which is closer to the edge of the image - closest X or closest Y coordinate?
				if(smallestLastX < smallestLastY) {
					lastY = polygonLastY;
				}
				else {
					lastX = polygonLastX;
				}
				
				
				Polygon newP = new Polygon();
				newP.addPoint(firstX, firstY);
				for(int a=0; a<p.npoints; a++) {
					newP.addPoint(p.xpoints[a], p.ypoints[a]);
				}
				newP.addPoint(lastX, lastY);
				
				int type;
				if(polyRoi instanceof FreehandRoi) {
					type = Roi.FREELINE;
				}
				else {
					type = Roi.POLYLINE;
				}
				
				PolygonRoi newRoi = new PolygonRoi(newP, type);
				
				imp.killRoi(); //remove current ROI.

				imp.setRoi(newRoi); //add new ROI.
			}
		
		}//end actionPerformed()
	}//end class RoiExtenderTool
	
	
	
	
	/**
	 * This class describes the RoiLinkerTool which provides the functionality to 
	 * link each end of an Roi.
	 * @author stevenwest
	 *
	 */
	public class RoiLinkerTool extends JButton implements ActionListener {
		
		ImageWindowWithPanel iwp;
		ImagePlus imp;
		Roi roi;
		
		
		public RoiLinkerTool(ImageWindowWithPanel iwp) {
			super(); //call JButton constructor - essential to generate the Button object! 
				//Note: The blank constructor generates a button with no text of Icon..
			this.setPreferredSize(new Dimension(30, 30)); //set button to desired size.
			this.addActionListener(this);
			this.iwp = iwp;
			ImageIcon icon = createImageIcon("/Icons/DisplayAll.png", "Icon for RoiExtenderTool");
			this.setIcon(icon);
			this.setBorder(new LineBorder(Color.BLACK,1,false));
			this.setToolTipText("Roi Linker - will link the current Polygon Selection to form a sealed polygon");

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
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			imp = iwp.getImagePlus();
			roi = imp.getRoi();
			if(roi instanceof PolygonRoi) {
				PolygonRoi polyRoi = (PolygonRoi)roi;
				Polygon p = polyRoi.getPolygon(); //alter this polygon to add first and last points, 
												//then re-make a polRoi object, 
												//remove the original from the imp, 
												//and set the new one to the imp.
				
				int width = imp.getWidth();
				int height = imp.getHeight();
				
				int firstX, firstY;
				int lastX, lastY;
				
				int polygonFirstX = p.xpoints[0];
				int polygonFirstY = p.ypoints[0];
				
				int polygonLastX = p.xpoints[p.npoints-1];
				int polygonLastY = p.ypoints[p.npoints-1];
				
				
				Polygon newP = new Polygon();
				for(int a=0; a<p.npoints; a++) {
					newP.addPoint(p.xpoints[a], p.ypoints[a]);
				}
				newP.addPoint(p.xpoints[0], p.ypoints[0]);
				
				int type;
				if(polyRoi instanceof FreehandRoi) {
					type = Roi.FREELINE;
				}
				else {
					type = Roi.POLYLINE;
				}
				
				PolygonRoi newRoi = new PolygonRoi(newP, type);
				
				imp.killRoi(); //remove current ROI.

				imp.setRoi(newRoi); //add new ROI.
			}
		
		}//end actionPerformed()
		
	}//end class RoiLinkerTool
	
} //end class ImageWindowWithPanel
