package stereomate.data;

public abstract class DataContainer2 {

	double[] dataValues;
	String[] dataTitles;
	
	/**
	 * Initialises the DataObj with default 0 and 0.0 values.
	 */
	public DataContainer2() {
		setDataValuesAndTitles();
	}
	
	/**
	 * Set the  dataValues and dataTitles arrays to the data values and datapoint titles.
	 */
	public abstract void setDataValuesAndTitles();
	

	public double[] returnData() {
		return dataValues;
	}

	public String[] returnDataTitles() {
		return dataTitles;
	}
	
	
}
