package stereomate.plugins;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.text.PlainDocument;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ij.IJ;
import ij.gui.GUI;
import ij.plugin.PlugIn;
import edu.emory.mathcs.utils.pc.ConcurrencyUtils;
import stereomate.settings.MyIntFilter;


/**
 * This class provides the interface between the user and the options available for StereoMate
 * image processing.  The options are stored in XML files located in the StereoMate Directory
 * (they are present in the hidden ".settings" DIR).
 * <p>
 * The user can set settings for: 
 * <p>
 * - StereoMate Input-Output: which affect the way the DialogWindow, fileSelectors, and 
 * InputOutputFramework process the input and output.
 * <p>
 * - StereoMate Deconvolution: To set all options relating to deconvolution algorithm.
 * <p>
 * - ROI DiSector: need to add this -> add options to enable and disable the IJ window and keyboard
 * shortcuts.
 * <p>
 * - StereoAnalyser: To set options relating to image analysis -> minimum object size, default data output
 * (this can be modified in the StereoAnalyser DialogWindow).
 * <p>
 * - About: Tab to show information about this plugin suite, and where to find relevant documentation for help.
 * 
 * @author stevenwest
 *
 */
public class StereoMateSettings implements PlugIn {
	
	int COMBOBOX_SETTINGS = 0;
	int TEXTFIELD_SETTINGS = 1;
	
	//File file;
	//File stereoMateSettings;
	//InputStream in;
	
	Document doc;
	//DocumentBuilder dBuilder;
	//DocumentBuilderFactory dbFactory;
	
	//INPUT:
	TabPanel inputPanel;
	
	//DECONVOLUTION:
	TabPanel decPanel;
	
	//ROI DISECTOR PANEL:
	TabPanel roiPanel;
	
	//THRESHOLD MANAGER PANEL:
	// TabPanel thresholdPanel;
		
	//OBJECT MANAGER PANEL:
	TabPanel objectPanel;
	
	//STEREOANALYSER PANEL:
	TabPanel analyserPanel;
	
	//ABOUT PANEL:
	TabPanel aboutPanel;
	

	
	//JFRAME:
	JFrame smSettingsFrame;
	
	//JPANELS:
	JPanel framePanel;
	JPanel tabsPanel;
	JPanel optionsPanel;
	

	
	//PLAINDOC - for editing JTextField's:
	PlainDocument plainDoc;
	

	
	int maxWidth;
	int maxHeight;
	
	JPanel actionPanel;
	JButton cancel;
	JButton OK;
	
	ArrayList<TabPanel> tabPanels;
	
	/**
	 * This class represents a generic Tab Panel, and includes the panel as well as an array holding
	 * all the settings.  Each settings is an object of the inner class Settings.  Note, this class EXTENDS
	 * JPanel, so this class itself IS the JPanel!!
	 * @author stevenwest
	 *
	 */
	public class TabPanel extends JPanel {

		 int COMBOBOX_SETTINGS = 0;
		 int TEXTFIELD_SETTINGS = 1;
		
		//Panel object to hold all components of this tab panel:
			//No longer needed as the TabPanel itself is the JPanel!
		//JPanel tabPanel;
		
		//Button to display this tabPanel - to be displayed on the smSettingsFrame JFrame:
		JButton tabButton;
		
		//Array of settings objects - these contain the settings and their behaviour.
		ArrayList<Settings> settingsArray;
		
		//Button and Panel for adding ability to reset settings to default:
		JButton resetButton;
		JPanel resetPanel;
		
		Document doc;
		
		
		/**
		 * Constructor:  initialise all objects.
		 * @param tabTitle The title of this TabPanel -> this is the label added to the Button for this Tab
		 * Panel.  It will accept html formatting (add '<br>' to put a line break into title).
		 * @param doc A W3C Document representing the XML file containing the settings this TabPanel will
		 * display and edit.
		 */
		public TabPanel(String tabTitle, Document doc) {
			
			//INSTANTIATE COMPONENTS:
			//this = new JPanel();
			//instantiate the TabPanel JPanel:
			super();
			
			tabButton = new JButton("<html><div style='text-align: center;'>"+tabTitle+"</div></html>");
			
			settingsArray = new ArrayList<Settings>();
			
			resetButton = new JButton();
			resetPanel = new JPanel();
			
			//SET DIMENIONS OF BUTTONS:
		 	//set preferred size to 2xheight to make big buttons on tabsPanel:
			Dimension dim = tabButton.getPreferredSize();
			dim.height = dim.height*2;
			dim.width = dim.width * 2;
			
			tabButton.setPreferredSize(dim);
			tabButton.setMaximumSize(dim);
			
			//ADD BEHAVIOUR TO BUTTONS:

			tabButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					//First, save any textField contents on the current tabPanel displayed:
						//need to retrieve the correct TabPanel object
						//Do this by storing all tabPanels into array, and looping through to see
						//which of these is currently on the optionsPanel:
					for(int a=0; a<tabPanels.size(); a++) {
						if( optionsPanel.getComponent(0) == tabPanels.get(a) ) {
							//When the correct tabPanel is found, call ITS setTextFieldStr() method:
							tabPanels.get(a).setTextFieldStr();
						}
					}
					//check if optionsPanel is any panel with TextFields
					//if(optionsPanel.getComponent(0) == decPanel) {
						//if so, save textFields contents to their Strings:
							//prevents this info being lost!
						//setTextFieldDecStr();
					//}
					optionsPanel.remove(0);
					optionsPanel.add(TabPanel.this, BorderLayout.CENTER);
					optionsPanel.validate();
					optionsPanel.repaint();
				}
				
			});
			
			this.doc = doc;
			
			
		} //end constructor
		
		
		public void addSettings(int PANEL_TYPE, String domElementStr, String labelTitle) {
			addSettings( PANEL_TYPE,  domElementStr,  labelTitle, true);
		}
		
		
		/**
		 * Add a new setting to this TabPanel.
		 */
		public void addSettings(int PANEL_TYPE, String domElementStr, String labelTitle, boolean numericTextField) {
			
			//create a String[] object to store ComboBox String Array to:
			
			String[] cbStringArray = null;

			//First, retrieve the Setting value from the Document:
			
			//normalise the doc:
			doc.getDocumentElement().normalize();

			//get nodelist based on the domElementStr variable - 
				//String of Dom Element to retrieve data from:
			NodeList nList2 = doc.getElementsByTagName(domElementStr);
			String settingStr = ((Element)nList2.item(0)).getAttribute("str");
			
			//If this Settings obj is to be a COMBOBOX, need to retrieve the combobox string array
				//This is the strings to add to the Combobox:
			if(PANEL_TYPE == COMBOBOX_SETTINGS) {
				Element element = (Element)nList2.item(0);
				NodeList labelTitles = element.getElementsByTagName("comboboxString");
			
				cbStringArray = new String[ labelTitles.getLength() ];
			
				for (int count = 0; count < labelTitles.getLength(); count++) {
					cbStringArray[count] = labelTitles.item(count).getTextContent();
				}
			
			} //end if PANEL_TYPE

			//Finally, generate the Settings object with correct variables:
			Settings settings = new Settings(PANEL_TYPE, domElementStr, settingStr, labelTitle, cbStringArray, numericTextField);

			//and add this settings obj to the settingsArray:
			settingsArray.add(settings);
			
		}
		
		
		
		
		/**
		 * Layout the options panel - add all Settings Components to this panel.  Only call this
		 * after all settings objects have been added to this Tab Panel.
		 */
		public void layoutOptionsPanel() {
			
			//SET LAYOUT & BORDER & ADD PANELS:
			
			//First set layout and border of this panel:
			this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));			
			this.setBorder(BorderFactory.createEmptyBorder(0,0,0,5));
			
			//next, loop through all Settings objects and add them to this panel:
			for(int a=0; a<settingsArray.size(); a++) {
				this.add( settingsArray.get(a) );
			}
			
			
			//ADD RESET BUTTON & ITS BEHAVIOUR TO PANEL:
			
			resetButton = new JButton("Restore Defaults");
			
			resetButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					restoreDefaults();
				}
				
			});
			
			resetPanel = new JPanel();
			
			resetPanel.add(resetButton);
			
			this.add(resetPanel);
			
			
		}
		
		
		/**
		 * Restores the default values for all Settings components on this TabPanel.
		 */
		public void restoreDefaults() {
			
			//First, set up Document:
			
			//Normalise:
			doc.getDocumentElement().normalize();
		
			//Generate local NodeList obj:
			NodeList nList2;
			
			//loop through all Settings obj in settingsArray:
			for(int a=0; a<settingsArray.size(); a++) {
				
				//get the NodeList obj based on the settingsArray domElementString + "Def":
				nList2 = doc.getElementsByTagName( ( settingsArray.get(a).domElementString + "Def" ) );
				
				//and set the settingsArray settingString to the value stored in this element:
				settingsArray.get(a).settingString = ((Element)nList2.item(0)).getAttribute("str");
				
			}

			//Finally, call setComboBoxes to set all ComboBoxes to the value in the settingString:
			setComboBoxes();
			//And setTextFields to set all TextFields to the value of the settingString:
			setTextFields();
			
		}
		
		
		
		/**
		 * Sets all ComboBoxes to the item which matches the settingsString.
		 */
		public void setComboBoxes() {
			//Loop through all Settings objects in this TabPanel:
			for(int a=0; a<settingsArray.size(); a++) {
				//If the Settings Obj is a combobox:
				if(settingsArray.get(a).panelType == COMBOBOX_SETTINGS) {
					//Then run its setComboBox method:
					settingsArray.get(a).setComboBox();
				}
			}
		}
		
		
		/**
		 * Sets all Textfields to the value of settingsString.
		 * 
		 */
		public void setTextFields() {
			
			//Loop through all Settings objects in this TabPanel:
			for(int a=0; a<settingsArray.size(); a++) {
				//If the Settings Obj is a Textfield:
				if(settingsArray.get(a).panelType == TEXTFIELD_SETTINGS) {
					//Then run its setComboBox method:
					settingsArray.get(a).setTextField();
					//settingsArray.get(a).textField.setText( settingsArray.get(a).settingString );
				}
			}
			
		}
		
		
		
		/**
		 * Set all the textfield setting String variables to the contents of the textfields.
		 */
		public void setTextFieldStr() {
			
			for(int a=0; a<settingsArray.size(); a++) {
				if(settingsArray.get(a).panelType == TEXTFIELD_SETTINGS) {
					settingsArray.get(a).setTextFieldStr();
					//settingsArray.get(a).settingString = settingsArray.get(a).textField.getText();
				}
			}
			
		}
		
		
		
		
		/**
		 * Save all settings in this TabPanel to the DOM they came from.
		 */
		public void saveSettings() {
			
			//First set up document:
			
			//Normalise:
			doc.getDocumentElement().normalize();
			//IJ.showMessage("Root Element: "+ doc.getDocumentElement().getNodeName() );
		
			//Generate local NodeList obj:
			NodeList nList2;

			//loop through all Settings obj in settingsArray:
			for(int a=0; a<settingsArray.size(); a++) {
				
				//get the NodeList obj based on the settingsArray domElementString:
				nList2 = doc.getElementsByTagName( settingsArray.get(a).domElementString );
				
				//save the attribute from the settings obj to this Node:
				((Element)nList2.item(0)).setAttribute("str", settingsArray.get(a).settingString);
			}
			
			//save the doc to the stereoMateSettings file:
			
			try {
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				//get the path to the stereoMateSettings.xml file:
				File file = null;	
				try {
					file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
				//Retrieve parent, and then formulate new file object pointing to the StereoMateSettings.xml file:
				file = file.getParentFile();
				//File stereoMateSettings = new File(file.getAbsolutePath() + File.separator + ".settings" + File.separator + "StereoMateSettings.xml");
				File stereoMateSettings = new File(file.getAbsolutePath() + File.separator + "stereo_mate_settings" + File.separator + "StereoMateSettings.xml");		
				
				//save data to stereoMateSettings.xml:
				StreamResult result = new StreamResult(stereoMateSettings);
				transformer.transform(source, result);
			}
			catch(TransformerConfigurationException e) {
				e.printStackTrace();
			}
			catch(TransformerException e) {
				e.printStackTrace();
			}
			
		}
		
		
		/**
		 * This class represents a settings object - a combination of a Label, combobox/textfield and panel.
		 * It also includes methods to deal with changing of settings.
		 * @author stevenwest
		 *
		 */
		public class Settings extends JPanel {
			
			 int COMBOBOX_SETTINGS = 0;
			 int TEXTFIELD_SETTINGS = 1;
			
			int panelType;
			
			JLabel label;
			String[] StringCB;
			JComboBox comboBox;
			JTextField textField;
			
			JPanel panel;
			
			String settingString;
			
			String domElementString;
			
			/**
			 * Instantiate the Settings Obj.
			 * @param PANEL_TYPE If ComboBox set to 0, if Textfield set to 1.
			 * @param domElementStr Title of the element in the DOM which holds this Settings value.
			 * @param settingStr The content of the setting as retrieved from the DOM or set by the user.
			 * @param labelTitle The title for the label of this setting.
			 * @param cbStringArray An array of String's for filling the ComboBox.
			 */
			public Settings(int PANEL_TYPE, String domElementStr, String settingStr, String labelTitle, String[] cbStringArray, boolean numericTextField) {
				
				//instantiate this JPanel.
				super();
				
				domElementString = domElementStr;
				settingString = settingStr;
				
				//ADD LABEL TO PANEL:
				//generate the label & add to this panel:
				label = new JLabel(labelTitle);
				this.add(label);

				//save PANEL_TYPE to the int panelType:
				panelType = PANEL_TYPE;
				
				
				//instantiate components related to comboboxes & behaviour
				if(panelType == COMBOBOX_SETTINGS) {
					
				//ADD COMBOBOX TO PANEL:
					
					//String[] for ComboBox:
					StringCB = cbStringArray;
					
					//ComboBox:
					comboBox = new JComboBox<String>(StringCB);
					
					this.add(comboBox);
					
					
				//SETTINGS:
					
					//SET ALIGNMENTS OF PANELS:
					
					//Set to right alignment:
					this.setAlignmentX(Component.RIGHT_ALIGNMENT);
					
					//SET MAX SIZE OF PANELS:
					
					//Set to match the label and combobox/textfield the panel contains:
					int maxWidth = label.getPreferredSize().width + comboBox.getPreferredSize().width + 20;
					int maxHeight = comboBox.getPreferredSize().height + 4;
					this.setMaximumSize(new Dimension(maxWidth, maxHeight) );
					
					
					//SET COMBOBOX:
					//No need to set the Textfield -> automatically set at construction
						//with setText() method.
					//values found in the Str variables:
					for(int a=0; a<cbStringArray.length; a++) {
						if(settingStr.equalsIgnoreCase(cbStringArray[a])) {
							comboBox.setSelectedIndex(a);
						}
					}
					
					//PROGRAM BEHAVIOUR OF COMBOBOX:

					//When ComboBox content is changed, update the relevant Variable:
					comboBox.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							
							for(int a=0; a<StringCB.length; a++) {
								if( comboBox.getSelectedItem().equals(StringCB[a]) ) {
									settingString = StringCB[a].toUpperCase();
								}
							}
							
						}
						
					});
					
				} //end combobox settings.
				
				//instantiate components related to textfields & behaviour
				if(panelType == TEXTFIELD_SETTINGS) {
					
					textField = new JTextField(6);
					textField.setText(settingString);
					if(numericTextField == true) {
						plainDoc = (PlainDocument) textField.getDocument();
						plainDoc.setDocumentFilter(new MyIntFilter());
					}
					
					this.add(textField);
					
				//SETTINGS:
					
					//SET ALIGNMENTS OF PANELS:
					
					//Set to right alignment:
					this.setAlignmentX(Component.RIGHT_ALIGNMENT);
					
					//SET MAX SIZE OF PANELS:
					
					//Set to match the label and combobox/textfield the panel contains:
					int maxWidth = label.getPreferredSize().width + textField.getPreferredSize().width + 20;
					int maxHeight = textField.getPreferredSize().height + 4;
					this.setMaximumSize(new Dimension(maxWidth, maxHeight) );
					
				} //end textfield settings.
				
				
			} //end constructor
			
			
			
			
			/**
			 * Set the combobox to the index which matches settingString in this class:
			 */
			public void setComboBox() {
				for(int a=0; a<StringCB.length; a++) {
					if(settingString.equalsIgnoreCase(StringCB[a])) {
						comboBox.setSelectedIndex(a);
					}
				}
			}
			
			
			/**
			 * Sets  Textfield to the value of settingsString.
			 */
			public void setTextField() {
				textField.setText( settingString );
			}
			
			
			/**
			 * Set  the textfield setting String variable to the content of the textfield.
			 */
			public void setTextFieldStr() {
				settingString = textField.getText();
			}
			
			
		} //end Settings class
		
	} //end TabPanel class


	/**
	 * 
	 */
	@Override
	public void run(String arg) {
				
		//build W3C Document of StereoMateSettings.xml:
		doc = buildDomStereoMateSettings();
		
		//Instantiate tabPanels:
		
		tabPanels = new ArrayList<TabPanel>();
		
		//Next, generate all TabPanel objects:
		
		inputPanel = new TabPanel("Input<br>Output", doc);
		
		decPanel = new TabPanel("SM<br>Deconvolution", doc);
		
		roiPanel = new TabPanel("ROI<br>DiSector", doc);
		
		// thresholdPanel = new TabPanel("Threshold<br>Manager", doc);
		
		objectPanel = new TabPanel("Object<br>Manager", doc);
		
		analyserPanel = new TabPanel("SM<br>Analyser", doc);
		
		aboutPanel = new TabPanel("About<br>StereoMate", doc);
		
		
		//add TabPanel objects to tabPanels:
		tabPanels.add(inputPanel);
		tabPanels.add(decPanel);
		tabPanels.add(objectPanel);
		tabPanels.add(roiPanel);
		tabPanels.add(analyserPanel);
		tabPanels.add(aboutPanel);
		
		// for each TabPanel, add Settings to it which are present in the DOM:
		
		//INPUT PANEL:
		inputPanel.addSettings(COMBOBOX_SETTINGS, "useMinDepth", "<html>Process Images at Minimum or Maximum<br>Depth "
				+ "of DirTree in Compound DIRs:</html>");
		
		inputPanel.addSettings(COMBOBOX_SETTINGS, "logProcessing", "Log image processing information:");

		//DEC PANEL:
		decPanel.addSettings(COMBOBOX_SETTINGS, "boundary", "Deconvolution boundary condition:");
		decPanel.addSettings(COMBOBOX_SETTINGS, "resizing", "Blurred image resizing:");
		decPanel.addSettings(COMBOBOX_SETTINGS, "output", "Image output type:");
		decPanel.addSettings(COMBOBOX_SETTINGS, "precision", "Deconvolution precision:");
		decPanel.addSettings(TEXTFIELD_SETTINGS, "threshold", "Deblurred threshold [-1, disabled]:");
		decPanel.addSettings(TEXTFIELD_SETTINGS, "maxIters", "Max. number of iterations:");
		decPanel.addSettings(TEXTFIELD_SETTINGS, "nOfThreads", "Number of threads:");
			decPanel.settingsArray.get(6).settingString = "" + ConcurrencyUtils.getNumberOfProcessors();
			decPanel.settingsArray.get(6).setTextField();
		decPanel.addSettings(COMBOBOX_SETTINGS, "showIterations", "Display image during deconvolution:");
		decPanel.addSettings(TEXTFIELD_SETTINGS, "gamma", "Wiener pre-conditioner gamma:");
		decPanel.addSettings(TEXTFIELD_SETTINGS, "filterXY", "Lateral [XY] low-pass filter:");
		decPanel.addSettings(TEXTFIELD_SETTINGS, "filterZ", "Axial [Z] low-pass filter:");
		decPanel.addSettings(COMBOBOX_SETTINGS, "normalize", "Normalise PSF:");
		decPanel.addSettings(COMBOBOX_SETTINGS, "logMean", "Log mean pixel value:");
		decPanel.addSettings(COMBOBOX_SETTINGS, "antiRing", "Perform anti-ring step:");
		decPanel.addSettings(TEXTFIELD_SETTINGS, "changeThreshPercent", "Terminate iteration if x% change is below:");
		decPanel.addSettings(COMBOBOX_SETTINGS, "db", "Input in Db:");
		decPanel.addSettings(COMBOBOX_SETTINGS, "detectDivergence", "Detect divergence:");
		

		//ROI PANEL:
		roiPanel.addSettings(COMBOBOX_SETTINGS, "enableIJWin", "Enable IJ Window:");
		roiPanel.addSettings(COMBOBOX_SETTINGS, "enableKey", "Enable Keyboard:");
		
		//THRESHOLD MANAGER PANEL:
		// thresholdPanel.addSettings(PANEL_TYPE, domElementStr, labelTitle);
		
		//OBJECT MANAGER PANEL
		objectPanel.addSettings(TEXTFIELD_SETTINGS, "gaussianObj", "Gaussian Object Selection String:", false);
		objectPanel.addSettings(TEXTFIELD_SETTINGS, "linearObj", "Linear Object Selection int:");
		objectPanel.addSettings(TEXTFIELD_SETTINGS, "maxObjSel", "Max. No. of Objects selectable int:");
		objectPanel.addSettings(TEXTFIELD_SETTINGS, "roughClassifier", "Rough ML CLassifier Title [Weka]:", false);
		objectPanel.addSettings(TEXTFIELD_SETTINGS, "sliceProjection", "Initial Slice Projection presented:", false);
		
		//ANALYSER PANEL:
		analyserPanel.addSettings(TEXTFIELD_SETTINGS,"analyserMinObj", "Min. Obj Size:");
		analyserPanel.addSettings(TEXTFIELD_SETTINGS,"analyserMaxObjXY", "Max. Obj Size XY:");
		analyserPanel.addSettings(TEXTFIELD_SETTINGS,"analyserMaxObjZ", "Max. Obj Size Z:");
		
		
		
		//layout OptionsPanel for each TabPanel:
		
		inputPanel.layoutOptionsPanel();
		
		decPanel.layoutOptionsPanel();
		
		roiPanel.layoutOptionsPanel();
		
		objectPanel.layoutOptionsPanel();
		
		analyserPanel.layoutOptionsPanel();
		
		aboutPanel.layoutOptionsPanel();
		
		
		//Now all of the data has been read, need to construct a JFrame to display the information on.
		
		//Here will use a JFrame with a series of TABS on the Left hand side used to display settings
		//from desired PlugIns.
		

		//INSTANTIATE JFRAME, CORE JPANELS & JBUTTONS:
		
		 smSettingsFrame = new JFrame("StereoMate Settings");
		
		 framePanel = new JPanel();
		 tabsPanel = new JPanel();
		 optionsPanel = new JPanel();
		

		
		
		//LAYOUT TABSPANEL:
		//GridLayout with no border, and add all buttons.
		tabsPanel.setLayout(new GridLayout(0,1,0,0) );
		tabsPanel.add(inputPanel.tabButton);
		tabsPanel.add(decPanel.tabButton);
		tabsPanel.add(roiPanel.tabButton);
		tabsPanel.add(objectPanel.tabButton);
		tabsPanel.add(analyserPanel.tabButton);
		tabsPanel.add(aboutPanel.tabButton);
		
		//set max size of tabsPanel:
		Dimension dim = inputPanel.tabButton.getPreferredSize();
			dim.height = dim.height*2;
			dim.width = dim.width * 2;
		tabsPanel.setMaximumSize(new Dimension(100, dim.height*8));

		
		//LAYOUT OPTIONS PANELS:
			//Layout each optionPanel for each of the settings.
		//layoutOptionsPanels();
		
		//And initially set the options panel to the inputPanel:
		optionsPanel.setLayout(new BorderLayout() );
		optionsPanel.add(inputPanel, BorderLayout.CENTER);
		optionsPanel.setPreferredSize(new Dimension(450, 750));
		
		
		//SET PANELS TO JFRAME:
			//Add TabsPanel and OptionsPanel to FramePanel:
		framePanel.setLayout(new BorderLayout());
		framePanel.add(tabsPanel,BorderLayout.WEST);
		framePanel.add(optionsPanel, BorderLayout.CENTER);
		framePanel.add( addActionPanel(), BorderLayout.SOUTH);
		
		//Set FramePanel to JFrame:
		smSettingsFrame.setContentPane(framePanel);
		
		//Pack, validate, centre, no resize, make visible:
		smSettingsFrame.pack(); //pack content into frame.
		smSettingsFrame.validate(); //validate this layout.
		GUI.center(smSettingsFrame); //centre Window on screen.
		smSettingsFrame.setResizable(false);  //stop the Dialog from being resized.
		smSettingsFrame.setVisible(true); //make this DialgoWindow visible.

		
		
	} //end run()

	
	/**
	 * 
	 * @return
	 */
	public JPanel addActionPanel() {
		/* ACTION PANEL */
		//create a JPanel to hold "Process" and "Cancel" Buttons:
		JPanel p = new JPanel();
		actionPanel = new JPanel();
		
		//create JButtons:
		OK = new JButton("OK");
		cancel = new JButton("Cancel");
		
		
		//add action listeners with appropriate behaviours:
		cancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				IJ.showStatus("SM Settings - cancelled"); //set the status bar.
				//dispose of image settings without saving:
				smSettingsFrame.dispose();
				System.gc();
				
				
			}
			
		});
		
		OK.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				//first, set the current TabPanel's TextField variables to their String variables:
				TabPanel tp = (TabPanel)optionsPanel.getComponent(0);
				tp.setTextFieldStr();
				//next, write all of the variables to the Dom Document, to update the xml file:
				//And save the XML file to stereoMateSettings.xml [the stereMateSettings file!]
				for(int a=0; a<tabPanels.size(); a++) {
					tabPanels.get(a).saveSettings();
				}
				//saveSettings();
				//finally, dispose of all of the objects by calling disposeSettings() :
				smSettingsFrame.dispose();
				System.gc();
				
			}
			
		});
		
		//set layout & border, and add components:
		actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.LINE_AXIS) );
		actionPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2) );
		actionPanel.add(Box.createHorizontalGlue() );
		actionPanel.add(cancel);
		actionPanel.add(Box.createRigidArea(new Dimension(5,0) ) );
		actionPanel.add(OK);
		
		p.add( new JSeparator() );
		p.add(actionPanel);
		
		return p;
		
	} //end addActionPanel()
	
	
	
	//  ***   STATIC METHODS   ***  //
	
	/**
	 * This static method builds the DOM for retrieving data from the stereoMateSettings.xml file.
	 * 
	 * @return A Document containing the StereoMateSettings.xml file.
	 */
	public static Document buildDomStereoMateSettings() {

		//Can generate an InputStream from a file using getResourceAsStrem().
				//This works, as the DocumentBuilder can parse directly from inputStrem objects.
				//InputStream in = getClass().getResourceAsStream("Settings/StereoMateSettings.xml");
				
				/*
				InputStream in = null;
					try {
						in = getClass().getResourceAsStream("Settings"+File.separator+"StereoMateSettings.xml");
						
					}
					catch (NullPointerException e) {
						IJ.showMessage("StereoMateSettings Not Found");
						e.printStackTrace();
						try {
							in.close();
						}
						catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				
					
					//attempt to read components of the file:
					try {
						doc.getDocumentElement().normalize();
					
						IJ.showMessage("Root Element: "+ doc.getDocumentElement().getNodeName() );
					
						NodeList nList = doc.getElementsByTagName("DIRtree");
						for(int a=0; a<nList.getLength(); a++) {
							Node node = nList.item(a);
							NodeList subList = node.getChildNodes();
							for(int b=0; b<subList.getLength(); b++) {
								Node subNode = subList.item(b);
								if(subNode.getNodeType() == Node.ELEMENT_NODE) {
									IJ.showMessage("Node: "+subNode.getNodeName() );
									//typecast to element:
									Element e = (Element) subNode;
									//Extract relevant information, print in ImageJ:
									IJ.showMessage("Node Attr: "+ e.getAttribute("Title") );
					
									//To retrieve Text Content:
									IJ.showMessage("Text Content: "+ e.getTextContent() );

								} //end if
					
							} //end for b
						
						} //end for a
						
						NodeList nList2 = doc.getElementsByTagName("SampleFolderTitle");
						String sampleFolderTitle = ((Element)nList2.item(0)).getAttribute("Title");
						
						nList2 = doc.getElementsByTagName("RefFolderTitle");
						String refFolderTitle = ((Element)nList2.item(0)).getAttribute("Title");
						
						IJ.showMessage("Sample Folder Title: "+sampleFolderTitle);
						IJ.showMessage("Ref Folder Title: "+refFolderTitle);
					
					}//end try
					
					finally {
						//close InputStream
						try {
							in.close();
						}
						catch (IOException ex) {
							ex.printStackTrace();
						}
					}
					
					*/
				
				
				//For retrieving files EXTERNAL from the JAR files - which is required for Read AND WRITE functionality,
				//simply generate a file object based on the path to the JAR that leads to the desired file,
				//and generate an InputStream from that file using FileInputStream:
				
				//Retrieve file representation of JAR file - to retrieve its absolute path:
		
		
		//Retrieve file representation of JAR file - to retrieve its absolute path:
		File file = null;	
		try {
			file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		//Retrieve parent, and then formulate new file object pointing to the StereoMateSettings.xml file:
		file = file.getParentFile();
		//File stereoMateSettings = new File(file.getAbsolutePath() + File.separator + ".settings" + File.separator + "StereoMateSettings.xml");
		File stereoMateSettings = new File(file.getAbsolutePath() + File.separator + "stereo_mate_settings" + File.separator + "StereoMateSettings.xml");		
			
		// need to check if stereMateSettings exists ,and if not, make it - stereoMateSettings.exists();
		if( !stereoMateSettings.exists() ) {
			createStereoMateSettingsFile(stereoMateSettings);
		}
		
		//Here, the InputStream is used inside appropriate try... catch statements:
		InputStream in = null;
		
		try {
			in = new FileInputStream(stereoMateSettings);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		//Once an InputStream is established, next build the DOM Document:
		
		//generate Document Builder:
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			//close InputStream
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
				
		Document docSMS = null;
		try {
			docSMS = dBuilder.parse(in);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			//close InputStream
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return docSMS;

	} //end buildDomStereoMateSettings()
	
	
	/**
	 * Generic method to build a DOM from the ".settings" DIR inside the StereoMate folder.  This method
	 * returns a Document object.
	 * @param fileName a String of the File Name to build a DOM from.
	 */
	public static Document buildDom(String fileName) {
		
		Document document;
		
		File file;

		//Retrieve file representation of JAR file - to retrieve its absolute path:
		 file = null;	
		try {
			file = new File(StereoMateSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		//Retrieve parent, and then formulate new file object pointing to the StereoMateSettings.xml file:
		file = file.getParentFile();
		 file = new File(file.getAbsolutePath() + File.separator + "stereo_mate_settings" + File.separator + fileName);
		 
		 
		//Here, the InputStream is used inside appropriate try... catch statements:
		 InputStream in = null;
		
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		//Once an InputStream is established, next build the DOM Document:
		
		//generate Document Builder:
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			//close InputStream
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
				
		 document = null;
		try {
			document = dBuilder.parse(in);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			//close InputStream
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		return document;

	} //end buildDom(String)
	
	
	/**
	 * This method will CREATE a new StereoMate Settings file with the name and
	 * location specified in the stereoMateSettings File object.
	 */
	public static void createStereoMateSettingsFile(File stereoMateSettings) {
					
		stereoMateSettings.getParentFile().mkdir(); // make the parent DIR!
			
			String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><stereomate>\n" + 
					"  <DIRtree>\n" + 
					"    <SampleFolderTitle Title=\"SAMPLE\">Sample Text Content</SampleFolderTitle>\n" + 
					"    <RefFolderTitle Title=\"ref\">Ref Text Content</RefFolderTitle>\n" + 
					"  </DIRtree>\n" + 
					"  <ROIDiSector ROInumber=\"4\">\n" + 
					"    <ROIBoundaries TouchBoundary=\"true\">Touch Boundary:True</ROIBoundaries>\n" + 
					"  </ROIDiSector>\n" + 
					"  <InputOutputFramework>\n" + 
					"	<Default>\n" + 
					"  		<useMinDepthDef str=\"true\"/>\n" + 
					"		<logProcessingDef str=\"true\"/>\n" + 
					"	</Default>\n" + 
					"	<Current>\n" + 
					"		<useMinDepth str=\"TRUE\">\n" + 
					"			<comboboxString>true</comboboxString>\n" + 
					"			<comboboxString>false</comboboxString>\n" + 
					"		</useMinDepth>\n" + 
					"		<logProcessing str=\"TRUE\">\n" + 
					"			<comboboxString>true</comboboxString>\n" + 
					"			<comboboxString>false</comboboxString>\n" + 
					"		</logProcessing>\n" + 
					"	</Current>\n" + 
					"  </InputOutputFramework>\n" + 
					"  <StereoMateDeconvolution>\n" + 
					"  	<Default>\n" + 
					" 		<boundaryDef str=\"REFLEXIVE\"/>\n" + 
					" 		<resizingDef str=\"MINIMAL\"/>\n" + 
					" 		<outputDef str=\"BYTE\"/>\n" + 
					" 		<precisionDef str=\"SINGLE\"/>\n" + 
					" 		<thresholdDef str=\"-1\"/>\n" + 
					" 		<maxItersDef str=\"40\"/>\n" + 
					" 		<showIterationsDef str=\"false\"/>\n" + 
					" 		<gammaDef str=\"0.0\"/>\n" + 
					" 		<filterXYDef str=\"1.0\"/>\n" + 
					" 		<filterZDef str=\"1.0\"/>\n" + 
					" 		<normalizeDef str=\"false\"/>\n" + 
					" 		<logMeanDef str=\"false\"/>\n" + 
					" 		<antiRingDef str=\"true\"/>\n" + 
					" 		<changeThreshPercentDef str=\"0.01\"/>\n" + 
					" 		<dbDef str=\"false\"/>\n" + 
					" 		<detectDivergenceDef str=\"true\"/>\n" + 
					" 	</Default>\n" + 
					" 	<Current>\n" + 
					" 	 	<boundary str=\"REFLEXIVE\">\n" + 
					"			<comboboxString>REFLEXIVE</comboboxString>\n" + 
					"			<comboboxString>PERIODIC</comboboxString>\n" + 
					"			<comboboxString>ZERO</comboboxString>\n" + 
					"		</boundary>\n" + 
					" 		<resizing str=\"AUTO\">\n" + 
					"			<comboboxString>MINIMAL</comboboxString>\n" + 
					"			<comboboxString>AUTO</comboboxString>\n" + 
					"			<comboboxString>NEXT_POWER_OF_TWO</comboboxString>\n" + 
					"		</resizing>\n" + 
					" 		<output str=\"BYTE\">\n" + 
					"			<comboboxString>SAME_AS_SOURCE</comboboxString>\n" + 
					"			<comboboxString>BYTE</comboboxString>\n" + 
					"			<comboboxString>SHORT</comboboxString>\n" + 
					"			<comboboxString>FLOAT</comboboxString>\n" + 
					"		</output>\n" + 
					" 		<precision str=\"SINGLE\">\n" + 
					"			<comboboxString>SINGLE</comboboxString>\n" + 
					"			<comboboxString>DOUBLE</comboboxString>\n" + 
					"		</precision>\n" + 
					" 		<threshold str=\"-1\"/>\n" + 
					" 		<maxIters str=\"40\"/>\n" + 
					"		<nOfThreads str=\"4\"/>\n" + 
					" 		<showIterations str=\"false\">\n" + 
					"			<comboboxString>true</comboboxString>\n" + 
					"			<comboboxString>false</comboboxString>\n" + 
					"		</showIterations>\n" + 
					" 		<gamma str=\"0.0\"/>\n" + 
					" 		<filterXY str=\"1.0\"/>\n" + 
					" 		<filterZ str=\"1.0\"/>\n" + 
					" 		<normalize str=\"false\">\n" + 
					"			<comboboxString>true</comboboxString>\n" + 
					"			<comboboxString>false</comboboxString>\n" + 
					"		</normalize>\n" + 
					" 		<logMean str=\"false\">\n" + 
					"			<comboboxString>true</comboboxString>\n" + 
					"			<comboboxString>false</comboboxString>\n" + 
					"		</logMean>\n" + 
					" 		<antiRing str=\"true\">\n" + 
					"			<comboboxString>true</comboboxString>\n" + 
					"			<comboboxString>false</comboboxString>\n" + 
					"		</antiRing>\n" + 
					" 		<changeThreshPercent str=\"0.01\"/>\n" + 
					" 		<db str=\"false\">\n" + 
					"			<comboboxString>true</comboboxString>\n" + 
					"			<comboboxString>false</comboboxString>\n" + 
					"		</db>\n" + 
					" 		<detectDivergence str=\"true\">\n" + 
					"			<comboboxString>true</comboboxString>\n" + 
					"			<comboboxString>false</comboboxString>\n" + 
					"		</detectDivergence>\n" + 
					" 	</Current>\n" + 
					"  </StereoMateDeconvolution>\n" + 
					"  <RoiDiSector>\n" + 
					"	<Default>\n" + 
					"		<enableIJWinDef str=\"true\"/>\n" + 
					"		<enableKeyDef str=\"true\"/>\n" + 
					"	</Default>\n" + 
					"	<Current>\n" + 
					"		<enableIJWin str=\"true\">\n" + 
					"			<comboboxString>true</comboboxString>\n" + 
					"			<comboboxString>false</comboboxString>\n" + 
					"		</enableIJWin>\n" + 
					"		<enableKey str=\"true\">\n" + 
					"			<comboboxString>true</comboboxString>\n" + 
					"			<comboboxString>false</comboboxString>\n" + 
					"		</enableKey>\n" + 
					"	</Current>\n" + 
					"  </RoiDiSector>\n" + 
					"  <ObjectManager>\n" + 
					"	<Default>\n" + 
					"		<gaussianObjDef str=\"1,2,4,2,1\"/>\n" + 
					"		<linearObjDef str=\"5\"/>\n" + 
					"		<maxObjSelDef str=\"1000\"/>\n" + 
					"		<roughClassifierDef str=\"IBk\"/>\n" + 
					"		<sliceProjectionDef str=\"1-10\"/>\n" + 
					"	</Default>\n" + 
					"	<Current>\n" + 
					"		<gaussianObj str=\"1,2,4,2,1\"/>\n" + 
					"		<linearObj str=\"5\"/>\n" + 
					"		<maxObjSel str=\"1000\"/>\n" + 
					"		<roughClassifier str=\"IBk\"/>\n" + 
					"		<sliceProjection str=\"1-10\"/>\n" + 
					"	</Current>\n" + 
					"  </ObjectManager>\n" + 
					"  <StereoAnalyser>\n" + 
					"	<Default>\n" + 
					"		<analyserMinObjDef str=\"100\"/>\n" + 
					"		<analyserMinObjXYDef str=\"0\"/>\n" + 
					"		<analyserMinObjZDef str=\"2\"/>\n" + 
					"		<analyserMaxObjDef str=\"2147483647\"/>\n" + 
					"		<analyserMaxObjXYDef str=\"30\"/>\n" + 
					"		<analyserMaxObjZDef str=\"20\"/>\n" + 
					"	</Default>\n" + 
					"	<Current>\n" + 
					"		<analyserMinObj str=\"100\"/>\n" + 
					"		<analyserMinObjXY str=\"0\"/>\n" + 
					"		<analyserMinObjZ str=\"2\"/>\n" + 
					"		<analyserMaxObj str=\"2147483647\"/>\n" + 
					"		<analyserMaxObjXY str=\"1\"/>\n" + 
					"		<analyserMaxObjZ str=\"1\"/>\n" + 
					"	</Current>\n" + 
					"  </StereoAnalyser>\n" + 
					"  \n" + 
					"</stereomate>";
			
	        FileWriter fr = null;
	        try {
	            fr = new FileWriter(stereoMateSettings);
	            fr.write(content);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }finally{
	            //close resources
	            try {
	                fr.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	        
	        content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + 
	        "<wplAuto>\n" + 
	        "  <values z=\"55\">\n" + 
	        "    <xyLength memory=\"1048576000\">520</xyLength>\n" + 
	        "    <xyLength memory=\"2097152000\">550</xyLength>\n" + 
	        "    <xyLength memory=\"3110076416\">930</xyLength>\n" + 
	        "    <xyLength memory=\"3145728000\">940</xyLength>\n" + 
	        "    <xyLength memory=\"4194304000\">1150</xyLength>\n" + 
	        "    <xyLength memory=\"5242880000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"6291456000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"7340032000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"8388608000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"9437184000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"10485760000\">1980</xyLength>\n" + 
	        "    <xyLength memory=\"11534336000\">2100</xyLength>\n" + 
	        "    <xyLength memory=\"12582912000\">2180</xyLength>\n" + 
	        "    <xyLength memory=\"13631488000\">2270</xyLength>\n" + 
	        "    <xyLength memory=\"14680064000\">2360</xyLength>\n" + 
	        "    <xyLength memory=\"15728640000\">2440</xyLength>\n" + 
	        "  </values>\n" + 
	        "<values z=\"50\">\n" + 
	        "    <xyLength memory=\"1048576000\">520</xyLength>\n" + 
	        "    <xyLength memory=\"2097152000\">550</xyLength>\n" + 
	        "    <xyLength memory=\"3110076416\">930</xyLength>\n" + 
	        "    <xyLength memory=\"3145728000\">940</xyLength>\n" + 
	        "    <xyLength memory=\"4194304000\">1150</xyLength>\n" + 
	        "    <xyLength memory=\"5242880000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"6291456000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"7340032000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"8388608000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"9437184000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"10485760000\">1980</xyLength>\n" + 
	        "    <xyLength memory=\"11534336000\">2100</xyLength>\n" + 
	        "    <xyLength memory=\"12582912000\">2180</xyLength>\n" + 
	        "    <xyLength memory=\"13631488000\">2270</xyLength>\n" + 
	        "    <xyLength memory=\"14680064000\">2360</xyLength>\n" + 
	        "    <xyLength memory=\"15728640000\">2440</xyLength>\n" + 
	        "  </values>\n" + 
	        "<values z=\"40\">\n" + 
	        "    <xyLength memory=\"1048576000\">520</xyLength>\n" + 
	        "    <xyLength memory=\"2097152000\">550</xyLength>\n" + 
	        "    <xyLength memory=\"3110076416\">930</xyLength>\n" + 
	        "    <xyLength memory=\"3145728000\">940</xyLength>\n" + 
	        "    <xyLength memory=\"4194304000\">1150</xyLength>\n" + 
	        "    <xyLength memory=\"5242880000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"6291456000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"7340032000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"8388608000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"9437184000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"10485760000\">1980</xyLength>\n" + 
	        "    <xyLength memory=\"11534336000\">2100</xyLength>\n" + 
	        "    <xyLength memory=\"12582912000\">2180</xyLength>\n" + 
	        "    <xyLength memory=\"13631488000\">2270</xyLength>\n" + 
	        "    <xyLength memory=\"14680064000\">2360</xyLength>\n" + 
	        "    <xyLength memory=\"15728640000\">2440</xyLength>\n" + 
	        "  </values>\n" + 
	        "<values z=\"30\">\n" + 
	        "    <xyLength memory=\"1048576000\">520</xyLength>\n" + 
	        "    <xyLength memory=\"2097152000\">550</xyLength>\n" + 
	        "    <xyLength memory=\"3110076416\">930</xyLength>\n" + 
	        "    <xyLength memory=\"3145728000\">940</xyLength>\n" + 
	        "    <xyLength memory=\"4194304000\">1150</xyLength>\n" + 
	        "    <xyLength memory=\"5242880000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"6291456000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"7340032000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"8388608000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"9437184000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"10485760000\">1980</xyLength>\n" + 
	        "    <xyLength memory=\"11534336000\">2100</xyLength>\n" + 
	        "    <xyLength memory=\"12582912000\">2180</xyLength>\n" + 
	        "    <xyLength memory=\"13631488000\">2270</xyLength>\n" + 
	        "    <xyLength memory=\"14680064000\">2360</xyLength>\n" + 
	        "    <xyLength memory=\"15728640000\">2440</xyLength>\n" + 
	        "  </values>\n" + 
	        "<values z=\"20\">\n" + 
	        "    <xyLength memory=\"1048576000\">520</xyLength>\n" + 
	        "    <xyLength memory=\"2097152000\">550</xyLength>\n" + 
	        "    <xyLength memory=\"3110076416\">930</xyLength>\n" + 
	        "    <xyLength memory=\"3145728000\">940</xyLength>\n" + 
	        "    <xyLength memory=\"4194304000\">1150</xyLength>\n" + 
	        "    <xyLength memory=\"5242880000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"6291456000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"7340032000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"8388608000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"9437184000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"10485760000\">1980</xyLength>\n" + 
	        "    <xyLength memory=\"11534336000\">2100</xyLength>\n" + 
	        "    <xyLength memory=\"12582912000\">2180</xyLength>\n" + 
	        "    <xyLength memory=\"13631488000\">2270</xyLength>\n" + 
	        "    <xyLength memory=\"14680064000\">2360</xyLength>\n" + 
	        "    <xyLength memory=\"15728640000\">2440</xyLength>\n" + 
	        "  </values>\n" + 
	        "<values z=\"10\">\n" + 
	        "    <xyLength memory=\"1048576000\">520</xyLength>\n" + 
	        "    <xyLength memory=\"2097152000\">550</xyLength>\n" + 
	        "    <xyLength memory=\"3110076416\">930</xyLength>\n" + 
	        "    <xyLength memory=\"3145728000\">940</xyLength>\n" + 
	        "    <xyLength memory=\"4194304000\">1150</xyLength>\n" + 
	        "    <xyLength memory=\"5242880000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"6291456000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"7340032000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"8388608000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"9437184000\">1240</xyLength>\n" + 
	        "    <xyLength memory=\"10485760000\">1980</xyLength>\n" + 
	        "    <xyLength memory=\"11534336000\">2100</xyLength>\n" + 
	        "    <xyLength memory=\"12582912000\">2180</xyLength>\n" + 
	        "    <xyLength memory=\"13631488000\">2270</xyLength>\n" + 
	        "    <xyLength memory=\"14680064000\">2360</xyLength>\n" + 
	        "    <xyLength memory=\"15728640000\">2440</xyLength>\n" + 
	        "  </values>\n" + 
	        "<efficientLengths>\n" + 
	        "	<effLength start=\"190\">380</effLength>\n" + 
	        "	<effLength start=\"400\">430</effLength>\n" + 
	        "	<effLength start=\"460\">500</effLength>\n" + 
	        "	<effLength start=\"540\">920</effLength>\n" + 
	        "	<effLength start=\"1240\">1920</effLength>\n" + 
	        "  </efficientLengths>\n" + 
	        "</wplAuto>";
	        
	        fr = null;
	        File wplSettings = new File(stereoMateSettings.getParent() + File.separator + "WPL AUTO Stats.xml");
	        try {
	            fr = new FileWriter(wplSettings);
	            fr.write(content);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }finally{
	            //close resources
	            try {
	                fr.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
		
	}
	

}// end class
