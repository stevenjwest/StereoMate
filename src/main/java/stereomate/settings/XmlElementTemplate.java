package stereomate.settings;

import java.util.ArrayList;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Represents an element that can be inserted into an XmlDocument.  The Element consists of
 * a String title, a possible String attribute, and a possible String content.  The content
 * can be either a String, Double, Integer, Boolean, etc.  Any primitive type.  Therefore,
 * FLAGS for all primitive types are defined as static variables in this template, and this
 * is set to the contentType variable when an XmlElement is constructed.
 * <p>
 * NB: This is a prime place for forming an abstract class & concrete sub-classes, one for
 * each primitive type.
 * @author stevenwest
 *
 */
public class XmlElementTemplate {

	/**
	 * The XmlElements Title:
	 */
	String title;
	
	/**
	 * The XmlElements Attribute Value and Title:
	 */
	String attribute, attributeTitle;
	
	/**
	 * The XmlElements content value (as a String):
	 */
	String content;
	
	/**
	 * The XmlElement content type - set to one of the named constants.
	 */
	int contentType;
	
	/**
	 * Named Constants to represent the content type that an XmlElement can be set to.
	 */
	public final static int STRING = 0;
	public final static int INT = 1;
	public final static int DOUBLE = 2;
	public final static int BOOLEAN = 3;
	
	/**
	 * ArrayList of XmlElementTemplate Objects. every XmlElementTemplate object has this array list, which
	 * allows for nesting of XmlElementTemplate objects to create a tree of elements.
	 */
	ArrayList<XmlElementTemplate> xmlElements;
	
	/**
	 * XmlElementsTemplate constructor.  Set the title, attribute, attributeTitle and content Strings to 
	 * appropriate values.  If the attribute or content are not to be used, set them to null.  The contentType 
	 * is set to one of the Named Constant Flags present in XmlElementTemplate class: 
	 * <p>
	 * i.e, for content type String, pass: XmlElementTemplate.STRING.
	 * <p>
	 * The elements variable is also initialised here.  The elements variable is an ArrayList of XmlElementTemplate
	 * objects.  This allows an XmlElementTemplate object to hold references to other XmlElementsTemplate objects,
	 * and therefore construct a tree of XmlElementTemplate objects to insert into an XmlDocument.
	 * 
	 * @param title
	 * @param attribute
	 * @param content
	 * @param contentType
	 * @param elements
	 */
	public XmlElementTemplate(String title, String attribute, String attributeTitle, String content, int contentType) {
		
		this.title = title;
		this.attribute = attribute;
		this.attributeTitle = attributeTitle;
		this.content = content;
		this.contentType = contentType;
		
		xmlElements = new ArrayList<XmlElementTemplate>();
		
	}
	
	/**
	 * Add a child XmlElementTemplate object to this XmlElementTemplate array list.
	 * @param x
	 */
	public void addXmlElement(XmlElementTemplate x) {
		xmlElements.add(x);
	}
	
	public void removeXmlElement(int index) {
		xmlElements.remove(index);
	}
	
	public void removeXmlElement(XmlElementTemplate x) {
		xmlElements.remove(x);
	}
	
	/**
	 * Append the XmlElementTemplate as the root element to the doc.  This method will also
	 * add any XmlElementTemplate objects in the xmlElements Arraylist as child elements to itself iteratively.
	 * @param doc
	 */
	public void appendElement(Document doc) {
		// first append this element:
		Element e = doc.createElement(title);
		if(attribute != null && attributeTitle != null) {
			 Attr att = doc.createAttribute(attributeTitle);
        	att.setValue(attribute);
        	e.setAttributeNode(att);
		 }
		 if(content != null) {
			 e.appendChild(doc.createTextNode(content));
		 }
		doc.appendChild(e);
		
		//then append any child elements:
		for(int a=0; a<xmlElements.size(); a++) {
			xmlElements.get(a).appendElement(doc, e);
		}
	}
	
	/**
	 * Append the XmlElementTemplate as a child element to Element e, in Document doc. This method will also
	 * add any XmlElementTemplate objects in the xmlElements Arraylist as child elements to itself iteratively.
	 * @param doc
	 * @param e
	 */
	public void appendElement(Document doc, Element e) {

		// first append this element:
		 Element e2 = doc.createElement(title);
		 if(attribute != null && attributeTitle != null) {
			 Attr att = doc.createAttribute(attributeTitle);
         	att.setValue(attribute);
         	e2.setAttributeNode(att);
		 }
		 if(content != null) {
			 e2.appendChild(doc.createTextNode(content));
		 }
         e.appendChild(e2);
         
         // then append any child elements:
         for(int a=0; a<xmlElements.size(); a++) {
 			xmlElements.get(a).appendElement(doc, e2);
 		}
	}
	
	/**
	 * Retrieve the content for this XmlElement (based on the title variable) from the
	 * passed doc object.
	 * @param doc
	 */
	public void retrieveContent(Document doc) {
		
		// first retrieve this element:
		NodeList nList = doc.getElementsByTagName(title);
		if(attributeTitle != null) {
			// only try to retrieve the attribute if the attributeTitle is set:
			attribute = ((Element)nList.item(0)).getAttribute(attributeTitle);
		}
		content = ((Element)nList.item(0)).getTextContent();
		
		// then retrieve any child elements:
		for(int a=0; a<xmlElements.size(); a++) {
			xmlElements.get(a).retrieveContent(doc);
		}
	}
	
	/**
	 * Returns the content of this XmlElementTemplate.  The content return type will
	 * match the contentType FLAG as set in this Element (either String, int, double, boolean).
	 * @return the content as the content type.
	 */
	public Object getContent() {
		if(contentType == STRING) {
			return (String)content;
		}
		else if(contentType == INT) {
			return Integer.parseInt(content);
		}
		else if(contentType == DOUBLE) {
			return Double.parseDouble(content);
		}
		else if(contentType == BOOLEAN) {
			return Boolean.parseBoolean(content);
		}
		else {
			return null;
		}
	}
	
	/**
	 * Returns the content of this element as a String.
	 * @return the content as a String.
	 */
	public String getContentAsString() {
		return content;
	}
	
	public void setContent(Object o) {
		content = ""+o;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the attribute
	 */
	public String getAttribute() {
		return attribute;
	}

	/**
	 * @param attribute the attribute to set
	 */
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	/**
	 * @return the attributeTitle
	 */
	public String getAttributeTitle() {
		return attributeTitle;
	}

	/**
	 * @param attributeTitle the attributeTitle to set
	 */
	public void setAttributeTitle(String attributeTitle) {
		this.attributeTitle = attributeTitle;
	}

	/**
	 * @return the contentType
	 */
	public int getContentType() {
		return contentType;
	}

	/**
	 * @param contentType the contentType to set
	 */
	public void setContentType(int contentType) {
		this.contentType = contentType;
	}

	/**
	 * @return the xmlElements
	 */
	public ArrayList<XmlElementTemplate> getXmlElements() {
		return xmlElements;
	}

	/**
	 * @param xmlElements the xmlElements to set
	 */
	public void setXmlElements(ArrayList<XmlElementTemplate> xmlElements) {
		this.xmlElements = xmlElements;
	}	
	
}
