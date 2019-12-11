package stereomate.settings;

import java.awt.GridLayout;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import ij.IJ;

/**
 * This class represents an OptionsPanel.  This extends a JPanel to include an array of options to
 * put onto the Panel, and allows retrieval of the options set on the options panel as an appropriate String.
 * @author stevenwest
 *
 */
public class OptionsPanel extends JPanel {

	/**
	 * This contains the title of the command - used for called correct algorithm in IJ.run(imp, cmd, options)
	 */
	public String commandTitle;
	
	/**
	 * ArrayList of all option fields added to this OptionsPanel.
	 */
	public ArrayList<Option> options;
	
	/**
	 * String representing the output of the options fields added.
	 */
	public String optionsString;
	
	/**
	 * This constructor creates a new JPanel object, and initialises the options ArrayList.
	 * @param numericInput
	 * @param textInput
	 */
	public OptionsPanel(String algorithmTitle) {
		super();
		commandTitle = algorithmTitle;
		options = new ArrayList<Option>();
		optionsString = "";
	}
	
	
	
	/**
	 * This method will update the options panel on the imageProcessingPanel, to allow the appropriate
	 * options for the selected algorithm to appear and be edited by the user.  This allows the user to
	 * select appropriate options for the algorithm they have selected.
	 * <p>
	 * This method works on all image processing steps: pre-processing, thresholding & post-processing, to
	 * ensure the correct options are presented for the selected algorithm.
	 * @param algorithmTitle Title of Algorithm - used to decide what options to put onto the panel.
	 * @param customOptions Potential custom options that can be passed.  Must fit with the options for the
	 * algorithm being passed.
	 */
	public void updateOptionsPanel(String algorithmTitle, String customOptions) {
		
		//Set up the optionsPanel according to the selected algorithm & customOptions:

		
		//PRE PROCESSING:				
				
		if(algorithmTitle ==  "Gaussian Blur 3D...") {
			//Add options for the Gaussian Blur 3D algorithm:
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addNumericField("x", Integer.parseInt( extractValue(customOptions, "x") ) );
				this.addNumericField("y", Integer.parseInt( extractValue(customOptions, "y") ) );
				this.addNumericField("z", Integer.parseInt( extractValue(customOptions, "z") ) );
				
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("x", 2);
				this.addNumericField("y", 2);
				this.addNumericField("z", 2);
			}
			
			//"x=2 y=2 z=2"
		}
		
		else if(algorithmTitle ==  "Gaussian Blur...") { 
			//Add options for the Gaussian Blur algorithm:
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addNumericField("sigma", Integer.parseInt( extractValue(customOptions, "sigma") ) );
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("sigma", 2);
			}
			//"sigma=2"
		}
		
		else if(algorithmTitle == "Convolve...") {
			//Add options for the Convolve algorithm:
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addTextArea("text1", extractTextArea(customOptions, "text1") );
				this.addCheckBox("normalize", contains(customOptions, "normalize") );
			}
			else {
				//if null, fill options panel with default options:
				this.addTextArea("text1", "-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 24 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n");
				this.addCheckBox("normalize", false);
			}
			//"text1=[-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 24 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n] normalize"
		}
		
		else if(algorithmTitle == "Median...") {
			//Add options for the Median algorithm:
			if(customOptions != null) {
				this.addNumericField("radius", Integer.parseInt( extractValue(customOptions, "radius") ) );
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("radius", 2);
			}
			//"radius=2"
		}
		
		else if(algorithmTitle == "Mean...") {
			//Add options for the Mean algorithm:
			if(customOptions != null) {
				this.addNumericField("radius", Integer.parseInt( extractValue(customOptions, "radius") ) );
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("radius", 2);
			}
			//"radius=2"
		}
		
		else if(algorithmTitle == "Minimum...") {
			//Add options for the Minimum algorithm:
			if(customOptions != null) {
				this.addNumericField("radius", Integer.parseInt( extractValue(customOptions, "radius") ) );
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("radius", 2);
			}
			//"radius=2"
		}
		
		else if(algorithmTitle == "Maximum...") {
			//Add options for the Maximum algorithm:
			if(customOptions != null) {
				this.addNumericField("radius", Integer.parseInt( extractValue(customOptions, "radius") ) );
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("radius", 2);
			}
			//"radius=2"
		}
		
		else if(algorithmTitle == "Unsharp Mask...") {
			//Add options for the Unsharp Mask algorithm:
			if(customOptions != null) {
				this.addNumericField("radius", Integer.parseInt( extractValue(customOptions, "radius") ) );
				this.addNumericField("mask", Integer.parseInt( extractValue(customOptions, "mask") ) );
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("radius", 1);
				this.addNumericField("mask", 0.60);
			}
			
			//"radius=1 mask=0.60"
		}
		
		else if(algorithmTitle == "Variance...") {
			//Add options for the Variance algorithm:
			if(customOptions != null) {
				this.addNumericField("radius", Integer.parseInt( extractValue(customOptions, "radius") ) );
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("radius", 2);
			}
			//"radius=2"
		}
		
		else if(algorithmTitle == "Median 3D...") {
			//Add options for the Median 3D algorithm:
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addNumericField("x", Integer.parseInt( extractValue(customOptions, "x") ) );
				this.addNumericField("y", Integer.parseInt( extractValue(customOptions, "y") ) );
				this.addNumericField("z", Integer.parseInt( extractValue(customOptions, "z") ) );
				
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("x", 2);
				this.addNumericField("y", 2);
				this.addNumericField("z", 2);
			}
			//"x=2 y=2 z=2"
		}
		
		else if(algorithmTitle == "Mean 3D...") {
			//Add options for the Mean 3D algorithm:
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addNumericField("x", Integer.parseInt( extractValue(customOptions, "x") ) );
				this.addNumericField("y", Integer.parseInt( extractValue(customOptions, "y") ) );
				this.addNumericField("z", Integer.parseInt( extractValue(customOptions, "z") ) );
				
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("x", 2);
				this.addNumericField("y", 2);
				this.addNumericField("z", 2);
			}
			//"x=2 y=2 z=2"
		}
		
		else if(algorithmTitle == "Minimum 3D...") {
			//Add options for the Minimum 3D algorithm:
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addNumericField("x", Integer.parseInt( extractValue(customOptions, "x") ) );
				this.addNumericField("y", Integer.parseInt( extractValue(customOptions, "y") ) );
				this.addNumericField("z", Integer.parseInt( extractValue(customOptions, "z") ) );
				
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("x", 2);
				this.addNumericField("y", 2);
				this.addNumericField("z", 2);
			}
			//"x=2 y=2 z=2"
		}
		
		else if(algorithmTitle == "Maximum 3D...") {
			//Add options for the Maximum 3D algorithm:
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addNumericField("x", Integer.parseInt( extractValue(customOptions, "x") ) );
				this.addNumericField("y", Integer.parseInt( extractValue(customOptions, "y") ) );
				this.addNumericField("z", Integer.parseInt( extractValue(customOptions, "z") ) );
				
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("x", 2);
				this.addNumericField("y", 2);
				this.addNumericField("z", 2);
			}
			//"x=2 y=2 z=2"
		}
		
		else if(algorithmTitle == "Variance 3D...") {
			//Add options for the Variance 3D algorithm:
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addNumericField("x", Integer.parseInt( extractValue(customOptions, "x") ) );
				this.addNumericField("y", Integer.parseInt( extractValue(customOptions, "y") ) );
				this.addNumericField("z", Integer.parseInt( extractValue(customOptions, "z") ) );
				
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("x", 2);
				this.addNumericField("y", 2);
				this.addNumericField("z", 2);
			}
			//"x=2 y=2 z=2"
		}
		
		else if(algorithmTitle == "Kuwahara Filter") {
			//Add options for the Kuwahara Filter algorithm:
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addNumericField("sampling", Integer.parseInt( extractValue(customOptions, "sampling") ) );
				this.addCheckBox("stack", contains(customOptions, "stack") );
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("sampling", 5);
				this.addCheckBox("stack",  true);
			}
			//"sampling=5 stack"
		}
		
		else if(algorithmTitle == "Linear Kuwahara") {
			//Add options for the Linear Kuwahara algorithm:
			String[] comboBoxStrings = new String[] { "Variance / Mean^2", "Variance / Mean", "Variance" };
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addNumericField("number_of_angles", Integer.parseInt( extractValue(customOptions, "number_of_angles") ) );
				this.addNumericField("line_length", Integer.parseInt( extractValue(customOptions, "line_length") ) );
					//ensure the array is made so the substrings are deeper in comboBoxStrings than its superString..
				this.addComboBox("criterion", findString(customOptions, comboBoxStrings ), comboBoxStrings, false );
			}
			else {
				//if null, fill options panel with default options:
				this.addNumericField("number_of_angles", 30);
				this.addNumericField("line_length", 11);
				this.addComboBox("criterion", "Variance", comboBoxStrings, false );
			}
			//"number_of_angles=30 line_length=11 criterion=Variance"
		}
		
		else if(algorithmTitle == "8-bit") {
			//Add options for the 8-bit algorithm:
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				//none to add for this algorithm!
			}
			else {
				//if null, fill options panel with default options:
					//none to add for this algorithm!
			}
			//IJ.run(imp, "8-bit", "");
		}
		
		// IJ.run(imp, "Gaussian Blur 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Gaussian Blur...", "sigma=2");
		// IJ.run(imp, "Convolve...", "text1=[-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 24 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n] normalize");
		// IJ.run(imp, "Median...", "radius=2");
		// IJ.run(imp, "Mean...", "radius=2");
		// IJ.run(imp, "Minimum...", "radius=2");
		// IJ.run(imp, "Maximum...", "radius=2");
		// IJ.run(imp, "Unsharp Mask...", "radius=1 mask=0.60");
		// IJ.run(imp, "Variance...", "radius=2");
		// IJ.run(imp, "Median 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Mean 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Minimum 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Maximum 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Variance 3D...", "x=2 y=2 z=2");
		// IJ.run(imp, "Kuwahara Filter", "sampling=5 stack");
		// IJ.run(imp, "Linear Kuwahara", "number_of_angles=30 line_length=11 criterion=Variance");
		// IJ.run(imp, "8-bit", "");
		
		
		
		// THRESHOLDING:
		
		else if(algorithmTitle == "Auto Threshold") {
			//Add options for the Auto Threshold algorithm:
			String[] comboBoxStrings = new String[] { "Otsu", "Intermodes", "Huang", "IsoData", "Li", "MaxEntropy",
					"Mean", "MinError(I)", "Minimum", "Moments", "Percentile", "RenyiEntropy", "Shanbhag", 
					"Triangle", "Yen" };
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				//ensure the array is made so the substrings are deeper in comboBoxStrings than its superString..
				this.addComboBox("method", findString(customOptions, comboBoxStrings ), comboBoxStrings, true );
				this.addBlankOption("white");
				this.addBlankOption("stack");
				this.addCheckBox("use_stack_histogram", contains(customOptions, "use_stack_histogram") );
			}
			else {
				//if null, fill options panel with default options:
				this.addComboBox("method", "Otsu", comboBoxStrings, true);
				this.addBlankOption("white");
				this.addBlankOption("stack");
				this.addCheckBox("use_stack_histogram",  false);
			}
			//"method=Otsu white stack use_stack_histogram");
		}
		
		
		// IJ.run(imp, "Auto Threshold", "method=Otsu white");
		
		// IJ.run(imp, "Auto Threshold", "method=Default white stack use_stack_histogram");
		// IJ.run(imp, "Auto Threshold", "method=Intermodes white stack use_stack_histogram");
		// IJ.run(imp, "Auto Threshold", "method=Huang white stack use_stack_histogram");
		// IJ.run(imp, "Auto Threshold", "method=IsoData white");
		// IJ.run(imp, "Auto Threshold", "method=Li white");
		// IJ.run(imp, "Auto Threshold", "method=MaxEntropy white");
		// IJ.run(imp, "Auto Threshold", "method=Mean white");
		// IJ.run(imp, "Auto Threshold", "method=MinError(I) white");
		// IJ.run(imp, "Auto Threshold", "method=Minimum white");
		// IJ.run(imp, "Auto Threshold", "method=Moments white");
		// IJ.run(imp, "Auto Threshold", "method=Percentile white");
		// IJ.run(imp, "Auto Threshold", "method=RenyiEntropy white");
		// IJ.run(imp, "Auto Threshold", "method=Shanbhag white");
		// IJ.run(imp, "Auto Threshold", "method=Triangle white");
		// IJ.run(imp, "Auto Threshold", "method=Yen white");
		
					// All the Auto Threshold Algorithms -> ALWAYS use options: white stack
														// USER SHOULD CHOOSE:  use_stack_histogram
							//If image is not a stack -> DO NOT USE stack or allow selection of use_stack_histogram

		
		//POST-PROCESSING:

		else if(algorithmTitle == "Erode") {
			//Add options for the Auto Threshold algorithm:
				//none to add!
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addBlankOption("stack");
			}
			else {
				//if null, fill options panel with default options:
				this.addBlankOption("stack");
			}
			// IJ.run(imp, "Erode", "stack");
		}
		
		
		else if(algorithmTitle == "Dilate") {
			//Add options for the Auto Threshold algorithm:
				//none to add!
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addBlankOption("stack");
			}
			else {
				//if null, fill options panel with default options:
				this.addBlankOption("stack");
			}
			// IJ.run(imp, "Dilate", "stack");
		}
		
		
		else if(algorithmTitle == "Open") {
			//Add options for the Auto Threshold algorithm:
				//none to add!
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addBlankOption("stack");
			}
			else {
				//if null, fill options panel with default options:
				this.addBlankOption("stack");
			}
			// IJ.run(imp, "Open", "stack");
		}
		
		
		else if(algorithmTitle == "Close-") {
			//Add options for the Auto Threshold algorithm:
				//none to add!
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addBlankOption("stack");
			}
			else {
				//if null, fill options panel with default options:
				this.addBlankOption("stack");
			}
			// IJ.run(imp, "Close-", "stack");
		}
		
		
		else if(algorithmTitle == "Outline") {
			//Add options for the Auto Threshold algorithm:
				//none to add!
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addBlankOption("stack");
			}
			else {
				//if null, fill options panel with default options:
				this.addBlankOption("stack");
			}
			// IJ.run(imp, "Outline", "stack");
		}
		
		
		else if(algorithmTitle == "Fill Holes") {
			//Add options for the Auto Threshold algorithm:
				//none to add!
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addBlankOption("stack");
			}
			else {
				//if null, fill options panel with default options:
				this.addBlankOption("stack");
			}
			// IJ.run(imp, "Fill Holes", "stack");
		}
		
		
		else if(algorithmTitle == "Skeletonize") {
			//Add options for the Auto Threshold algorithm:
				//none to add!
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addBlankOption("stack");
			}
			else {
				//if null, fill options panel with default options:
				this.addBlankOption("stack");
			}
			// IJ.run(imp, "Skeletonize", "stack");
		}
		
		
		else if(algorithmTitle == "Watershed") {
			//Add options for the Auto Threshold algorithm:
				//none to add!
			if(customOptions != null) {
				//extract the options and fill the fields with these options:
				this.addBlankOption("stack");
			}
			else {
				//if null, fill options panel with default options:
				this.addBlankOption("stack");
			}
			// IJ.run(imp, "Watershed", "stack");
		}
		
		
			// IJ.run(imp, "Erode", "stack");
			// IJ.run(imp, "Dilate", "stack");
			// 	IJ.run(imp, "Open", "stack");
			// IJ.run(imp, "Close-", "stack");
			// IJ.run(imp, "Outline", "stack");
			// IJ.run(imp, "Fill Holes", "stack");
			// IJ.run(imp, "Skeletonize", "stack");
		
			// IJ.run(imp, "Watershed", "stack");
				
		
		//CUSTOM:
		
		else if(algorithmTitle == "Custom") {
			//Add options for the Gaussian Blur 3D algorithm:
			this.addTextField("command", "");
			this.addTextField("options", "");			
				//This is a special instance of options panel, and will deal with this differently
					//to retrieve the command and options lines for running in the IJ.run(imp, cmd, opt) method..
		}
		
		
		//Finally, need to run the addOptions method to add the options to the optionsPanel! 
		this.addOptions();
		
	}//end updateOptionsPanel(String)



	/**
	 * Returns the array item which fully matches a String in customOptions.  If any of the Strings in 
	 * array are a substring of another item, make sure this String is DEEPER in the array than the String
	 * that contains it!  Returns null if array is empty.
	 * @param customOptions
	 * @param array
	 * @return
	 */
	public String findString(String customOptions, String[] array ) {
		
		//return null if array is empty:
		if(array.length == 0) {
			return null;
		}
		
		//loop through array:
		for(int a=0; a<array.length; a++) {
			//collect index of string 'array[a]' in customOptions
			if(customOptions.indexOf(array[a]) >= 0 ) {
				//If index is 0 or above, have found the array items in customOptions.
				//Just return this item - there are no '[' or ']' in the array!
				return array[a];
			}
		}
		
		//if no String is found, then just return the first value in array:
		
		return array[0];
		
	}



	/**
	 * Returns true if the customOptions string contains the field String.
	 * @param customOptions
	 * @param field
	 * @return
	 */
	public boolean contains(String customOptions, String field) {
		//IJ.showMessage("contains: "+customOptions.indexOf(field) );
		if(customOptions.indexOf(field) >= 0 ) {
			//IJ.showMessage("Contains: TRUE");
			return true;
		}
		else {
			return false;
		}
	}



	/**
	 * Returns the TextArea value from the customOptions String, which is associated with the fieldTitle.
	 * TextArea values exist as: [title]=[[val1] [val2] [val3]], after title, the value String is between '[' and
	 * ']'.  Therefore, this method extracts the String between these characters immediately after the fieldTitle.
	 * @param customOptions
	 * @param fieldTitle
	 * @return
	 */
	public String extractTextArea(String customOptions, String fieldTitle) {
		//"text1=[-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 24 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n] normalize"
			//need to extract String between the [ ] for the value text1.
		String value = "";
		
		//first isolate the field title & its value + any extra characters in the String after it:
		value = customOptions.substring( customOptions.indexOf(fieldTitle) );
		
		//Now, the String is always [title]=[[val1] [val2] [val3]] -> a String enclosed by '[' and ']'
			//So extract the String BETWEEN these two Characters:
		value = value.substring(value.indexOf("[")+1, value.indexOf("]"));
			//this will always retrieve the FIRST index of these characters, so will extract the FIRSt string surrounded by
				//'[' and ']', which is the correct string to extract..
		
		//return the value:
		return value;
	}




	/**
	 * This method will extract teh value from the customOptions String which relates to the fieldTitle. For example,
	 * if the custom string is "x=1 y=2 z=3", and fieldTitle is "x", this methofd will return "1".
	 * @param customOptions String containing the fields and their values.  Each field is separated by a space, except the
	 * last field which reaches the end of the String.
	 * @param fieldTitle The title of the field which the value is to be extracted from.
	 * @return The value of the field.
	 */
	public String extractValue(String customOptions, String fieldTitle) {
		String value = "";
	
		int titleIndex = customOptions.indexOf(fieldTitle);
	
		//first isolate the field title & its value + any extra characters in the String after it:
		value = customOptions.substring( customOptions.indexOf(fieldTitle) );
	
		//Now, the String is either [title]=[value][END OF STRING] (if the fieldTitle is the last field in this customOptions string
		//Or, the String is [title]=[value] [nextTitle]=[value] etc.
		//The difference is the SPACE!  If the string contains a space, THEN extract from the '=' to the Space
			//Else, extract from the '=' to the end of the String:
		if(value.indexOf(" ") >= 0 ) {
			//The String is in the form -> [title]=[value] [nextTitle]=[value] etc.
				//Extract from AFTER the "=" to the SPACE:
			value = value.substring( value.indexOf("=")+1, value.indexOf(" ") );
		
		}
		else {
			//else the String is in the form -> [title]=[value][END OF STRING]
				//Extract from AFTER the "=" to the END OF STRING:
			value = value.substring(value.indexOf("=")+1); //no endIndex, extract to end of string.
		
		}
	
		//IJ.showMessage("Extracted Field - title: "+fieldTitle+".");
	
		//IJ.showMessage("Extracted Field - value: "+value+".");
	
	
		return value;
	}

	
	public void addBlankOption(String option) {
		options.add( new Option(null, null, option) );
	}
	
	
	public void addNumericField(String fieldTitle, int initialValue) {
		options.add( new Option("textfield", fieldTitle, initialValue) );
	}
	
	
	public void addNumericField(String fieldTitle, double initialValue) {
		options.add( new Option("textfield", fieldTitle, initialValue) );
	}
	
	
	
	public void addTextField(String fieldTitle, String initialValue) {
		options.add( new Option("textfield", fieldTitle, initialValue) );
	}
	
	
	
	public void addTextArea(String fieldTitle, String initialValue) {
		options.add( new Option("textarea", fieldTitle, initialValue) );
	}
	
	
	public void addCheckBox(String boxTitle, boolean initialValue) {
		options.add( new Option("checkbox", boxTitle, initialValue) );
	}
	
	public void addComboBox(String boxTitle, String initialValue, String[] potentialValues, boolean titleEqualsString) {
		options.add( new Option("combobox", boxTitle, initialValue, potentialValues, titleEqualsString) );
	}
	
	public void addOptions() {
		// set layout to gridbaglayout:
		
		for(int a=0; a<options.size(); a++) {
			//Just need to add the panel, which contains all necessary components:
			//only add if not null:
			if(options.get(a).panel != null) {
				this.add( options.get(a).panel );
			}
		}
	}
	
	
	/**
	 * This returns the options string associated with the set of options set.  It returns the
	 * optionsTitle, followed by a "=" followed by the optionsValue. Each set of these are separated by a " "
	 * character, except the last option title-value pair.
	 * @return
	 */
	public String getOptionsString() {
		//First, generate the optionsString from the options array:
		optionsString = "";
		for(int a=0; a<options.size(); a++) {
			optionsString = optionsString + options.get(a).returnValue();
			optionsString = optionsString + " ";
		}
		
		//remove the last " " character inserted into optionsString:
			//only if options have been inserted - i.e if optionsString length is above 0!:
		if(optionsString.length() > 0) {
			optionsString = optionsString.substring( 0, optionsString.length()-1 );
		}
		
		//Finally, return the optionsString:
		return optionsString;
	}
	
	
	public class Option implements ActionListener, DocumentListener {
		JComponent field;
		JLabel fieldTitle;
		
		String fieldTitleString;
		String fieldValue;
		
		JPanel panel;
		
		boolean titleEqualsString;
		
		/**
		 * A constructor to make a text JTextField or JTextArea.
		 * @param component
		 * @param title
		 * @param value
		 */
		public Option(String component, String title, String value) {
						
			fieldTitleString = title;
			fieldValue = value;
			
			if(component == "textfield") {
				fieldTitle = new JLabel(title);
				
				field = new JTextField(value, 20);
				((JTextField)field).addActionListener(this);
				
				panel = new JPanel();
				panel.setLayout( new GridLayout(0,1) );
				
				panel.add(fieldTitle);
				panel.add(field);
			}
			
			if(component == "textarea") {
				fieldTitle = new JLabel(title);
				
				field = new JTextArea(value);
				((JTextArea)field).getDocument().addDocumentListener(this);
				
				panel = new JPanel();
				panel.setLayout( new GridLayout(1,0) );
				
				panel.add(fieldTitle);
				panel.add(field);
			}
			
			if(component == null) {
				//special case where no component is inserted, but a String is still appended to the fieldTitle & Value:
				field = null;
				fieldTitle = null;
				panel = null; //As panel is null, it will not be inserted into OptionsPanel.
			}
			
		}
		
		/**
		 * A constuctor to make a numeric JTextField.
		 * @param component
		 * @param title
		 * @param value
		 */
		public Option(String component, String title, int value) {
			
			fieldTitle = new JLabel(title);
			
			fieldTitleString = title;
			fieldValue = ""+value;
			
			if(component == "textfield") {
				field = new JTextField( (""+value), 4 );
				((JTextField)field).addActionListener(this);
			}
			
			panel = new JPanel();
			panel.setLayout( new GridLayout(1,0) );
			
			panel.add(fieldTitle);
			panel.add(field);
			
		}
		
		
		/**
		 * A constuctor to make a numeric JTextField with a double
		 * @param component
		 * @param title
		 * @param value
		 */
		public Option(String component, String title, double value) {
			
			fieldTitle = new JLabel(title);
			
			fieldTitleString = title;
			fieldValue = ""+value;
			
			if(component == "textfield") {
				field = new JTextField( (""+value), 4 );
				((JTextField)field).addActionListener(this);
			}
			
			panel = new JPanel();
			panel.setLayout( new GridLayout(1,0) );
			
			panel.add(fieldTitle);
			panel.add(field);
			
		}
		
		/**
		 * Constructor to make a JCheckBox.
		 * @param component
		 * @param title
		 * @param value
		 */
		public Option(String component, String title, boolean value) {
			
			fieldTitle = new JLabel(title);
			
			fieldTitleString = title;
			fieldValue = ""+value;
			
			if(component == "checkbox") {
				field = new JCheckBox(title, value);
				((JCheckBox)field).addActionListener(this);
			}
			
			panel = new JPanel();
			panel.setLayout( new GridLayout(1,0) );
			
			panel.add(field);
			
		}
		
		
		public Option(String component, String title, String initialValue, String[] potentialValues, boolean titleEqualsString) {
			
			this.titleEqualsString = titleEqualsString;
			
			fieldTitle = new JLabel(title);
			
			fieldTitleString = title;
			fieldValue = ""+initialValue;
			
			if(component == "combobox") {
				field = new JComboBox<String>(potentialValues);
				((JComboBox<String>)field).addActionListener(this);
				((JComboBox<String>)field).setSelectedIndex( arrayIndexOf(initialValue, potentialValues) );
			}
			
			
			
			panel = new JPanel();
			panel.setLayout( new GridLayout(1,0) );
			
			panel.add(fieldTitle);
			panel.add(field);
			
		}

		/**
		 * This listens for events on the field component, and updates the fieldValue appropriately.
		 * @param e
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
			String content = "";
						
			JComponent c = (JComponent)e.getSource();
			
			if(c instanceof JTextField) {
				content = ((JTextField)c).getText();
			}
			
			else if(c instanceof JTextArea) {
				content = ((JTextArea)c).getText();
			}
			
			else if(c instanceof JComboBox ) {
				content = (String)((JComboBox<String>)c).getSelectedItem();
			}
			
			else if(c instanceof JCheckBox) {
				if( ((JCheckBox)c).isSelected() ) {
					//set content to the fieldValue - this must be inserted 
					content= this.fieldTitle.getText();
				}
				else {
					content = "";
				}
			}
			
			else if(c == null) {
				//if null want to keep fieldValue to its initial pre-set value:
					//so set content to fieldValue:
				content = fieldValue;
			}
						
			//Set content to the fieldValue:
			this.fieldValue = content;
			
		} //end actionPerformed(e)
		
		
		/**
		 * Returns the array index of instance of the string 'initialValue' in String array 'potentialValues'.
		 * If string is not found in array, returns -1.
		 * @param initialValue
		 * @param potentialValues
		 * @return
		 */
		public int arrayIndexOf(String initialValue, String[] potentialValues) {
			
			for(int a=0; a<potentialValues.length; a++) {
				if(potentialValues[a].equals(initialValue) ) {
					return a;
				}
			}
						
			//if String is not found, return -1
			
			return -1;
		}
		
		
		public String returnValue() {
			
			//UPDATE the fieldValue:
			
			String content = "";
						
			
			if(field instanceof JTextField) {
				content = ((JTextField)field).getText();
			}
			
			else if(field instanceof JTextArea) {
				content = ((JTextArea)field).getText();
			}
			
			else if(field instanceof JComboBox ) {
				content = (String)((JComboBox<String>)field).getSelectedItem();
			}
			
			else if(field instanceof JCheckBox) {
				if( ((JCheckBox)field).isSelected() ) {
					//set content to the fieldValue - this must be inserted 
					content= fieldTitle.getText();
				}
				else {
					content = "";
				}
			}
			else if(field == null) {
				//if field is null, just want to use the fieldValue as it is!
					//So here just set content to fieldValue:
				content = fieldValue;
			}
						
			//Set content to the fieldValue:
			fieldValue = content;
			
			
			if(field instanceof JCheckBox) {
				//if this is a checkbox, only need to return the fieldValue - the string
				//only requires the field value in it, and no title or "=" character.
				return fieldValue;
			}
			if(field instanceof JComboBox) {
				//if this is a checkbox, only need to return the fieldValue If titleEqualsString is FALSE
					//- the string only requires the field value in it, and no title or "=" character.
				if(titleEqualsString == false ) {
					return fieldValue;
				}
				else {
					//else need to return the fieldTitle + "=" + fieldValue:
					return fieldTitleString+"="+fieldValue;
				}
			}
			else if(fieldValue.indexOf(" ") >= 0 ) {
				//if fieldValue contains one of more spaces, need to contain it in square brackets - [ & ]
				fieldValue = "["+fieldValue+"]";
				return fieldTitleString+"="+fieldValue;
			}
			else if(field == null) {
				//if field is null, just need to return the fieldValue:
				return fieldValue;
			}
			else {
				//else just return the fieldTitle + "=" + fieldValue:
				return fieldTitleString+"="+fieldValue;
			}
			
		}
		
		
		

		@Override
		public void insertUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub
			try {
				//IJ.showMessage("insUpdate");
				String txt = e.getDocument().getText(0, e.getDocument().getLength() );
				fieldValue = txt;
				
			} catch (BadLocationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub
			try {
				//IJ.showMessage("remUpdate");
				String txt = e.getDocument().getText(0, e.getDocument().getLength() );
				fieldValue = txt;
				
			} catch (BadLocationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		
		
	}
	
}
