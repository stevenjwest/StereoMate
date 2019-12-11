package stereomate.object;

import ij.IJ;
import ij.gui.Roi;
import ij.measure.ResultsTable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import mcib3d.geom.IntCoord3D;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Point3D;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.ImageByte;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageShort;
import stereomate.data.ObjectDataContainer;
import stereomate.settings.OM_ProcedureSettings;

/**
 * This file has been modified from the original source code, as detailed below.
 *
 * Flood3D from mcib3D 3.83
 * 
 * This now also calculates pixel locations in 26-connected algorithms, whether each pixel is within
 * a provided ROI -> can use this to check if the Object is 50%+ within the ROI.
 * 
 * SJW 2017.
 * 
 **
 * /**
 * Copyright (C) 2012 Jean Ollion
 *
 *
 *
 * This file is part of tango
 *
 * tango is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Jean Ollion
 */
public class ObjectVoxelProcessing {
	
	ArrayList<Short> oldValsProcessed;
	
	int thresholdVal = 255;
	int inRoiVal = 250;
	int outRoiVal = 128;
	int tempRoiVal = 1;
	//byte outRoiValByte = (byte) outRoiVal;  //note -> encoding unsigned int as a signed byte!
												//convert to int with int i = (b & 0xff);
	//int i = (outRoiValByte & 0xff);  //correct conversion of byte to int.
	
	String objConnectivity;

	
	public ObjectVoxelProcessing(String objConnectivity) {
		oldValsProcessed = new ArrayList<Short>();
		this.objConnectivity = objConnectivity;
		
	}
	
	
	/**
	 * 
	 * @param img
	 * @param seedX
	 * @param seedY
	 * @param seedZ
	 * @param borderRoi
	 * @param newVal
	 * @return
	 */
	public int[] borderObjPixCount3d62(ImageInt img, int seedX, int seedY, int seedZ, Roi borderRoi, int newVal) {
        IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
        int[] pixCounts = null;
        if (img instanceof ImageShort) {
        	//IJ.showMessage("Short");
            pixCounts = borderObjPixCount3D6((ImageShort) img, seed, borderRoi, newVal);
        } else if (img instanceof ImageByte) {
        	//IJ.showMessage("Byte");
        	pixCounts = borderObjPixCount3DByte6((ImageByte) img, seed, borderRoi, newVal);
        }
            return pixCounts;
    }
	
	
	private int[] borderObjPixCount3D6(ImageShort img, IntCoord3D seed, Roi borderRoi, int newVal) {
			int[] pixCounts = new int[2];
	        short[][] pixels = img.pixels;
	        int sizeX = img.sizeX;
	        int sizeY = img.sizeY;
	        int sizeZ = img.sizeZ;
	        short oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
	        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
	        queue.add(seed);
	        while (!queue.isEmpty()) {
	            IntCoord3D curCoord = queue.remove(0); // FIXME last element?
	            int xy = curCoord.x + curCoord.y * sizeX;
	            
	            if (pixels[curCoord.z][xy] == oldVal) {
	            	
	            	//check if pixel is inside the ROI:
	            	if( borderRoi.contains(curCoord.x, curCoord.y ) ) {
		            	pixCounts[0] = pixCounts[0] + 1;
		            }
		            else {
		            	pixCounts[1] = pixCounts[1] + 1;
		            }
	            	
	                pixels[curCoord.z][xy] = (short)newVal;
	                if (curCoord.x > 0 && pixels[curCoord.z][xy - 1] == oldVal) {
	                    queue.add(new IntCoord3D(curCoord.x - 1, curCoord.y, curCoord.z));
	                }
	                if (curCoord.x < (sizeX - 1) && pixels[curCoord.z][xy + 1] == oldVal) {
	                    queue.add(new IntCoord3D(curCoord.x + 1, curCoord.y, curCoord.z));
	                }
	                if (curCoord.y > 0 && pixels[curCoord.z][xy - sizeX] == oldVal) {
	                    queue.add(new IntCoord3D(curCoord.x, curCoord.y - 1, curCoord.z));
	                }
	                if (curCoord.y < (sizeY - 1) && pixels[curCoord.z][xy + sizeX] == oldVal) {
	                    queue.add(new IntCoord3D(curCoord.x, curCoord.y + 1, curCoord.z));
	                }
	                if (curCoord.z > 0 && pixels[curCoord.z - 1][xy] == oldVal) {
	                    queue.add(new IntCoord3D(curCoord.x, curCoord.y, curCoord.z - 1));
	                }
	                if (curCoord.z < (sizeZ - 1) && pixels[curCoord.z + 1][xy] == oldVal) {
	                    queue.add(new IntCoord3D(curCoord.x, curCoord.y, curCoord.z + 1));
	                }
	            }
	            else {
	            	//IJ.showMessage("pixels[curCoord.z][xy] == oldVal - FLASE");
	            }
	        }
	        
	        return pixCounts;
	    }
	
	
	private int[] borderObjPixCount3DByte6(ImageByte img, IntCoord3D seed, Roi borderRoi, int newVal) {
		int[] pixCounts = new int[2];
        byte[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        short oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            IntCoord3D curCoord = queue.remove(0); // FIXME last element?
            int xy = curCoord.x + curCoord.y * sizeX;
            
            if (pixels[curCoord.z][xy] == oldVal) {
            	
            	if( borderRoi.contains(curCoord.x, curCoord.y ) ) {
                	pixCounts[0] = pixCounts[0] + 1;
                }
                else {
                	pixCounts[1] = pixCounts[1] + 1;
                }
            	
                pixels[curCoord.z][xy] = (byte)newVal;
                if (curCoord.x > 0 && pixels[curCoord.z][xy - 1] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x - 1, curCoord.y, curCoord.z));
                }
                if (curCoord.x < (sizeX - 1) && pixels[curCoord.z][xy + 1] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x + 1, curCoord.y, curCoord.z));
                }
                if (curCoord.y > 0 && pixels[curCoord.z][xy - sizeX] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y - 1, curCoord.z));
                }
                if (curCoord.y < (sizeY - 1) && pixels[curCoord.z][xy + sizeX] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y + 1, curCoord.z));
                }
                if (curCoord.z > 0 && pixels[curCoord.z - 1][xy] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y, curCoord.z - 1));
                }
                if (curCoord.z < (sizeZ - 1) && pixels[curCoord.z + 1][xy] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y, curCoord.z + 1));
                }
            }
        }
        
        return pixCounts;
    }
	
	
	
	
	public int[] borderObjPixCount3d262(ImageInt img, int seedX, int seedY, int seedZ, Roi borderRoi, int newVal) {
        IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
        int[] pixCounts = null;
        if (img instanceof ImageShort) {
        	//IJ.showMessage("Short");
            pixCounts = borderObjPixCount3DShort26((ImageShort) img, seed, borderRoi, newVal);
        } else if (img instanceof ImageByte) {
        	//IJ.showMessage("Byte");
        	pixCounts = borderObjPixCount3DByte26((ImageByte) img, seed, borderRoi, newVal);
        }
            return pixCounts;
    }
	
	
	private int[] borderObjPixCount3DShort26(ImageShort img, IntCoord3D seed, Roi borderRoi, int newVal) {
		
		//boolean to check if this  value has already been processed
			//Stops running through this algorithm repeatedly when obj. sits outside the ROI
		boolean processedOldVal = false;
		
		int[] pixCounts = new int[2];   
		short[][] pixels = img.pixels;
	    int sizeX = img.sizeX;
	    int sizeY = img.sizeY;
	    int sizeZ = img.sizeZ;
	    short oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
	    

	    //check oldVal has not been processed already:
	    	//this only guaranteed error-free with obj counts below 65535 in the sample!
	    for(int a=0; a<oldValsProcessed.size(); a++) {
	    	if(oldVal == oldValsProcessed.get(a) ) {
	    		processedOldVal = true;
	    	}
	    }
	    
	    ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
	    ArrayList<IntCoord3D> refill = new ArrayList<IntCoord3D>();
	    
	    if(!processedOldVal) {
	   
	    	queue.add(seed);
	    
	        
	    while (!queue.isEmpty()) {
	        IntCoord3D curCoord = queue.remove(0); // FIXME last element?
	        int xy = curCoord.x + curCoord.y * sizeX;
	            
	        if (pixels[curCoord.z][xy] == oldVal) {
	            	
	        	//add to refill to refill at end - if pix IN is LOWER than pix OUT:
	        	refill.add(curCoord);
	            	
	        	if( borderRoi.contains(curCoord.x, curCoord.y ) ) {
	 	        	pixCounts[0] = pixCounts[0] + 1;
	 	        }
	            else if( borderRoi.contains(curCoord.x-1, curCoord.y ) || borderRoi.contains(curCoord.x, curCoord.y-1 ) ) {
	            	// this deals with a border case
	            	pixCounts[0] = pixCounts[0] + 1;
	            }
	 	        else {
	 	        	pixCounts[1] = pixCounts[1] + 1;
	 	        }
	            
	        	//set the pixel to new val -> prevents this pixel being re-added to queue!
	            pixels[curCoord.z][xy] = (short)newVal;
	                
	            //loop and look all pixels in 26-dimensions around current pixel:
	                int curZ, curY, curX;
	                for (int zz = -1; zz < 2; zz++) {
	                    curZ = curCoord.z + zz;
	                    if (curZ > 0 && curZ < (sizeZ - 1)) {
	                        for (int yy = -1; yy < 2; yy++) {
	                            curY = curCoord.y + yy;
	                            if (curY > 0 && curY < (sizeY - 1)) {
	                                for (int xx = -1; xx < 2; xx++) {
	                                    curX = curCoord.x + xx;
	                                    if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
	                                        if (pixels[curZ][curX + curY * sizeX] == oldVal) {
	                                            queue.add(new IntCoord3D(curX, curY, curZ));
	                                        }
	                                    }
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	        } //end while loop
	    
	    
	    //Can create a Voxel3D Obj. from refill -> which will include all voxels that were contained in
	    //ArrayList<Voxel3D> vox3D;
	    //for(int a=0; a<refill.size(); a++) {
	    	//IntCoord3D curCoord = refill.get(a);
	    	//vox3D.add( new Voxel3D(refill.get(a).x, refill.get(a).y, refill.get(a).z, oldVal) );
	    //}

	        
	    //refill the pixels in refill with oldVal, 
	        			//IF IN pix (pixCounts[0]) is Above or Equal to OUT pix (pixCounts[1]):
	    			//Only refilling when IN ensures searching roi histogram allows all obj on border over 
	    				//half IN to be assessed:
	    //MUST REFILL ALL OBJS - as if on border, must be in one or the other!
	    //if(pixCounts[0] >= pixCounts[1]) {
	        	while( !refill.isEmpty() ) {
	        		IntCoord3D curCoord = refill.remove(0); // FIXME last element?
		            int xy = curCoord.x + curCoord.y * sizeX;
		            pixels[curCoord.z][xy] = oldVal;
	        	}
	      //}
	    
	    //finally, add this value to oldValsProcessed:
	    oldValsProcessed.add(oldVal);
	        
	 
	    }//end if !processedOldVal
	        
	    return pixCounts;
	    
	}
	
	
	
	private int[] borderObjPixCount3DByte26(ImageByte img, IntCoord3D seed, Roi borderRoi, int newVal) {
		int[] pixCounts = new int[2];
        byte[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        //short oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
        short oldVal = (short) ((byte)thresholdVal);
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        ArrayList<IntCoord3D> refill = new ArrayList<IntCoord3D>();
        queue.add(seed);
        
        //loop through queue:
        while (!queue.isEmpty()) {
        	//get the coord obj - and remove from queue:
            IntCoord3D curCoord = queue.remove(0); // FIXME last element?
            int xy = curCoord.x + curCoord.y * sizeX;
            
            //if pixel value of this coord equals old value:
            if (pixels[curCoord.z][xy] == oldVal) {
            	
            	//add coord to refill to refill at end - if pix IN is LOWER than pix OUT:
            	refill.add(curCoord);
            	
            	//add to pixCounts depending on whether this coord is in the roi:
            	 if( borderRoi.contains(curCoord.x, curCoord.y ) ) {
            		 //if in ROI, add to ref 0: IN
                 	pixCounts[0] = pixCounts[0] + 1;
                 }
            	 else if( borderRoi.contains(curCoord.x-1, curCoord.y ) || borderRoi.contains(curCoord.x, curCoord.y-1 ) ) {
            		 //if on border of ROI, add to ref 0: IN
            		 pixCounts[0] = pixCounts[0] + 1;
            	 }
                 else {
                	 //if away from border, add to ref 1: OUT
                 	pixCounts[1] = pixCounts[1] + 1;
                 }
            	
            	// IJ.showMessage("current Pix value: "+pixels[curCoord.z][xy]);
            	// IJ.showMessage("new val int: "+newVal);
            	// IJ.showMessage("new val byte: "+ ((byte)newVal) );
                pixels[curCoord.z][xy] = (byte)newVal;
                int curZ, curY, curX;
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > 0 && curZ < (sizeZ - 1)) {
                        for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > 0 && curY < (sizeY - 1)) {
                                for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
                                        if (pixels[curZ][curX + curY * sizeX] == oldVal) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } //end while loop
        
        //refill the pixels in refill with oldVal, 
        	//IF IN pix (pixCounts[0]) is below OUT pix (pixCounts[1]):
        //Actually, refill no matter what (wont affect the pixCounts, and need to keep image filled with objects!)
        if(pixCounts[0] < pixCounts[1]) {
        	while( !refill.isEmpty() ) {
        		IntCoord3D curCoord = refill.remove(0); // FIXME last element?
	            int xy = curCoord.x + curCoord.y * sizeX;
	            pixels[curCoord.z][xy] = (byte)outRoiVal;
        	}
        }
        else {
        	while( !refill.isEmpty() ) {
        		IntCoord3D curCoord = refill.remove(0); // FIXME last element?
	            int xy = curCoord.x + curCoord.y * sizeX;
	            pixels[curCoord.z][xy] = (byte)outRoiVal;
        	}
        }
        
        return pixCounts;
    }
	
	
	
	/**
	 * This method will assess the object at seed (seedX, seedY, seedZ) in the ImageInt img, and set its value
	 * to the newPixValue int passed.  It will return an object which contains the FirstPixel of this object
	 * (the first occurance of this obj in XYZ).
	 * which contains all of the measures made on this object.
	 * @param img
	 * @param intensityImg
	 * @param seedX
	 * @param seedY
	 * @param seedZ
	 * @param objCounter
	 */
	public SelectedObject selectObj3d(ImageInt img, int seedX, int seedY, int seedZ, int newPixValue,
			int newPixValueUnselected) {
        IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
        //int[] pixCounts = null;
        if (img instanceof ImageShort) {
        	//IJ.showMessage("Analysis Short");
            return selectObj3DShort((ImageShort) img, seed, newPixValue, newPixValueUnselected);
        } else if (img instanceof ImageByte) {
        	//IJ.showMessage("Analysis Byte");
        	return selectObj3DByte((ImageByte) img, seed, newPixValue, newPixValueUnselected);
        }
        else {
        	return null;
        }

    }
	
	
	private SelectedObject selectObj3DShort(ImageShort img, IntCoord3D seed, int newPixValue,
			int newPixValueUnselected) {
		
		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;
				
		//int to store xy ref:
		int xy = 0;
		        
		//local reference to the pixel values, and image sizes:
		short[][] pixels = img.pixels;
		int sizeX = img.sizeX;
		int sizeY = img.sizeY;
		int sizeZ = img.sizeZ;
		        
		//reference to the original voxel value of the obj -> for appropriate filtering:
		short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
		        
		//array list of the IntCoord3D -> for the queue (to process all voxels in this method):
		ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
		        
		//Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
		//LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
		LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
		        
		//First, add the initial seed to the queue:
		queue.add(seed);
		        
		//IJ.showMessage("Old Value: "+ ((int)oldVal) );
		        
		//IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
		        
		//IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
		        
		//loop through queue:
		while (!queue.isEmpty()) {
			//get the coord obj - and remove from queue:
		    curCoord = queue.remove(0); // FIXME last element?
		            
		    //get xy ref:
		    xy = curCoord.x + curCoord.y * sizeX;
		            
		    //if pixel value of this coord equals original value:
		    if (pixels[curCoord.z][xy] == origVal) {
		            	
		    	//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
		        //analyse.add(curCoord);
		        analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );
		            	 
		        //Set pixel to newPixValue -> remove this pixel from further analysis:
		        pixels[curCoord.z][xy] = (byte)newPixValue;
		                
		        //loop through all 26 voxels surrounding this voxel:
		        int curZ, curY, curX;
		        for (int zz = -1; zz < 2; zz++) {
		        	curZ = curCoord.z + zz;
		            if (curZ > 0 && curZ < (sizeZ - 1)) {
		            	for (int yy = -1; yy < 2; yy++) {
		            		curY = curCoord.y + yy;
		                    if (curY > 0 && curY < (sizeY - 1)) {
		                    	for (int xx = -1; xx < 2; xx++) {
		                    		curX = curCoord.x + xx;
		                            if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
		 
		                            	//if any of these equal the original voxel value, add to queue:
		                                        
		                            	if (pixels[curZ][curX + curY * sizeX] == origVal) {
		                            		queue.add(new IntCoord3D(curX, curY, curZ));
		                            	}
		                                    	
		                            }
		                    	}
		                    }
		            	}
		            }
		        }
		    }
		} //end while loop
		        
		//IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
		        
		        
		//analyse the pixels in analyse:
			//Want to analyse pixel number (size), geometry (location and bounding box), and
			//Shape characteristics:
		Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
		        
		int[] boundingBox = obj3Dvox.getBoundingBox();
		        
		//Analyse Pixels IF this Obj passes the objGeoFilter High-Pass Filter:
			//No low-pass filter required as no obj is too big... This will be dealt with in the
				//ObjectFilter.
		   // if(obj3Dvox.getVolumePixels() >= objGeoFilter.minSize && boundingBox[0] >= objGeoFilter.minXY
			//		&& boundingBox[2] >= objGeoFilter.maxXY && boundingBox[4] >= objGeoFilter.minZ ) {
		        	
		        	
		//FIRST PIXEL:
		Point3D p3d = getFirstVoxel(obj3Dvox);
	        	
		// TODO what should be returned here?!
		return new SelectedObject(p3d.getRoundX(), p3d.getRoundY(), p3d.getRoundZ(), true, newPixValue, newPixValueUnselected );
	    
	}
	
	
	
	private SelectedObject selectObj3DByte(ImageByte img, IntCoord3D seed, 
												int newPixValue, int newPixValueUnselected) {

		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;
		
		//int to store xy ref:
		int xy = 0;
        
		//local reference to the pixel values, and image sizes:
		byte[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        
        //reference to the original voxel value of the obj -> for appropriate filtering:
        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
        
        //array list of the IntCoord3D -> for the queue (to process all voxels in this method):
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        
        //Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
        LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
        
        //First, add the initial seed to the queue:
        queue.add(seed);
        
        //IJ.showMessage("Old Value: "+ ((int)oldVal) );
        
        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
        
        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
        
        //loop through queue:
        while (!queue.isEmpty()) {
        	//get the coord obj - and remove from queue:
            curCoord = queue.remove(0); // FIXME last element?
            
            //get xy ref:
            xy = curCoord.x + curCoord.y * sizeX;
            
            //if pixel value of this coord equals original value:
            if (pixels[curCoord.z][xy] == origVal) {
            	
            	//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
            	//analyse.add(curCoord);
            	analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );
            	 
            	 //Set pixel to newPixValue -> remove this pixel from further analysis:
                pixels[curCoord.z][xy] = (byte)newPixValue;
                
                //loop through all 26 voxels surrounding this voxel:
                int curZ, curY, curX;
                
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > -1 && curZ < sizeZ) {
                        
                    	for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > -1 && curY < sizeY) {
                                
                            	for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > -1 && curX < sizeX && (xx != 0 || yy != 0 || zz != 0)) {
 
                                    	//if any of these equal the original voxel value, add to queue:
                                        
                                    	if (pixels[curZ][curX + curY * sizeX] == origVal &&
                                    		objConnectivityFilter(xx,yy,zz,objConnectivity) ) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    	
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } //end while loop
        
        //IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
        
        //analyse the pixels in analyse:
        	//Want to analyse pixel number (size), geometry (location and bounding box), and
        	//Shape characteristics:
        Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
        
        int[] boundingBox = obj3Dvox.getBoundingBox();
        
        //Analyse Pixels IF this Obj passes the objGeoFilter High-Pass Filter:
        	//No low-pass filter required as no obj is too big... This will be dealt with in the
        		//ObjectFilter.
        //if(obj3Dvox.getVolumePixels() >= objGeoFilter.minSize && boundingBox[0] >= objGeoFilter.minXY
        	//	&& boundingBox[2] >= objGeoFilter.maxXY && boundingBox[4] >= objGeoFilter.minZ ) {
        
        	//FIRST PIXEL - could use Bary Centre -> but not guaranteed to be a voxel in the obj!  Therefore could potentially
        		//CLASH with another obj Bary Centre!
        		//Therefore, I have made a new method -> getFirstVoxel()
        			//Takes Object3DVoxels obj as parameter, it gets its ArrayList, converted it to HashSet, and
        				//then searches through the BoundingBox (starting at xMin, yMin, zMin) and finds the
        				//FIRST VOXEL -> which is returned as a Point3D object:
        	
        	Point3D p3d = getFirstVoxel(obj3Dvox);
        	
        	// TODO what should be returned here?!
        	return new SelectedObject(p3d.getRoundX(), p3d.getRoundY(), p3d.getRoundZ(), true, newPixValue, newPixValueUnselected );
        	
    }
	
	
	
	
	/**
	 * This method will assess the object at seed (seedX, seedY, seedZ) in the ImageInt img, and set its value
	 * to the newPixValue int passed.  It will return an object which contains the FirstPixel of this object
	 * (the first occurance of this obj in XYZ).
	 * which contains all of the measures made on this object.
	 * @param img
	 * @param intensityImg
	 * @param seedX
	 * @param seedY
	 * @param seedZ
	 * @param objCounter
	 */
	public SelectedObject selectObj3d262(ImageInt img, int seedX, int seedY, int seedZ, int newPixValue,
			int newPixValueUnselected) {
        IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
        //int[] pixCounts = null;
        if (img instanceof ImageShort) {
        	//IJ.showMessage("Analysis Short");
            return selectObj3DShort26((ImageShort) img, seed, newPixValue, newPixValueUnselected);
        } else if (img instanceof ImageByte) {
        	//IJ.showMessage("Analysis Byte");
        	return selectObj3DByte26((ImageByte) img, seed, newPixValue, newPixValueUnselected);
        }
        else {
        	return null;
        }

    }
	
	
	private SelectedObject selectObj3DShort26(ImageShort img, IntCoord3D seed, int newPixValue,
			int newPixValueUnselected) {
		
		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;
				
		//int to store xy ref:
		int xy = 0;
		        
		//local reference to the pixel values, and image sizes:
		short[][] pixels = img.pixels;
		int sizeX = img.sizeX;
		int sizeY = img.sizeY;
		int sizeZ = img.sizeZ;
		        
		//reference to the original voxel value of the obj -> for appropriate filtering:
		short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
		        
		//array list of the IntCoord3D -> for the queue (to process all voxels in this method):
		ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
		        
		//Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
		LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
		        
		//First, add the initial seed to the queue:
		queue.add(seed);
		        
		//IJ.showMessage("Old Value: "+ ((int)oldVal) );
		        
		//IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
		        
		//IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
		        
		//loop through queue:
		while (!queue.isEmpty()) {
			//get the coord obj - and remove from queue:
		    curCoord = queue.remove(0); // FIXME last element?
		            
		    //get xy ref:
		    xy = curCoord.x + curCoord.y * sizeX;
		            
		    //if pixel value of this coord equals original value:
		    if (pixels[curCoord.z][xy] == origVal) {
		            	
		    	//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
		        //analyse.add(curCoord);
		        analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );
		            	 
		        //Set pixel to newPixValue -> remove this pixel from further analysis:
		        pixels[curCoord.z][xy] = (byte)newPixValue;
		                
		        //loop through all 26 voxels surrounding this voxel:
		        int curZ, curY, curX;
		        for (int zz = -1; zz < 2; zz++) {
		        	curZ = curCoord.z + zz;
		            if (curZ > 0 && curZ < (sizeZ - 1)) {
		            	for (int yy = -1; yy < 2; yy++) {
		            		curY = curCoord.y + yy;
		                    if (curY > 0 && curY < (sizeY - 1)) {
		                    	for (int xx = -1; xx < 2; xx++) {
		                    		curX = curCoord.x + xx;
		                            if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
		 
		                            	//if any of these equal the original voxel value, add to queue:
		                                        
		                            	if (pixels[curZ][curX + curY * sizeX] == origVal) {
		                            		queue.add(new IntCoord3D(curX, curY, curZ));
		                            	}
		                                    	
		                            }
		                    	}
		                    }
		            	}
		            }
		        }
		    }
		} //end while loop
		        
		//IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
		        
		        
		//analyse the pixels in analyse:
			//Want to analyse pixel number (size), geometry (location and bounding box), and
			//Shape characteristics:
		Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
		        
		int[] boundingBox = obj3Dvox.getBoundingBox();
		        
		//Analyse Pixels IF this Obj passes the objGeoFilter High-Pass Filter:
			//No low-pass filter required as no obj is too big... This will be dealt with in the
				//ObjectFilter.
		   // if(obj3Dvox.getVolumePixels() >= objGeoFilter.minSize && boundingBox[0] >= objGeoFilter.minXY
			//		&& boundingBox[2] >= objGeoFilter.maxXY && boundingBox[4] >= objGeoFilter.minZ ) {
		        	
		        	
		//FIRST PIXEL:
		Point3D p3d = getFirstVoxel(obj3Dvox);
	        	
		// TODO what should be returned here?!
		return new SelectedObject(p3d.getRoundX(), p3d.getRoundY(), p3d.getRoundZ(), true, newPixValue, newPixValueUnselected );
	    
	}
	
	
	
	private SelectedObject selectObj3DByte26(ImageByte img, IntCoord3D seed, int newPixValue, int newPixValueUnselected) {

		
		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;
		
		//int to store xy ref:
		int xy = 0;
        
		//local reference to the pixel values, and image sizes:
		byte[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        
        //reference to the original voxel value of the obj -> for appropriate filtering:
        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
        
        //array list of the IntCoord3D -> for the queue (to process all voxels in this method):
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        
        //Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
        LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
        
        //First, add the initial seed to the queue:
        queue.add(seed);
        
        //IJ.showMessage("Old Value: "+ ((int)oldVal) );
        
        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
        
        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
        
        //loop through queue:
        while (!queue.isEmpty()) {
        	//get the coord obj - and remove from queue:
            curCoord = queue.remove(0); // FIXME last element?
            
            //get xy ref:
            xy = curCoord.x + curCoord.y * sizeX;
            
            //if pixel value of this coord equals original value:
            if (pixels[curCoord.z][xy] == origVal) {
            	
            	//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
            	//analyse.add(curCoord);
            	analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );
            	 
            	 //Set pixel to newPixValue -> remove this pixel from further analysis:
                pixels[curCoord.z][xy] = (byte)newPixValue;
                
                //loop through all 26 voxels surrounding this voxel:
                int curZ, curY, curX;
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > -1 && curZ < sizeZ) {
                        for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > -1 && curY < sizeY) {
                                for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > -1 && curX < sizeX && (xx != 0 || yy != 0 || zz != 0)) {
 
                                    	//if any of these equal the original voxel value, add to queue:
                                        
                                    	if (pixels[curZ][curX + curY * sizeX] == origVal) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    	
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } //end while loop
        
        //IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
        
        //analyse the pixels in analyse:
        	//Want to analyse pixel number (size), geometry (location and bounding box), and
        	//Shape characteristics:
        Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
        
        int[] boundingBox = obj3Dvox.getBoundingBox();
        
        //Analyse Pixels IF this Obj passes the objGeoFilter High-Pass Filter:
        	//No low-pass filter required as no obj is too big... This will be dealt with in the
        		//ObjectFilter.
        //if(obj3Dvox.getVolumePixels() >= objGeoFilter.minSize && boundingBox[0] >= objGeoFilter.minXY
        	//	&& boundingBox[2] >= objGeoFilter.maxXY && boundingBox[4] >= objGeoFilter.minZ ) {
        
        	//FIRST PIXEL - could use Bary Centre -> but not guaranteed to be a voxel in the obj!  Therefore could potentially
        		//CLASH with another obj Bary Centre!
        		//Therefore, I have made a new method -> getFirstVoxel()
        			//Takes Object3DVoxels obj as parameter, it gets its ArrayList, converted it to HashSet, and
        				//then searches through the BoundingBox (starting at xMin, yMin, zMin) and finds the
        				//FIRST VOXEL -> which is returned as a Point3D object:
        	
        	Point3D p3d = getFirstVoxel(obj3Dvox);
        	
        	// TODO what should be returned here?!
        	return new SelectedObject(p3d.getRoundX(), p3d.getRoundY(), p3d.getRoundZ(), true, newPixValue, newPixValueUnselected );
        	
    }
	
	
	
	
	/**
	 * This method will assess the object at seed (seedX, seedY, seedZ) in the ImageInt img, and return an object
	 * which contains all of the measures made on this object.
	 * @param img
	 * @param intensityImg
	 * @param seedX
	 * @param seedY
	 * @param seedZ
	 * @param objCounter
	 */
	public ObjectDataContainer objAssessment3d(ImageInt img, ImageInt intensityImg, int seedX, int seedY, int seedZ, 
												int objCounter, int newPixValue, boolean convexMeasures) {
        IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
        //int[] pixCounts = null;
        if (img instanceof ImageShort) {
        	//IJ.showMessage("Analysis Short");
            return objAssessment3DShort((ImageShort) img, intensityImg, seed, objCounter, newPixValue);
        } else if (img instanceof ImageByte) {
        	// IJ.showMessage("Analysis Byte");
        	return objAssessment3DByte((ImageByte) img, intensityImg, seed, objCounter, newPixValue, convexMeasures);
        }
        else {
        	return null;
        }

    }
	
	
	private ObjectDataContainer objAssessment3DShort(ImageShort img, ImageInt intensityImg, IntCoord3D seed, 
													int objCounter, int newPixValue) {
		
		//create new MCIB_SM_DataObj to fill with data & return:
		ObjectDataContainer dataObj = new ObjectDataContainer();

		//A reference to IntCorrd3D object:
				IntCoord3D curCoord;
				
				//int to store xy ref:
				int xy = 0;
		        
				//local reference to the pixel values, and image sizes:
				short[][] pixels = img.pixels;
		        int sizeX = img.sizeX;
		        int sizeY = img.sizeY;
		        int sizeZ = img.sizeZ;
		        
		        //reference to the original voxel value of the obj -> for appropriate filtering:
		        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
		        
		        //array list of the IntCoord3D -> for the queue (to process all voxels in this method):
		        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
		        
		        //Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
		        //LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
		        LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
		        
		        //First, add the initial seed to the queue:
		        queue.add(seed);
		        
		        //IJ.showMessage("Old Value: "+ ((int)oldVal) );
		        
		        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
		        
		        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
		        
		        //loop through queue:
		        while (!queue.isEmpty()) {
		        	//get the coord obj - and remove from queue:
		            curCoord = queue.remove(0); // FIXME last element?
		            
		            //get xy ref:
		            xy = curCoord.x + curCoord.y * sizeX;
		            
		            //if pixel value of this coord equals original value:
		            if (pixels[curCoord.z][xy] == origVal) {
		            	
		            	//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
		            	//analyse.add(curCoord);
		            	analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );
		            	 
		            	 //Set pixel to newPixValue -> remove this pixel from further analysis:
		                pixels[curCoord.z][xy] = (byte)newPixValue;
		                
		                //loop through all 26 voxels surrounding this voxel:
		                int curZ, curY, curX;
		                for (int zz = -1; zz < 2; zz++) {
		                    curZ = curCoord.z + zz;
		                    if (curZ > 0 && curZ < (sizeZ - 1)) {
		                        for (int yy = -1; yy < 2; yy++) {
		                            curY = curCoord.y + yy;
		                            if (curY > 0 && curY < (sizeY - 1)) {
		                                for (int xx = -1; xx < 2; xx++) {
		                                    curX = curCoord.x + xx;
		                                    if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
		 
		                                    	//if any of these equal the original voxel value, add to queue:
		                                        
		                                    	if (pixels[curZ][curX + curY * sizeX] == origVal) {
		                                            queue.add(new IntCoord3D(curX, curY, curZ));
		                                        }
		                                    	
		                                    }
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        } //end while loop
		        
		        //IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
		        
		        
		        //analyse the pixels in analyse:
		        	//Want to analyse pixel number (size), geometry (location and bounding box), and
		        	//Shape characteristics:
		        Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
		        
		        int[] boundingBox = obj3Dvox.getBoundingBox();
		        
		        //Analyse Pixels IF this Obj passes the objGeoFilter High-Pass Filter:
		        	//No low-pass filter required as no obj is too big... This will be dealt with in the
		        		//ObjectFilter.
		        //if(obj3Dvox.getVolumePixels() >= objGeoFilter.minSize && boundingBox[0] >= objGeoFilter.minXY
		        	//	&& boundingBox[2] >= objGeoFilter.maxXY && boundingBox[4] >= objGeoFilter.minZ ) {
		        
		        	//FIRST PIXEL - could use Bary Centre -> but not guaranteed to be a voxel in the obj!  Therefore could potentially
		        		//CLASH with another obj Bary Centre!
		        		//Therefore, I have made a new method -> getFirstVoxel()
		        			//Takes Object3DVoxels obj as parameter, it gets its ArrayList, converted it to HashSet, and
		        				//then searches through the BoundingBox (starting at xMin, yMin, zMin) and finds the
		        				//FIRST VOXEL -> which is returned as a Point3D object:
		        	
		        //FIRST PIXEL:
		        Point3D p3d = getFirstVoxel(obj3Dvox);

		        int x1 = p3d.getRoundX();
		        int y1 = p3d.getRoundY();
		        int z1 = p3d.getRoundZ();

		        //Obj Counter:
		        int objNo = objCounter;

		        //GEOMETRICAL MEASURES:
		        int volVoxels = obj3Dvox.getVolumePixels();
		        double areaVoxels = obj3Dvox.getAreaPixels() ;


		        int xMin = boundingBox[0];
		        int xMax = boundingBox[1];
		        int yMin = boundingBox[2];
		        int yMax = boundingBox[3];
		        int zMin = boundingBox[4];
		        int zMax = boundingBox[5];

		        double centreX = obj3Dvox.getCenterX();
		        double centreY = obj3Dvox.getCenterY();
		        double centreZ = obj3Dvox.getCenterZ();

		        //SHAPE MEASURES:
		        double compactness = obj3Dvox.getCompactness(true);
		        double sphericity = obj3Dvox.getSphericity(true);

		        double volConvex = 0.0;
		        double surfConvex = 0.0;
		        double solidity3D = 1.0;
		        double convexity3D = 1.0;

		        if( (xMax-xMin) != 0 && 
		        		(yMax-yMin) != 0 && 
		        		(zMax-zMin) != 0 ) {
		        	// the object exists in more than one slice in X Y & Z - so compute 3D Convex Hull:

		        	//compute the convex obj to calculate solidity3D and convexity3D:
		        	Object3DVoxels convex3Dobj = obj3Dvox.getConvexObject();
		        	//get its volume and surface area:
		        	volConvex = convex3Dobj.getVolumePixels();
		        	surfConvex = convex3Dobj.getAreaPixels();

		        	//get original obj vol. and surf.:
		        	//double vol = obj3Dvox.getVolumePixels();
		        	//double surf = obj3Dvox.getAreaPixels();

		        	//Compute Solidity3D and Convexity3D:
		        	//rt.addValue("Solidity 3D", (vol/volConvex) );
		        	//rt.addValue("Convexity 3D", (surf/surfConvex) );
		        	solidity3D = ((double)volVoxels/volConvex);
		        	convexity3D = (surfConvex/areaVoxels);

		        	//IJ.log(" ");
		        	//LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();
		        	//IJ.log("co-planar z: obj3Dvox: "+vox3D);
		        	//IJ.log(" ");

		        }
		        // TODO This method needs improving, can the convex voxels be derived from a co-planar objecr
		        // in a better way?  Need to look at where the error occurs, and try to adjustObject3DVoxels
		        else {
		        	// if object is only in one plane in any dimension - need to calculate the convex hull in 2D:

		        	// will calculate the convex volume and surface convex area of co-planar objects by 
		        	// putting two of them together:
		        	if( (xMax-xMin) != 0 && 
		        			(yMax-yMin) != 0 && 
		        			(zMax-zMin) == 0 ) {

		        		Object3DVoxels obj3Dvox2 = new Object3DVoxels();

		        		//LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();
		        		LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

		        		obj3Dvox2.addVoxels(vox3D);
		        		obj3Dvox2.addVoxels(vox3D);

		        		//IJ.log(" ");
		        		//IJ.log("COPLANAER Z");
		        		//IJ.log(" ");

		        		//IJ.log("co-planar z: obj3Dvox: "+vox3D);
		        		//IJ.log(" ");

		        		//compute the convex obj to calculate solidity3D and convexity3D:
		        		Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();

		        		//get its volume and surface area:
		        		volConvex = convex3Dobj.getVolumePixels();
		        		surfConvex = convex3Dobj.getAreaPixels();

		        		//Compute Solidity3D and Convexity3D:
		        		solidity3D = (double)volVoxels/volConvex;
		        		convexity3D = (surfConvex/areaVoxels);

		        	} // Z

		        	else if( (xMax-xMin) != 0 && 
		        			(yMax-yMin) == 0 && 
		        			(zMax-zMin) != 0 ) {

		        		Object3DVoxels obj3Dvox2 = new Object3DVoxels();

		        		//LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();
		        		LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

		        		obj3Dvox2.addVoxels(vox3D);
		        		obj3Dvox2.addVoxels(vox3D);

		        		//for(int a=0; a<vox3D.size(); a++) {
		        			//vox3D.get(a).setY( vox3D.get(a).getY() + 1 );
		        		//}

		        		//compute the convex obj to calculate solidity3D and convexity3D:
		        		Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();

		        		//get its volume and surface area:
		        		volConvex = convex3Dobj.getVolumePixels();
		        		surfConvex = convex3Dobj.getAreaPixels();

		        		//Compute Solidity3D and Convexity3D:
		        		solidity3D = (double)volVoxels/volConvex;
		        		convexity3D = (surfConvex/areaVoxels);

		        	} // Y

		        	else if( (xMax-xMin) == 0 && 
		        			(yMax-yMin) != 0 && 
		        			(zMax-zMin) != 0 ) {

		        		Object3DVoxels obj3Dvox2 = new Object3DVoxels();

		        		LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

		        		obj3Dvox2.addVoxels(vox3D);
		        		obj3Dvox2.addVoxels(vox3D);

		        		//for(int a=0; a<vox3D.size(); a++) {
		        			//vox3D.get(a).setX( vox3D.get(a).getX() + 1 );
		        		//}

		        		//compute the convex obj to calculate solidity3D and convexity3D:
		        		Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();
		        		//get its volume and surface area:
		        		volConvex = convex3Dobj.getVolumePixels();
		        		surfConvex = convex3Dobj.getAreaPixels();

		        		//get original obj vol. and surf.:
		        		//double vol = obj3Dvox.getVolumePixels();
		        		//double surf = obj3Dvox.getAreaPixels();

		        		//Compute Solidity3D and Convexity3D:
		        		//rt.addValue("Solidity 3D", (vol/volConvex) );
		        		//rt.addValue("Convexity 3D", (surf/surfConvex) );
		        		solidity3D = (double)volVoxels/volConvex;
		        		convexity3D = (surfConvex/areaVoxels);

		        	} // X

		        	else {
		        		// more than one dimension is co-planar -> so set solidity and convexity to 1.0:
		        		// the values are 1.0 by default.
		        		// solidity3D = 1.0;
		        		// convexity3D = 1.0;
		        	}

		        }

		        double volToVolBox = obj3Dvox.getRatioBox();

		        double mainElong = obj3Dvox.getMainElongation();
		        double medianElong = obj3Dvox.getMedianElongation();

		        double volEllipse = obj3Dvox.getVolumeEllipseUnit();
		        double volToVolEllipse = obj3Dvox.getRatioEllipsoid();
		        
	        	// Moment Invariants:
	        	
	        	double[] homInv = obj3Dvox.getHomogeneousInvariants(); // n=5
	        	
	        	double homInv1 = homInv[0];
	        	double homInv2 = homInv[1];
	        	double homInv3 = homInv[2];
	        	double homInv4 = homInv[3];
	        	double homInv5 = homInv[4];
	        	
	        	double[] geoInv = obj3Dvox.getGeometricInvariants(); // n=6
	        	
	        	double geoInv1 = geoInv[0];
	        	double geoInv2 = geoInv[1];
	        	double geoInv3 = geoInv[2];
	        	double geoInv4 = geoInv[3];
	        	double geoInv5 = geoInv[4];
	        	double geoInv6 = geoInv[5];
	        	
	        	double[] inv = obj3Dvox.getMoments3D(); // n=5
	        	
	        	double biocatJ1 = inv[0];
	        	double biocatJ2 = inv[1];
	        	double biocatJ3 = inv[2];
	        	double biocatI1 = inv[3];
	        	double biocatI2 = inv[4];

		        //INTENSITY MEASURES
		        double meanPix = obj3Dvox.getPixMeanValue(intensityImg);
		        double sdPix = obj3Dvox.getPixStdDevValue(intensityImg);
		        double maxPix = obj3Dvox.getPixMaxValue(intensityImg);
		        double medianPix = obj3Dvox.getPixMedianValue(intensityImg);
		        double minPix = obj3Dvox.getPixMinValue(intensityImg);

		        // } //end objGeoFilter high-pass Filter

		        dataObj.setDataValues( x1,  y1,  z1,  objNo,  volVoxels,  areaVoxels,  xMin,  xMax,  yMin,
		        		yMax,  zMin,  zMax,  centreX,  centreY,  centreZ,  compactness,
		        		sphericity,  volConvex,  surfConvex,  solidity3D,  convexity3D, 
		        		volToVolBox,  mainElong,  medianElong,  volEllipse,  volToVolEllipse, 
						 homInv1, homInv2, homInv3, homInv4, homInv5, 
						 geoInv1, geoInv2, geoInv3, geoInv4, geoInv5, geoInv6, 
						 biocatJ1, biocatJ2, biocatJ3, biocatI1, biocatI2,
		        		meanPix,  sdPix,  maxPix,  medianPix,  minPix);

		        return dataObj;

	    
	}
	
	
	
	private ObjectDataContainer objAssessment3DByte(ImageByte img, ImageInt intensityImg, IntCoord3D seed, 
												int objCounter, int newPixValue, boolean convexMeasures) {
		//long startTime = System.currentTimeMillis();
		//long currentTime;
		//IJ.log("start time: "+startTime);
		
		//IJ.log("seed: x: "+seed.x+" y: "+seed.y+" z: "+seed.z);
		
		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;
		
		//int to store xy ref:
		int xy = 0;
        
		//local reference to the pixel values, and image sizes:
		byte[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        
        //reference to the original voxel value of the obj -> for appropriate filtering:
        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
        
        //array list of the IntCoord3D -> for the queue (to process all voxels in this method):
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        
        //Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
        LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
        
        //First, add the initial seed to the queue:
        queue.add(seed);
        
        // IJ.showMessage("Original Value: "+ ((int)origVal) );
        
        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
        
        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
        
        //loop through queue:
        while (!queue.isEmpty()) {
        	//get the coord obj - and remove from queue:
            curCoord = queue.remove(0); // FIXME last element?
            
            //get xy ref:
            xy = curCoord.x + curCoord.y * sizeX;
            // IJ.log("current coord xy: "+xy);
            
            //if pixel value of this coord equals original value:
            if (pixels[curCoord.z][xy] == origVal) {
            	// IJ.log("add: x: "+curCoord.x+" y: "+curCoord.y+" z: "+curCoord.z);
            	//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
            	//analyse.add(curCoord);
            	analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );
            	 
            	// IJ.log("Coord val: "+pixels[curCoord.z][xy]);
            	 //Set pixel to newPixValue -> remove this pixel from further analysis:
                pixels[curCoord.z][xy] = (byte)newPixValue;
                // IJ.log("Coord val set to: "+pixels[curCoord.z][xy]);
                // IJ.log("");
                
                //loop through all 26 voxels surrounding this voxel:
                int curZ, curY, curX;
                
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > -1 && curZ < sizeZ) {
                       
                    	for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > -1 && curY < sizeY) {
                               
                            	for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > -1 && curX < sizeX) { 
                                    	
                                    	if( (xx != 0 || yy != 0 || zz != 0) ) { 
                                    		// xx, yy, zz must not all equal 0, as this is the central pixel!
                                    		
                                    		//if any of these equal the original voxel value, add to queue:
                                    		if ( (pixels[curZ][curX + curY * sizeX] == origVal) && 
                                        			objConnectivityFilter(xx,yy,zz,objConnectivity) ) {
                                    			queue.add(new IntCoord3D(curX, curY, curZ));
                                    			// IJ.log("added: "+curX+" "+curY+" "+curZ );
                                    		}	
                                    	}
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // IJ.log("");
        } //end while loop
        
        // IJ.showMessage("Reached end of while loop!");
        
        //analyse the pixels in analyse:
        	//Want to analyse pixel number (size), geometry (location and bounding box), and
        	//Shape characteristics:
        
       // currentTime = System.currentTimeMillis() - startTime;
        //IJ.log("creating object3Dvoxels time: "+currentTime );
        
        Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
        
        //currentTime = System.currentTimeMillis() - startTime;
        //IJ.log("creating bounding box time: "+currentTime );
        
        int[] boundingBox = obj3Dvox.getBoundingBox();
        
        //Analyse Pixels IF this Obj passes the objGeoFilter High-Pass Filter:
        	//No low-pass filter required as no obj is too big... This will be dealt with in the
        		//ObjectFilter.
        //if(obj3Dvox.getVolumePixels() >= objGeoFilter.minSize && boundingBox[0] >= objGeoFilter.minXY
        	//	&& boundingBox[2] >= objGeoFilter.maxXY && boundingBox[4] >= objGeoFilter.minZ ) {
        
        	//FIRST PIXEL - could use Bary Centre -> but not guaranteed to be a voxel in the obj!  Therefore could potentially
        		//CLASH with another obj Bary Centre!
        		//Therefore, I have made a new method -> getFirstVoxel()
        			//Takes Object3DVoxels obj as parameter, it gets its ArrayList, converted it to HashSet, and
        				//then searches through the BoundingBox (starting at xMin, yMin, zMin) and finds the
        				//FIRST VOXEL -> which is returned as a Point3D object:
        	
        // TODO Move the Data Extraction to the MCIB_SM_DataObj class!
        // Therefore, this method should just return either 'analyse' (ArrayList of Voxel3D obj), or
        	// return the Object3DVoxels made from 'analyse'
        	// By extracting the data in the same class as the data points are defined, it is easy to add
        	// or modify the set of data points collected.
        
       // currentTime = System.currentTimeMillis() - startTime;
       // IJ.log("getFirstVoxel time: "+currentTime );
        
        	Point3D p3d = getFirstVoxel(obj3Dvox);
        	
        	int x1 = p3d.getRoundX();
        	int y1 = p3d.getRoundY();
        	int z1 = p3d.getRoundZ();
        
        	//Obj Counter:
        	int objNo = objCounter;
        
        	//GEOMETRICAL MEASURES:
        	int volVoxels = obj3Dvox.getVolumePixels();
        	double areaVoxels = obj3Dvox.getAreaPixels() ;
        
        
        	int xMin = boundingBox[0];
        	int xMax = boundingBox[1];
        	int yMin = boundingBox[2];
        	int yMax = boundingBox[3];
        	int zMin = boundingBox[4];
        	int zMax = boundingBox[5];
        	
        	int xLength = xMax - xMin + 1; // +1 to account for the FIRST PIXEL (i,e is first is 10 and last is 10,
        	int yLength = yMax - yMin + 1; // its still 1 voxel thick!)
        	int zLength = zMax - zMin + 1;
        
        	double centreX = obj3Dvox.getCenterX();
        	double centreY = obj3Dvox.getCenterY();
        	double centreZ = obj3Dvox.getCenterZ();
        
        	//SHAPE MEASURES:
        	double compactness = obj3Dvox.getCompactness(true);
        	double sphericity = obj3Dvox.getSphericity(true);

        	double volConvex = 0.0;
        	double surfConvex = 0.0;
        	double solidity3D = 1.0;
			double convexity3D = 1.0;
        	
			if(convexMeasures == true) {
				if( (xMax-xMin) != 0 && 
						(yMax-yMin) != 0 && 
						(zMax-zMin) != 0 ) {
					// the object exists in more than one slice in X Y & Z - so compute 3D Convex Hull:

					//compute the convex obj to calculate solidity3D and convexity3D:
					Object3DVoxels convex3Dobj = obj3Dvox.getConvexObject();
					//get its volume and surface area:
					volConvex = convex3Dobj.getVolumePixels();
					surfConvex = convex3Dobj.getAreaPixels();

					//get original obj vol. and surf.:
					//double vol = obj3Dvox.getVolumePixels();
					//double surf = obj3Dvox.getAreaPixels();

					//Compute Solidity3D and Convexity3D:
					//rt.addValue("Solidity 3D", (vol/volConvex) );
					//rt.addValue("Convexity 3D", (surf/surfConvex) );
					solidity3D = ((double)volVoxels/volConvex);
					convexity3D = (surfConvex/areaVoxels);

					//IJ.log(" ");
					//LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();
					//IJ.log("co-planar z: obj3Dvox: "+vox3D);
					//IJ.log(" ");

				}
				// TODO This method needs improving, can the convex voxels be derived from a co-planar objecr
				// in a better way?  Need to look at where the error occurs, and try to adjustObject3DVoxels
				else {
					// if object is only in one plane in any dimension - need to calculate the convex hull in 2D:

					// will calculate the convex volume and surface convex area of co-planar objects by 
					// putting two of them together:
					if( (xMax-xMin) != 0 && 
							(yMax-yMin) != 0 && 
							(zMax-zMin) == 0 ) {

						Object3DVoxels obj3Dvox2 = new Object3DVoxels();

						LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

						obj3Dvox2.addVoxels(vox3D);
						obj3Dvox2.addVoxels(vox3D);

						//IJ.log(" ");
						//IJ.log("COPLANAER Z");
						//IJ.log(" ");

						//IJ.log("co-planar z: obj3Dvox: "+vox3D);
						//IJ.log(" ");

						//compute the convex obj to calculate solidity3D and convexity3D:
						Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();

						//get its volume and surface area:
						volConvex = convex3Dobj.getVolumePixels();
						surfConvex = convex3Dobj.getAreaPixels();

						//Compute Solidity3D and Convexity3D:
						solidity3D = (double)volVoxels/volConvex;
						convexity3D = (surfConvex/areaVoxels);

					} // Z

					else if( (xMax-xMin) != 0 && 
							(yMax-yMin) == 0 && 
							(zMax-zMin) != 0 ) {

						Object3DVoxels obj3Dvox2 = new Object3DVoxels();

						LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

						obj3Dvox2.addVoxels(vox3D);
						obj3Dvox2.addVoxels(vox3D);

						//for(int a=0; a<vox3D.size(); a++) {
							//vox3D.get(a).setY( vox3D.get(a).getY() + 1 );
						//}

						//compute the convex obj to calculate solidity3D and convexity3D:
						Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();

						//get its volume and surface area:
						volConvex = convex3Dobj.getVolumePixels();
						surfConvex = convex3Dobj.getAreaPixels();

						//Compute Solidity3D and Convexity3D:
						solidity3D = (double)volVoxels/volConvex;
						convexity3D = (surfConvex/areaVoxels);

					} // Y

					else if( (xMax-xMin) == 0 && 
							(yMax-yMin) != 0 && 
							(zMax-zMin) != 0 ) {

						Object3DVoxels obj3Dvox2 = new Object3DVoxels();

						LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

						obj3Dvox2.addVoxels(vox3D);
						obj3Dvox2.addVoxels(vox3D);

						//for(int a=0; a<vox3D.size(); a++) {
							//vox3D.get(a).setX( vox3D.get(a).getX() + 1 );
						//}

						//compute the convex obj to calculate solidity3D and convexity3D:
						Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();
						//get its volume and surface area:
						volConvex = convex3Dobj.getVolumePixels();
						surfConvex = convex3Dobj.getAreaPixels();

						//get original obj vol. and surf.:
						//double vol = obj3Dvox.getVolumePixels();
						//double surf = obj3Dvox.getAreaPixels();

						//Compute Solidity3D and Convexity3D:
						//rt.addValue("Solidity 3D", (vol/volConvex) );
						//rt.addValue("Convexity 3D", (surf/surfConvex) );
						solidity3D = (double)volVoxels/volConvex;
						convexity3D = (surfConvex/areaVoxels);

					} // X

					else {
						// more than one dimension is co-planar -> so set solidity and convexity to 1.0:
						// the values are 1.0 by default.
						// solidity3D = 1.0;
						// convexity3D = 1.0;
					}

				}
			}
        	
        	//currentTime = System.currentTimeMillis() - startTime;
	        //IJ.log("vol elong and intensity Measures time: "+currentTime );
        
        	double volToVolBox = obj3Dvox.getRatioBox();
        
        	double mainElong = obj3Dvox.getMainElongation();
        	double medianElong = obj3Dvox.getMedianElongation();
        
        	double volEllipse = obj3Dvox.getVolumeEllipseUnit();
        	double volToVolEllipse = obj3Dvox.getRatioEllipsoid();
        	
        	// Moment Invariants:
        	
        	double[] homInv = obj3Dvox.getHomogeneousInvariants(); // n=5
        	
        	double homInv1 = homInv[0];
        	double homInv2 = homInv[1];
        	double homInv3 = homInv[2];
        	double homInv4 = homInv[3];
        	double homInv5 = homInv[4];
        	
        	double[] geoInv = obj3Dvox.getGeometricInvariants(); // n=6
        	
        	double geoInv1 = geoInv[0];
        	double geoInv2 = geoInv[1];
        	double geoInv3 = geoInv[2];
        	double geoInv4 = geoInv[3];
        	double geoInv5 = geoInv[4];
        	double geoInv6 = geoInv[5];
        	
        	double[] inv = obj3Dvox.getMoments3D(); // n=5
        	
        	double biocatJ1 = inv[0];
        	double biocatJ2 = inv[1];
        	double biocatJ3 = inv[2];
        	double biocatI1 = inv[3];
        	double biocatI2 = inv[4];
        
        	//INTENSITY MEASURES
        	double meanPix = obj3Dvox.getPixMeanValue(intensityImg);
        	double sdPix = obj3Dvox.getPixStdDevValue(intensityImg);
        	double maxPix = obj3Dvox.getPixMaxValue(intensityImg);
        	double medianPix = obj3Dvox.getPixMedianValue(intensityImg);
        	double minPix = obj3Dvox.getPixMinValue(intensityImg);
        
       // } //end objGeoFilter high-pass Filter
        	
        	// IJ.showMessage("x1: "+x1+" y1: "+y1+" z1: "+z1);
        	
        	//currentTime = System.currentTimeMillis() - startTime;
	        //IJ.log("Return Obj time: "+currentTime );
        	//IJ.log("");
        	return new ObjectDataContainer( x1,  y1,  z1,  objNo,  volVoxels,  areaVoxels,  xMin,  yMin,
					 zMin,  xLength, yLength, zLength,  centreX,  centreY,  centreZ,  compactness,
					 sphericity,  volConvex,  surfConvex,  solidity3D,  convexity3D, 
					 volToVolBox,  mainElong,  medianElong,  volEllipse,  volToVolEllipse,
					 homInv1, homInv2, homInv3, homInv4, homInv5, 
					 geoInv1, geoInv2, geoInv3, geoInv4, geoInv5, geoInv6, 
					 biocatJ1, biocatJ2, biocatJ3, biocatI1, biocatI2,
					 meanPix,  sdPix,  maxPix,  medianPix,  minPix);
        
    }
	
	/**
	 * Returns true if the passed XYZ coordinates indicate a coordinate which would be considered
	 * connected to the central pixel with a given type of object connectivity.
	 * <p>
	 * The objConnectivity parameter is a String from the OM_ProcedureSettings class:  it is either
	 * CONNECTED6, CONNECTED18, or CONNECTED26.  This parameter determines how the XYZ coordinates are
	 * filtered in this method:  CONNECTED6 looks to see if the coordinates indicate connectivity in
	 * the 6-connected configuration, and CONNECTED18 looks to see if the coordinates indicate
	 * connectivity in the 18-connected configuration.  All coordinates around a central pixel
	 * would fit 26-connected, and so this is not tested here.
	 * @param xx
	 * @param yy
	 * @param zz
	 * @param objConnectivity
	 * @return
	 */
	public boolean objConnectivityFilter(int xx,int yy,int zz,String objConnectivity) {
		
		if( objConnectivity.equalsIgnoreCase(OM_ProcedureSettings.CONNECTED6) ) {
			// return false for all corners in 2D and 3D:
			if( (xx==0 && yy==0) || (yy==0 && zz==0) || (xx==0 && zz==0) ) {
				return true; 	// only the 6 of 26 possible pixel connectivity orientations 
			}					// will meet this if statements criteria
			else {
				return false;
			}
		}
		else if( objConnectivity.equalsIgnoreCase(OM_ProcedureSettings.CONNECTED18) ) {
			// return false for all corners in 3D only:
			if( xx==0 || yy==0 || zz==0 ) {
				return true;	// the 18 of 26 possible pixel connectivity orientations
			}					// will meet this if statements criteria
			else {
				return false;
			}
		}
		// default -> return true:
		return true;
	}
	
	/**
	 * This method will assess the object at seed (seedX, seedY, seedZ) in the ImageInt img, and return an object
	 * which contains all of the measures made on this object.
	 * @param img
	 * @param intensityImg
	 * @param seedX
	 * @param seedY
	 * @param seedZ
	 * @param objCounter
	 */
	public ObjectDataContainer objAssessment3d262(ImageInt img, ImageInt intensityImg, int seedX, int seedY, int seedZ, 
												int objCounter, int newPixValue, boolean convexMeasures) {
        IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
        //int[] pixCounts = null;
        if (img instanceof ImageShort) {
        	//IJ.showMessage("Analysis Short");
            return objAssessment3DShort26((ImageShort) img, intensityImg, seed, objCounter, newPixValue);
        } else if (img instanceof ImageByte) {
        	// IJ.showMessage("Analysis Byte");
        	return objAssessment3DByte26((ImageByte) img, intensityImg, seed, objCounter, newPixValue, convexMeasures);
        }
        else {
        	return null;
        }

    }
	
	
	private ObjectDataContainer objAssessment3DShort26(ImageShort img, ImageInt intensityImg, IntCoord3D seed, 
													int objCounter, int newPixValue) {
		
		//create new MCIB_SM_DataObj to fill with data & return:
		ObjectDataContainer dataObj = new ObjectDataContainer();

		//A reference to IntCorrd3D object:
				IntCoord3D curCoord;
				
				//int to store xy ref:
				int xy = 0;
		        
				//local reference to the pixel values, and image sizes:
				short[][] pixels = img.pixels;
		        int sizeX = img.sizeX;
		        int sizeY = img.sizeY;
		        int sizeZ = img.sizeZ;
		        
		        //reference to the original voxel value of the obj -> for appropriate filtering:
		        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
		        
		        //array list of the IntCoord3D -> for the queue (to process all voxels in this method):
		        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
		        
		        //Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
		        LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
		        
		        //First, add the initial seed to the queue:
		        queue.add(seed);
		        
		        //IJ.showMessage("Old Value: "+ ((int)oldVal) );
		        
		        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
		        
		        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
		        
		        //loop through queue:
		        while (!queue.isEmpty()) {
		        	//get the coord obj - and remove from queue:
		            curCoord = queue.remove(0); // FIXME last element?
		            
		            //get xy ref:
		            xy = curCoord.x + curCoord.y * sizeX;
		            
		            //if pixel value of this coord equals original value:
		            if (pixels[curCoord.z][xy] == origVal) {
		            	
		            	//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
		            	//analyse.add(curCoord);
		            	analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );
		            	 
		            	 //Set pixel to newPixValue -> remove this pixel from further analysis:
		                pixels[curCoord.z][xy] = (byte)newPixValue;
		                
		                //loop through all 26 voxels surrounding this voxel:
		                int curZ, curY, curX;
		                for (int zz = -1; zz < 2; zz++) {
		                    curZ = curCoord.z + zz;
		                    if (curZ > 0 && curZ < (sizeZ - 1)) {
		                        for (int yy = -1; yy < 2; yy++) {
		                            curY = curCoord.y + yy;
		                            if (curY > 0 && curY < (sizeY - 1)) {
		                                for (int xx = -1; xx < 2; xx++) {
		                                    curX = curCoord.x + xx;
		                                    if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
		 
		                                    	//if any of these equal the original voxel value, add to queue:
		                                        
		                                    	if (pixels[curZ][curX + curY * sizeX] == origVal) {
		                                            queue.add(new IntCoord3D(curX, curY, curZ));
		                                        }
		                                    	
		                                    }
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        } //end while loop
		        
		        //IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
		        
		        
		        //analyse the pixels in analyse:
		        	//Want to analyse pixel number (size), geometry (location and bounding box), and
		        	//Shape characteristics:
		        Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
		        
		        int[] boundingBox = obj3Dvox.getBoundingBox();
		        
		        //Analyse Pixels IF this Obj passes the objGeoFilter High-Pass Filter:
		        	//No low-pass filter required as no obj is too big... This will be dealt with in the
		        		//ObjectFilter.
		        //if(obj3Dvox.getVolumePixels() >= objGeoFilter.minSize && boundingBox[0] >= objGeoFilter.minXY
		        	//	&& boundingBox[2] >= objGeoFilter.maxXY && boundingBox[4] >= objGeoFilter.minZ ) {
		        
		        	//FIRST PIXEL - could use Bary Centre -> but not guaranteed to be a voxel in the obj!  Therefore could potentially
		        		//CLASH with another obj Bary Centre!
		        		//Therefore, I have made a new method -> getFirstVoxel()
		        			//Takes Object3DVoxels obj as parameter, it gets its ArrayList, converted it to HashSet, and
		        				//then searches through the BoundingBox (starting at xMin, yMin, zMin) and finds the
		        				//FIRST VOXEL -> which is returned as a Point3D object:
		        	
		        //FIRST PIXEL:
		        Point3D p3d = getFirstVoxel(obj3Dvox);

		        int x1 = p3d.getRoundX();
		        int y1 = p3d.getRoundY();
		        int z1 = p3d.getRoundZ();

		        //Obj Counter:
		        int objNo = objCounter;

		        //GEOMETRICAL MEASURES:
		        int volVoxels = obj3Dvox.getVolumePixels();
		        double areaVoxels = obj3Dvox.getAreaPixels() ;


		        int xMin = boundingBox[0];
		        int xMax = boundingBox[1];
		        int yMin = boundingBox[2];
		        int yMax = boundingBox[3];
		        int zMin = boundingBox[4];
		        int zMax = boundingBox[5];

		        double centreX = obj3Dvox.getCenterX();
		        double centreY = obj3Dvox.getCenterY();
		        double centreZ = obj3Dvox.getCenterZ();

		        //SHAPE MEASURES:
		        double compactness = obj3Dvox.getCompactness(true);
		        double sphericity = obj3Dvox.getSphericity(true);

		        double volConvex = 0.0;
		        double surfConvex = 0.0;
		        double solidity3D = 1.0;
		        double convexity3D = 1.0;

		        if( (xMax-xMin) != 0 && 
		        		(yMax-yMin) != 0 && 
		        		(zMax-zMin) != 0 ) {
		        	// the object exists in more than one slice in X Y & Z - so compute 3D Convex Hull:

		        	//compute the convex obj to calculate solidity3D and convexity3D:
		        	Object3DVoxels convex3Dobj = obj3Dvox.getConvexObject();
		        	//get its volume and surface area:
		        	volConvex = convex3Dobj.getVolumePixels();
		        	surfConvex = convex3Dobj.getAreaPixels();

		        	//get original obj vol. and surf.:
		        	//double vol = obj3Dvox.getVolumePixels();
		        	//double surf = obj3Dvox.getAreaPixels();

		        	//Compute Solidity3D and Convexity3D:
		        	//rt.addValue("Solidity 3D", (vol/volConvex) );
		        	//rt.addValue("Convexity 3D", (surf/surfConvex) );
		        	solidity3D = ((double)volVoxels/volConvex);
		        	convexity3D = (surfConvex/areaVoxels);

		        	//IJ.log(" ");
		        	//LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();
		        	//IJ.log("co-planar z: obj3Dvox: "+vox3D);
		        	//IJ.log(" ");

		        }
		        // TODO This method needs improving, can the convex voxels be derived from a co-planar objecr
		        // in a better way?  Need to look at where the error occurs, and try to adjustObject3DVoxels
		        else {
		        	// if object is only in one plane in any dimension - need to calculate the convex hull in 2D:

		        	// will calculate the convex volume and surface convex area of co-planar objects by 
		        	// putting two of them together:
		        	if( (xMax-xMin) != 0 && 
		        			(yMax-yMin) != 0 && 
		        			(zMax-zMin) == 0 ) {

		        		Object3DVoxels obj3Dvox2 = new Object3DVoxels();

		        		LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

		        		obj3Dvox2.addVoxels(vox3D);
		        		obj3Dvox2.addVoxels(vox3D);

		        		//IJ.log(" ");
		        		//IJ.log("COPLANAER Z");
		        		//IJ.log(" ");

		        		//IJ.log("co-planar z: obj3Dvox: "+vox3D);
		        		//IJ.log(" ");

		        		//compute the convex obj to calculate solidity3D and convexity3D:
		        		Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();

		        		//get its volume and surface area:
		        		volConvex = convex3Dobj.getVolumePixels();
		        		surfConvex = convex3Dobj.getAreaPixels();

		        		//Compute Solidity3D and Convexity3D:
		        		solidity3D = (double)volVoxels/volConvex;
		        		convexity3D = (surfConvex/areaVoxels);

		        	} // Z

		        	else if( (xMax-xMin) != 0 && 
		        			(yMax-yMin) == 0 && 
		        			(zMax-zMin) != 0 ) {

		        		Object3DVoxels obj3Dvox2 = new Object3DVoxels();

		        		LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

		        		obj3Dvox2.addVoxels(vox3D);
		        		obj3Dvox2.addVoxels(vox3D);

		        		//for(int a=0; a<vox3D.size(); a++) {
		        			//vox3D.get(a).setY( vox3D.get(a).getY() + 1 );
		        		//}

		        		//compute the convex obj to calculate solidity3D and convexity3D:
		        		Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();

		        		//get its volume and surface area:
		        		volConvex = convex3Dobj.getVolumePixels();
		        		surfConvex = convex3Dobj.getAreaPixels();

		        		//Compute Solidity3D and Convexity3D:
		        		solidity3D = (double)volVoxels/volConvex;
		        		convexity3D = (surfConvex/areaVoxels);

		        	} // Y

		        	else if( (xMax-xMin) == 0 && 
		        			(yMax-yMin) != 0 && 
		        			(zMax-zMin) != 0 ) {

		        		Object3DVoxels obj3Dvox2 = new Object3DVoxels();

		        		LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

		        		obj3Dvox2.addVoxels(vox3D);
		        		obj3Dvox2.addVoxels(vox3D);

		        		//for(int a=0; a<vox3D.size(); a++) {
		        			//vox3D.get(a).setX( vox3D.get(a).getX() + 1 );
		        		//}

		        		//compute the convex obj to calculate solidity3D and convexity3D:
		        		Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();
		        		//get its volume and surface area:
		        		volConvex = convex3Dobj.getVolumePixels();
		        		surfConvex = convex3Dobj.getAreaPixels();

		        		//get original obj vol. and surf.:
		        		//double vol = obj3Dvox.getVolumePixels();
		        		//double surf = obj3Dvox.getAreaPixels();

		        		//Compute Solidity3D and Convexity3D:
		        		//rt.addValue("Solidity 3D", (vol/volConvex) );
		        		//rt.addValue("Convexity 3D", (surf/surfConvex) );
		        		solidity3D = (double)volVoxels/volConvex;
		        		convexity3D = (surfConvex/areaVoxels);

		        	} // X

		        	else {
		        		// more than one dimension is co-planar -> so set solidity and convexity to 1.0:
		        		// the values are 1.0 by default.
		        		// solidity3D = 1.0;
		        		// convexity3D = 1.0;
		        	}

		        }

		        double volToVolBox = obj3Dvox.getRatioBox();

		        double mainElong = obj3Dvox.getMainElongation();
		        double medianElong = obj3Dvox.getMedianElongation();

		        double volEllipse = obj3Dvox.getVolumeEllipseUnit();
		        double volToVolEllipse = obj3Dvox.getRatioEllipsoid();
		        
	        	// Moment Invariants:
	        	
	        	double[] homInv = obj3Dvox.getHomogeneousInvariants(); // n=5
	        	
	        	double homInv1 = homInv[0];
	        	double homInv2 = homInv[1];
	        	double homInv3 = homInv[2];
	        	double homInv4 = homInv[3];
	        	double homInv5 = homInv[4];
	        	
	        	double[] geoInv = obj3Dvox.getGeometricInvariants(); // n=6
	        	
	        	double geoInv1 = geoInv[0];
	        	double geoInv2 = geoInv[1];
	        	double geoInv3 = geoInv[2];
	        	double geoInv4 = geoInv[3];
	        	double geoInv5 = geoInv[4];
	        	double geoInv6 = geoInv[5];
	        	
	        	double[] inv = obj3Dvox.getMoments3D(); // n=5
	        	
	        	double biocatJ1 = inv[0];
	        	double biocatJ2 = inv[1];
	        	double biocatJ3 = inv[2];
	        	double biocatI1 = inv[3];
	        	double biocatI2 = inv[4];

		        //INTENSITY MEASURES
		        double meanPix = obj3Dvox.getPixMeanValue(intensityImg);
		        double sdPix = obj3Dvox.getPixStdDevValue(intensityImg);
		        double maxPix = obj3Dvox.getPixMaxValue(intensityImg);
		        double medianPix = obj3Dvox.getPixMedianValue(intensityImg);
		        double minPix = obj3Dvox.getPixMinValue(intensityImg);

		        // } //end objGeoFilter high-pass Filter

		        dataObj.setDataValues( x1,  y1,  z1,  objNo,  volVoxels,  areaVoxels,  xMin,  xMax,  yMin,
		        		yMax,  zMin,  zMax,  centreX,  centreY,  centreZ,  compactness,
		        		sphericity,  volConvex,  surfConvex,  solidity3D,  convexity3D, 
		        		volToVolBox,  mainElong,  medianElong,  volEllipse,  volToVolEllipse, 
						 homInv1, homInv2, homInv3, homInv4, homInv5, 
						 geoInv1, geoInv2, geoInv3, geoInv4, geoInv5, geoInv6, 
						 biocatJ1, biocatJ2, biocatJ3, biocatI1, biocatI2,
		        		meanPix,  sdPix,  maxPix,  medianPix,  minPix);

		        return dataObj;

	    
	}
	
	
	
	private ObjectDataContainer objAssessment3DByte26(ImageByte img, ImageInt intensityImg, IntCoord3D seed, 
												int objCounter, int newPixValue, boolean convexMeasures) {
		//long startTime = System.currentTimeMillis();
		//long currentTime;
		//IJ.log("start time: "+startTime);
		
		//IJ.log("seed: x: "+seed.x+" y: "+seed.y+" z: "+seed.z);
		
		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;
		
		//int to store xy ref:
		int xy = 0;
        
		//local reference to the pixel values, and image sizes:
		byte[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        
        //reference to the original voxel value of the obj -> for appropriate filtering:
        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
        
        //array list of the IntCoord3D -> for the queue (to process all voxels in this method):
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        
        //Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
        LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
        
        //First, add the initial seed to the queue:
        queue.add(seed);
        
        // IJ.showMessage("Original Value: "+ ((int)origVal) );
        
        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
        
        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
        
        //loop through queue:
        while (!queue.isEmpty()) {
        	//get the coord obj - and remove from queue:
            curCoord = queue.remove(0); // FIXME last element?
            
            //get xy ref:
            xy = curCoord.x + curCoord.y * sizeX;
            // IJ.log("current coord xy: "+xy);
            
            //if pixel value of this coord equals original value:
            if (pixels[curCoord.z][xy] == origVal) {
            	// IJ.log("add: x: "+curCoord.x+" y: "+curCoord.y+" z: "+curCoord.z);
            	//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
            	//analyse.add(curCoord);
            	analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );
            	 
            	// IJ.log("Coord val: "+pixels[curCoord.z][xy]);
            	 //Set pixel to newPixValue -> remove this pixel from further analysis:
                pixels[curCoord.z][xy] = (byte)newPixValue;
                // IJ.log("Coord val set to: "+pixels[curCoord.z][xy]);
                // IJ.log("");
                
                //loop through all 26 voxels surrounding this voxel:
                int curZ, curY, curX;
                
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > -1 && curZ < sizeZ) {
                       
                    	for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > -1 && curY < sizeY) {
                               
                            	for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > -1 && curX < sizeX) { 
                                    	
                                    	if((xx != 0 || yy != 0 || zz != 0)) { 
                                    		// xx, yy, zz must not all equal 0, as this is the central pixel!
                                    		
                                    		//if any of these equal the original voxel value, add to queue:
                                    		if (pixels[curZ][curX + curY * sizeX] == origVal) {
                                    			queue.add(new IntCoord3D(curX, curY, curZ));
                                    			// IJ.log("added: "+curX+" "+curY+" "+curZ );
                                    		}	
                                    	}
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // IJ.log("");
        } //end while loop
        
        // IJ.showMessage("Reached end of while loop!");
        
        //analyse the pixels in analyse:
        	//Want to analyse pixel number (size), geometry (location and bounding box), and
        	//Shape characteristics:
        
       // currentTime = System.currentTimeMillis() - startTime;
        //IJ.log("creating object3Dvoxels time: "+currentTime );
        
        Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
        
        //currentTime = System.currentTimeMillis() - startTime;
        //IJ.log("creating bounding box time: "+currentTime );
        
        int[] boundingBox = obj3Dvox.getBoundingBox();
        
        //Analyse Pixels IF this Obj passes the objGeoFilter High-Pass Filter:
        	//No low-pass filter required as no obj is too big... This will be dealt with in the
        		//ObjectFilter.
        //if(obj3Dvox.getVolumePixels() >= objGeoFilter.minSize && boundingBox[0] >= objGeoFilter.minXY
        	//	&& boundingBox[2] >= objGeoFilter.maxXY && boundingBox[4] >= objGeoFilter.minZ ) {
        
        	//FIRST PIXEL - could use Bary Centre -> but not guaranteed to be a voxel in the obj!  Therefore could potentially
        		//CLASH with another obj Bary Centre!
        		//Therefore, I have made a new method -> getFirstVoxel()
        			//Takes Object3DVoxels obj as parameter, it gets its ArrayList, converted it to HashSet, and
        				//then searches through the BoundingBox (starting at xMin, yMin, zMin) and finds the
        				//FIRST VOXEL -> which is returned as a Point3D object:
        	
        // TODO Move the Data Extraction to the MCIB_SM_DataObj class!
        // Therefore, this method should just return either 'analyse' (ArrayList of Voxel3D obj), or
        	// return the Object3DVoxels made from 'analyse'
        	// By extracting the data in the same class as the data points are defined, it is easy to add
        	// or modify the set of data points collected.
        
       // currentTime = System.currentTimeMillis() - startTime;
       // IJ.log("getFirstVoxel time: "+currentTime );
        
        	Point3D p3d = getFirstVoxel(obj3Dvox);
        	
        	int x1 = p3d.getRoundX();
        	int y1 = p3d.getRoundY();
        	int z1 = p3d.getRoundZ();
        
        	//Obj Counter:
        	int objNo = objCounter;
        
        	//GEOMETRICAL MEASURES:
        	int volVoxels = obj3Dvox.getVolumePixels();
        	double areaVoxels = obj3Dvox.getAreaPixels() ;
        
        
        	int xMin = boundingBox[0];
        	int xMax = boundingBox[1];
        	int yMin = boundingBox[2];
        	int yMax = boundingBox[3];
        	int zMin = boundingBox[4];
        	int zMax = boundingBox[5];
        	
        	int xLength = xMax - xMin + 1; // +1 to account for the FIRST PIXEL (i,e is first is 10 and last is 10,
        	int yLength = yMax - yMin + 1; // its still 1 voxel thick!)
        	int zLength = zMax - zMin + 1;
        
        	double centreX = obj3Dvox.getCenterX();
        	double centreY = obj3Dvox.getCenterY();
        	double centreZ = obj3Dvox.getCenterZ();
        
        	//SHAPE MEASURES:
        	double compactness = obj3Dvox.getCompactness(true);
        	double sphericity = obj3Dvox.getSphericity(true);

        	double volConvex = 0.0;
        	double surfConvex = 0.0;
        	double solidity3D = 1.0;
			double convexity3D = 1.0;
        	
			if(convexMeasures == true) {
				if( (xMax-xMin) != 0 && 
						(yMax-yMin) != 0 && 
						(zMax-zMin) != 0 ) {
					// the object exists in more than one slice in X Y & Z - so compute 3D Convex Hull:

					//compute the convex obj to calculate solidity3D and convexity3D:
					Object3DVoxels convex3Dobj = obj3Dvox.getConvexObject();
					//get its volume and surface area:
					volConvex = convex3Dobj.getVolumePixels();
					surfConvex = convex3Dobj.getAreaPixels();

					//get original obj vol. and surf.:
					//double vol = obj3Dvox.getVolumePixels();
					//double surf = obj3Dvox.getAreaPixels();

					//Compute Solidity3D and Convexity3D:
					//rt.addValue("Solidity 3D", (vol/volConvex) );
					//rt.addValue("Convexity 3D", (surf/surfConvex) );
					solidity3D = ((double)volVoxels/volConvex);
					convexity3D = (surfConvex/areaVoxels);

					//IJ.log(" ");
					//LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();
					//IJ.log("co-planar z: obj3Dvox: "+vox3D);
					//IJ.log(" ");

				}
				// TODO This method needs improving, can the convex voxels be derived from a co-planar objecr
				// in a better way?  Need to look at where the error occurs, and try to adjustObject3DVoxels
				else {
					// if object is only in one plane in any dimension - need to calculate the convex hull in 2D:

					// will calculate the convex volume and surface convex area of co-planar objects by 
					// putting two of them together:
					if( (xMax-xMin) != 0 && 
							(yMax-yMin) != 0 && 
							(zMax-zMin) == 0 ) {

						Object3DVoxels obj3Dvox2 = new Object3DVoxels();

						LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

						obj3Dvox2.addVoxels(vox3D);
						obj3Dvox2.addVoxels(vox3D);

						//IJ.log(" ");
						//IJ.log("COPLANAER Z");
						//IJ.log(" ");

						//IJ.log("co-planar z: obj3Dvox: "+vox3D);
						//IJ.log(" ");

						//compute the convex obj to calculate solidity3D and convexity3D:
						Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();

						//get its volume and surface area:
						volConvex = convex3Dobj.getVolumePixels();
						surfConvex = convex3Dobj.getAreaPixels();

						//Compute Solidity3D and Convexity3D:
						solidity3D = (double)volVoxels/volConvex;
						convexity3D = (surfConvex/areaVoxels);

					} // Z

					else if( (xMax-xMin) != 0 && 
							(yMax-yMin) == 0 && 
							(zMax-zMin) != 0 ) {

						Object3DVoxels obj3Dvox2 = new Object3DVoxels();

						LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

						obj3Dvox2.addVoxels(vox3D);
						obj3Dvox2.addVoxels(vox3D);

						//for(int a=0; a<vox3D.size(); a++) {
							//vox3D.get(a).setY( vox3D.get(a).getY() + 1 );
						//}

						//compute the convex obj to calculate solidity3D and convexity3D:
						Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();

						//get its volume and surface area:
						volConvex = convex3Dobj.getVolumePixels();
						surfConvex = convex3Dobj.getAreaPixels();

						//Compute Solidity3D and Convexity3D:
						solidity3D = (double)volVoxels/volConvex;
						convexity3D = (surfConvex/areaVoxels);

					} // Y

					else if( (xMax-xMin) == 0 && 
							(yMax-yMin) != 0 && 
							(zMax-zMin) != 0 ) {

						Object3DVoxels obj3Dvox2 = new Object3DVoxels();

						LinkedList<Voxel3D> vox3D = obj3Dvox.getVoxels();

						obj3Dvox2.addVoxels(vox3D);
						obj3Dvox2.addVoxels(vox3D);

						//for(int a=0; a<vox3D.size(); a++) {
							//vox3D.get(a).setX( vox3D.get(a).getX() + 1 );
						//}

						//compute the convex obj to calculate solidity3D and convexity3D:
						Object3DVoxels convex3Dobj = obj3Dvox2.getConvexObject();
						//get its volume and surface area:
						volConvex = convex3Dobj.getVolumePixels();
						surfConvex = convex3Dobj.getAreaPixels();

						//get original obj vol. and surf.:
						//double vol = obj3Dvox.getVolumePixels();
						//double surf = obj3Dvox.getAreaPixels();

						//Compute Solidity3D and Convexity3D:
						//rt.addValue("Solidity 3D", (vol/volConvex) );
						//rt.addValue("Convexity 3D", (surf/surfConvex) );
						solidity3D = (double)volVoxels/volConvex;
						convexity3D = (surfConvex/areaVoxels);

					} // X

					else {
						// more than one dimension is co-planar -> so set solidity and convexity to 1.0:
						// the values are 1.0 by default.
						// solidity3D = 1.0;
						// convexity3D = 1.0;
					}

				}
			}
        	
        	//currentTime = System.currentTimeMillis() - startTime;
	        //IJ.log("vol elong and intensity Measures time: "+currentTime );
        
        	double volToVolBox = obj3Dvox.getRatioBox();
        
        	double mainElong = obj3Dvox.getMainElongation();
        	double medianElong = obj3Dvox.getMedianElongation();
        
        	double volEllipse = obj3Dvox.getVolumeEllipseUnit();
        	double volToVolEllipse = obj3Dvox.getRatioEllipsoid();
        	
      	// Moment Invariants:
        	
        	double[] homInv = obj3Dvox.getHomogeneousInvariants(); // n=5
        	
        	double homInv1 = homInv[0];
        	double homInv2 = homInv[1];
        	double homInv3 = homInv[2];
        	double homInv4 = homInv[3];
        	double homInv5 = homInv[4];
        	
        	double[] geoInv = obj3Dvox.getGeometricInvariants(); // n=6
        	
        	double geoInv1 = geoInv[0];
        	double geoInv2 = geoInv[1];
        	double geoInv3 = geoInv[2];
        	double geoInv4 = geoInv[3];
        	double geoInv5 = geoInv[4];
        	double geoInv6 = geoInv[5];
        	
        	double[] inv = obj3Dvox.getMoments3D(); // n=5
        	
        	double biocatJ1 = inv[0];
        	double biocatJ2 = inv[1];
        	double biocatJ3 = inv[2];
        	double biocatI1 = inv[3];
        	double biocatI2 = inv[4];
        
        	//INTENSITY MEASURES
        	double meanPix = obj3Dvox.getPixMeanValue(intensityImg);
        	double sdPix = obj3Dvox.getPixStdDevValue(intensityImg);
        	double maxPix = obj3Dvox.getPixMaxValue(intensityImg);
        	double medianPix = obj3Dvox.getPixMedianValue(intensityImg);
        	double minPix = obj3Dvox.getPixMinValue(intensityImg);
        
       // } //end objGeoFilter high-pass Filter
        	
        	// IJ.showMessage("x1: "+x1+" y1: "+y1+" z1: "+z1);
        	
        	//currentTime = System.currentTimeMillis() - startTime;
	        //IJ.log("Return Obj time: "+currentTime );
        	//IJ.log("");
        	return new ObjectDataContainer( x1,  y1,  z1,  objNo,  volVoxels,  areaVoxels,  xMin,  yMin,
					 zMin,  xLength, yLength, zLength,  centreX,  centreY,  centreZ,  compactness,
					 sphericity,  volConvex,  surfConvex,  solidity3D,  convexity3D, 
					 volToVolBox,  mainElong,  medianElong,  volEllipse,  volToVolEllipse, 
					 homInv1, homInv2, homInv3, homInv4, homInv5, 
					 geoInv1, geoInv2, geoInv3, geoInv4, geoInv5, geoInv6, 
					 biocatJ1, biocatJ2, biocatJ3, biocatI1, biocatI2,
					 meanPix,  sdPix,  maxPix,  medianPix,  minPix);
        
    }
	
	
	
	
	public static Point3D getFirstVoxel(Object3DVoxels obj3Dvox) {
		
		//create a new ArrayList from the obj3Dvox:
		LinkedList<Voxel3D> arlist = obj3Dvox.getVoxels();
		
		//Convert the ArrayList to a HashSet:
			//It is much more efficient to use contains on a HashSet than ArrayList!!
		HashSet<Voxel3D> arset = new HashSet<Voxel3D>(arlist);
		
		//loop through all voxels, starting at xMin, yMin, zMin and proceeding through all the X coordinates FIRST
			//THEN all the Y coordinates SECOND, and finally all the Z coordinates LAST
		for(int z = obj3Dvox.getBoundingBox()[4]; z<= obj3Dvox.getBoundingBox()[5]; z++) {
			for(int y = obj3Dvox.getBoundingBox()[2]; y<= obj3Dvox.getBoundingBox()[3]; y++) {
				for(int x = obj3Dvox.getBoundingBox()[0]; x<= obj3Dvox.getBoundingBox()[1]; x++) {

					//Test if the current coordinate is in the HashSet:
					
					if(arset.contains(new Voxel3D(x,y,z,(float)255))) {
					
						//If it is - have found the FIRST VOXEL -> so return this as a Point3D obj:
						return new Point3D((double)x,(double)y,(double)z);
					
					}
					
				}
			}
		}
		
		return null;
		
	}
	
	
	
	public static Object3DVoxels reconstructObj3d(ImageInt img, int seedX, int seedY, int seedZ, 
			int objCounter, int newPixValue) {
		IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
		//int[] pixCounts = null;
		if (img instanceof ImageShort) {
			//IJ.showMessage("Analysis Short");
			return reconstructObj3DShort((ImageShort) img, seed, newPixValue);
		} else if (img instanceof ImageByte) {
			//IJ.showMessage("Analysis Byte");
			return reconstructObj3DByte((ImageByte) img, seed, newPixValue);
		}
		else {
			return null;
		}

	}
	
	
	
	protected static Object3DVoxels reconstructObj3DShort(ImageShort img,  IntCoord3D seed, int newPixValue) {
		

		//A reference to IntCorrd3D object:
				IntCoord3D curCoord;
				
				//int to store xy ref:
				int xy = 0;
		        
				//local reference to the pixel values, and image sizes:
				short[][] pixels = img.pixels;
		        int sizeX = img.sizeX;
		        int sizeY = img.sizeY;
		        int sizeZ = img.sizeZ;
		        
		        //reference to the original voxel value of the obj -> for appropriate filtering:
		        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
		        
		        //array list of the IntCoord3D -> for the queue (to process all voxels in this method):
		        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
		        
		        //Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
		        LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
		        
		        //First, add the initial seed to the queue:
		        queue.add(seed);
		        
		        //IJ.showMessage("Old Value: "+ ((int)oldVal) );
		        
		        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
		        
		        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
		        
		        //loop through queue:
		        while (!queue.isEmpty()) {
		        	//get the coord obj - and remove from queue:
		            curCoord = queue.remove(0); // FIXME last element?
		            
		            //get xy ref:
		            xy = curCoord.x + curCoord.y * sizeX;
		            
		            //if pixel value of this coord equals original value:
		            if (pixels[curCoord.z][xy] == origVal) {
		            	
		            	//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
		            	//analyse.add(curCoord);
		            	analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );
		            	 
		            	 //Set pixel to 0 -> remove this pixel from further analysis:
		                pixels[curCoord.z][xy] = (byte)0;
		                
		                //loop through all 26 voxels surrounding this voxel:
		                int curZ, curY, curX;
		                for (int zz = -1; zz < 2; zz++) {
		                    curZ = curCoord.z + zz;
		                    if (curZ > 0 && curZ < (sizeZ - 1)) {
		                        for (int yy = -1; yy < 2; yy++) {
		                            curY = curCoord.y + yy;
		                            if (curY > 0 && curY < (sizeY - 1)) {
		                                for (int xx = -1; xx < 2; xx++) {
		                                    curX = curCoord.x + xx;
		                                    if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
		 
		                                    	//if any of these equal the original voxel value, add to queue:
		                                        
		                                    	if (pixels[curZ][curX + curY * sizeX] == origVal) {
		                                            queue.add(new IntCoord3D(curX, curY, curZ));
		                                        }
		                                    	
		                                    }
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        } //end while loop
		        
		        //IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
		        
		        
		        //analyse the pixels in analyse:
		        	//Want to analyse pixel number (size), geometry (location and bounding box), and
		        	//Shape characteristics:
		        Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
		        
		        return obj3Dvox;
		        
	}
	
	
	protected static Object3DVoxels reconstructObj3DByte(ImageByte img,  IntCoord3D seed, int newPixValue) {

		//create new MCIB_SM_DataObj to fill with data & return:
		ObjectDataContainer dataObj = new ObjectDataContainer();

		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;

		//int to store xy ref:
		int xy = 0;

		//local reference to the pixel values, and image sizes:
		byte[][] pixels = img.pixels;
		int sizeX = img.sizeX;
		int sizeY = img.sizeY;
		int sizeZ = img.sizeZ;

		//reference to the original voxel value of the obj -> for appropriate filtering:
		short origVal = pixels[seed.z][seed.x + seed.y * sizeX];

		//array list of the IntCoord3D -> for the queue (to process all voxels in this method):
		ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();

		//Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
		LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();

		//First, add the initial seed to the queue:
		queue.add(seed);

		//IJ.showMessage("Old Value: "+ ((int)oldVal) );

		//IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );

		//IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );

		//loop through queue:
		while (!queue.isEmpty()) {
			//get the coord obj - and remove from queue:
			curCoord = queue.remove(0); // FIXME last element?

			//get xy ref:
			xy = curCoord.x + curCoord.y * sizeX;

			//if pixel value of this coord equals original value:
			if (pixels[curCoord.z][xy] == origVal) {

				//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
				//analyse.add(curCoord);
				analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );

				//Set pixel to newPixValue -> remove this pixel from further analysis:
				pixels[curCoord.z][xy] = (byte)newPixValue;

				//loop through all 26 voxels surrounding this voxel:
				int curZ, curY, curX;
				
				for (int zz = -1; zz < 2; zz++) {
					curZ = curCoord.z + zz;
					if (curZ > -1 && curZ < sizeZ) {
						
						for (int yy = -1; yy < 2; yy++) {
							curY = curCoord.y + yy;
							if (curY > -1 && curY < sizeY) {
								
								for (int xx = -1; xx < 2; xx++) {
									curX = curCoord.x + xx;
									if (curX > -1 && curX < sizeX && (xx != 0 || yy != 0 || zz != 0)) {

										//if any of these equal the original voxel value, add to queue:

										if ( (pixels[curZ][curX + curY * sizeX] == origVal) ) {
											queue.add(new IntCoord3D(curX, curY, curZ));
										}

									}
								}
							}
						}
					}
				}
			}
		} //end while loop

		//IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);

		//analyse the pixels in analyse:
		//Want to analyse pixel number (size), geometry (location and bounding box), and
		//Shape characteristics:
		Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
		
		return obj3Dvox;
		
	}
	
	
	
	public static Object3DVoxels reconstructObj3d262(ImageInt img, int seedX, int seedY, int seedZ, 
			int objCounter, int newPixValue) {
		IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
		//int[] pixCounts = null;
		if (img instanceof ImageShort) {
			//IJ.showMessage("Analysis Short");
			return reconstructObj3DShort26((ImageShort) img, seed, newPixValue);
		} else if (img instanceof ImageByte) {
			//IJ.showMessage("Analysis Byte");
			return reconstructObj3DByte26((ImageByte) img, seed, newPixValue);
		}
		else {
			return null;
		}

	}
	
	
	
	protected static Object3DVoxels reconstructObj3DShort26(ImageShort img,  IntCoord3D seed, int newPixValue) {
		

		//A reference to IntCorrd3D object:
				IntCoord3D curCoord;
				
				//int to store xy ref:
				int xy = 0;
		        
				//local reference to the pixel values, and image sizes:
				short[][] pixels = img.pixels;
		        int sizeX = img.sizeX;
		        int sizeY = img.sizeY;
		        int sizeZ = img.sizeZ;
		        
		        //reference to the original voxel value of the obj -> for appropriate filtering:
		        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
		        
		        //array list of the IntCoord3D -> for the queue (to process all voxels in this method):
		        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
		        
		        //Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
		        LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();
		        
		        //First, add the initial seed to the queue:
		        queue.add(seed);
		        
		        //IJ.showMessage("Old Value: "+ ((int)oldVal) );
		        
		        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
		        
		        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
		        
		        //loop through queue:
		        while (!queue.isEmpty()) {
		        	//get the coord obj - and remove from queue:
		            curCoord = queue.remove(0); // FIXME last element?
		            
		            //get xy ref:
		            xy = curCoord.x + curCoord.y * sizeX;
		            
		            //if pixel value of this coord equals original value:
		            if (pixels[curCoord.z][xy] == origVal) {
		            	
		            	//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
		            	//analyse.add(curCoord);
		            	analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );
		            	 
		            	 //Set pixel to 0 -> remove this pixel from further analysis:
		                pixels[curCoord.z][xy] = (byte)0;
		                
		                //loop through all 26 voxels surrounding this voxel:
		                int curZ, curY, curX;
		                for (int zz = -1; zz < 2; zz++) {
		                    curZ = curCoord.z + zz;
		                    if (curZ > 0 && curZ < (sizeZ - 1)) {
		                        for (int yy = -1; yy < 2; yy++) {
		                            curY = curCoord.y + yy;
		                            if (curY > 0 && curY < (sizeY - 1)) {
		                                for (int xx = -1; xx < 2; xx++) {
		                                    curX = curCoord.x + xx;
		                                    if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
		 
		                                    	//if any of these equal the original voxel value, add to queue:
		                                        
		                                    	if (pixels[curZ][curX + curY * sizeX] == origVal) {
		                                            queue.add(new IntCoord3D(curX, curY, curZ));
		                                        }
		                                    	
		                                    }
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        } //end while loop
		        
		        //IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
		        
		        
		        //analyse the pixels in analyse:
		        	//Want to analyse pixel number (size), geometry (location and bounding box), and
		        	//Shape characteristics:
		        Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
		        
		        return obj3Dvox;
		        
	}
	
	
	protected static Object3DVoxels reconstructObj3DByte26(ImageByte img,  IntCoord3D seed, int newPixValue) {

		//create new MCIB_SM_DataObj to fill with data & return:
		ObjectDataContainer dataObj = new ObjectDataContainer();

		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;

		//int to store xy ref:
		int xy = 0;

		//local reference to the pixel values, and image sizes:
		byte[][] pixels = img.pixels;
		int sizeX = img.sizeX;
		int sizeY = img.sizeY;
		int sizeZ = img.sizeZ;

		//reference to the original voxel value of the obj -> for appropriate filtering:
		short origVal = pixels[seed.z][seed.x + seed.y * sizeX];

		//array list of the IntCoord3D -> for the queue (to process all voxels in this method):
		ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();

		//Array list of -> for analysis (once all voxels have been collected, want to analyse this obj):
		LinkedList<Voxel3D> analyse = new LinkedList<Voxel3D>();

		//First, add the initial seed to the queue:
		queue.add(seed);

		//IJ.showMessage("Old Value: "+ ((int)oldVal) );

		//IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );

		//IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );

		//loop through queue:
		while (!queue.isEmpty()) {
			//get the coord obj - and remove from queue:
			curCoord = queue.remove(0); // FIXME last element?

			//get xy ref:
			xy = curCoord.x + curCoord.y * sizeX;

			//if pixel value of this coord equals original value:
			if (pixels[curCoord.z][xy] == origVal) {

				//add coord to analyse at end - will analyse this obj at end when all voxels are collected:
				//analyse.add(curCoord);
				analyse.add(new Voxel3D(curCoord.x, curCoord.y, curCoord.z, (float)(255) ) );

				//Set pixel to newPixValue -> remove this pixel from further analysis:
				pixels[curCoord.z][xy] = (byte)newPixValue;

				//loop through all 26 voxels surrounding this voxel:
				int curZ, curY, curX;
				for (int zz = -1; zz < 2; zz++) {
					curZ = curCoord.z + zz;
					if (curZ > -1 && curZ < sizeZ) {
						for (int yy = -1; yy < 2; yy++) {
							curY = curCoord.y + yy;
							if (curY > -1 && curY < sizeY) {
								for (int xx = -1; xx < 2; xx++) {
									curX = curCoord.x + xx;
									if (curX > -1 && curX < sizeX && (xx != 0 || yy != 0 || zz != 0)) {

										//if any of these equal the original voxel value, add to queue:

										if (pixels[curZ][curX + curY * sizeX] == origVal) {
											queue.add(new IntCoord3D(curX, curY, curZ));
										}

									}
								}
							}
						}
					}
				}
			}
		} //end while loop

		//IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);

		//analyse the pixels in analyse:
		//Want to analyse pixel number (size), geometry (location and bounding box), and
		//Shape characteristics:
		Object3DVoxels obj3Dvox = new Object3DVoxels(analyse);
		
		return obj3Dvox;
		
	}
	
	
	public void borderObjFilter3d(ImageInt img, int seedX, int seedY, int seedZ, 
			Roi borderRoi, int inVal, int outVal) {
        IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
        //int[] pixCounts = null;
        if (img instanceof ImageShort) {
        	//IJ.showMessage("Short");
            borderObjFilter3DShort((ImageShort) img, seed, borderRoi, inVal, outVal);
        } else if (img instanceof ImageByte) {
        	//IJ.showMessage("Byte");
        	borderObjFilter3DByte((ImageByte) img, seed, borderRoi, inVal, outVal);
        }

    }
	
	
	private void borderObjFilter3DShort(ImageShort img, IntCoord3D seed, Roi borderRoi, int inVal, int outVal) {
		

		//this array stores the number of pixels which are inside the ROI and how many are outside the ROI:
		int[] pixCounts = new int[2];
		
		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;
		
		//int to store xy ref:
		int xy = 0;
        
		//local reference to the pixel values, and image sizes:
		short[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        
        //reference to the original voxel value of the obj -> for appropriate filtering:
        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
        
        //Two array lists of the IntCoord3D -> one for the queue (to process all voxels), and one for
        	//refill (in case the voxels need to be filled with the outVal instead of the inVal):
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        ArrayList<IntCoord3D> refill = new ArrayList<IntCoord3D>();
        
        //First, add the initial seed to the queue:
        queue.add(seed);
        
        //IJ.showMessage("Old Value: "+ ((int)oldVal) );
        
        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
        
        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
        
        //loop through queue:
        while (!queue.isEmpty()) {
        	//get the coord obj - and remove from queue:
            curCoord = queue.remove(0); // FIXME last element?
            
            //get xy ref:
            xy = curCoord.x + curCoord.y * sizeX;
            
            //if pixel value of this coord equals original value:
            if (pixels[curCoord.z][xy] == origVal) {
            	
            	//add coord to refill at end - will refill with outVal if pix IN is LOWER than pix OUT:
            	refill.add(curCoord);
            	
            	//add to pixCounts depending on whether this coord is in the roi:
            	 if( borderRoi.contains(curCoord.x, curCoord.y ) ) {
            		 //if in ROI, add to ref 0: IN
                 	pixCounts[0] = pixCounts[0] + 1;
                 }
            	 else if( borderRoi.contains(curCoord.x-1, curCoord.y ) || borderRoi.contains(curCoord.x, curCoord.y-1 ) ) {
            		 //if on border of ROI, add to ref 0: IN
            		 pixCounts[0] = pixCounts[0] + 1;
            	 }
                 else {
                	 //if away from border, add to ref 1: OUT
                 	pixCounts[1] = pixCounts[1] + 1;
                 }
            	
            	 
            	 //Set pixel to inVal -> assume this obj is INSIDE the ROI
                pixels[curCoord.z][xy] = (byte)inVal;
                
                //loop through all 26 voxels surrounding this voxel:
                int curZ, curY, curX;
                
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > 0 && curZ < (sizeZ - 1)) {
                        
                    	for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > 0 && curY < (sizeY - 1)) {
                                
                            	for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
 
                                    	//if any of these equal the original voxel value, add to queue:
                                        
                                    	if ( (pixels[curZ][curX + curY * sizeX] == origVal) &&
                                    		objConnectivityFilter(xx,yy,zz,objConnectivity) ) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    	
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } //end while loop
        
        //IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
        
        //refill the pixels in refill with OUT ROI Value -> outVal 
        	//IF IN pix (pixCounts[0]) is below OUT pix (pixCounts[1]):
        if(pixCounts[0] < pixCounts[1]) {
        	while( !refill.isEmpty() ) {
        		curCoord = refill.remove(0); // FIXME last element?
	            xy = curCoord.x + curCoord.y * sizeX;
	            pixels[curCoord.z][xy] = (byte)outVal;
        	}
        }

	    
	}
	
	
	
	private void borderObjFilter3DByte(ImageByte img, IntCoord3D seed, Roi borderRoi, int inVal, int outVal) {
		
		//this array stores the number of pixels which are inside the ROI and how many are outside the ROI:
		int[] pixCounts = new int[2];
		
		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;
		
		//int to store xy ref:
		int xy = 0;
        
		//local reference to the pixel values, and image sizes:
		byte[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        
        //reference to the original voxel value of the obj -> for appropriate filtering:
        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
        
        //Two array lists of the IntCoord3D -> one for the queue (to process all voxels), and one for
        	//refill (in case the voxels need to be filled with the outVal instead of the inVal):
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        ArrayList<IntCoord3D> refill = new ArrayList<IntCoord3D>();
        
        //First, add the initial seed to the queue:
        queue.add(seed);
        
        //IJ.showMessage("Old Value: "+ ((int)oldVal) );
        
        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
        
        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
        
        //loop through queue:
        while (!queue.isEmpty()) {
        	//get the coord obj - and remove from queue:
            curCoord = queue.remove(0); // FIXME last element?
            
            //get xy ref:
            xy = curCoord.x + curCoord.y * sizeX;
            
            //if pixel value of this coord equals original value:
            if (pixels[curCoord.z][xy] == origVal) {
            	
            	//add coord to refill at end - will refill with outVal if pix IN is LOWER than pix OUT:
            	refill.add(curCoord);
            	
            	//add to pixCounts depending on whether this coord is in the roi:
            	 if( borderRoi.contains(curCoord.x, curCoord.y ) ) {
            		 //if in ROI, add to ref 0: IN
                 	pixCounts[0] = pixCounts[0] + 1;
                 }
            	 //else if( borderRoi.contains(curCoord.x-1, curCoord.y ) || borderRoi.contains(curCoord.x, curCoord.y-1 ) ) {
            		 //if on border of ROI, add to ref 0: IN
            	 	// This is not needed as now the roi is concatenated in RoiAssessmentHandler prior to calling this method!
            		// pixCounts[0] = pixCounts[0] + 1;
            	 //}
                 else {
                	 //if away from border, add to ref 1: OUT
                 	pixCounts[1] = pixCounts[1] + 1;
                 }
            	
            	 
            	 //Set pixel to inVal -> assume this obj is INSIDE the ROI
                pixels[curCoord.z][xy] = (byte)inVal;
                
                //loop through all 26 voxels surrounding this voxel:
                int curZ, curY, curX;
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > -1 && curZ < sizeZ) {
                        for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > -1 && curY < sizeY) {
                                for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > -1 && curX < sizeX && (xx != 0 || yy != 0 || zz != 0)) {
 
                                    	//if any of these equal the original voxel value, add to queue:
                                        
                                    	if (pixels[curZ][curX + curY * sizeX] == origVal) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    	
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } //end while loop
        
       // IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
        
        //refill the pixels in refill with OUT ROI Value -> outVal 
        	//IF IN pix (pixCounts[0]) is below OUT pix (pixCounts[1]):
        if(pixCounts[0] < pixCounts[1]) {
        	while( !refill.isEmpty() ) {
        		curCoord = refill.remove(0); // FIXME last element?
	            xy = curCoord.x + curCoord.y * sizeX;
	            pixels[curCoord.z][xy] = (byte)outVal;
        	}
        }
        
    }
	
	
	public void borderObjFilter3d262(ImageInt img, int seedX, int seedY, int seedZ, 
			Roi borderRoi, int inVal, int outVal) {
        IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
        //int[] pixCounts = null;
        if (img instanceof ImageShort) {
        	//IJ.showMessage("Short");
            borderObjFilter3DShort26((ImageShort) img, seed, borderRoi, inVal, outVal);
        } else if (img instanceof ImageByte) {
        	//IJ.showMessage("Byte");
        	borderObjFilter3DByte26((ImageByte) img, seed, borderRoi, inVal, outVal);
        }

    }
	
	
	private void borderObjFilter3DShort26(ImageShort img, IntCoord3D seed, Roi borderRoi, int inVal, int outVal) {
		

		//this array stores the number of pixels which are inside the ROI and how many are outside the ROI:
		int[] pixCounts = new int[2];
		
		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;
		
		//int to store xy ref:
		int xy = 0;
        
		//local reference to the pixel values, and image sizes:
		short[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        
        //reference to the original voxel value of the obj -> for appropriate filtering:
        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
        
        //Two array lists of the IntCoord3D -> one for the queue (to process all voxels), and one for
        	//refill (in case the voxels need to be filled with the outVal instead of the inVal):
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        ArrayList<IntCoord3D> refill = new ArrayList<IntCoord3D>();
        
        //First, add the initial seed to the queue:
        queue.add(seed);
        
        //IJ.showMessage("Old Value: "+ ((int)oldVal) );
        
        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
        
        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
        
        //loop through queue:
        while (!queue.isEmpty()) {
        	//get the coord obj - and remove from queue:
            curCoord = queue.remove(0); // FIXME last element?
            
            //get xy ref:
            xy = curCoord.x + curCoord.y * sizeX;
            
            //if pixel value of this coord equals original value:
            if (pixels[curCoord.z][xy] == origVal) {
            	
            	//add coord to refill at end - will refill with outVal if pix IN is LOWER than pix OUT:
            	refill.add(curCoord);
            	
            	//add to pixCounts depending on whether this coord is in the roi:
            	 if( borderRoi.contains(curCoord.x, curCoord.y ) ) {
            		 //if in ROI, add to ref 0: IN
                 	pixCounts[0] = pixCounts[0] + 1;
                 }
            	 else if( borderRoi.contains(curCoord.x-1, curCoord.y ) || borderRoi.contains(curCoord.x, curCoord.y-1 ) ) {
            		 //if on border of ROI, add to ref 0: IN
            		 pixCounts[0] = pixCounts[0] + 1;
            	 }
                 else {
                	 //if away from border, add to ref 1: OUT
                 	pixCounts[1] = pixCounts[1] + 1;
                 }
            	
            	 
            	 //Set pixel to inVal -> assume this obj is INSIDE the ROI
                pixels[curCoord.z][xy] = (byte)inVal;
                
                //loop through all 26 voxels surrounding this voxel:
                int curZ, curY, curX;
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > 0 && curZ < (sizeZ - 1)) {
                        for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > 0 && curY < (sizeY - 1)) {
                                for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
 
                                    	//if any of these equal the original voxel value, add to queue:
                                        
                                    	if (pixels[curZ][curX + curY * sizeX] == origVal) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    	
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } //end while loop
        
        //IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
        
        //refill the pixels in refill with OUT ROI Value -> outVal 
        	//IF IN pix (pixCounts[0]) is below OUT pix (pixCounts[1]):
        if(pixCounts[0] < pixCounts[1]) {
        	while( !refill.isEmpty() ) {
        		curCoord = refill.remove(0); // FIXME last element?
	            xy = curCoord.x + curCoord.y * sizeX;
	            pixels[curCoord.z][xy] = (byte)outVal;
        	}
        }

	    
	}
	
	
	
	private void borderObjFilter3DByte26(ImageByte img, IntCoord3D seed, Roi borderRoi, int inVal, int outVal) {
		
		//this array stores the number of pixels which are inside the ROI and how many are outside the ROI:
		int[] pixCounts = new int[2];
		
		//A reference to IntCorrd3D object:
		IntCoord3D curCoord;
		
		//int to store xy ref:
		int xy = 0;
        
		//local reference to the pixel values, and image sizes:
		byte[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        
        //reference to the original voxel value of the obj -> for appropriate filtering:
        short origVal = pixels[seed.z][seed.x + seed.y * sizeX];
        
        //Two array lists of the IntCoord3D -> one for the queue (to process all voxels), and one for
        	//refill (in case the voxels need to be filled with the outVal instead of the inVal):
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        ArrayList<IntCoord3D> refill = new ArrayList<IntCoord3D>();
        
        //First, add the initial seed to the queue:
        queue.add(seed);
        
        //IJ.showMessage("Old Value: "+ ((int)oldVal) );
        
        //IJ.showMessage("Old Value (byte to int): "+ ( (int)(0xFF & oldVal) ) );
        
        //IJ.showMessage("Old Value (short to int): "+ ( (int)(0xFFFF & oldVal) ) );
        
        //loop through queue:
        while (!queue.isEmpty()) {
        	//get the coord obj - and remove from queue:
            curCoord = queue.remove(0); // FIXME last element?
            
            //get xy ref:
            xy = curCoord.x + curCoord.y * sizeX;
            
            //if pixel value of this coord equals original value:
            if (pixels[curCoord.z][xy] == origVal) {
            	
            	//add coord to refill at end - will refill with outVal if pix IN is LOWER than pix OUT:
            	refill.add(curCoord);
            	
            	//add to pixCounts depending on whether this coord is in the roi:
            	 if( borderRoi.contains(curCoord.x, curCoord.y ) ) {
            		 //if in ROI, add to ref 0: IN
                 	pixCounts[0] = pixCounts[0] + 1;
                 }
            	 //else if( borderRoi.contains(curCoord.x-1, curCoord.y ) || borderRoi.contains(curCoord.x, curCoord.y-1 ) ) {
            		 //if on border of ROI, add to ref 0: IN
            	 	// This is not needed as now the roi is concatenated in RoiAssessmentHandler prior to calling this method!
            		// pixCounts[0] = pixCounts[0] + 1;
            	 //}
                 else {
                	 //if away from border, add to ref 1: OUT
                 	pixCounts[1] = pixCounts[1] + 1;
                 }
            	
            	 
            	 //Set pixel to inVal -> assume this obj is INSIDE the ROI
                pixels[curCoord.z][xy] = (byte)inVal;
                
                //loop through all 26 voxels surrounding this voxel:
                int curZ, curY, curX;
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > -1 && curZ < sizeZ) {
                        for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > -1 && curY < sizeY) {
                                for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > -1 && curX < sizeX && (xx != 0 || yy != 0 || zz != 0)) {
 
                                    	//if any of these equal the original voxel value, add to queue:
                                        
                                    	if (pixels[curZ][curX + curY * sizeX] == origVal) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    	
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } //end while loop
        
       // IJ.showMessage("Pix IN: "+pixCounts[0]+" Pix OUT: "+pixCounts[1]);
        
        //refill the pixels in refill with OUT ROI Value -> outVal 
        	//IF IN pix (pixCounts[0]) is below OUT pix (pixCounts[1]):
        if(pixCounts[0] < pixCounts[1]) {
        	while( !refill.isEmpty() ) {
        		curCoord = refill.remove(0); // FIXME last element?
	            xy = curCoord.x + curCoord.y * sizeX;
	            pixels[curCoord.z][xy] = (byte)outVal;
        	}
        }
        
    }
	
	

    public static void flood3d62(ImageInt img, int seedX, int seedY, int seedZ, int newVal) {
        IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
        if (img instanceof ImageShort) {
            flood3DShort6((ImageShort) img, seed, (short) newVal);
        } else if (img instanceof ImageByte) {
            flood3DByte6((ImageByte) img, seed, (byte) newVal);
        }
    }
    
    
    /**
     * This method will flood fill the object in img at coordinate seedX seedY seedZ with newVal.
     * @param img
     * @param seedX
     * @param seedY
     * @param seedZ
     * @param newVal
     */
    public void flood3d(ImageInt img, int seedX, int seedY, int seedZ, int newVal) {
        IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
        if (img instanceof ImageShort) {
            flood3DShort((ImageShort) img, seed, (short) newVal);
        } else if (img instanceof ImageByte) {
            flood3DByte((ImageByte) img, seed, (byte) newVal);
        }
    }
    
    
    private void flood3DShort(ImageShort img, IntCoord3D seed, short newVal) {
        short[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        short oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            IntCoord3D curCoord = queue.remove(0); // FIXME last element?
            int xy = curCoord.x + curCoord.y * sizeX;
            if (pixels[curCoord.z][xy] == oldVal) {
                pixels[curCoord.z][xy] = newVal;
                int curZ, curY, curX;
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > 0 && curZ < (sizeZ - 1)) {
                        for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > 0 && curY < (sizeY - 1)) {
                                for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
                                        if (pixels[curZ][curX + curY * sizeX] == oldVal) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void flood3DByte(ImageByte img, IntCoord3D seed, byte newVal) {
        byte[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        short oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            IntCoord3D curCoord = queue.remove(0); // FIXME last element?
            int xy = curCoord.x + curCoord.y * sizeX;
            if (pixels[curCoord.z][xy] == oldVal) {
                pixels[curCoord.z][xy] = newVal;
                int curZ, curY, curX;
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > -1 && curZ < sizeZ) {
                        for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > -1 && curY < sizeY) {
                                for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > -1 && curX < sizeX && (xx != 0 || yy != 0 || zz != 0)) {
                                        if ( (pixels[curZ][curX + curY * sizeX] == oldVal) &&
                                        		objConnectivityFilter(xx,yy,zz,objConnectivity) ) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This method will flood fill the object in img at coordinate seedX seedY seedZ with newVal.
     * @param img
     * @param seedX
     * @param seedY
     * @param seedZ
     * @param newVal
     */
    public static void flood3d262(ImageInt img, int seedX, int seedY, int seedZ, int newVal) {
        IntCoord3D seed = new IntCoord3D(seedX, seedY, seedZ);
        if (img instanceof ImageShort) {
            flood3DShort26((ImageShort) img, seed, (short) newVal);
        } else if (img instanceof ImageByte) {
            flood3DByte26((ImageByte) img, seed, (byte) newVal);
        }
    }

    private static void flood3DShort6(ImageShort img, IntCoord3D seed, short newVal) {
        short[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        short oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            IntCoord3D curCoord = queue.remove(0); // FIXME last element?
            int xy = curCoord.x + curCoord.y * sizeX;
            if (pixels[curCoord.z][xy] == oldVal) {
                pixels[curCoord.z][xy] = newVal;
                if (curCoord.x > 0 && pixels[curCoord.z][xy - 1] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x - 1, curCoord.y, curCoord.z));
                }
                if (curCoord.x < (sizeX - 1) && pixels[curCoord.z][xy + 1] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x + 1, curCoord.y, curCoord.z));
                }
                if (curCoord.y > 0 && pixels[curCoord.z][xy - sizeX] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y - 1, curCoord.z));
                }
                if (curCoord.y < (sizeY - 1) && pixels[curCoord.z][xy + sizeX] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y + 1, curCoord.z));
                }
                if (curCoord.z > 0 && pixels[curCoord.z - 1][xy] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y, curCoord.z - 1));
                }
                if (curCoord.z < (sizeZ - 1) && pixels[curCoord.z + 1][xy] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y, curCoord.z + 1));
                }
            }
        }
    }

    public static void flood3DNoiseShort62(ImageShort img, IntCoord3D seed, short limit, short newVal) {
        short[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        int oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
        //int limit=oldVal-noise;
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            IntCoord3D curCoord = queue.remove(0); // FIXME last element?
            IJ.log("processing " + curCoord.x + " " + curCoord.y + " " + curCoord.z + " " + oldVal + " " + limit);
            int xy = curCoord.x + curCoord.y * sizeX;
            if (pixels[curCoord.z][xy] >= limit) {
                pixels[curCoord.z][xy] = newVal;
                if (curCoord.x > 0 && pixels[curCoord.z][xy - 1] >= limit) {
                    queue.add(new IntCoord3D(curCoord.x - 1, curCoord.y, curCoord.z));
                }
                if (curCoord.x < (sizeX - 1) && pixels[curCoord.z][xy + 1] >= limit) {
                    queue.add(new IntCoord3D(curCoord.x + 1, curCoord.y, curCoord.z));
                }
                if (curCoord.y > 0 && pixels[curCoord.z][xy - sizeX] >= limit) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y - 1, curCoord.z));
                }
                if (curCoord.y < (sizeY - 1) && pixels[curCoord.z][xy + sizeX] >= limit) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y + 1, curCoord.z));
                }
                if (curCoord.z > 0 && pixels[curCoord.z - 1][xy] >= limit) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y, curCoord.z - 1));
                }
                if (curCoord.z < (sizeZ - 1) && pixels[curCoord.z + 1][xy] >= limit) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y, curCoord.z + 1));
                }
            }
        }
    }

    private static void flood3DByte6(ImageByte img, IntCoord3D seed, byte newVal) {
        byte[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        short oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            IntCoord3D curCoord = queue.remove(0); // FIXME last element?
            int xy = curCoord.x + curCoord.y * sizeX;
            if (pixels[curCoord.z][xy] == oldVal) {
                pixels[curCoord.z][xy] = newVal;
                if (curCoord.x > 0 && pixels[curCoord.z][xy - 1] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x - 1, curCoord.y, curCoord.z));
                }
                if (curCoord.x < (sizeX - 1) && pixels[curCoord.z][xy + 1] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x + 1, curCoord.y, curCoord.z));
                }
                if (curCoord.y > 0 && pixels[curCoord.z][xy - sizeX] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y - 1, curCoord.z));
                }
                if (curCoord.y < (sizeY - 1) && pixels[curCoord.z][xy + sizeX] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y + 1, curCoord.z));
                }
                if (curCoord.z > 0 && pixels[curCoord.z - 1][xy] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y, curCoord.z - 1));
                }
                if (curCoord.z < (sizeZ - 1) && pixels[curCoord.z + 1][xy] == oldVal) {
                    queue.add(new IntCoord3D(curCoord.x, curCoord.y, curCoord.z + 1));
                }
            }
        }
    }

    private static void flood3DShort26(ImageShort img, IntCoord3D seed, short newVal) {
        short[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        short oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            IntCoord3D curCoord = queue.remove(0); // FIXME last element?
            int xy = curCoord.x + curCoord.y * sizeX;
            if (pixels[curCoord.z][xy] == oldVal) {
                pixels[curCoord.z][xy] = newVal;
                int curZ, curY, curX;
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > 0 && curZ < (sizeZ - 1)) {
                        for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > 0 && curY < (sizeY - 1)) {
                                for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > 0 && curX < (sizeX - 1) && (xx != 0 || yy != 0 || zz != 0)) {
                                        if (pixels[curZ][curX + curY * sizeX] == oldVal) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void flood3DNoise262(ImageHandler img, IntCoord3D seed, int limit, int newVal) {
        //short[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        //short oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
        //short newval = (short) newVal;
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            IntCoord3D curCoord = queue.remove(0); // FIXME last element?
            //int xy = curCoord.x + curCoord.y * sizeX;
            if (img.getPixel(curCoord.x, curCoord.y, curCoord.z) >= limit) {
                //IJ.log("Flood " + curCoord.x + " " + curCoord.y + " " + curCoord.z + " " + img.getPixel(curCoord.x, curCoord.y, curCoord.z) + " " + limit);
                img.setPixel(curCoord.x, curCoord.y, curCoord.z, newVal);
                //pixels[curCoord.z][xy] = newval;
                int curZ, curY, curX;
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if ((curZ >= 0) && (curZ <= (sizeZ - 1))) {
                        for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if ((curY >= 0) && (curY <= (sizeY - 1))) {
                                for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if ((curX >= 0) && (curX <= (sizeX - 1))) {
                                        if (img.getPixel(curX, curY, curZ) >= limit) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void flood3DByte26(ImageByte img, IntCoord3D seed, byte newVal) {
        byte[][] pixels = img.pixels;
        int sizeX = img.sizeX;
        int sizeY = img.sizeY;
        int sizeZ = img.sizeZ;
        short oldVal = pixels[seed.z][seed.x + seed.y * sizeX];
        ArrayList<IntCoord3D> queue = new ArrayList<IntCoord3D>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            IntCoord3D curCoord = queue.remove(0); // FIXME last element?
            int xy = curCoord.x + curCoord.y * sizeX;
            if (pixels[curCoord.z][xy] == oldVal) {
                pixels[curCoord.z][xy] = newVal;
                int curZ, curY, curX;
                for (int zz = -1; zz < 2; zz++) {
                    curZ = curCoord.z + zz;
                    if (curZ > -1 && curZ < sizeZ) {
                        for (int yy = -1; yy < 2; yy++) {
                            curY = curCoord.y + yy;
                            if (curY > -1 && curY < sizeY) {
                                for (int xx = -1; xx < 2; xx++) {
                                    curX = curCoord.x + xx;
                                    if (curX > -1 && curX < sizeX && (xx != 0 || yy != 0 || zz != 0)) {
                                        if (pixels[curZ][curX + curY * sizeX] == oldVal) {
                                            queue.add(new IntCoord3D(curX, curY, curZ));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}











