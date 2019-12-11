package stereomate.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class represents the Object Manager procedure Settings.  It includes the data on
 * the Exclusion Zone dimensions & information on the low/high- pass filter which can be
 * set on a single measurement.
 * <p>
 * This class includes useful code for loading and saving the Procedure Settings file, and
 * for altering the instance variables stored in this class.
 * 
 * @author stevenwest
 *
 */
public class OM_ProcedureSettings extends XmlDocumentTemplate {
	
	public static final String WHOLEOBJECT = "Whole Object", OBJECTFRAGMENT = "Object Fragment";
	
	public static final String FILTEROVERCLASSIFIER = "true", NOFILTEROVERCLASSIFIER = "false";
	
	public static final String FILTERMAXVALREACHED = "true", FILTERMAXVALNOTREACHED = "false";
	
	public static final String CONNECTED6 = "6-connected", CONNECTED18 = "18-connected", CONNECTED26 = "26-connected";
	
	/**
	 * STRING Instance variable representing the XML Element for the selection of the Object Assessment Mode.
	 */
	public XmlElementTemplate objectAssessmentMode;	
	
	/**
	 * STRING Instance variable representing the XML Element for the selection of the Object Assessment Mode.
	 */
	public XmlElementTemplate applyFilterOverClassifier;
	
	/**
	 * STRING Instance variable representing the XML Element for the selection of the Object Connectivity.
	 */
	public XmlElementTemplate objectConnectivity;
	
	/**
	 * INT Instance variables representing the XML Elements for the Max Object dimensions (95% max in X Y Z).
	 */
	public XmlElementTemplate maxX, maxY, maxZ;
	
	/**
	 * INT Instance variables representing the XML Elements for the high and low pass filter settings.
	 */
	public XmlElementTemplate filterIndexVal;
	
	/**
	 * DOUBLE Instance variables representing the XML Elements for the high and low pass filter settings.
	 */
	public XmlElementTemplate filterMinVal, filterMaxVal, filterMaxValReached;
	
	/**
	 * Default constructor, which labels the Settings file as "OM_ProcedureSettings.xml"
	 * by default.
	 */
	public OM_ProcedureSettings() {
		this("OM_ProcedureSettings.xml");
	}
	
	/**
	 * OM_ProcedureSettings constructor - this sets the OM_ProcedureSettings file title to title
	 * and constructs the XML Elements with their default settings.  These can either then be set in the program,
	 * or set from a previous OM_ProcedureSettings file by loading that file.
	 */
	public OM_ProcedureSettings(String title) {
		
		// call the superclass constructor:
			// to set the title of this document and construct the xmlElements array list:
		super(title);
		
		// Create the Xml Elements for this Document:
		xmlElements.add( new XmlElementTemplate("procedureSettings",null,null,null,XmlElementTemplate.STRING) );
		
			
			objectAssessmentMode = new XmlElementTemplate("objectAssessmentMode",null,null,"0",XmlElementTemplate.STRING);
			xmlElements.get(0).addXmlElement( objectAssessmentMode );
			
			applyFilterOverClassifier = new XmlElementTemplate("applyFilterOverClassifier",null,null,"0",XmlElementTemplate.STRING);
			xmlElements.get(0).addXmlElement( applyFilterOverClassifier );
			
			objectConnectivity = new XmlElementTemplate("objectConnectivity",null,null,"0",XmlElementTemplate.STRING);
			xmlElements.get(0).addXmlElement( objectConnectivity );
		
			maxX = new XmlElementTemplate("maxX",null,null,"0",XmlElementTemplate.INT);
			xmlElements.get(0).addXmlElement( maxX );
			
			maxY = new XmlElementTemplate("maxY",null,null,"0",XmlElementTemplate.INT);
			xmlElements.get(0).addXmlElement( maxY );
			
			maxZ = new XmlElementTemplate("maxZ",null,null,"0",XmlElementTemplate.INT);
			xmlElements.get(0).addXmlElement( maxZ );
			
			
			filterIndexVal = new XmlElementTemplate("filterIndexVal",null,null,"0",XmlElementTemplate.INT);
			xmlElements.get(0).addXmlElement( filterIndexVal );
			
			filterMinVal = new XmlElementTemplate("filterMinVal",null,null,"0",XmlElementTemplate.DOUBLE);
			xmlElements.get(0).addXmlElement( filterMinVal );
			
			filterMaxVal = new XmlElementTemplate("filterMaxVal",null,null,"0",XmlElementTemplate.DOUBLE);
			xmlElements.get(0).addXmlElement( filterMaxVal );
			
			filterMaxValReached = new XmlElementTemplate("filterMaxValReached",null,null,"0",XmlElementTemplate.STRING);
			xmlElements.get(0).addXmlElement( filterMaxValReached );
			
			
	}
	
	/**
	 * Sets the object assessment mode String - should be set to the Strings in this class:
	 * WHOLEOBJECT, or OBJECTFRAGMENT.
	 * @param objAssessmentMode
	 */
	public void setObjectAssessmentMode(String objAssessmentMode) {
		objectAssessmentMode.setContent( objAssessmentMode);
	}
	
	/**
	 * Sets the applyFilterOverClassifier String - should be set to the Strings in this class:
	 * FILTEROVERCLASSIFIER, or NOFILTEROVERCLASSIFIER.
	 * @param applyFilterOverClassifier
	 */
	public void setApplyFilterOverClassifier(String applyFilterOverClassifier) {
		this.applyFilterOverClassifier.setContent( applyFilterOverClassifier );
	}
	
	/**
	 * Sets the objectConnectivity String - should be set to the Strings in this class:
	 * CONNECTED6, CONNECTED18, or CONNECTED26.
	 * @param objectConnectivity
	 */
	public void setObjectConnectivity(String objectConnectivity) {
		this.objectConnectivity.setContent( objectConnectivity );
	}
	
	/**
	 * Returns true if applyFilterOverClassifier is set to FILTEROVERCLASSIFIER [NAMED CONSTANT in this
	 * class].
	 * @return
	 */
	public boolean isApplyFilterOverClassifier() {
		return ( applyFilterOverClassifier.getContentAsString().equalsIgnoreCase(FILTEROVERCLASSIFIER)  );
	}
	
	/**
	 * Returns true only if the objectAssessmentMode is set to WHOLEOBJECT in om_ProcedureSettings.
	 * @return
	 */
	public boolean isWholeObjectAnalysis() {
		return ( objectAssessmentMode.getContentAsString().equalsIgnoreCase(WHOLEOBJECT)  );
	}
	
	/**
	 * Returns true only if the objectConnectivity is set to CONNECTED6 in om_ProcedureSettings.
	 * @return
	 */
	public boolean isConnected6() {
		return ( objectConnectivity.getContentAsString().equalsIgnoreCase(CONNECTED6)  );
	}
	
	/**
	 * Returns true only if the objectConnectivity is set to CONNECTED18 in om_ProcedureSettings.
	 * @return
	 */
	public boolean isConnected18() {
		return ( objectConnectivity.getContentAsString().equalsIgnoreCase(CONNECTED18)  );
	}
	
	/**
	 * Returns true only if the objectConnectivity is set to CONNECTED26 in om_ProcedureSettings.
	 * @return
	 */
	public boolean isConnected26() {
		return ( objectConnectivity.getContentAsString().equalsIgnoreCase(CONNECTED26)  );
	}
	
	/**
	 * Returns true only if the filterMaxValReached is set to FILTERMAXVALREACHED in om_ProcedureSettings.
	 * @return
	 */
	public boolean isMaxValReached() {
		return ( filterMaxValReached.getContentAsString().equalsIgnoreCase(FILTERMAXVALREACHED)  );
	}
	
	/**
	 * Sets the maxX, maxY, maxZ content to x, y, z.
	 * @param x
	 * @param y
	 * @param z
	 */
	public void setMaxXYZ(int x, int y, int z) {
		maxX.setContent(x);
		maxY.setContent(y);
		maxZ.setContent(z);
	}
	
	public void setFilterValues(int indexVal, double minVal, double maxVal, boolean maxValReached) {
		
		// only set the maxVal and the boolean if its valid to do so:
			// DO NOT set if the original maxValReached is FALSE (as the maxVal and maxValReached encode
				// a legitimate setting of maxVal on the previous data)
				// This is only the case is the passed maxValReached is TRUE, and that maxVal is NOT greater
					// than original maxVal
		
		if( isMaxValReached() == false && maxValReached == true 
				&& ((double)filterMaxVal.getContent() ) > maxVal
				&& ((int)filterIndexVal.getContent() ) == indexVal) {
			// in this case do not change the max val or maxValReached
			// just set minVal (may be same, but dont worry about checking!
			filterMinVal.setContent(minVal);
		}
		else { // either filterMaxVal was adjusted UP to higher maxValReached, or filterIndex was changed
				// either way, need to set all the values for filter:
		
			filterIndexVal.setContent(indexVal);
			filterMinVal.setContent(minVal);
			filterMaxVal.setContent(maxVal);
			if(maxValReached == true) {
				filterMaxValReached.setContent( FILTERMAXVALREACHED );
			}
			else {
				filterMaxValReached.setContent( FILTERMAXVALNOTREACHED );
			}
		}
	}
	/** 
	 * @return objectAssessmentMode content.
	 */
	public String getObjectAssessmentMode() {
		return (String)objectAssessmentMode.getContent();
	}
	/**
	 * @return applyFilterOverClassifier content.
	 */
	public String getApplyFilterOverClassifier() {
		return (String)applyFilterOverClassifier.getContent();
	}
	/**
	 * @return objectConnectivity content.
	 */
	public String getObjectConnectivity() {
		return (String)objectConnectivity.getContent();
	}
	/**
	 * @return maxX content.
	 */
	public int getMaxX() {
		return (int)maxX.getContent();
	}
	/**
	 * @return maxY content.
	 */
	public int getMaxY() {
		return (int)maxY.getContent();
	}
	/**
	 * @return maxZ content.
	 */
	public int getMaxZ() {
		return (int)maxZ.getContent();
	}
	/**
	 * @return filterIndexValue content.
	 */
	public int getFilterIndexValue() {
		return (int)filterIndexVal.getContent();
	}
	/**
	 * @return filterMinValue content.
	 */
	public double getFilterMinValue() {
		return (double)filterMinVal.getContent();
	}
	/**
	 * @return filterMaxValue content.
	 */
	public double getFilterMaxValue() {
		return (double)filterMaxVal.getContent();
	}
	
	/**
	 * Returns the mean of maxX and maxY elements.  Always rounds down.
	 * @return
	 */
	public int getMaxXYMean() {
		// return ( (int)maxX.getContent()  + (int)maxY.getContent() ) / 2 ;
		return ( getMaxX() + getMaxY() ) / 2;
		
	}

}
