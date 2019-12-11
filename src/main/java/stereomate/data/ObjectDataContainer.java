package stereomate.data;

import java.util.ArrayList;

import ij.IJ;
import ij.measure.ResultsTable;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Point3D;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.ImageInt;
import stereomate.object.ObjectVoxelProcessing;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.Remove;

/**
 * This class represents an object which contains the data for all the attributes measured on an object
 * by the MCIB_SM_BorderObjPixCount3D object.  It is used to transfer data from that class to another class,
 * by returning this object from methods inside MCIB_SM_BorderObjPixCount3D.
 * <p>
 * The class also contains static references to Attributes, and static methods for generating new objects of these
 * references to return and insert into Instances objects.
 * @author stevenwest
 *
 */
public class ObjectDataContainer implements DataContainer {
	
	double[] dataValues;
	
	/*
	 * TO ADD NEW ATTRIBUTES:
	 * 
	 * This assumes the measures in ObjectVoxelProcessing class have also been adjusted in line with
	 * 	addition made here...
	 * 
	 *  1. Create attribute variable - a double for all Attribute Types.
	 *  2. Create Attribute Title - a String.
	 *  3. Alter MANUAL_CLASS_INDEX, FILTER_CLASS_INDEX, CLASSIFIER_CLASS_INDEX to correct indices!
	 *  4. Edit dataTitles to contain the new Attribute Titles in the correct position.
	 *  5. Adjust ObjectDataContainer constructor, setDataValues(), setDataValuesAndTitles(), 
	 *  returnObjectMeasuresAttributes(), returnRealNumberAttributeTitles()
	 *  
	 */
	
	//FIRST VOXEL:
	// "x1", "y1", "z1"
	private int x1, y1, z1;
	
	
	//FROM MCIB3D OUTPUT:
	//Obj Counter: "Obj No."
	private int objNo;
	

	//GEOMETRICAL MEASURES:
	// "Vol. Voxels"	"Area Voxels"
	private int volVoxels;		private double areaVoxels;

	//LOCATION -> defines bounding box and central voxel:
		// is it more useful to have the xLength, yLength and zLength of the bounding box?	
	// "xMin", "xMax", "yMin", "yMax", "zMin", "zMax"
	private int xMin, yMin, zMin, xLength, yLength, zLength;

	// "Centre X", "Centre Y", "Centre Z"	
	private double centreX, centreY, centreZ;

	//SHAPE MEASURES:
	// "Compactness"	"Sphericity"	convexVolume	convexSurface	3D solidity		3D convexity
	private double compactness, sphericity, 	volConvex,		surfConvex, 	solidity3D, 	convexity3D;
	
	//"Vol. to Vol. Box"	"Main Elong."	"Median Elong."		"Vol. Ellipse"		"Vol. to Vol. Ellipse"
	private double volToVolBox, 	mainElong,		medianElong, 		volEllipse, 		volToVolEllipse;
	
	// Moments:
	private double homInv1, homInv2, homInv3, homInv4, homInv5;
	private double  geoInv1, geoInv2, geoInv3, geoInv4, geoInv5, geoInv6;
	private double  biocatJ1, biocatJ2, biocatJ3, biocatI1, biocatI2;

	//INTENSITY MEASURES
	//"Mean Pix."	"SD Pix."	"Max Pix."	"Median Pix."	"Min Pix."
	private double meanPix,	sdPix, 		maxPix,		medianPix,		minPix;
	
	
	/**  String NAMED CONSTANTS for Attribute Titles  */
	
	// IMAGE INDEX
	public static final String IMAGEINDEX = "ImageIndex";
	
	// IMAGE INDEX
	public static final String IMAGENAME = "ImageIdentifier";
	
	//FIRST VOXEL:
	// "x1", "y1", "z1"
	public static final String X1 = "x1", Y1 = "y1", Z1 = "z1";
	
	
	//FROM MCIB3D OUTPUT:
	
	//Obj Counter: "Obj No."
	public static final String OBJNO = "ObjNum";



	//GEOMETRICAL MEASURES:
	// "Vol. Voxels", 	"Area Voxels"	
	public static final String VOLVOXELS = "VolVoxels", AREAVOXELS = "AreaVoxels";

	//LOCATION -> defines bounding box:	
	// "xMin", "xMax", "yMin", "yMax", "zMin", "zMax"
	public static final String XMIN = "xMin", XLENGTH = "xLength", 
						YMIN = "yMin", YLENGTH = "yLength", 
						ZMIN = "zMin", ZLENGTH = "zLength";

	//SHAPE MEASURES:	
	// The CENTRE of the object - taking into account its mass:
	// "Centre X", "Centre Y", "Centre Z"
	public static final String CENTREX = "CentreX", CENTREY = "CentreY", CENTREZ = "CentreZ";

	// "Compactness"	"Sphericity"	convexVolume	convexSurface	3D solidity		3D convexity	
	public static final String COMPACTNESS = "Compactness", SPHERICITY = "Sphericity", VOLCONVEX = "ConvexVolume";
	public static final String SURFCONVEX = "ConvexSurface", SOLIDITY3D = "Solidity", CONVEXITY3D = "Convexity";

	//"Vol. to Vol. Box"	"Main Elong."	"Median Elong."		"Vol. Ellipse"		"Vol. to Vol. Ellipse"
	public static final String VOLTOVOLBOX = "VolToVolBox", MAINELONG = "MainElong", MEDIANELONG = "MedianElong";	
	public static final String VOLELLIPSE = "VolEllipse", VOLTOVOLELLIPSE = "VolToVolEllipse";
	
	public static final String HOMINV1 = "HOMINV1", HOMINV2 = "HOMINV2", HOMINV3 = "HOMINV3", 
							   HOMINV4 = "HOMINV4", HOMINV5 = "HOMINV5"; 
	public static final String GEOINV1 = "GEOINV1", GEOINV2 = "GEOINV2", GEOINV3 = "GEOINV3", 
							   GEOINV4 = "GEOINV4", GEOINV5 = "GEOINV5", GEOINV6 = "GEOINV6";
	public static final String BIOCATJ1 = "BIOCATJ1", BIOCATJ2 = "BIOCATJ2", BIOCATJ3 = "BIOCATJ3", 
							   BIOCATI1 = "BIOCATI1", BIOCATI2 = "BIOCATI2";

	//INTENSITY MEASURES
	//"Mean Pix."	"SD Pix."	"Max Pix."	"Median Pix."	"Min Pix."
	public static final String MEANPIX = "MeanPixel", 		SDPIX = "SDPixel", 		MAXPIX = "MaxPixel";
	public static final String MEDIANPIX = "MedianPixel", 	MINPIX = "MinPixel";
	
	
	/** STRINGS named constants representing the Attribute Title Values for Manual, Filter, Classifier Class Attributes */
	
	public static final String 
			MANUALCLASS = "ManualClass";

	public static final String FILTERCLASS = "FilterClass";

	public static final String CLASSIFIERCLASS = "ClassifierClass";
	
	/**	Title for the PROBABILITY Attribute, used to store the output Probability from a Classifier on each Instance. */
	public static final String
			PROBABILITY = "Probability";
	
	/** Named Constants of Attribute Nominal Values */
	
	public final static String 
			UNCLASSIFIEDATR = "Unclassified";

	public static final String FEATUREATR = "Feature";

	public static final String NONFEATUREATR = "Non-Feature";

	public static final String CONNECTEDATR = "Connected";

	public static final String PASSEDATR = "Passed";

	public static final String NOTPASSEDATR = "Not-Passed";
	
	/**
	 * String Arrays which contain the String values for Manual, Filter and Classifier Class Attributes.
	 */
	public static final String[] 
			
			MANUALCLASSVALS = new String[] { UNCLASSIFIEDATR, FEATUREATR, NONFEATUREATR, CONNECTEDATR },
						
			FILTERCLASSVALS = new String[] { PASSEDATR, NOTPASSEDATR, },
						
			CLASSIFIERCLASSVALS = new String[] { UNCLASSIFIEDATR, FEATUREATR, NONFEATUREATR, CONNECTEDATR };
	
	
	/**
	 * String Arrays which contain the String values for Manual, Filter and Classifier Class Attributes.
	 */
	public static final String[] 
			
			MANUALCLASSTITLESVALS = new String[] { MANUALCLASS, UNCLASSIFIEDATR, FEATUREATR, NONFEATUREATR, CONNECTEDATR },
			
			FILTERCLASSTITLESVALS = new String[] { FILTERCLASS, PASSEDATR, NOTPASSEDATR, },
			
			CLASSIFIERCLASSTITLESVALS = new String[] { CLASSIFIERCLASS, UNCLASSIFIEDATR, FEATUREATR, NONFEATUREATR, CONNECTEDATR };
		
	/**
	 * Integer to represent the BASE VALUE -> the first value which the ObjectIdentifier will use to iterate through
	 * all possible values of objects (ManualClass x FilterClass x ClassifierClass).
	 */
	public static final int BASE_VALUE = 223;
	
	/**
	 * Integers to represent the SELECTED VALUES for an object.  These are better placed in SelectedObject class...
	 */
	
	/** Value indexes for Unclassified, Feature, Non-Feature and Connected 
	 * in the Manual, Passed & Not-Passed in the Filter and Non-Feature, Feature
	 * and Connected in the Classifier Attributes of ARFF dataset.
	 */
	public static final int UNCLASSIFIED_INDEX = 0;
	public static final int FEATURE_INDEX = 1;
	public static final int NONFEATURE_INDEX = 2;
	public static final int CONNECTED_INDEX = 3;
	
	public static final int MAN_UNCLASSIFIED_INDEX = 0,  MAN_FEATURE_INDEX = 1, 
						   MAN_NONFEATURE_INDEX = 2, MAN_CONNECTED_INDEX = 3;
	
	public static final int FIL_PASSED_INDEX = 0, FIL_NOTPASSED_INDEX = 1;
	
	// protected static final int CLAS_NONFEATURE_INDEX = 0, CLAS_FEATURE_INDEX = 1, CLAS_CONNECTED_INDEX = 2; //?
		// did I now move the CLASSIFIER classifications to match the MANUAL indexes??
		// THESE NUMBERS ARE NOT EVEN USED IN THE OBJECT MANAGER!
	public static final int CLAS_UNCLASSIFIED_INDEX = 0, CLAS_FEATURE_INDEX = 1, 
											CLAS_NONFEATURE_INDEX = 2, CLAS_CONNECTED_INDEX = 3;
	
	// protected static int CLAS_FEATURE_INDEX = 1, CLAS_NONFEATURE_INDEX = 2, CLAS_CONNECTED_INDEX = 3;
	
	/**
	 * These named constants are the index of the manual, filter and classifier classes in the arff dataset,
	 * which contains every attribute.
	 */
	public static final int MANUAL_CLASS_INDEX = 47, FILTER_CLASS_INDEX = 48, CLASSIFIER_CLASS_INDEX = 49;
	
	
	public static String[] dataTitles  = new String[] {
			X1, Y1, Z1, OBJNO, VOLVOXELS, AREAVOXELS, XMIN, YMIN, ZMIN, XLENGTH, YLENGTH, ZLENGTH, CENTREX, CENTREY,
			CENTREZ, COMPACTNESS, SPHERICITY, VOLCONVEX, SURFCONVEX, SOLIDITY3D, CONVEXITY3D, VOLTOVOLBOX,
			MAINELONG, MEDIANELONG, VOLELLIPSE, VOLTOVOLELLIPSE,
			HOMINV1, HOMINV2, HOMINV3, HOMINV4, HOMINV5, 
			GEOINV1, GEOINV2, GEOINV3, GEOINV4, GEOINV5, GEOINV6, 
			BIOCATJ1, BIOCATJ2, BIOCATJ3, BIOCATI1, BIOCATI2,
			MEANPIX, SDPIX, MAXPIX, MEDIANPIX, MINPIX };
	
	/**
	 * Retrieve the value index from one of the nominal attributes: Manual, Filter or Classifier class attributes.
	 * The Attribute to be searched is passed as its String title in attributeTitle, and the String value
	 * which the index is returned for is passed in attributeValue.
	 * <p>
	 * Alternatively, the correct index can be obtained through the following declared variables:
	 * MAN_UNCLASSIFIED_INDEX, FIL_PASSED_INDEX, CLAS_FEATURE_INDEX etc.  However, this method is presented here
	 * to prevent excessive logical requirements in any class which needs to retrieve these indexes from the
	 * String values.
	 * @param attributeTitle
	 * @param attributeValue
	 * @return
	 */
	public static int getValueIndex(String attributeTitle, String attributeValue) {
		if( attributeTitle.equals( MANUALCLASS) ) {
			return getValueIndex(attributeValue, MANUALCLASSVALS);
		}
		else if( attributeTitle.equals( FILTERCLASS) ) {
			return getValueIndex(attributeValue, FILTERCLASSVALS);
		}
		else if( attributeTitle.equals( CLASSIFIERCLASS) ) {
			return getValueIndex(attributeValue, CLASSIFIERCLASSVALS);
		}
		return -1;
	}
	
	public static int getValueIndex(String attributeValue, String[] attrVals) {
		for(int a=0; a<attrVals.length; a++) {
			if( attrVals[a].equals(attributeValue) ) {
				return a;
			}
		}
		return -1;
	}
	
	/**
	 * Returns the appropriate CLASS VALUE from the attribute class title (Manual, Filter or
	 * Classifier Class) and the Value Index.  Returns Unclassified as default.
	 * @param attributeTitle
	 * @param attributeIndex
	 * @return
	 */
	public static String getValueTitleFromIndex(String attributeTitle, int valueIndex) {
		
		if( attributeTitle.equals( MANUALCLASS) ) {
			return MANUALCLASSVALS[valueIndex];
		}
		else if( attributeTitle.equals( FILTERCLASS) ) {
			return FILTERCLASSVALS[valueIndex];
		}
		else if( attributeTitle.equals( CLASSIFIERCLASS) ) {
			return CLASSIFIERCLASSVALS[valueIndex];
		}
		return UNCLASSIFIEDATR;
	}
	
	
	/**
	 * Initialises the DataObj with default 0 and 0.0 values.
	 */
	public ObjectDataContainer() {
		setDataValuesAndTitles();
	}
	
	/**
	 * Initialises the DataObj, setting each instance variable to the value specified in the constructors parameters.
	 */
	public ObjectDataContainer(int x1, int y1, int z1, int objNo, int volVoxels, double areaVoxels, int xMin, int yMin, int zMin,
							int xLength, int yLength, int zLength, double centreX, double centreY, double centreZ, double compactness,
							double sphericity, double volConvex, double surfConvex, double solidity3D, double convexity3D, 
							double volToVolBox, double mainElong, double medianElong, double volEllipse, double volToVolEllipse,
							double homInv1, double homInv2, double homInv3, double homInv4, double homInv5, 
							double geoInv1, double geoInv2, double geoInv3, double geoInv4, double geoInv5, double geoInv6, 
							double biocatJ1, double biocatJ2, double biocatJ3, double biocatI1, double biocatI2,
							double meanPix, double sdPix, double maxPix, double medianPix, double minPix) {
		
		this.x1 = x1;	this.y1 = y1;	this.z1 = z1;
		
		this.objNo = objNo;
		
		this.volVoxels = volVoxels;	this.areaVoxels = areaVoxels;
		
		this.xMin = xMin;	this.xLength = xLength;	
		this.yMin = yMin;	this.yLength = yLength;	
		this.zMin = zMin;	this.zLength = zLength;
		
		this.centreX = centreX;	this.centreY = centreY;	this.centreZ = centreZ;
		
		this.compactness = compactness;	this.sphericity = sphericity;
		
		this.volConvex = volConvex;	this.surfConvex = surfConvex;
		
		this.solidity3D = solidity3D;	this.convexity3D = convexity3D;
		
		this.volToVolBox = volToVolBox;
		
		this.mainElong = mainElong;	this.medianElong = medianElong;
		
		this.volEllipse = volEllipse;	this.volToVolEllipse = volToVolEllipse;
		
		this.homInv1 = homInv1; this.homInv2 = homInv2; this.homInv3 = homInv3; 
		this.homInv4 = homInv4; this.homInv5 = homInv5; 
		
		this.geoInv1 = geoInv1; this.geoInv2 = geoInv2; this.geoInv3 = geoInv3; 
		this.geoInv4 = geoInv4; this.geoInv5 = geoInv5; this.geoInv6 = geoInv6; 
		
		this.biocatJ1 = biocatJ1; this.biocatJ2 = biocatJ2; this.biocatJ3 = biocatJ3; 
		this.biocatI1 = biocatI1; this.biocatI2 = biocatI2;
		
		this.meanPix = meanPix;	this.sdPix = sdPix;	this.maxPix = maxPix;	this.medianPix = medianPix;	this.minPix = minPix;
		
		setDataValuesAndTitles();
		
	}
	
	/**
	 * Sets data values to the passed values.
	 */
	public void setDataValues(int x1, int y1, int z1, int objNo, int volVoxels, double areaVoxels, int xMin, int yMin, int zMin, 
							int xLength, int yLength, int zLength, double centreX, double centreY, double centreZ, double compactness,
							double sphericity, double volConvex, double surfConvex, double solidity3D, double convexity3D, 
							double volToVolBox, double mainElong, double medianElong, double volEllipse, double volToVolEllipse,
							double homInv1, double homInv2, double homInv3, double homInv4, double homInv5, 
							double geoInv1, double geoInv2, double geoInv3, double geoInv4, double geoInv5, double geoInv6, 
							double biocatJ1, double biocatJ2, double biocatJ3, double biocatI1, double biocatI2,
							double meanPix, double sdPix, double maxPix, double medianPix, double minPix) {
		
		this.x1 = x1;	this.y1 = y1;	this.z1 = z1;
		
		this.objNo = objNo;
		
		this.volVoxels = volVoxels;	this.areaVoxels = areaVoxels;
		
		this.xMin = xMin;	this.xLength = xLength;	
		this.yMin = yMin;	this.yLength = yLength;	
		this.zMin = zMin;	this.zLength = zLength;
		
		this.centreX = centreX;	this.centreY = centreY;	this.centreZ = centreZ;
		
		this.compactness = compactness;	this.sphericity = sphericity;
		
		this.volConvex = volConvex;	this.surfConvex = surfConvex;
		
		this.solidity3D = solidity3D;	this.convexity3D = convexity3D;
		
		this.volToVolBox = volToVolBox;
		
		this.mainElong = mainElong;	this.medianElong = medianElong;
		
		this.volEllipse = volEllipse;	this.volToVolEllipse = volToVolEllipse;
		
		this.homInv1 = homInv1; this.homInv2 = homInv2; this.homInv3 = homInv3; 
		this.homInv4 = homInv4; this.homInv5 = homInv5; 
		
		this.geoInv1 = geoInv1; this.geoInv2 = geoInv2; this.geoInv3 = geoInv3; 
		this.geoInv4 = geoInv4; this.geoInv5 = geoInv5; this.geoInv6 = geoInv6; 
		
		this.biocatJ1 = biocatJ1; this.biocatJ2 = biocatJ2; this.biocatJ3 = biocatJ3; 
		this.biocatI1 = biocatI1; this.biocatI2 = biocatI2;
		
		this.meanPix = meanPix;	this.sdPix = sdPix;	this.maxPix = maxPix;	this.medianPix = medianPix;	this.minPix = minPix;
	
		dataValues = new double[] {
				x1, y1, z1, objNo, volVoxels, areaVoxels, xMin, yMin, zMin, xLength, yLength, zLength, centreX, centreY,
				centreZ, compactness, sphericity, volConvex, surfConvex, solidity3D, convexity3D, volToVolBox,
				mainElong, medianElong, volEllipse, volToVolEllipse,
				homInv1, homInv2, homInv3, homInv4, homInv5, 
				geoInv1, geoInv2, geoInv3, geoInv4, geoInv5, geoInv6, 
				biocatJ1, biocatJ2, biocatJ3, biocatI1, biocatI2,
				meanPix, sdPix, maxPix, medianPix, minPix };
	}
	
	public int getX1() {
		return x1;
	}
	
	public int getY1() {
		return y1;
	}
	
	public int getZ1() {
		return z1;
	}
	
	/**
	 * Set the  dataValues and dataTitles arrays to the data values and datapoint titles.
	 */
	public void setDataValuesAndTitles() {
		
		dataValues = new double[] {
				x1, y1, z1, objNo, volVoxels, areaVoxels, xMin, yMin, zMin, xLength, yLength, zLength, centreX, centreY,
				centreZ, compactness, sphericity, volConvex, surfConvex, solidity3D, convexity3D, volToVolBox,
				mainElong, medianElong, volEllipse, volToVolEllipse,
				homInv1, homInv2, homInv3, homInv4, homInv5, 
				geoInv1, geoInv2, geoInv3, geoInv4, geoInv5, geoInv6, 
				biocatJ1, biocatJ2, biocatJ3, biocatI1, biocatI2,
				meanPix, sdPix, maxPix, medianPix, minPix };
		
		dataTitles = new String[] {
				X1, Y1, Z1, OBJNO, VOLVOXELS, AREAVOXELS, XMIN, YMIN, ZMIN, XLENGTH, YLENGTH, ZLENGTH, CENTREX, CENTREY,
				CENTREZ, COMPACTNESS, SPHERICITY, VOLCONVEX, SURFCONVEX, SOLIDITY3D, CONVEXITY3D, VOLTOVOLBOX,
				MAINELONG, MEDIANELONG, VOLELLIPSE, VOLTOVOLELLIPSE, 
				HOMINV1, HOMINV2, HOMINV3, HOMINV4, HOMINV5, 
				GEOINV1, GEOINV2, GEOINV3, GEOINV4, GEOINV5, GEOINV6, 
				BIOCATJ1, BIOCATJ2, BIOCATJ3, BIOCATI1, BIOCATI2,
				MEANPIX, SDPIX, MAXPIX, MEDIANPIX, MINPIX };
	}
	
	
	/**
	 * Returns a new attribute given an attributeTitle as String.  Can be used to generate an Attribute
	 * with the String titles included in this class as NAMED CONSTANTS.
	 * @param attributeTitle
	 * @return
	 */
	public static Attribute returnAttribute(String attributeTitle) {
		return new Attribute(attributeTitle);
	}
	
	/**
	 * Returns a new String attribute given an attributeTitle as String.  Can be used to generate an 
	 * Attribute with the String titles included in this class as NAMED CONSTANTS.
	 * @param attributeTitle
	 * @return
	 */
	public static Attribute returnStringAttribute(String attributeTitle) {
		return new Attribute(attributeTitle, (ArrayList<String>) null);
	}
	
	
	/**
	 * Return as an ArrayList all the Attributes which make up the different measures of an object
	 * assessed with objAssessment3d26() method in the MCIB_SM_BorderObjPxCount3D object.
	 * @return
	 */
	public static ArrayList<Attribute> returnObjectMeasuresAttributes() {
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>(); 
		
		//ATTRIBUTES -> i.e. the columns:  These are all NUMERIC
    
		//FIRST VOXEL:
		// "x1", "y1", "z1"
		attributes.add( new Attribute(X1) );	attributes.add( new Attribute(Y1) );	attributes.add( new Attribute(Z1) );
		
		//FROM MCIB3D OUTPUT:
    	//Obj Counter -"Obj No."
		attributes.add( new Attribute(OBJNO) );

    
    	//GEOMETRICAL MEASURES:
    	// "Vol. Voxels"							"Area Voxels"
		attributes.add( new Attribute(VOLVOXELS) );	attributes.add( new Attribute(AREAVOXELS) );
    
		//LOCATION -> defines bounding box:	
		// "xMin", "xMax", "yMin", "yMax", "zMin", "zMax"
		attributes.add( new Attribute(XMIN) );	attributes.add( new Attribute(YMIN) );	attributes.add( new Attribute(ZMIN) );
		attributes.add( new Attribute(XLENGTH) );	attributes.add( new Attribute(YLENGTH) );	attributes.add( new Attribute(ZLENGTH) );

		//SHAPE MEASURES:	
		// The CENTRE of the object - taking into account its mass:
		// "Centre X", "Centre Y", "Centre Z"
		attributes.add( new Attribute(CENTREX) );	attributes.add( new Attribute(CENTREY) );	attributes.add( new Attribute(CENTREZ) );
    
		// "Compactness"	"Sphericity"	convexVolume	convexSurface	3D solidity		3D convexity
		attributes.add( new Attribute(COMPACTNESS) );	attributes.add( new Attribute(SPHERICITY) );
		attributes.add( new Attribute(VOLCONVEX) );	attributes.add( new Attribute(SURFCONVEX) );
		attributes.add( new Attribute(SOLIDITY3D) );	attributes.add( new Attribute(CONVEXITY3D) );
	
		//"Vol. to Vol. Box"		
		attributes.add( new Attribute(VOLTOVOLBOX) );

    	//"Main Elong."		"Median Elong."		
		attributes.add( new Attribute(MAINELONG) );	attributes.add( new Attribute(MEDIANELONG) );

    
    	//"Vol. Ellipse"		"Vol. to Vol. Ellipse"		
		attributes.add( new Attribute(VOLELLIPSE) );	attributes.add( new Attribute(VOLTOVOLELLIPSE) );
		
		// Moments:
		attributes.add( new Attribute(HOMINV1) );	attributes.add( new Attribute(HOMINV2) );
		attributes.add( new Attribute(HOMINV3) );	attributes.add( new Attribute(HOMINV4) );
		attributes.add( new Attribute(HOMINV5) );
		
		attributes.add( new Attribute(GEOINV1) );	attributes.add( new Attribute(GEOINV2) );
		attributes.add( new Attribute(GEOINV3) );	attributes.add( new Attribute(GEOINV4) );
		attributes.add( new Attribute(GEOINV5) );	attributes.add( new Attribute(GEOINV6) );
		attributes.add( new Attribute(BIOCATJ1) );	attributes.add( new Attribute(BIOCATJ2) );
		attributes.add( new Attribute(BIOCATJ3) );
		attributes.add( new Attribute(BIOCATI1) );	attributes.add( new Attribute(BIOCATI2) );
    
		//INTENSITY MEASURES
		//"Mean Pix."	"SD Pix."	"Max Pix."	"Median Pix."	"Min Pix."

		attributes.add( new Attribute(MEANPIX) );	attributes.add( new Attribute(SDPIX) );
		attributes.add( new Attribute(MAXPIX) );	attributes.add( new Attribute(MEDIANPIX) );
		attributes.add( new Attribute(MINPIX) );
		
		return attributes;
	}
	
	/**
	 * Returns a nominal Attribute with four possible values:  [0] "Unclassified", [1] "Feature", [2] "Non-Feature",
	 * [3] "Connected".
	 * @return
	 */
	public static Attribute returnManClassAttribute() {
		//MANUAL CLASSIFICATION ATTRIBUTES:
		ArrayList<String> labels = new ArrayList<String>(); 
		labels.add(UNCLASSIFIEDATR);		labels.add(FEATUREATR); 
		labels.add(NONFEATUREATR); 			labels.add(CONNECTEDATR); 
		
		return new Attribute(MANUALCLASS, labels);
		
	}
	
	/**
	 * Returns a nominal Attribute with three possible values: [0] "Feature", [1] "Non-Feature",
	 * [2] "Connected".
	 * <p>
	 * This is no longer used, as all class attributes will contain the four possible values.
	 * @return
	 */
	public static Attribute returnManClassAttributeNoUnclassified() {
		//MANUAL CLASSIFICATION ATTRIBUTES:
		ArrayList<String> labels = new ArrayList<String>(); 
		// labels.add(UNCLASSIFIEDATR);
		labels.add(FEATUREATR); 		labels.add(NONFEATUREATR); 
		labels.add(CONNECTEDATR); 
		
		return new Attribute(MANUALCLASS, labels);
		
	}
	
	/**
	 * Returns a nominal Attribute with two possible values:  [0] "Passed", [1] "Not-Passed".
	 * @return
	 */
	public static Attribute returnFilterClassAttribute() {
		//FILTER CLASSIFICATION ATTRIBUTES:
		ArrayList<String> labels = new ArrayList<String>(); 
		labels.add(PASSEDATR);		labels.add(NOTPASSEDATR); 
		return new Attribute(FILTERCLASS, labels);
	}
	
	/**
	 * Returns a nominal Attribute with three possible values:  [0] "Non-Feature", [1] "Feature",
	 * [2] "Connected".
	 * @return
	 */
	public static Attribute returnClassifierClassAttribute() {
		//CLASSIFIER CLASSIFICATION ATTRIBUTES:
		ArrayList<String> labels = new ArrayList<String>(); 
		labels.add(UNCLASSIFIEDATR);		labels.add(FEATUREATR);
		labels.add(NONFEATUREATR); 			labels.add(CONNECTEDATR);
		return new Attribute(CLASSIFIERCLASS, labels);
	}
	
	/**
	 * Returns as an ArrayList the three nominal Attributes which constitute the Manual Classification,
	 * Filter Classification and Classifier Classification Attributes with relevant nominal values.
	 * @return
	 */
	public static ArrayList<Attribute> returnObjectClassificationsAttributes() {
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		
		attributes.add( returnManClassAttribute() );
		attributes.add( returnFilterClassAttribute() );
		attributes.add( returnClassifierClassAttribute() );
		
		return attributes;
	}
	
	
	/**
	 * Returns as an ArrayList all the Attributes that constitute the Object Measures Attributes and the
	 * Object Classification nominal Attributes.  This returns all Object measure and classification 
	 * attributes.
	 * @return
	 */
	public static ArrayList<Attribute> returnAllObjectMeasuresClassificationsAttributes() {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		
		attributes.addAll( returnObjectMeasuresAttributes() );
		attributes.addAll( returnObjectClassificationsAttributes() );
		
		return attributes;
	}
	
	/**
	 * Returns as an ArrayList all the Attributes that constitute the Object Measures Attributes and the
	 * Object Manual Classification nominal Attribute only.
	 * @return
	 */
	public static ArrayList<Attribute> returnObjectMeasuresAndManClassAttributes() {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		
		attributes.addAll( returnObjectMeasuresAttributes() );
		attributes.add( returnManClassAttribute() );
		
		return attributes;
	}
	
	/**
	 * Returns as an ArrayList all the Attributes that constitute the Object Measures Attributes and the
	 * Object Classifier Classification nominal Attribute only.
	 * @return
	 */
	public static ArrayList<Attribute> returnObjectMeasuresAndClassifierClassAttributes() {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		
		attributes.addAll( returnObjectMeasuresAttributes() );
		attributes.add( returnClassifierClassAttribute() );
		
		return attributes;
	}
	
	/**
	 * This method returns as an ArrayList a list of the names of Attributes which are real numbers,
	 * and typically need to be rounded to a certain level of accuracy for display in Instances datasets.
	 * @return
	 */
	public static ArrayList<String> returnRealNumberAttributeTitles() {
		
		ArrayList<String> a = new ArrayList<String>();
		
		a.add(CENTREX);		a.add(CENTREY);		a.add(CENTREZ);
		a.add(COMPACTNESS);	a.add(SPHERICITY);
		a.add(SOLIDITY3D);	a.add(CONVEXITY3D);
		a.add(VOLTOVOLBOX);	a.add(MEDIANELONG);
		a.add(VOLELLIPSE);	a.add(VOLTOVOLELLIPSE);		
		a.add(HOMINV1);		a.add(HOMINV2);		a.add(HOMINV3);
		a.add(HOMINV4);		a.add(HOMINV5);
		a.add(GEOINV1);		a.add(GEOINV2);		a.add(GEOINV3);
		a.add(GEOINV4);		a.add(GEOINV5);		a.add(GEOINV6);
		a.add(BIOCATJ1);	a.add(BIOCATJ2);	a.add(BIOCATJ3);
		a.add(BIOCATI1);	a.add(BIOCATI2);
		
		return a;
		
	}
	
	/**
	 * Returns the default atributes for Classifier building - currently VOLVOXELS and SPHERICITY, with
	 * the last Attribute (the Class Attribute) being MANUALCLASS.
	 * @return
	 */
	public static ArrayList<Attribute> returnClassifierAttributes() {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		
		// TODO this needs to be flexible to include whatever attributes the user has set in the Options
			// for this plugin.
		attributes.add( ObjectDataContainer.returnAttribute( ObjectDataContainer.VOLVOXELS ) );
		attributes.add( ObjectDataContainer.returnAttribute( ObjectDataContainer.SPHERICITY ) );
		
		attributes.add( ObjectDataContainer.returnManClassAttribute() );
		
		return attributes;
	}
	
	/**
	 * Returns the index of a nominal value in one of the Class Attributes.  This covers the Manual, Filter
	 * and Classifier Class Attributes, and returns the index (position) in the Attribute of the values,
	 * including: UNCLASSIFIED, FEATURE, NON-FEATURE, CONNECTED, PASSED, NOT-PASSED.
	 * <p>
	 * If the passed String is not equal to one of the potential values in the Manual, Filter or Classifier
	 * Class Attributes, this method returns -1.
	 * @param attributeTitle
	 * @return
	 */
	public static int returnAttributeValueIndex(String attributeTitle) {
		
		if( attributeTitle.equalsIgnoreCase(UNCLASSIFIEDATR) ) {
			return UNCLASSIFIED_INDEX;
		}
		else if( attributeTitle.equalsIgnoreCase(FEATUREATR) ) {
			return FEATURE_INDEX;
		}
		else if( attributeTitle.equalsIgnoreCase(NONFEATUREATR) ) {
			return NONFEATURE_INDEX;
		}
		else if( attributeTitle.equalsIgnoreCase(CONNECTEDATR) ) {
			return CONNECTED_INDEX;
		}
		else if( attributeTitle.equalsIgnoreCase(PASSEDATR) ) {
			return FIL_PASSED_INDEX;
		}
		else if( attributeTitle.equalsIgnoreCase(NOTPASSEDATR) ) {
			return FIL_NOTPASSED_INDEX;
		}
		return -1;		
	}

	@Override
	public double[] returnData() {
		return dataValues;
	}

	@Override
	public String[] returnDataTitles() {
		return dataTitles;
	}
	
	public static boolean dataTitlesContains(String title) {
		for(int a=0; a<dataTitles.length; a++) {
			if(dataTitles[a].equals(title) ) {
				return true;
			}
		}
		//if the for loop completes, title is not in dataTitles:
		return false;
	}

}
