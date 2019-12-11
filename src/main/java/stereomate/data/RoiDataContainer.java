package stereomate.data;

import java.util.ArrayList;

import weka.core.Attribute;

/**
 * The Roi DataContainer contains the following data for forming Attributes:
 * <p>
 * INTRINSIC ROI Measures:  Image and ROI identifiers (name and number), ROI volume, dimensions, shape?
 * <p>
 * Aggregate Object Measures:  Object count (numObjects) & density (objectDensity), 
 * pixel coverage (total [objectPixelCount] & percentage of ROI volume [objectCoverage]).
 * <p>
 * descriptive stats (mean, median, SD, IQR): volVoxel, areaVoxels, compactness, sphericity, solidity3D, 
 * convexity3D.
 * 
 * @author stevenwest
 *
 */
public class RoiDataContainer extends DataContainer2 {

	// TODO do not need summary stats of object measures - as can easily compute this in data analysis
		// Only want ROI-level Measures -> volume and shape of ROI, etc.
	
	String imageTitle; int roiNumber; int roiVolume; int roiX; int roiY; int roiZ;
	
	int numObjects; double objectDensity; int objectPixelCount; double objectCoverage;
	
	double volVoxMean; double volVoxSD; double volVox95cil; double volVox95cih;
	double volVoxMin; double volVox25; double volVoxMedian;double volVox75; double volVoxMax;
	
	double areaVoxMean; double areaVoxSD; double areaVox95cil; double areaVox95cih;
	double areaVoxMin; double areaVox25; double areaVoxMedian;double areaVox75; double areaVoxMax;
	
	double compMean; double compSD; double comp95cil; double comp95cih;
	double compMin; double comp25; double compMedian;double comp75; double compMax;
	
	double spherMean; double spherSD; double spher95cil; double spher95cih;
	double spherMin; double spher25; double spherMedian;double spher75; double spherMax;
	
	double solidMean; double solidSD; double solid95cil; double solid95cih;
	double solidMin; double solid25; double solidMedian;double solid75; double solidMax;
	
	double convexMean; double convexSD; double convex95cil; double convex95cih;
	double convexMin; double convex25; double convexMedian;double convex75; double convexMax;

	
	static final String IMAGETITLE = "ImageTitle";	static final String ROINUMBER = "ROINumber";
	
	static final String ROIVOLUME = "ROIVolume";		
	static final String ROIX = "ROIlengthX";	static final String ROIY = "ROIlengthY";	static final String ROIZ = "ROIlengthZ";

	static final String NUMOBJECTS = "TotalObjectNum";		static final String OBJECTDENSITY = "NumberOfObjectPerPixel";
	static final String OBJECTPIXELCOUNT = "NumberOfObjectPixels"; static final String OBJECTCOVERAGE = "FractionOfObjectPixelsToROIPixels";
	
	static final String VOLVOXMEAN = "ObjectVolumeVoxelsmean";	static final String VOLVOXSD = "ObjectVolumeVoxelsSD";
	static final String VOLVOX95CIL = "ObjectVolumeVoxels95%CIlower"; static final String VOLVOX95CIH = "ObjectVolumeVoxels95%CIhigher";
	static final String VOLVOXMIN = "ObjectVolumeVoxelsMin";		static final String VOLVOX25 = "ObjectVolumeVoxels25thPercentile";
	static final String VOLVOXMEDIAN = "ObjectVolumeVoxelsmedian";static final String VOLVOX75 = "ObjectVolumeVoxels75thPercentile";
	static final String VOLVOXMAX = "ObjectVolumeVoxelsMax";	
	
	static final String AREAVOXMEAN = "ObjectAreaVoxelsmean";	static final String AREAVOXSD = "ObjectAreaVoxelsSD";
	static final String AREAVOX95CIL = "ObjectAreaVoxels95%CIlower"; static final String AREAVOX95CIH = "ObjectAreaVoxels95%CIhigher";
	static final String AREAVOXMIN = "ObjectAreaVoxelsMin";		static final String AREAVOX25 = "ObjectAreaVoxels25thPercentile";
	static final String AREAVOXMEDIAN = "ObjectAreaVoxelsmedian";static final String AREAVOX75 = "ObjectAreaVoxels75thPercentile";
	static final String AREAVOXMAX = "ObjectAreaVoxelsMax";	
	
	static final String COMPACTMEAN = "ObjectCompactnessmean";	static final String COMPACTSD = "ObjectCompactnessSD";
	static final String COMPACT95CIL = "ObjectCompactness95%CIlower"; static final String COMPACT95CIH = "ObjectCompactness95%CIhigher";
	static final String COMPACTMIN = "ObjectCompactnessMin";		static final String COMPACT25 = "ObjectCompactness25thPercentile";
	static final String COMPACTMEDIAN = "ObjectCompactnessmedian";static final String COMPACT75 = "ObjectCompactness75thPercentile";
	static final String COMPACTMAX = "ObjectCompactnessMax";	
	
	static final String SPHERICITYMEAN = "ObjectSphericitymean";	static final String SPHERICITYSD = "ObjectSphericitySD";
	static final String SPHERICITY95CIL = "ObjectSphericity95%CIlower"; static final String SPHERICITY95CIH = "ObjectSphericity95%CIhigher";
	static final String SPHERICITYMIN = "ObjectSphericityMin";		static final String SPHERICITY25 = "ObjectSphericity25thPercentile";
	static final String SPHERICITYMEDIAN = "ObjectSphericitymedian";static final String SPHERICITY75 = "ObjectSphericity75thPercentile";
	static final String SPHERICITYMAX = "ObjectSphericityMax";	
	
	static final String SOLIDITYMEAN = "ObjectSoliditymean";	static final String SOLIDITYSD = "ObjectSoliditySD";
	static final String SOLIDITY95CIL = "ObjectSolidity95%CIlower"; static final String SOLIDITY95CIH = "ObjectSolidity95%CIhigher";
	static final String SOLIDITYMIN = "ObjectSolidityMin";		static final String SOLIDITY25 = "ObjectSolidity25thPercentile";
	static final String SOLIDITYMEDIAN = "ObjectSoliditymedian";static final String SOLIDITY75 = "ObjectSolidity75thPercentile";
	static final String SOLIDITYMAX = "ObjectSolidityMax";	
	
	static final String CONVEXITYMEAN = "ObjectConvexitymean";	static final String CONVEXITYSD = "ObjectConvexitySD";
	static final String CONVEXITY95CIL = "ObjectConvexity95%CIlower"; static final String CONVEXITY95CIH = "ObjectConvexity95%CIhigher";
	static final String CONVEXITYMIN = "ObjectConvexityMin";		static final String CONVEXITY25 = "ObjectConvexity25thPercentile";
	static final String CONVEXITYMEDIAN = "ObjectConvexitymedian";static final String CONVEXITY75 = "ObjectConvexity75thPercentile";
	static final String CONVEXITYMAX = "ObjectConvexityMax";	
	
	/**
	 * Initialises the DataObj with default 0 and 0.0 values, and sets the dataValues and dataTitles arrays.
	 */
	public RoiDataContainer() {
		
		super();
		
	}
	
	/**
	 * Initialises the DataObj with the passed parameter values.
	 */
	public RoiDataContainer(String imageTitle, int roiNumber, int roiVolume, int roiX, int roiY, int roiZ,
							int numObjects, double objectDensity, int objectPixelCount, double objectCoverage,
							double volVoxMean, double volVoxSD, double volVox95cil, double volVox95cih,
							double volVoxMin, double volVox25, double volVoxMedian,double volVox75, double volVoxMax,
							double areaVoxMean, double areaVoxSD, double areaVox95cil, double areaVox95cih,
							double areaVoxMin, double areaVox25, double areaVoxMedian,double areaVox75, double areaVoxMax,
							double compMean, double compSD, double comp95cil, double comp95cih,
							double compMin, double comp25, double compMedian,double comp75, double compMax,
							double spherMean, double spherSD, double spher95cil, double spher95cih,
							double spherMin, double spher25, double spherMedian,double spher75, double spherMax,
							double solidMean, double solidSD, double solid95cil, double solid95cih,
							double solidMin, double solid25, double solidMedian,double solid75, double solidMax,
							double convexMean, double convexSD, double convex95cil, double convex95cih,
							double convexMin, double convex25, double convexMedian,double convex75, double convexMax) {
		
		this.imageTitle = imageTitle; this.roiNumber = roiNumber; this.roiVolume = roiVolume; 
		this.roiX = roiX; this.roiY = roiY; this.roiZ = roiZ;
		
		this.numObjects = numObjects; this.objectDensity = objectDensity; 
		this.objectPixelCount = objectPixelCount; this.objectCoverage = objectCoverage;
		
		//this.volVoxMean = volVoxMean; this.volVoxSD = volVoxSD; this.volVox95cil = volVox95cil; this.volVox95cih = volVox95cih;
		//this.volVoxMin = volVoxMin; this.volVox25 = volVox25; this.volVoxMedian = volVoxMedian; this.volVox75 = volVox75; this.volVoxMax = volVoxMax;
		
		//this.areaVoxMean = areaVoxMean; this.areaVoxSD = areaVoxSD; this.areaVox95cil = areaVox95cil; this.areaVox95cih = areaVox95cih;
		//this.areaVoxMin = areaVoxMin; this.areaVox25 = areaVox25; this.areaVoxMedian = areaVoxMedian; this.areaVox75 = areaVox75; this.areaVoxMax = areaVoxMax;
		
		//this.compMean = compMean; this.compSD = compSD; this.comp95cil = comp95cil; this.comp95cih = comp95cih;
		//this.compMin = compMin; this.comp25 = comp25; this.compMedian = compMedian; this.comp75 = comp75; this.compMax = compMax;
		
		//this.spherMean = spherMean; this.spherSD = spherSD; this.spher95cil = spher95cil; this.spher95cih = spher95cih;
		//this.spherMin = spherMin; this.spher25 = spher25; this.spherMedian = spherMedian; this.spher75 = spher75; this.spherMax = spherMax;
		
		//this.solidMean = solidMean; this.solidSD = solidSD; this.solid95cil = solid95cil; this.solid95cih = solid95cih;
		//this.solidMin = solidMin; this.solid25 = solid25; this.solidMedian = solidMedian; this.solid75 = solid75; this.solidMax = solidMax;
		
		//this.convexMean = convexMean; this.convexSD = convexSD; this.convex95cil = convex95cil; this.convex95cih = convex95cih;
		//this.convexMin = convexMin; this.convex25 = convex25; this.convexMedian = convexMedian; this.convex75 = convex75; this.convexMax = convexMax;
		
		setDataValuesAndTitles();
		
	}



	/**
	 * @return the imageTitle
	 */
	public String getImageTitle() {
		return imageTitle;
	}

	/**
	 * @param imageTitle the imageTitle to set
	 */
	public void setImageTitle(String imageTitle) {
		this.imageTitle = imageTitle;
	}

	/**
	 * @return the roiNumber
	 */
	public int getRoiNumber() {
		return roiNumber;
	}

	/**
	 * @param roiNumber the roiNumber to set
	 */
	public void setRoiNumber(int roiNumber) {
		this.roiNumber = roiNumber;
	}

	/**
	 * @return the roiVolume
	 */
	public int getRoiVolume() {
		return roiVolume;
	}

	/**
	 * @param roiVolume the roiVolume to set
	 */
	public void setRoiVolume(int roiVolume) {
		this.roiVolume = roiVolume;
	}

	/**
	 * @return the roiX
	 */
	public int getRoiX() {
		return roiX;
	}

	/**
	 * @param roiX the roiX to set
	 */
	public void setRoiX(int roiX) {
		this.roiX = roiX;
	}

	/**
	 * @return the roiY
	 */
	public int getRoiY() {
		return roiY;
	}

	/**
	 * @param roiY the roiY to set
	 */
	public void setRoiY(int roiY) {
		this.roiY = roiY;
	}

	/**
	 * @return the roiZ
	 */
	public int getRoiZ() {
		return roiZ;
	}

	/**
	 * @param roiZ the roiZ to set
	 */
	public void setRoiZ(int roiZ) {
		this.roiZ = roiZ;
	}

	/**
	 * @return the numObjects
	 */
	public int getNumObjects() {
		return numObjects;
	}

	/**
	 * @param numObjects the numObjects to set
	 */
	public void setNumObjects(int numObjects) {
		this.numObjects = numObjects;
	}

	/**
	 * @return the objectDensity
	 */
	public double getObjectDensity() {
		return objectDensity;
	}

	/**
	 * @param objectDensity the objectDensity to set
	 */
	public void setObjectDensity(double objectDensity) {
		this.objectDensity = objectDensity;
	}

	/**
	 * @return the objectPixelCount
	 */
	public int getObjectPixelCount() {
		return objectPixelCount;
	}

	/**
	 * @param objectPixelCount the objectPixelCount to set
	 */
	public void setObjectPixelCount(int objectPixelCount) {
		this.objectPixelCount = objectPixelCount;
	}

	/**
	 * @return the objectCoverage
	 */
	public double getObjectCoverage() {
		return objectCoverage;
	}

	/**
	 * @param objectCoverage the objectCoverage to set
	 */
	public void setObjectCoverage(double objectCoverage) {
		this.objectCoverage = objectCoverage;
	}

	/**
	 * @return the volVoxMean
	 */
	public double getVolVoxMean() {
		return volVoxMean;
	}

	/**
	 * @param volVoxMean the volVoxMean to set
	 */
	public void setVolVoxMean(double volVoxMean) {
		this.volVoxMean = volVoxMean;
	}

	/**
	 * @return the volVoxSD
	 */
	public double getVolVoxSD() {
		return volVoxSD;
	}

	/**
	 * @param volVoxSD the volVoxSD to set
	 */
	public void setVolVoxSD(double volVoxSD) {
		this.volVoxSD = volVoxSD;
	}

	/**
	 * @return the volVox95cil
	 */
	public double getVolVox95cil() {
		return volVox95cil;
	}

	/**
	 * @param volVox95cil the volVox95cil to set
	 */
	public void setVolVox95cil(double volVox95cil) {
		this.volVox95cil = volVox95cil;
	}

	/**
	 * @return the volVox95cih
	 */
	public double getVolVox95cih() {
		return volVox95cih;
	}

	/**
	 * @param volVox95cih the volVox95cih to set
	 */
	public void setVolVox95cih(double volVox95cih) {
		this.volVox95cih = volVox95cih;
	}

	/**
	 * @return the volVoxMin
	 */
	public double getVolVoxMin() {
		return volVoxMin;
	}

	/**
	 * @param volVoxMin the volVoxMin to set
	 */
	public void setVolVoxMin(double volVoxMin) {
		this.volVoxMin = volVoxMin;
	}

	/**
	 * @return the volVox25
	 */
	public double getVolVox25() {
		return volVox25;
	}

	/**
	 * @param volVox25 the volVox25 to set
	 */
	public void setVolVox25(double volVox25) {
		this.volVox25 = volVox25;
	}

	/**
	 * @return the volVoxMedian
	 */
	public double getVolVoxMedian() {
		return volVoxMedian;
	}

	/**
	 * @param volVoxMedian the volVoxMedian to set
	 */
	public void setVolVoxMedian(double volVoxMedian) {
		this.volVoxMedian = volVoxMedian;
	}

	/**
	 * @return the volVox75
	 */
	public double getVolVox75() {
		return volVox75;
	}

	/**
	 * @param volVox75 the volVox75 to set
	 */
	public void setVolVox75(double volVox75) {
		this.volVox75 = volVox75;
	}

	/**
	 * @return the volVoxMax
	 */
	public double getVolVoxMax() {
		return volVoxMax;
	}

	/**
	 * @param volVoxMax the volVoxMax to set
	 */
	public void setVolVoxMax(double volVoxMax) {
		this.volVoxMax = volVoxMax;
	}

	/**
	 * @return the areaVoxMean
	 */
	public double getAreaVoxMean() {
		return areaVoxMean;
	}

	/**
	 * @param areaVoxMean the areaVoxMean to set
	 */
	public void setAreaVoxMean(double areaVoxMean) {
		this.areaVoxMean = areaVoxMean;
	}

	/**
	 * @return the areaVoxSD
	 */
	public double getAreaVoxSD() {
		return areaVoxSD;
	}

	/**
	 * @param areaVoxSD the areaVoxSD to set
	 */
	public void setAreaVoxSD(double areaVoxSD) {
		this.areaVoxSD = areaVoxSD;
	}

	/**
	 * @return the areaVox95cil
	 */
	public double getAreaVox95cil() {
		return areaVox95cil;
	}

	/**
	 * @param areaVox95cil the areaVox95cil to set
	 */
	public void setAreaVox95cil(double areaVox95cil) {
		this.areaVox95cil = areaVox95cil;
	}

	/**
	 * @return the areaVox95cih
	 */
	public double getAreaVox95cih() {
		return areaVox95cih;
	}

	/**
	 * @param areaVox95cih the areaVox95cih to set
	 */
	public void setAreaVox95cih(double areaVox95cih) {
		this.areaVox95cih = areaVox95cih;
	}

	/**
	 * @return the areaVoxMin
	 */
	public double getAreaVoxMin() {
		return areaVoxMin;
	}

	/**
	 * @param areaVoxMin the areaVoxMin to set
	 */
	public void setAreaVoxMin(double areaVoxMin) {
		this.areaVoxMin = areaVoxMin;
	}

	/**
	 * @return the areaVox25
	 */
	public double getAreaVox25() {
		return areaVox25;
	}

	/**
	 * @param areaVox25 the areaVox25 to set
	 */
	public void setAreaVox25(double areaVox25) {
		this.areaVox25 = areaVox25;
	}

	/**
	 * @return the areaVoxMedian
	 */
	public double getAreaVoxMedian() {
		return areaVoxMedian;
	}

	/**
	 * @param areaVoxMedian the areaVoxMedian to set
	 */
	public void setAreaVoxMedian(double areaVoxMedian) {
		this.areaVoxMedian = areaVoxMedian;
	}

	/**
	 * @return the areaVox75
	 */
	public double getAreaVox75() {
		return areaVox75;
	}

	/**
	 * @param areaVox75 the areaVox75 to set
	 */
	public void setAreaVox75(double areaVox75) {
		this.areaVox75 = areaVox75;
	}

	/**
	 * @return the areaVoxMax
	 */
	public double getAreaVoxMax() {
		return areaVoxMax;
	}

	/**
	 * @param areaVoxMax the areaVoxMax to set
	 */
	public void setAreaVoxMax(double areaVoxMax) {
		this.areaVoxMax = areaVoxMax;
	}

	/**
	 * @return the compMean
	 */
	public double getCompMean() {
		return compMean;
	}

	/**
	 * @param compMean the compMean to set
	 */
	public void setCompMean(double compMean) {
		this.compMean = compMean;
	}

	/**
	 * @return the compSD
	 */
	public double getCompSD() {
		return compSD;
	}

	/**
	 * @param compSD the compSD to set
	 */
	public void setCompSD(double compSD) {
		this.compSD = compSD;
	}

	/**
	 * @return the comp95cil
	 */
	public double getComp95cil() {
		return comp95cil;
	}

	/**
	 * @param comp95cil the comp95cil to set
	 */
	public void setComp95cil(double comp95cil) {
		this.comp95cil = comp95cil;
	}

	/**
	 * @return the comp95cih
	 */
	public double getComp95cih() {
		return comp95cih;
	}

	/**
	 * @param comp95cih the comp95cih to set
	 */
	public void setComp95cih(double comp95cih) {
		this.comp95cih = comp95cih;
	}

	/**
	 * @return the compMin
	 */
	public double getCompMin() {
		return compMin;
	}

	/**
	 * @param compMin the compMin to set
	 */
	public void setCompMin(double compMin) {
		this.compMin = compMin;
	}

	/**
	 * @return the comp25
	 */
	public double getComp25() {
		return comp25;
	}

	/**
	 * @param comp25 the comp25 to set
	 */
	public void setComp25(double comp25) {
		this.comp25 = comp25;
	}

	/**
	 * @return the compMedian
	 */
	public double getCompMedian() {
		return compMedian;
	}

	/**
	 * @param compMedian the compMedian to set
	 */
	public void setCompMedian(double compMedian) {
		this.compMedian = compMedian;
	}

	/**
	 * @return the comp75
	 */
	public double getComp75() {
		return comp75;
	}

	/**
	 * @param comp75 the comp75 to set
	 */
	public void setComp75(double comp75) {
		this.comp75 = comp75;
	}

	/**
	 * @return the compMax
	 */
	public double getCompMax() {
		return compMax;
	}

	/**
	 * @param compMax the compMax to set
	 */
	public void setCompMax(double compMax) {
		this.compMax = compMax;
	}

	/**
	 * @return the spherMean
	 */
	public double getSpherMean() {
		return spherMean;
	}

	/**
	 * @param spherMean the spherMean to set
	 */
	public void setSpherMean(double spherMean) {
		this.spherMean = spherMean;
	}

	/**
	 * @return the spherSD
	 */
	public double getSpherSD() {
		return spherSD;
	}

	/**
	 * @param spherSD the spherSD to set
	 */
	public void setSpherSD(double spherSD) {
		this.spherSD = spherSD;
	}

	/**
	 * @return the spher95cil
	 */
	public double getSpher95cil() {
		return spher95cil;
	}

	/**
	 * @param spher95cil the spher95cil to set
	 */
	public void setSpher95cil(double spher95cil) {
		this.spher95cil = spher95cil;
	}

	/**
	 * @return the spher95cih
	 */
	public double getSpher95cih() {
		return spher95cih;
	}

	/**
	 * @param spher95cih the spher95cih to set
	 */
	public void setSpher95cih(double spher95cih) {
		this.spher95cih = spher95cih;
	}

	/**
	 * @return the spherMin
	 */
	public double getSpherMin() {
		return spherMin;
	}

	/**
	 * @param spherMin the spherMin to set
	 */
	public void setSpherMin(double spherMin) {
		this.spherMin = spherMin;
	}

	/**
	 * @return the spher25
	 */
	public double getSpher25() {
		return spher25;
	}

	/**
	 * @param spher25 the spher25 to set
	 */
	public void setSpher25(double spher25) {
		this.spher25 = spher25;
	}

	/**
	 * @return the spherMedian
	 */
	public double getSpherMedian() {
		return spherMedian;
	}

	/**
	 * @param spherMedian the spherMedian to set
	 */
	public void setSpherMedian(double spherMedian) {
		this.spherMedian = spherMedian;
	}

	/**
	 * @return the spher75
	 */
	public double getSpher75() {
		return spher75;
	}

	/**
	 * @param spher75 the spher75 to set
	 */
	public void setSpher75(double spher75) {
		this.spher75 = spher75;
	}

	/**
	 * @return the spherMax
	 */
	public double getSpherMax() {
		return spherMax;
	}

	/**
	 * @param spherMax the spherMax to set
	 */
	public void setSpherMax(double spherMax) {
		this.spherMax = spherMax;
	}

	/**
	 * @return the solidMean
	 */
	public double getSolidMean() {
		return solidMean;
	}

	/**
	 * @param solidMean the solidMean to set
	 */
	public void setSolidMean(double solidMean) {
		this.solidMean = solidMean;
	}

	/**
	 * @return the solidSD
	 */
	public double getSolidSD() {
		return solidSD;
	}

	/**
	 * @param solidSD the solidSD to set
	 */
	public void setSolidSD(double solidSD) {
		this.solidSD = solidSD;
	}

	/**
	 * @return the solid95cil
	 */
	public double getSolid95cil() {
		return solid95cil;
	}

	/**
	 * @param solid95cil the solid95cil to set
	 */
	public void setSolid95cil(double solid95cil) {
		this.solid95cil = solid95cil;
	}

	/**
	 * @return the solid95cih
	 */
	public double getSolid95cih() {
		return solid95cih;
	}

	/**
	 * @param solid95cih the solid95cih to set
	 */
	public void setSolid95cih(double solid95cih) {
		this.solid95cih = solid95cih;
	}

	/**
	 * @return the solidMin
	 */
	public double getSolidMin() {
		return solidMin;
	}

	/**
	 * @param solidMin the solidMin to set
	 */
	public void setSolidMin(double solidMin) {
		this.solidMin = solidMin;
	}

	/**
	 * @return the solid25
	 */
	public double getSolid25() {
		return solid25;
	}

	/**
	 * @param solid25 the solid25 to set
	 */
	public void setSolid25(double solid25) {
		this.solid25 = solid25;
	}

	/**
	 * @return the solidMedian
	 */
	public double getSolidMedian() {
		return solidMedian;
	}

	/**
	 * @param solidMedian the solidMedian to set
	 */
	public void setSolidMedian(double solidMedian) {
		this.solidMedian = solidMedian;
	}

	/**
	 * @return the solid75
	 */
	public double getSolid75() {
		return solid75;
	}

	/**
	 * @param solid75 the solid75 to set
	 */
	public void setSolid75(double solid75) {
		this.solid75 = solid75;
	}

	/**
	 * @return the solidMax
	 */
	public double getSolidMax() {
		return solidMax;
	}

	/**
	 * @param solidMax the solidMax to set
	 */
	public void setSolidMax(double solidMax) {
		this.solidMax = solidMax;
	}

	/**
	 * @return the convexMean
	 */
	public double getConvexMean() {
		return convexMean;
	}

	/**
	 * @param convexMean the convexMean to set
	 */
	public void setConvexMean(double convexMean) {
		this.convexMean = convexMean;
	}

	/**
	 * @return the convexSD
	 */
	public double getConvexSD() {
		return convexSD;
	}

	/**
	 * @param convexSD the convexSD to set
	 */
	public void setConvexSD(double convexSD) {
		this.convexSD = convexSD;
	}

	/**
	 * @return the convex95cil
	 */
	public double getConvex95cil() {
		return convex95cil;
	}

	/**
	 * @param convex95cil the convex95cil to set
	 */
	public void setConvex95cil(double convex95cil) {
		this.convex95cil = convex95cil;
	}

	/**
	 * @return the convex95cih
	 */
	public double getConvex95cih() {
		return convex95cih;
	}

	/**
	 * @param convex95cih the convex95cih to set
	 */
	public void setConvex95cih(double convex95cih) {
		this.convex95cih = convex95cih;
	}

	/**
	 * @return the convexMin
	 */
	public double getConvexMin() {
		return convexMin;
	}

	/**
	 * @param convexMin the convexMin to set
	 */
	public void setConvexMin(double convexMin) {
		this.convexMin = convexMin;
	}

	/**
	 * @return the convex25
	 */
	public double getConvex25() {
		return convex25;
	}

	/**
	 * @param convex25 the convex25 to set
	 */
	public void setConvex25(double convex25) {
		this.convex25 = convex25;
	}

	/**
	 * @return the convexMedian
	 */
	public double getConvexMedian() {
		return convexMedian;
	}

	/**
	 * @param convexMedian the convexMedian to set
	 */
	public void setConvexMedian(double convexMedian) {
		this.convexMedian = convexMedian;
	}

	/**
	 * @return the convex75
	 */
	public double getConvex75() {
		return convex75;
	}

	/**
	 * @param convex75 the convex75 to set
	 */
	public void setConvex75(double convex75) {
		this.convex75 = convex75;
	}

	/**
	 * @return the convexMax
	 */
	public double getConvexMax() {
		return convexMax;
	}

	/**
	 * @param convexMax the convexMax to set
	 */
	public void setConvexMax(double convexMax) {
		this.convexMax = convexMax;
	}

	/**
	 * @return the imagetitle
	 */
	public static String getImagetitle() {
		return IMAGETITLE;
	}

	/**
	 * @return the roinumber
	 */
	public static String getRoinumber() {
		return ROINUMBER;
	}

	/**
	 * @return the roivolume
	 */
	public static String getRoivolume() {
		return ROIVOLUME;
	}

	/**
	 * @return the roix
	 */
	public static String getRoix() {
		return ROIX;
	}

	/**
	 * @return the roiy
	 */
	public static String getRoiy() {
		return ROIY;
	}

	/**
	 * @return the roiz
	 */
	public static String getRoiz() {
		return ROIZ;
	}

	/**
	 * @return the numobjects
	 */
	public static String getNumobjects() {
		return NUMOBJECTS;
	}

	/**
	 * @return the objectdensity
	 */
	public static String getObjectdensity() {
		return OBJECTDENSITY;
	}

	/**
	 * @return the objectpixelcount
	 */
	public static String getObjectpixelcount() {
		return OBJECTPIXELCOUNT;
	}

	/**
	 * @return the objectcoverage
	 */
	public static String getObjectcoverage() {
		return OBJECTCOVERAGE;
	}

	/**
	 * @return the volvoxmean
	 */
	public static String getVolvoxmean() {
		return VOLVOXMEAN;
	}

	/**
	 * @return the volvoxsd
	 */
	public static String getVolvoxsd() {
		return VOLVOXSD;
	}

	/**
	 * @return the volvox95cil
	 */
	public static String getVolvox95cil() {
		return VOLVOX95CIL;
	}

	/**
	 * @return the volvox95cih
	 */
	public static String getVolvox95cih() {
		return VOLVOX95CIH;
	}

	/**
	 * @return the volvoxmin
	 */
	public static String getVolvoxmin() {
		return VOLVOXMIN;
	}

	/**
	 * @return the volvox25
	 */
	public static String getVolvox25() {
		return VOLVOX25;
	}

	/**
	 * @return the volvoxmedian
	 */
	public static String getVolvoxmedian() {
		return VOLVOXMEDIAN;
	}

	/**
	 * @return the volvox75
	 */
	public static String getVolvox75() {
		return VOLVOX75;
	}

	/**
	 * @return the volvoxmax
	 */
	public static String getVolvoxmax() {
		return VOLVOXMAX;
	}

	/**
	 * @return the areavoxmean
	 */
	public static String getAreavoxmean() {
		return AREAVOXMEAN;
	}

	/**
	 * @return the areavoxsd
	 */
	public static String getAreavoxsd() {
		return AREAVOXSD;
	}

	/**
	 * @return the areavox95cil
	 */
	public static String getAreavox95cil() {
		return AREAVOX95CIL;
	}

	/**
	 * @return the areavox95cih
	 */
	public static String getAreavox95cih() {
		return AREAVOX95CIH;
	}

	/**
	 * @return the areavoxmin
	 */
	public static String getAreavoxmin() {
		return AREAVOXMIN;
	}

	/**
	 * @return the areavox25
	 */
	public static String getAreavox25() {
		return AREAVOX25;
	}

	/**
	 * @return the areavoxmedian
	 */
	public static String getAreavoxmedian() {
		return AREAVOXMEDIAN;
	}

	/**
	 * @return the areavox75
	 */
	public static String getAreavox75() {
		return AREAVOX75;
	}

	/**
	 * @return the areavoxmax
	 */
	public static String getAreavoxmax() {
		return AREAVOXMAX;
	}

	/**
	 * @return the compactmean
	 */
	public static String getCompactmean() {
		return COMPACTMEAN;
	}

	/**
	 * @return the compactsd
	 */
	public static String getCompactsd() {
		return COMPACTSD;
	}

	/**
	 * @return the compact95cil
	 */
	public static String getCompact95cil() {
		return COMPACT95CIL;
	}

	/**
	 * @return the compact95cih
	 */
	public static String getCompact95cih() {
		return COMPACT95CIH;
	}

	/**
	 * @return the compactmin
	 */
	public static String getCompactmin() {
		return COMPACTMIN;
	}

	/**
	 * @return the compact25
	 */
	public static String getCompact25() {
		return COMPACT25;
	}

	/**
	 * @return the compactmedian
	 */
	public static String getCompactmedian() {
		return COMPACTMEDIAN;
	}

	/**
	 * @return the compact75
	 */
	public static String getCompact75() {
		return COMPACT75;
	}

	/**
	 * @return the compactmax
	 */
	public static String getCompactmax() {
		return COMPACTMAX;
	}

	/**
	 * @return the sphericitymean
	 */
	public static String getSphericitymean() {
		return SPHERICITYMEAN;
	}

	/**
	 * @return the sphericitysd
	 */
	public static String getSphericitysd() {
		return SPHERICITYSD;
	}

	/**
	 * @return the sphericity95cil
	 */
	public static String getSphericity95cil() {
		return SPHERICITY95CIL;
	}

	/**
	 * @return the sphericity95cih
	 */
	public static String getSphericity95cih() {
		return SPHERICITY95CIH;
	}

	/**
	 * @return the sphericitymin
	 */
	public static String getSphericitymin() {
		return SPHERICITYMIN;
	}

	/**
	 * @return the sphericity25
	 */
	public static String getSphericity25() {
		return SPHERICITY25;
	}

	/**
	 * @return the sphericitymedian
	 */
	public static String getSphericitymedian() {
		return SPHERICITYMEDIAN;
	}

	/**
	 * @return the sphericity75
	 */
	public static String getSphericity75() {
		return SPHERICITY75;
	}

	/**
	 * @return the sphericitymax
	 */
	public static String getSphericitymax() {
		return SPHERICITYMAX;
	}

	/**
	 * @return the soliditymean
	 */
	public static String getSoliditymean() {
		return SOLIDITYMEAN;
	}

	/**
	 * @return the soliditysd
	 */
	public static String getSoliditysd() {
		return SOLIDITYSD;
	}

	/**
	 * @return the solidity95cil
	 */
	public static String getSolidity95cil() {
		return SOLIDITY95CIL;
	}

	/**
	 * @return the solidity95cih
	 */
	public static String getSolidity95cih() {
		return SOLIDITY95CIH;
	}

	/**
	 * @return the soliditymin
	 */
	public static String getSoliditymin() {
		return SOLIDITYMIN;
	}

	/**
	 * @return the solidity25
	 */
	public static String getSolidity25() {
		return SOLIDITY25;
	}

	/**
	 * @return the soliditymedian
	 */
	public static String getSoliditymedian() {
		return SOLIDITYMEDIAN;
	}

	/**
	 * @return the solidity75
	 */
	public static String getSolidity75() {
		return SOLIDITY75;
	}

	/**
	 * @return the soliditymax
	 */
	public static String getSoliditymax() {
		return SOLIDITYMAX;
	}

	/**
	 * @return the convexitymean
	 */
	public static String getConvexitymean() {
		return CONVEXITYMEAN;
	}

	/**
	 * @return the convexitysd
	 */
	public static String getConvexitysd() {
		return CONVEXITYSD;
	}

	/**
	 * @return the convexity95cil
	 */
	public static String getConvexity95cil() {
		return CONVEXITY95CIL;
	}

	/**
	 * @return the convexity95cih
	 */
	public static String getConvexity95cih() {
		return CONVEXITY95CIH;
	}

	/**
	 * @return the convexitymin
	 */
	public static String getConvexitymin() {
		return CONVEXITYMIN;
	}

	/**
	 * @return the convexity25
	 */
	public static String getConvexity25() {
		return CONVEXITY25;
	}

	/**
	 * @return the convexitymedian
	 */
	public static String getConvexitymedian() {
		return CONVEXITYMEDIAN;
	}

	/**
	 * @return the convexity75
	 */
	public static String getConvexity75() {
		return CONVEXITY75;
	}

	/**
	 * @return the convexitymax
	 */
	public static String getConvexitymax() {
		return CONVEXITYMAX;
	}

	public static Attribute returnImageTitleAttr() {
		return new Attribute(IMAGETITLE, (ArrayList<String>) null);
	}
	
	public static Attribute returnRoiNumberAttr() {
		return new Attribute(ROINUMBER);
	}
	
	public static Attribute returnRoiVolumeAttr() {
		return new Attribute(ROIVOLUME);
	}
	
	public static Attribute returnRoiXAttr() {
		return new Attribute(ROIX);
	}
	
	public static Attribute returnRoiYAttr() {
		return new Attribute(ROIY);
	}
	
	public static Attribute returnRoiZAttr() {
		return new Attribute(ROIZ);
	}
	
	public static Attribute returnNumObjectsAttr() {
		return new Attribute(NUMOBJECTS);
	}
	
	public static Attribute returnObjectDensityAttr() {
		return new Attribute(OBJECTDENSITY);
	}
	
	public static Attribute returnObjectPixelCountAttr() {
		return new Attribute(OBJECTPIXELCOUNT);
	}
	
	public static Attribute returnObjectCoverageAttr() {
		return new Attribute(OBJECTCOVERAGE);
	}
	
	
	
	public static Attribute returnVolVoxMeanAttr() {
		return new Attribute(VOLVOXMEAN);
	}
	
	public static Attribute returnVolVoxSDAttr() {
		return new Attribute(VOLVOXSD);
	}
	
	public static Attribute returnVolVox95CILAttr() {
		return new Attribute(VOLVOX95CIL);
	}
	
	public static Attribute returnVolVox95CIHAttr() {
		return new Attribute(VOLVOX95CIH);
	}
	
	public static Attribute returnVolVoxMinAttr() {
		return new Attribute(VOLVOXMIN);
	}
	
	public static Attribute returnVolVox25Attr() {
		return new Attribute(VOLVOX25);
	}
	
	public static Attribute returnVolVoxMedianAttr() {
		return new Attribute(VOLVOXMEDIAN);
	}
	
	public static Attribute returnVolVox75Attr() {
		return new Attribute(VOLVOX75);
	}
	
	public static Attribute returnVolVoxMaxAttr() {
		return new Attribute(VOLVOXMAX);
	}
	
	
	
	public static Attribute returnAreaVoxMeanAttr() {
		return new Attribute(AREAVOXMEAN);
	}
	
	public static Attribute returnAreaVoxSDAttr() {
		return new Attribute(AREAVOXSD);
	}
	
	public static Attribute returnAreaVox95CILAttr() {
		return new Attribute(AREAVOX95CIL);
	}
	
	public static Attribute returnAreaVox95CIHAttr() {
		return new Attribute(AREAVOX95CIH);
	}
	
	public static Attribute returnAreaVoxMinAttr() {
		return new Attribute(AREAVOXMIN);
	}
	
	public static Attribute returnAreaVox25Attr() {
		return new Attribute(AREAVOX25);
	}
	
	public static Attribute returnAreaVoxMedianAttr() {
		return new Attribute(AREAVOXMEDIAN);
	}
	
	public static Attribute returnAreaVox75Attr() {
		return new Attribute(AREAVOX75);
	}
	
	public static Attribute returnAreaVoxMaxAttr() {
		return new Attribute(AREAVOXMAX);
	}
	
	
	
	public static Attribute returnCompactMeanAttr() {
		return new Attribute(COMPACTMEAN);
	}
	
	public static Attribute returnCompactSDAttr() {
		return new Attribute(COMPACTSD);
	}
	
	public static Attribute returnCompact95CILAttr() {
		return new Attribute(COMPACT95CIL);
	}
	
	public static Attribute returnCompact95CIHAttr() {
		return new Attribute(COMPACT95CIH);
	}
	
	public static Attribute returnCompactMinAttr() {
		return new Attribute(COMPACTMIN);
	}
	
	public static Attribute returnCompact25Attr() {
		return new Attribute(COMPACT25);
	}
	
	public static Attribute returnCompactMedianAttr() {
		return new Attribute(COMPACTMEDIAN);
	}
	
	public static Attribute returnCompact75Attr() {
		return new Attribute(COMPACT75);
	}
	
	public static Attribute returnCompactMaxAttr() {
		return new Attribute(COMPACTMAX);
	}
	
	
	
	public static Attribute returnSphericityMeanAttr() {
		return new Attribute(SPHERICITYMEAN);
	}
	
	public static Attribute returnSphericitySDAttr() {
		return new Attribute(SPHERICITYSD);
	}
	
	public static Attribute returnSphericity95CILAttr() {
		return new Attribute(SPHERICITY95CIL);
	}
	
	public static Attribute returnSphericity95CIHAttr() {
		return new Attribute(SPHERICITY95CIH);
	}
	
	public static Attribute returnSphericityMinAttr() {
		return new Attribute(SPHERICITYMIN);
	}
	
	public static Attribute returnSphericity25Attr() {
		return new Attribute(SPHERICITY25);
	}
	
	public static Attribute returnSphericityMedianAttr() {
		return new Attribute(SPHERICITYMEDIAN);
	}
	
	public static Attribute returnSphericity75Attr() {
		return new Attribute(SPHERICITY75);
	}
	
	public static Attribute returnSphericityMaxAttr() {
		return new Attribute(SPHERICITYMAX);
	}
	
	
	
	public static Attribute returnSolidityMeanAttr() {
		return new Attribute(SOLIDITYMEAN);
	}
	
	public static Attribute returnSoliditySDAttr() {
		return new Attribute(SOLIDITYSD);
	}
	
	public static Attribute returnSolidity95CILAttr() {
		return new Attribute(SOLIDITY95CIL);
	}
	
	public static Attribute returnSolidity95CIHAttr() {
		return new Attribute(SOLIDITY95CIH);
	}
	
	public static Attribute returnSolidityMinAttr() {
		return new Attribute(SOLIDITYMIN);
	}
	
	public static Attribute returnSolidity25Attr() {
		return new Attribute(SOLIDITY25);
	}
	
	public static Attribute returnSolidityMedianAttr() {
		return new Attribute(SOLIDITYMEDIAN);
	}
	
	public static Attribute returnSolidity75Attr() {
		return new Attribute(SOLIDITY75);
	}
	
	public static Attribute returnSolidityMaxAttr() {
		return new Attribute(SOLIDITYMAX);
	}
	
	
	
	public static Attribute returnConvexityMeanAttr() {
		return new Attribute(CONVEXITYMEAN);
	}
	
	public static Attribute returnConvexitySDAttr() {
		return new Attribute(CONVEXITYSD);
	}
	
	public static Attribute returnConvexity95CILAttr() {
		return new Attribute(CONVEXITY95CIL);
	}
	
	public static Attribute returnConvexity95CIHAttr() {
		return new Attribute(CONVEXITY95CIH);
	}
	
	public static Attribute returnConvexityMinAttr() {
		return new Attribute(CONVEXITYMIN);
	}
	
	public static Attribute returnConvexity25Attr() {
		return new Attribute(CONVEXITY25);
	}
	
	public static Attribute returnConvexityMedianAttr() {
		return new Attribute(CONVEXITYMEDIAN);
	}
	
	public static Attribute returnConvexity75Attr() {
		return new Attribute(CONVEXITY75);
	}
	
	public static Attribute returnConvexityMaxAttr() {
		return new Attribute(CONVEXITYMAX);
	}
	
	
	
	public static ArrayList<Attribute> returnAttributes() {
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>(); 
		
		attributes.add( returnImageTitleAttr() );
		
		attributes.add( returnRoiNumberAttr() );
		attributes.add( returnRoiVolumeAttr() );
		
		attributes.add( returnRoiXAttr() );
		attributes.add( returnRoiYAttr() );
		attributes.add( returnRoiZAttr() );
		
		attributes.add( returnNumObjectsAttr() );
		attributes.add( returnObjectDensityAttr() );
		attributes.add( returnObjectPixelCountAttr() );
		attributes.add( returnObjectCoverageAttr() ); //10
		
		attributes.add( returnVolVoxMeanAttr() );
		attributes.add( returnVolVoxSDAttr() );
		attributes.add( returnVolVox95CILAttr() );
		attributes.add( returnVolVox95CIHAttr() );
		attributes.add( returnVolVoxMinAttr() );
		attributes.add( returnVolVox25Attr() );
		attributes.add( returnVolVoxMedianAttr() );
		attributes.add( returnVolVox75Attr() );
		attributes.add( returnVolVoxMaxAttr() );
		
		attributes.add( returnAreaVoxMeanAttr() );
		attributes.add( returnAreaVoxSDAttr() );
		attributes.add( returnAreaVox95CILAttr() );
		attributes.add( returnAreaVox95CIHAttr() );
		attributes.add( returnAreaVoxMinAttr() );
		attributes.add( returnAreaVox25Attr() );
		attributes.add( returnAreaVoxMedianAttr() );
		attributes.add( returnAreaVox75Attr() );
		attributes.add( returnAreaVoxMaxAttr() );
		
		attributes.add( returnCompactMeanAttr() );
		attributes.add( returnCompactSDAttr() );
		attributes.add( returnCompact95CILAttr() );
		attributes.add( returnCompact95CIHAttr() );
		attributes.add( returnCompactMinAttr() );
		attributes.add( returnCompact25Attr() );
		attributes.add( returnCompactMedianAttr() );
		attributes.add( returnCompact75Attr() );
		attributes.add( returnCompactMaxAttr() );
		
		attributes.add( returnSphericityMeanAttr() );
		attributes.add( returnSphericitySDAttr() );
		attributes.add( returnSphericity95CILAttr() );
		attributes.add( returnSphericity95CIHAttr() );
		attributes.add( returnSphericityMinAttr() );
		attributes.add( returnSphericity25Attr() );
		attributes.add( returnSphericityMedianAttr() );
		attributes.add( returnSphericity75Attr() );
		attributes.add( returnSphericityMaxAttr() );
		
		attributes.add( returnSolidityMeanAttr() );
		attributes.add( returnSoliditySDAttr() );
		attributes.add( returnSolidity95CILAttr() );
		attributes.add( returnSolidity95CIHAttr() );
		attributes.add( returnSolidityMinAttr() );
		attributes.add( returnSolidity25Attr() );
		attributes.add( returnSolidityMedianAttr() );
		attributes.add( returnSolidity75Attr() );
		attributes.add( returnSolidityMaxAttr() );
		
		attributes.add( returnConvexityMeanAttr() );
		attributes.add( returnConvexitySDAttr() );
		attributes.add( returnConvexity95CILAttr() );
		attributes.add( returnConvexity95CIHAttr() );
		attributes.add( returnConvexityMinAttr() );
		attributes.add( returnConvexity25Attr() );
		attributes.add( returnConvexityMedianAttr() );
		attributes.add( returnConvexity75Attr() );
		attributes.add( returnConvexityMaxAttr() );  //64
		
		return attributes;
		
	}
	
	
	/**
	 * Set the  dataValues and dataTitles arrays to the data values and datapoint titles.
	 */
	@Override
	public void setDataValuesAndTitles() {
		
		dataValues = new double[] { 
				  roiNumber,  roiVolume,  roiX,   roiY,   roiZ,
				  numObjects,   objectDensity,   objectPixelCount,   objectCoverage,
				  volVoxMean,   volVoxSD,   volVox95cil,   volVox95cih,
				  volVoxMin,   volVox25,   volVoxMedian,  volVox75,   volVoxMax,
				  areaVoxMean,   areaVoxSD,   areaVox95cil,   areaVox95cih,
				  areaVoxMin,   areaVox25,   areaVoxMedian,  areaVox75,   areaVoxMax,
				  compMean,   compSD,   comp95cil,   comp95cih,
				  compMin,   comp25,   compMedian,  comp75,   compMax,
				  spherMean,   spherSD,   spher95cil,   spher95cih,
				  spherMin,   spher25,   spherMedian,  spher75,   spherMax,
				  solidMean,   solidSD,   solid95cil,   solid95cih,
				  solidMin,   solid25,   solidMedian,  solid75,   solidMax,
				  convexMean,   convexSD,   convex95cil,   convex95cih,
				  convexMin,   convex25,   convexMedian,  convex75,   convexMax
		};
		
		dataTitles = new String[] {
				ROINUMBER, ROIVOLUME, ROIX, ROIY, ROIZ, NUMOBJECTS, OBJECTDENSITY, OBJECTPIXELCOUNT, OBJECTCOVERAGE,
				VOLVOXMEAN, VOLVOXSD, VOLVOX95CIL, VOLVOX95CIH, VOLVOXMIN, VOLVOX25, VOLVOXMEDIAN, VOLVOX75, VOLVOXMAX,
				AREAVOXMEAN, AREAVOXSD, AREAVOX95CIL, AREAVOX95CIH, AREAVOXMIN, AREAVOX25, AREAVOXMEDIAN, AREAVOX75, AREAVOXMAX,
				COMPACTMEAN, COMPACTSD, COMPACT95CIL, COMPACT95CIH, COMPACTMIN, COMPACT25, COMPACTMEDIAN, COMPACT75, COMPACTMAX,
				SPHERICITYMEAN, SPHERICITYSD, SPHERICITY95CIL, SPHERICITY95CIH, SPHERICITYMIN, SPHERICITY25, SPHERICITYMEDIAN, SPHERICITY75, SPHERICITYMAX,
				SOLIDITYMEAN, SOLIDITYSD, SOLIDITY95CIL, SOLIDITY95CIH, SOLIDITYMIN, SOLIDITY25, SOLIDITYMEDIAN, SOLIDITY75, SOLIDITYMAX,
				CONVEXITYMEAN, CONVEXITYSD, CONVEXITY95CIL, CONVEXITY95CIH, CONVEXITYMIN, CONVEXITY25, CONVEXITYMEDIAN, CONVEXITY75, CONVEXITYMAX
		};
		
	}

}
