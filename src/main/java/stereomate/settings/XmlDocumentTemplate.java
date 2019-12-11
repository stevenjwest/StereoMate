package stereomate.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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

import ij.IJ;

public class XmlDocumentTemplate {
	
	/**
	 * Title of the Xml Document.
	 */
	String documentTitle;
	
	/**
	 * ArrayList of xmlElements.  This is the top of the tree of XmlElementTemplate objects, and each
	 * element in this array list is 
	 */
	ArrayList<XmlElementTemplate> xmlElements;
	
	// Document xmlDoc;
	
	public XmlDocumentTemplate(String title) {
		
		this.documentTitle = title;
		
		xmlElements = new ArrayList<XmlElementTemplate>();
		
	}
	
	
	/**
	 * Load the XML Document into the parentDirectory specified.  The title of this XML Document will be
	 * documentTitle, which was set during the construction of this object.
	 * <p>
	 * This will retrieve the content (text content and attribute titles & values)
	 */
	public void loadXmlDoc(File parentDirectory) {
		
		File file = new File(parentDirectory.getAbsolutePath() + File.separator + documentTitle);

		// Load the XML Document from omProceduresFile:

		//Here, the InputStream is used inside appropriate try... catch statements:
		InputStream in = null;

		try {

			in = new FileInputStream(file);

			//Once an InputStream is established, next build the DOM Document:

			//generate Document Builder:
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			Document xmlDoc = dBuilder.parse(in);

			in.close();

			// NEXT - load all of the settings from this document:

			// first, normalise the document:
			xmlDoc.getDocumentElement().normalize();
			////IJ.showMessage("Root Element: "+ doc.getDocumentElement().getNodeName() );

			// loop through each element to retrieve its content:
			for(int a=0; a<xmlElements.size(); a++) {
				xmlElements.get(a).retrieveContent(xmlDoc);
				// note, this recursively searches all child elements to collect its content too!
			}		

		} 
		catch (FileNotFoundException e1) {
			e1.printStackTrace();
			IJ.error("unable to locate xml file: "+documentTitle);
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
			IJ.error("ParserConfiguration error with xml file: "+documentTitle);
			//close InputStream
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
				IJ.error("unable to close file input stream for xml file: "+documentTitle);
			}
		}
		catch (SAXException | IOException e) {
			e.printStackTrace();
			IJ.error("unable to parse file input stream for xml file: "+documentTitle);
			//close InputStream
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
				IJ.error("unable to close file input stream for xml file: "+documentTitle);
			}
		}

	}
	
	
	/**
	 * Save the XML Document into the parentDirectory specified.  The title of this XML Document will be
	 * documentTitle, which was set during the construction of this object.
	 * @param file
	 * @throws Exception
	 */
	public void saveXmlDoc(File parentDirectory) throws Exception {
		
		//want to save the procedureStack XML file to the file:
		//write the contents of procedureStack to a new XML Document:

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root element - procedure Stack:
		Document doc = docBuilder.newDocument();
		
		// append all XmlElementTemplate elements to the doc:
		for(int a=0; a<xmlElements.size(); a++) {
			xmlElements.get(a).appendElement(doc);
		}

		File file = new File(parentDirectory.getAbsolutePath() + File.separator + documentTitle);

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult( file );

		//ensure XML file is formatted for human reading:
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);

		transformer.transform(source, result);

	}
	
}
