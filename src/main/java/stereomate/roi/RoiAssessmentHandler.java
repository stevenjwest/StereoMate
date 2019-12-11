package stereomate.roi;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import ij.IJ;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageInt;
import stereomate.data.DatasetWrapper;
import stereomate.data.ObjectDataContainer;
import stereomate.data.ObjectDatasetMap;
import stereomate.image.ImageHandler;
import weka.core.AttributeStats;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;


/**
 * This class holds the representation of Regions of Interest, as defined by the Roi_DiSector plugin.
 * Here, the ROIs are processed to create a series of contiguous ROIs.  During this process, three
 * objects are created:
 * <p>
 * 1. Contiguous ROIs:  Each ROI from the Roi_DiSector plugin is processed to shift its bottom and right
 * boundaries down by 1 pixel.  This forces ROIs to be contiguous, eliminating the 1-pixel gap which
 * exists between ROIs derived from the Roi_DiSector.
 * <p>
 * 2. ROI Boundaries:  The Boundary of each ROI is stored in another object, which allows quick processing
 * of all pixels which sit on the ROI border.
 * <p>
 * 3. Exclusion Zone ROIs:  With each ROI in Contiguous ROIs, the Exclusion Zone algorithm is applied, which
 * will move image edge contacts back the Exclusion Zone length to apply stereological filtering to
 * whole object analysis.
 * 
 * @author stevenwest
 *
 */
public class RoiAssessmentHandler {
	
	private ArrayList<Points> borderPoints;
	
	ArrayList<DatasetWrapper> roiDatasets;
	
	private ArrayList<Roi> rois, roisExcl;
	
	private RoisWithEdgeContacts rec;
	
	private int xMin, yMin, xMax, yMax, maxXY, maxZ;
	
	/**
	 * Constructs the RoiAnalyser.  roiBoundaries, rois and roisExcl are all filled, as well as the 
	 * RoisWithEdgeContacts object: rec.
	 * @param rois
	 * @param xMin
	 * @param yMin
	 * @param xMax
	 * @param yMax
	 * @param maxXY
	 * @param maxZ
	 */
	public RoiAssessmentHandler(ArrayList<Roi> rois, ArrayList<DatasetWrapper> roiDatasets, 
								int xMin, int yMin, int xMax, int yMax, int maxXY, int maxZ) {
		
		//IJ.log("");
		//IJ.log("        getBorderPointArrays()...");
		borderPoints = getBorderPointArrays(rois, xMax, yMax );
		//logBorderPoints();
		
		//IJ.log("");
		//IJ.log("        concatRoisDownRight()...");
		this.rois = concatRoisDownRight(rois, xMax, yMax );
		//logRois(this.rois, "concatRois");
		
		
		//IJ.log("");
		//IJ.log("        applyExclusionZone()...");
		rec = applyExclusionZone( this.rois, xMin, yMin, xMax, yMax, maxXY );
		//logRois(rec.rois, "ExclRois");
		
		//IJ.log("");
		//IJ.log("        roisExcl set...");
		roisExcl = rec.rois;
		
		this.roiDatasets = roiDatasets;
		
		this.xMin = xMin;
		this.yMin = yMin;
		this.xMax = xMax;
		this.yMax = yMax;
		
		this.maxXY = maxXY;
		this.maxZ = maxZ;
	
	}
	
	public ArrayList<Points> getRoiBoundaries() {
		return borderPoints;
	}

	public void setRoiBoundaries(ArrayList<Points> roiBoundaries) {
		this.borderPoints = roiBoundaries;
	}

	public ArrayList<Roi> getRois() {
		return rois;
	}

	public void setRois(ArrayList<Roi> rois) {
		this.rois = rois;
	}

	public ArrayList<Roi> getRoisExcl() {
		return roisExcl;
	}

	public void setRoisExcl(ArrayList<Roi> roisExcl) {
		this.roisExcl = roisExcl;
	}

	public RoisWithEdgeContacts getRec() {
		return rec;
	}

	public void setRec(RoisWithEdgeContacts rec) {
		this.rec = rec;
	}

	public int getxMin() {
		return xMin;
	}

	public void setxMin(int xMin) {
		this.xMin = xMin;
	}

	public int getyMin() {
		return yMin;
	}

	public void setyMin(int yMin) {
		this.yMin = yMin;
	}

	public int getxMax() {
		return xMax;
	}

	public void setxMax(int xMax) {
		this.xMax = xMax;
	}

	public int getyMax() {
		return yMax;
	}

	public void setyMax(int yMax) {
		this.yMax = yMax;
	}

	public int getMaxXY() {
		return maxXY;
	}

	public void setMaxXY(int maxXY) {
		this.maxXY = maxXY;
	}

	public int getMaxZ() {
		return maxZ;
	}

	public void setMaxZ(int maxZ) {
		this.maxZ = maxZ;
	}

	/**
	 * Concatenates each roi in rois down and to the right to ensure each roi now occupies its original
	 * area, plus the area where the roi is shifted down, right, and down-right.  The image width
	 * and height should be passed which the rois are extracted from, to prevent the new rois from
	 * referencing any pixels outside the image borders.
	 * @param rois Rois to be concatenated down and to right.
	 * @param imageWidth The width of the image the ROIs are from.
	 * @param imageHeight The height of the image the ROIs are from.
	 * @return The new ArrayList<Roi> containing the concatenated rois.
	 */
	public ArrayList<Roi> concatRoisDownRight(ArrayList<Roi> rois, int imageWidth, int imageHeight) {

		ArrayList<Roi> rois2 = new ArrayList<Roi>();

		for(int a=0; a<rois.size(); a++) {
			Roi r = rois.get(a);
			r = concatRoiDownRight(r, imageWidth, imageHeight);
			rois2.add(r);
		}

		return rois2;
	}
	
	
	/**
	 * 
	 * @param roi
	 * @return
	 */
	public PolygonRoi concatRoiDownRight(Roi roi, int width, int height) {

		// get polygon boundary points:
		int[] x = roi.getPolygon().xpoints;
		int[] y = roi.getPolygon().ypoints;

		// original ROI:
		PolygonRoi roiOriginal = (PolygonRoi) roi;

		// DOWN ROI:
		float[] xf = new float[x.length];
		float[] yf = new float[y.length];

		for(int a=0; a<xf.length; a++) {
			xf[a] = x[a];
			yf[a] = checkEdge(y[a]+1, height);
			// IJ.log( "index: "+a+" xf: "+xf[a]+" yf: "+yf[a] );
		}
		PolygonRoi roiDown = new PolygonRoi(xf,yf,Roi.POLYGON);

		// RIGHT ROI:
		xf = new float[x.length];
		yf = new float[y.length];

		for(int a=0; a<xf.length; a++) {
			xf[a] = checkEdge(x[a]+1, width);
			yf[a] = y[a];
			// IJ.log( "index: "+a+" xf: "+xf[a]+" yf: "+yf[a] );
		}
		PolygonRoi roiRight = new PolygonRoi(xf,yf,Roi.POLYGON);

		// DOWN-RIGHT ROI:
		xf = new float[x.length];
		yf = new float[y.length];

		for(int a=0; a<xf.length; a++) {
			xf[a] = checkEdge(x[a]+1, width);
			yf[a] = checkEdge(y[a]+1, height);
			// IJ.log( "index: "+a+" xf: "+xf[a]+" yf: "+yf[a] );
		}
		PolygonRoi roiDownRight = new PolygonRoi(xf,yf,Roi.POLYGON);

		//concat all ROIs:
		ShapeRoi roiO = new ShapeRoi(roiOriginal);
		ShapeRoi roiD = new ShapeRoi(roiDown);
		ShapeRoi roiR = new ShapeRoi(roiRight);
		ShapeRoi roiDR = new ShapeRoi(roiDownRight);

		roiO.or(roiD);
		roiR.or(roiDR);

		roiO.or(roiR);

		Roi r = roiO.shapeToRoi();

		PolygonRoi pr = new PolygonRoi(r.getPolygon().xpoints, r.getPolygon().ypoints, r.getPolygon().npoints, Roi.POLYGON);

		return pr;

	}
	
	/**
	 * Log the Border Points Coordinates
	 */
	public void logBorderPoints() {
		for(int a=0; a<borderPoints.size(); a++) {
			Points pts = borderPoints.get(a);
			IJ.log("BorderPoints "+a);
			for(int p=0; p< pts.size(); p++) {	

				IJ.log("x: "+pts.get(p).x+" y: "+pts.get(p).y);

			} //end p
			IJ.log("");
			IJ.log("");
		}
	}
	
	/**
	 * Print coordinates of the rois arraylist of ROIs to the log.
	 * @param rois
	 */
	public void logRois( ArrayList<Roi> rois, String title) {

		for(int a=0; a<rois.size(); a++) {
			//int[] x = rois.get(a).getPolygon().xpoints;
			//int[] y = rois.get(a).getPolygon().ypoints;
			ImageProcessor mask = rois.get(a).getMask();
			Rectangle r = rois.get(a).getBounds();
			//int[] x = ((PolygonRoi)rois.get(a)).getXCoordinates();
			//int[] y = ((PolygonRoi)rois.get(a)).getYCoordinates();
			IJ.log(title+" "+a);
			int w = 0;
			int h = 0;
			for(int width=0; width<mask.getWidth(); width++) {
				for(int height =0; height<mask.getHeight(); height++) {
					if(mask.getPixel(width, height) > 0) {
						w = width+r.x;
						h = height+r.y;
						IJ.log("ROIS: x: "+w+" y: "+h);
					}
				}
			}
			//for(int p=0; p<x.length; p++) {
			//IJ.log("x: "+x[p]+" y: "+y[p]);
			//}
		}

		IJ.log(" ");
		IJ.log(" ");
	}
	

	/**
	 * This method returns for each ROI in an ArrayList, the border ROI as a series of 
	 * points along the entire ROI border, which can be used to look at each Point along 
	 * the original ROI border.
	 * @param rois ArrayList of ROIs which the border needs to be extracted from.
	 * @param imageWidth Width of the image -> to filter the border point values.
	 * @param imageHeight Height of image -> to filter the border point values.
	 * @return ArrayList of ArrayList<Point> objects which contain every point along the border of the ROI.
	 */
	public ArrayList<Points> getBorderPointArrays(ArrayList<Roi> rois, int width, int height) {

		ArrayList<Points> pointsArray = new ArrayList<Points>();

		for(int a=0; a<rois.size(); a++) {
			pointsArray.add( getBorderPoints( rois.get(a), width, height ) );
		}

		return pointsArray;

	}
	
	/**
	 * 
	 * @param roi
	 * @param width
	 * @param height
	 * @return
	 */
	public Points getBorderPoints(Roi roi, int width, int height) {

		Points p = new Points();

		int[] x = roi.getPolygon().xpoints;
		int[] y = roi.getPolygon().ypoints;

		for(int a=0; a<x.length; a++) {
			//checkEdge(x[a], width);
			//checkEdge(y[a], height); // these do not do anything?!
			// first, add the current x,y coord:
			p.add( new Point( checkEdge(x[a], width-1), checkEdge(y[a], height-1) ) );
			// p.add( checkPoint(roi, x, y, a) );

			// set nexta to index a + 1:
			int nexta = (a+1);
			if(nexta == x.length) {
				// if next a index breaches the index length, set it to 0:
				nexta = 0;
			}
			
			// then, check if the next value is maximum +/-1 away from current value in each dimension:
			if(x[nexta] > (x[a]+1)) {
				// Case - next x val is bigger:
				addBorderPointsXUP(p, checkEdge(x[a], width-1), checkEdge(x[nexta], width-1), checkEdge(y[a], height-1) );
			}
			else if(x[nexta] < (x[a]-1)) {
				// Case - next x val is smaller:
				addBorderPointsXDOWN(p, checkEdge(x[a], width-1), checkEdge(x[nexta], width-1), checkEdge(y[a], height-1) );
			}
			if(y[nexta] > (y[a]+1)) {
				// Case - next x val is bigger:
				addBorderPointsYUP(p, checkEdge(y[a], height-1), checkEdge(y[nexta], height-1), checkEdge(x[a], width-1) );
			}
			else if(y[nexta] < (y[a]-1)) {
				// Case - next x val is bigger:
				addBorderPointsYDOWN(p, checkEdge(y[a], height-1), checkEdge(y[nexta], height-1), checkEdge(x[a], width-1) );
			}
		}

		return p;

	}
	
	public void logPoints(ArrayList<Points> p, String title) {
		for(int a=0; a<p.size(); a++) {
			//int[] x = borderPoints2.get(a).x;
			//int[] y = borderPoints2.get(a).y;
			IJ.log(title+" "+a);
			for(int b=0; b<p.get(a).size(); b++) {
				IJ.log("POINTS: x: "+p.get(a).get(b).x+" y: "+p.get(a).get(b).y);
			}
		}

		IJ.log(" ");
		IJ.log(" ");
	}
	
	
	/**
	 * Checks whether value is below 0, or above maxValue, and if it is, it sets the value
	 * to either 0 or maxValue, and returns it.
	 * @param value
	 * @param maxLength
	 * @return
	 */
	public int checkEdge(int value, int maxValue) {
		//IJ.log("value: "+value);
		//IJ.log("max Val: "+maxValue);
		if(value < 0) {
			value = 0;
		}
		else if(value > maxValue ) {
			value = maxValue;
		}
		//IJ.log("value: "+value);
		//IJ.log("");
		return value;
	}
	
	
	/**
	 * Add to Points p all of the point lying between point (xStart, y) to (xEnd, y).
	 * xEnd should have been determined to be larger than xStart.
	 * @param p
	 * @param xStart
	 * @param xEnd
	 * @param y
	 */
	public void addBorderPointsXUP(Points p, int xStart, int xEnd, int y) {

		// loop from dimStart+1 to dimEnd-1, and add points (dimStart/End, paraDim) to p:
		for(int a=(xStart+1);a<xEnd;a++) {
			p.add( new Point(a, y) );
		}

	}

	/**
	 * Add to Points p all of the point lying between point (xStart, y) to (xEnd, y).
	 * xEnd should have been determined to be smaller than xStart.
	 * @param p
	 * @param xStart
	 * @param xEnd
	 * @param y
	 */
	public void addBorderPointsXDOWN(Points p, int xStart, int xEnd, int y) {

		// loop from dimStart+1 to dimEnd-1, and add points (dimStart/End, paraDim) to p:
		for(int a=(xStart-1);a>xEnd;a--) {
			p.add( new Point(a, y) );
		}

	}

	/**
	 * Add to Points p all of the point lying between point (x, yStart) to (x, yEnd).
	 * yEnd should have been determined to be larger than yStart.
	 * @param p
	 * @param yStart
	 * @param yEnd
	 * @param x
	 */
	public void addBorderPointsYUP(Points p, int yStart, int yEnd, int x) {

		// loop from dimStart+1 to dimEnd-1, and add points (dimStart/End, paraDim) to p:
		for(int a=(yStart+1);a<yEnd;a++) {
			p.add( new Point(x, a) );
		}

	}

	/**
	 * Add to Points p all of the point lying between point (x, yStart) to (x, yEnd).
	 * yEnd should have been determined to be smaller than yStart.
	 * @param p
	 * @param yStart
	 * @param yEnd
	 * @param x
	 */
	public void addBorderPointsYDOWN(Points p, int yStart, int yEnd, int x) {

		// loop from dimStart+1 to dimEnd-1, and add points (dimStart/End, paraDim) to p:
		for(int a=(yStart-1);a>yEnd;a--) {
			p.add( new Point(x, a) );
		}

	}
	
	
	/**
	 * Points is an ArrayList object of Point objects.
	 * @author stevenwest
	 *
	 */
	public class Points extends ArrayList<Point> {

		public float[] getFloatArrayX() {

			float[] x = new float[this.size()];

			for(int a=0; a<this.size(); a++) {
				x[a] = this.get(a).x;
			}

			return x;
		}

		public float[] getFloatArrayY() {

			float[] y = new float[this.size()];

			for(int a=0; a<this.size(); a++) {
				y[a] = this.get(a).y;
			}

			return y;
		}

	}
	
	
	/**
	 * Applies the ExclusionZone to the ROIs ArrayList parsed to this Exclusion Zone Object.  Note,
	 * this is only applied in XY (as ROIs are on defined in XY) and not in Z.  Z exclusion zone is
	 * applied through selectively screening each roi in each zSlice for objects from the top slice 
	 * (really, the second slice) down to the start of Z exclusion zone,
	 * which is calculated as (TotalZSlices - ExclusionZoneZ).
	 * @return New RoisWithEdgeContacts, which contains the ArrayList<Roi> object where all ROIs have 
	 * had the Exclusion Zone applied to it, AND a 2D int[][] obj which the first dimension encodes the
	 * ROI number, and the second dimension encodes the X or Y dimension of the ROI, and the number
	 * encodes whether no exclusion zone was applied [0], it was applied to the START [1], or it was
	 * applied to the END [2].
	 */
	public RoisWithEdgeContacts applyExclusionZone(ArrayList<Roi> rois, int xStart, int yStart,
			int xEnd, int yEnd, int maxXY) {
		
		if(maxXY == 0 ) { // if no exclusion zone to apply, just return the rois with default edgeContacts
			int[][] edgeContact = new int[rois.size()][2];
			RoisWithEdgeContacts rec = new RoisWithEdgeContacts(rois, edgeContact);
			return rec;
		}

		//Note, be careful to return a NEW ArrayList<Roi> Object & not to modify the original rois obj:

		//new arraylist which should be filled with new ROIs and returned:
		ArrayList<Roi> exclRois = new ArrayList<Roi>();
		
		// a 2D int array to retrieve the modified polygon xPoints and yPoints - to allow the arrays
			// to cascade through xPoint modification, and then the modified arrays to move through
			// yPoint modification
		int[][] pointArrays = new int[0][0]; // default set to a blank 2D array
			// pointArrays contains parallelPoints in [0] and orthogonalPoints in [1]
			// for xStart/xEnd - this means xPoints is [0] and yPoints is [1]
			// for yStart/yEnd - this means xPoints is [1] and yPoints is [0]

		//create an 2-D int array of depth 2 to store whether no exclusion zone is applied [0], it is applied
		//to the START [1] or it is applied to the END [2] of the ROI in the X [ref0] or Y [ref1] dimension.
		//The FIRST DIMENSION encodes the Roi number
		int[][] edgeContact = new int[rois.size()][2];

		for(int a=0; a<rois.size(); a++) {
			
			//IJ.log("get polygon - a: "+a);

			// Polygon polygon = ( (PolygonRoi) rois.get(a) ).getPolygon();
			Polygon polygon = rois.get(a).getPolygon();
			
			//IJ.log("got polygon");
			
			//IJ.log("get points");

			//Fill xPoints and yPoints -> these are needed to split the x and y coordinates for EdgeContacts objects:
			int[] xPoints = polygon.xpoints;
			int[] yPoints = polygon.ypoints;
			
			//IJ.log("got points - length: "+xPoints.length );
			
			//IJ.log("Roi Polygon points passed to Excl Zone: ");
			for(int b=0; b<xPoints.length; b++) {
				//IJ.log("xPoints: " + xPoints[b] + " yPoints: "+yPoints[b]);
			}
			//IJ.log("");
			//IJ.log("");


			//Screen points to find ones sitting on image edge:
			//Use xStart/End and yStart/End

			//First want to find all the edges of the ROI and measure them
			//Do this by: 
			//deducing points where x equals widthStart or widthEnd -> record y values.
			//deducing points where y equals heightStart or heightEnd -> record x values.

			//NOTE:  whenever a coordinate rests on the edge of an image, the NEXT or PREVIOUS point will also
			//be on the edge -> as every length of an ROI on an image edge will start at one point, and end at 
			//the next point [roi points exist each time a straight line changes direction].
			//Can calculate the length therefore by taking difference of the OTHER coordinate of these points.

			//ALSO NOTE:  As the ROI approaches and recedes from an image edge, it will not necessarily be straight.
			//Once an edge contact has been detected, the points either side of this contact which are the depth into
			//the image that is the EXCLUSION ZONE XY need to be determined
			//Then, all points between these two points need to be omitted -> just connect these Exclusion Zone
			//points to implement the Exclusion Zone.
			//This only needs to occur on ONE SIDE PER DIMENSION -> the side with the LARGEST ROI edge contacts.
			//Use largest edge to implement the exclusion zone maximally
			//Should explore this in the artificial dataset on exclusion zones and bias.

			//essential variables for calculating edges:

			//EdgeContactAnalyser objects:
			
			//IJ.log("Edge Contacts in X FIRST:");
			//IJ.log("construct the EdgeContactAnalyser objects:");

			EdgeContactAnalyser xStartEdgeContact = new EdgeContactAnalyser(xStart, xPoints, yPoints);
			EdgeContactAnalyser xEndEdgeContact = new EdgeContactAnalyser(xEnd, xPoints, yPoints);

			//IJ.log("constructed the EdgeContactAnalyser objects:");
			
			//Find all edges for EdgeContacts objects:

			//IJ.log("xStart findEdges():");			
			xStartEdgeContact.findEdges();
			//IJ.log("xEnd findEdges():");
			xEndEdgeContact.findEdges();

			//IJ.log("ROI: "+a+" xStart length: "+xStartEdgeContact.getLength() +" xStart: " + xStart );
			//IJ.log("ROI: "+a+" xEnd length: "+xEndEdgeContact.getLength()  +" xEnd: " + xEnd );

			//Now all EdgeIndex and EdgeLength arrays have been filled in EdgeContacts objects.

			//Next, need to determine which edges have contacts, and which are longest, to implement
			// ROI DiSector logic.

			//Compare sums of EdgeLengths:

			//First, if all EdgeLengths in EdgeContacts objects are 0, return the current ROI
			//This is because none of its edges reside on the side of the image

			if( xStartEdgeContact.getLength() == 0 && xEndEdgeContact.getLength() == 0 ) {
				//set edge contact ref0 [X] to 0:
				edgeContact[a][0] = 0;
			}
			//else, at least one of the image edges are contacted by the ROI -> need to determine which
			//and adjust the ROI appropriately:
			else {

				//IJ.log("Adjusting x Edge Contacts on ROI: "+a);
				
				//IJ.showMessage("Adjusting x Edge Contacts on ROI: "+a+" xStart: "+xStartEdgeContact.getLength()+
				//		" xEnd: "+xEndEdgeContact.getLength() );

				if(xStartEdgeContact.getLength() > xEndEdgeContact.getLength()) {
					//edit points array to remove points on largest edge:
					//xStart edge is largest so points will be removed from this edge:
					//IJ.log("xStart > xEnd - before adjustEdgeContacts: "+a);
					pointArrays = xStartEdgeContact.adjustEdgeContacts(maxXY);    
					//set edge contact ref0 [X] to 1 [START]:
					edgeContact[a][0] = 1;
					//IJ.log("xStart > xEnd - after adjustEdgeContacts: "+a);
					xPoints = pointArrays[0];
					yPoints = pointArrays[1];
				}
				else if(xStartEdgeContact.getLength() < xEndEdgeContact.getLength()) {
					//edit points array to remove points on largest edge:
					//xEnd edge is largest so points will be removed from this edge:
					//IJ.log("xStart < xEnd - before adjustEdgeContacts: "+a);
					pointArrays = xEndEdgeContact.adjustEdgeContacts(maxXY);
					//set edge contact ref0 [X] to 2 [END]:
					edgeContact[a][0] = 2;
					//IJ.log("xStart < xEnd - after adjustEdgeContacts: "+a);
					xPoints = pointArrays[0];
					yPoints = pointArrays[1];

				}
				else if(xStartEdgeContact.getLength() == xEndEdgeContact.getLength()) {
					//check if both are 0:
					if(xStartEdgeContact.getLength() == 0) {
						//do nothing to points array -> as none of the ROI resides on the image edge in X..
						//set edge contact ref0 [X] to 0:
						edgeContact[a][0] = 0;
					}
					else {
						//edit points array to remove points on largest edge:
						//in this case, will pick the xEnd [right] edge as default:
						//IJ.log("xStart == xEnd - before adjustEdgeContacts: "+a);
						pointArrays = xEndEdgeContact.adjustEdgeContacts(maxXY);
						//set edge contact ref0 [X] to 2 [END]:
						edgeContact[a][0] = 2;
						//IJ.log("xStart == xEnd - after adjustEdgeContacts: "+a);
						xPoints = pointArrays[0];
						yPoints = pointArrays[1];
					}
				}

			} //end else if all EdgeContact Objects equal 0
			
			
			// NOW look at y Edges:
			
			//IJ.log("Edge Contacts in Y SECOND:");
			//IJ.log("construct the EdgeContactAnalyser objects:");

			EdgeContactAnalyser yStartEdgeContact = new EdgeContactAnalyser(yStart, yPoints, xPoints);
			EdgeContactAnalyser yEndEdgeContact = new EdgeContactAnalyser(yEnd, yPoints, xPoints);

			//IJ.log("constructed the EdgeContactAnalyser objects:");

			//Find all edges for EdgeContacts objects:

			//IJ.log("yStart findEdges():");
			yStartEdgeContact.findEdges();
			//IJ.log("yEnd findEdges():");
			yEndEdgeContact.findEdges();

			//IJ.log("ROI: "+a+" yStart length: "+yStartEdgeContact.getLength() +" yStart: " + yStart );
			//IJ.log("ROI: "+a+" yEnd length: "+yEndEdgeContact.getLength()  +" yEnd: " + yEnd );

			//Now all EdgeIndex and EdgeLength arrays have been filled in EdgeContacts objects.

			//Next, need to determine which edges have contacts, and which are longest, to implement
			// ROI DiSector logic.

			//Compare sums of EdgeLengths:

			//First, if all EdgeLengths in EdgeContacts objects are 0, return the current ROI
			//This is because none of its edges reside on the side of the image

			if(  yStartEdgeContact.getLength() == 0 && yEndEdgeContact.getLength() == 0 ) {
				exclRois.add( rois.get(a) );
				//set edge contact ref1 [Y] to 0:
				edgeContact[a][1] = 0;
			}
			//else, at least one of the image edges are contacted by the ROI -> need to determine which
			//and adjust the ROI appropriately:
			else {

				//IJ.log("Adjusting y Edge Contacts on ROI: "+a);

				if(yStartEdgeContact.getLength() > yEndEdgeContact.getLength()) {
					//edit points array to remove points on largest edge:
					//yStart edge is largest so points will be removed from this edge:
					//IJ.log("yStart > yEnd - before adjustEdgeContacts: "+a);
					pointArrays = yStartEdgeContact.adjustEdgeContacts(maxXY);    
					//set edge contact ref1 [Y] to 1 [START]:
					edgeContact[a][1] = 1;
					//IJ.log("yStart > yEnd - after adjustEdgeContacts: "+a);
					xPoints = pointArrays[1];
					yPoints = pointArrays[0];
				}
				else if(yStartEdgeContact.getLength() < yEndEdgeContact.getLength()) {
					//edit points array to remove points on largest edge:
					//yEnd edge is largest so points will be removed from this edge:
					//IJ.log("yStart < yEnd - before adjustEdgeContacts: "+a);
					pointArrays = yEndEdgeContact.adjustEdgeContacts(maxXY);
					//set edge contact ref1 [Y] to 2 [END]:
					edgeContact[a][1] = 2;
					//IJ.log("yStart < yEnd - after adjustEdgeContacts: "+a);
					xPoints = pointArrays[1];
					yPoints = pointArrays[0];

				}
				else if(yStartEdgeContact.getLength() == yEndEdgeContact.getLength()) {
					//check if both are 0:
					if(yStartEdgeContact.getLength() == 0) {
						//do nothing to points array -> as none of the ROI resides on the image edge in X..
						//set edge contact ref1 [Y] to 0:
						edgeContact[a][1] = 0;
					}
					else {
						//edit points array to remove points on largest edge:
						//in this case, will pick the yEnd [bottom] edge as default:
						//IJ.log("yStart == yEnd - before adjustEdgeContacts: "+a);
						pointArrays = yEndEdgeContact.adjustEdgeContacts(maxXY);
						//set edge contact ref1 [Y] to 2 [END]:
						edgeContact[a][1] = 2;
						//IJ.log("yStart == yEnd - after adjustEdgeContacts: "+a);
						xPoints = pointArrays[1];
						yPoints = pointArrays[0];
					}
				}

				//IJ.log("");
				//IJ.log("");

				//IJ.log("Roi Polygon points - Post adjustEdgeContacts: ");
				for(int b=0; b<xPoints.length; b++) {
					//IJ.log("xPoints: " + xPoints[b] + " yPoints: "+yPoints[b]);
				}
				//IJ.log("");
				//IJ.log("");

				//Add the new ROI to the exclRois arrayList:

				//IJ.log("before new ROI");
				
				// generate a new Polygon with the xPoints and yPoints arrays:
				polygon = new Polygon(xPoints, yPoints, xPoints.length);

				//First, build the ROI from the point arrays:
				//use points.length for nPoints, and get the original type from rois for type:
				//Roi roi = new PolygonRoi(xPoints, yPoints, xPoints.length, rois.get(a).getType() );
				Roi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON );

				//IJ.log("after new ROI");

				//finally, addthis new roi to exclRois:

				exclRois.add(roi);

			} //end else if all EdgeContact Objects equal 0
			
			


		} //end for a ROI loop.

		//IJ.log("end loop - exclRois size: "+exclRois.size() );

		RoisWithEdgeContacts rec = new RoisWithEdgeContacts(exclRois, edgeContact);

		return rec;

	} //end applyExclusionZone.
	
	/**
	 * Assess objects within the ROI - whole objects.  Does NOT save Roi Overview Data.
	 * 
	 * @param imageHandler
	 * @param objVal
	 * @param inVal
	 * @param outVal
	 * @param exclusionZ
	 */
	public void analyseRoisWholeObjects(ImageHandler imageHandler, DatasetWrapper datasetHandler,
			ObjectDatasetMap firstPixObjNoMap, File outputFile, int objVal, int inVal, int outVal, 
						String ClassifierAttribute, boolean exclusionXY, boolean exclusionZ) {

		int roiVolume;

		// loop through each ROI object:
		for(int a=0; a<borderPoints.size(); a++) {
			
			//IJ.showMessage("Processing Border Points: "+a);

			// process all border points - sort objects into 50%+ IN or OUT of current ROI:
				// Objs 50%+ IN -> inVal  :  Objs >50% OUT -> outVal
			imageHandler.processBorderPoints(borderPoints.get(a), imageHandler.thresholdImgInt.sizeZ, 
																	inVal, outVal, objVal, rois.get(a));

			// process the ROI with exclusion Zone applied (if appropriate):
			Roi roiExcl = getRoiExcl(exclusionXY, a);
			//IJ.log("ROI index: "+a+"exclZ: "+maxZ+" sizeZ: "+imageHandler.thresholdImgInt.sizeZ);
			int zMax = getZlengthExcl(exclusionZ, imageHandler.thresholdImgInt.sizeZ);
			
			//IJ.showMessage("Processing ROI Objects: "+a);

			roiVolume = imageHandler.processRoiObjects(roiExcl, inVal, objVal, zMax, firstPixObjNoMap, 
														datasetHandler, roiDatasets.get(a), ClassifierAttribute );
			
			//IJ.showMessage("Re-setting Border Points: "+a);
			// Finally, process borderPoints again to set any objects designated as OUT back to the default maxVal
				// pixel value:
			imageHandler.setBorderPoints(borderPoints.get(a), imageHandler.thresholdImgInt.sizeZ, outVal, objVal);


		} // end roi loop
		
	}
	
	/**
	 * Assess objects within the ROI - whole objects.
	 * 
	 * @param imageHandler
	 * @param objVal
	 * @param inVal
	 * @param outVal
	 * @param exclusionZ
	 */
	public void analyseRoisWholeObjects(ImageHandler imageHandler, DatasetWrapper datasetHandler,
			ObjectDatasetMap firstPixObjNoMap, DatasetWrapper roiOverviewDataset, File outputFile,
			int objVal, int inVal, int outVal, String ClassifierAttribute, boolean exclusionXY, boolean exclusionZ) {

		int roiVolume;

		// loop through each ROI object:
		for(int a=0; a<borderPoints.size(); a++) {
			
			//IJ.showMessage("Processing Border Points: "+a);

			// process all border points - sort objects into 50%+ IN or OUT of current ROI:
				// Objs 50%+ IN -> inVal  :  Objs >50% OUT -> outVal
			imageHandler.processBorderPoints(borderPoints.get(a), imageHandler.thresholdImgInt.sizeZ, 
																	inVal, outVal, objVal, rois.get(a));

			// process the ROI with exclusion Zone applied (if appropriate):
			Roi roiExcl = getRoiExcl(exclusionXY, a);
			//IJ.log("ROI index: "+a+"exclZ: "+maxZ+" sizeZ: "+imageHandler.thresholdImgInt.sizeZ);
			int zMax = getZlengthExcl(exclusionZ, imageHandler.thresholdImgInt.sizeZ);
			
			//IJ.showMessage("Processing ROI Objects: "+a);

			roiVolume = imageHandler.processRoiObjects(roiExcl, inVal, objVal, zMax, firstPixObjNoMap, 
														datasetHandler, roiDatasets.get(a), ClassifierAttribute );
			
			//IJ.showMessage("Re-setting Border Points: "+a);
			// Finally, process borderPoints again to set any objects designated as OUT back to the default maxVal
				// pixel value:
			imageHandler.setBorderPoints(borderPoints.get(a), imageHandler.thresholdImgInt.sizeZ, outVal, objVal);

			//IJ.showMessage("Adding Overview Data: "+a);
			// Add the data to the overview Data instances object:
			roiOverviewDataset.addData(  getOverviewData( roiOverviewDataset, roiDatasets.get(a), a, roiVolume, 
					roiExcl, zMax, outputFile )  );
			
			//IJ.showMessage("ROI Analysis Complete!: "+a);

		} // end roi loop
		
	}
	
	
	/**
	 * Assess objects within the ROI - objects and fragments.  Does NOT save Roi Overview Data.
	 * <p>
	 * Each Fragment touching the ROI border
	 * 
	 * @param imageHandler
	 * @param objVal
	 * @param inVal
	 * @param outVal
	 * @param exclusionZ
	 */
	public void analyseRoisObjectsAndFragments(ImageHandler imageHandler, File outputFile, int objVal, 
						String ClassifierAttribute, boolean exclusionXY, boolean exclusionZ) {

		for(int a=0; a<borderPoints.size(); a++) {

			// process the ROI with exclusion Zone applied (if appropriate):
			Roi roiExcl = getRoiExcl(exclusionXY, a);
			int zMax = getZlengthExcl(exclusionZ, imageHandler.thresholdImgInt.sizeZ);

			int roiVolume = 
					imageHandler.processRoiObjectsFragments(roiExcl, objVal, zMax, 
											roiDatasets.get(a), ClassifierAttribute );

		} // end roi loop
		
	}
	

	
	/**
	 * Assess objects within the ROI - objects and fragments.
	 * <p>
	 * Each Fragment touching the ROI border
	 * 
	 * @param imageHandler
	 * @param objVal
	 * @param inVal
	 * @param outVal
	 * @param exclusionZ
	 */
	public void analyseRoisObjectsAndFragments(ImageHandler imageHandler, DatasetWrapper roiOverviewDataset, 
					File outputFile, int objVal, String ClassifierAttribute, boolean exclusionXY, boolean exclusionZ) {

		for(int a=0; a<borderPoints.size(); a++) {

			// process the ROI with exclusion Zone applied (if appropriate):
			Roi roiExcl = getRoiExcl(exclusionXY, a);
			int zMax = getZlengthExcl(exclusionZ, imageHandler.thresholdImgInt.sizeZ);

			int roiVolume = 
					imageHandler.processRoiObjectsFragments(roiExcl, objVal, zMax, 
											roiDatasets.get(a), ClassifierAttribute );

			// Add the data to the overview Data instances object:
			roiOverviewDataset.addData(  getOverviewData( roiOverviewDataset, roiDatasets.get(a), a, roiVolume, 
					roiExcl, zMax, outputFile )  );

		} // end roi loop
		
	}
	
	/**
	 * Returns either roisExcl at index if exclusionXY is true, otherwise it returns rois at
	 * index.
	 * @param exclusionXY
	 * @param index
	 * @return
	 */
	public Roi getRoiExcl(boolean exclusionXY, int index) {
		
		if(exclusionXY == true) {
			return roisExcl.get(index);
		}
		else {
			return rois.get(index);
		}
		
	}
	
	/**
	 * Returns either sizeZ minus the maxZ [max object length in Z) if exclusionZ is true, otherwise
	 * returns sizeZ as z length for exclusion.
	 * @param exclusionZ
	 * @param sizeZ
	 * @return
	 */
	public int getZlengthExcl(boolean exclusionZ, int sizeZ) {
		
		if(exclusionZ == true) {
			return sizeZ-maxZ;
		}
		else {
			// if exclusion zone is not to be applied in Z, use impIntThresh sizeZ for
			// loop through image depth.
			return sizeZ;
		}
	}
	
	public Instance getOverviewData( DatasetWrapper dataOverview, DatasetWrapper data, 
			int roiNumber, int roiVolume, Roi roiExcl, int zDepth, File currentOutputFile ) {

		// Including image title, ROI number, ROI size, number of objects, and 
		// mean obj size, mean sphericity...
		
		// look at RoiDataContainer to see what an instance should look like!
			// there are 64 Attribute Values in RoiDataContainer:

		// first instance
		double[] vals = new double[64];  // important: needs NEW array with each call!

		// image title (String):
		vals[0] = dataOverview.attribute(0).addStringValue( currentOutputFile.getName() );

		// ROI number:						ROI Volume (voxels):
		vals[1] = (double)roiNumber;		vals[2] = (double)roiVolume;
		
		// ROI X Y Z Lengths:
		Rectangle r = roiExcl.getBounds();
		vals[3] = r.getWidth();		vals[4] = r.getHeight();		vals[5] = (double)zDepth;

		// number of objects:							number of objects PER PIXEL:
		vals[6] = (double)data.numInstances();			vals[7] = vals[6] / (double)roiVolume;
		
		// total pixel counts of all objects:
		vals[8] = computeSum(data, ObjectDataContainer.VOLVOXELS); 
		
		// total pixel counts of all objects PER PIXEL (fraction of ROI which is occupied by Objects):
		vals[9] = vals[8] / (double)roiVolume;
		
		double[] d; // to store stats variables
		
		// VOLVOXELS:		
		d = computeStats( data, ObjectDataContainer.VOLVOXELS );
		
		vals[10] = d[0]; // mean
		vals[11] = d[1]; // sd
		vals[12] = d[2]; // 95% CI lower
		vals[13] = d[3]; // 95% CI higher
		vals[14] = d[4]; // min
		vals[15] = d[5]; // 25th percentile
		vals[16] = d[6]; // median
		vals[17] = d[7]; // 75th percentile
		vals[18] = d[8]; // max
		
		// AREAVOXELS:		
		d = computeStats( data, ObjectDataContainer.AREAVOXELS );
		
		vals[19] = d[0]; // mean
		vals[20] = d[1]; // sd
		vals[21] = d[2]; // 95% CI lower
		vals[22] = d[3]; // 95% CI higher
		vals[23] = d[4]; // min
		vals[24] = d[5]; // 25th percentile
		vals[25] = d[6]; // median
		vals[26] = d[7]; // 75th percentile
		vals[27] = d[8]; // max
		
		// COMPACTNESS:		
		d = computeStats( data, ObjectDataContainer.COMPACTNESS );
		
		vals[28] = d[0]; // mean
		vals[29] = d[1]; // sd
		vals[30] = d[2]; // 95% CI lower
		vals[31] = d[3]; // 95% CI higher
		vals[32] = d[4]; // min
		vals[33] = d[5]; // 25th percentile
		vals[34] = d[6]; // median
		vals[35] = d[7]; // 75th percentile
		vals[36] = d[8]; // max
		
		// SPHERICITY:		
		d = computeStats( data, ObjectDataContainer.SPHERICITY );
		
		vals[37] = d[0]; // mean
		vals[38] = d[1]; // sd
		vals[39] = d[2]; // 95% CI lower
		vals[40] = d[3]; // 95% CI higher
		vals[41] = d[4]; // min
		vals[42] = d[5]; // 25th percentile
		vals[43] = d[6]; // median
		vals[44] = d[7]; // 75th percentile
		vals[45] = d[8]; // max
		
		// SOLIDITY3D:		
		d = computeStats( data, ObjectDataContainer.SOLIDITY3D );
		
		vals[46] = d[0]; // mean
		vals[47] = d[1]; // sd
		vals[48] = d[2]; // 95% CI lower
		vals[49] = d[3]; // 95% CI higher
		vals[50] = d[4]; // min
		vals[51] = d[5]; // 25th percentile
		vals[52] = d[6]; // median
		vals[53] = d[7]; // 75th percentile
		vals[54] = d[8]; // max
		
		// CONVEXITY3D:		
		d = computeStats( data, ObjectDataContainer.CONVEXITY3D );

		vals[55] = d[0]; // mean
		vals[56] = d[1]; // sd
		vals[57] = d[2]; // 95% CI lower
		vals[58] = d[3]; // 95% CI higher
		vals[59] = d[4]; // min
		vals[60] = d[5]; // 25th percentile
		vals[61] = d[6]; // median
		vals[62] = d[7]; // 75th percentile
		vals[63] = d[8]; // max

		//add vals to arff:
		return new DenseInstance(1.0, vals);

	}
	
	/**
	 * Computes Mean, SD, 95% CI lower & upper, Min, 25th, 50th [median] & 75th percentile &
	 * Max values of the data under attributeTitle.  The returned double[] is of length 9, and
	 * contains the aforementioned statistics in that order.
	 * @param data
	 * @param attributeTitle
	 * @return
	 */
	public double[] computeStats(DatasetWrapper data, String attributeTitle) {
		
		// collect the data from data in attributeTitle:
		double[] array = new double[ data.numInstances() ];

		for(int a=0; a<data.numInstances(); a++) {
			array[a] = data.instance(a).value( data.attribute(attributeTitle) );
		}

		// compute descriptive stats for this data:
		double[] stats = new double[9];
		
		// use commons-math3 library:
		DescriptiveStatistics ds = new DescriptiveStatistics(array);
		
		stats[0] = ds.getMean();
		stats[1] = ds.getStandardDeviation();
		// Calculate 95% confidence interval
        double ci = calcMeanCI(ds, 0.95);
		stats[2] = (ds.getMean() - ci); //lower
		stats[3] = (ds.getMean() + ci); //upper
		// 5-number summary:
		stats[4] = ds.getMin();
		stats[5] = ds.getPercentile(25.0);
		stats[6] = ds.getPercentile(50.0);
		stats[7] = ds.getPercentile(75.0);
		stats[8] = ds.getMax();

		return stats;

	}
	
    private static double calcMeanCI(DescriptiveStatistics stats, double level) {
        try {
            // Create T Distribution with N-1 degrees of freedom
            TDistribution tDist = new TDistribution(stats.getN() - 1);
            // Calculate critical value
            double critVal = tDist.inverseCumulativeProbability(1.0 - (1 - level) / 2);
            // Calculate confidence interval
            return critVal * stats.getStandardDeviation() / Math.sqrt(stats.getN());
        } catch (MathIllegalArgumentException e) {
            return Double.NaN;
        }
    }

	public double computeMean(DatasetWrapper data, String attributeTitle) {

		double sum = 0.0;

		for(int a=0; a<data.numInstances(); a++) {
			sum = sum + data.instance(a).value( data.attribute(attributeTitle) );
		}

		return (sum / data.numInstances() );

	}

	public double computeSum(DatasetWrapper data, String attributeTitle) {

		double sum = 0.0;

		for(int a=0; a<data.numInstances(); a++) {
			sum = sum + data.instance(a).value( data.attribute(attributeTitle) );
		}

		return sum;

	}

}
