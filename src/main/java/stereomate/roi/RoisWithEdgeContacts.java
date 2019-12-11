package stereomate.roi;

import java.util.ArrayList;

import ij.gui.Roi;

public class RoisWithEdgeContacts {
	ArrayList<Roi> rois;
	int[][] edgeContacts;

	public RoisWithEdgeContacts (ArrayList<Roi> rois, int[][] edgeContacts ) {
		this.rois = rois;
		this.edgeContacts = edgeContacts;
	}
}
