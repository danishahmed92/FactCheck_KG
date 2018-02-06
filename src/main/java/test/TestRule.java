package test;

import utils.Constants;

public class TestRule {

	private String subjURI;
	private String predURI;
	private String objURI;
	private Double sigVal;

	public TestRule(String subjURI, String predURI, String objURI, Double sigVal) {
		super();
		this.subjURI = subjURI;
		this.predURI = predURI;
		this.objURI = objURI;
		this.sigVal = sigVal;
	}

	// Get select query
	/**
	 * Method to fetch the specific "Select" query string for the current TestRule
	 * 
	 * @param index
	 *            - index for the index
	 * @return - Query fragment String
	 */
	public String getSelQuery(int index) {
		StringBuilder queryStr = new StringBuilder();
		queryStr.append("SELECT (count(1) as ?cVal").append(index);
		queryStr.append(") WHERE { <").append(subjURI).append("> ");
		queryStr.append(" <").append(predURI).append("> ");
		queryStr.append(" ").append(encloseObj(objURI)).append(" . }");
		return queryStr.toString();
	}

	/**
	 * Method to enclose the Object data based on it being a URI or value
	 * 
	 * @param objData
	 *            - object data
	 * @return - objectData enclosed in <> or ""
	 */
	public static String encloseObj(String objData) {
		if (objData.matches(Constants.URI_REGEX)) {
			return "<" + objData + ">";
		} else
			return "\"" + objData.replace("\n", "") + "\"";
	}

	// Get bind phrase
	/**
	 * Method to get the Specific Bind phrase for confidence value calculation for
	 * current TestRule
	 * 
	 * @param index
	 *            - current index
	 * @return - Query fragment String
	 */
	public String getBindPhrase(int index) {
		StringBuilder queryStr = new StringBuilder();
		queryStr.append(" bind( if(?cVal").append(index).append("> 0, 1, -1) as ?result").append(index).append(" ) .");
		return queryStr.toString();
	}

	// Get calc phrase
	/**
	 * Method to fetch the query calculation part for the specific test rule
	 * 
	 * @param index
	 *            - current index
	 * @return - Query fragment string
	 */
	public String getCalcPhrase(int index) {
		StringBuilder queryStr = new StringBuilder();
		queryStr.append(" ?result").append(index).append("*").append(sigVal).append(" ");
		return queryStr.toString();
	}

	// Getter and Setters
	public String getSubjURI() {
		return subjURI;
	}

	public void setSubjURI(String subjURI) {
		this.subjURI = subjURI;
	}

	public String getPredURI() {
		return predURI;
	}

	public void setPredURI(String predURI) {
		this.predURI = predURI;
	}

	public String getObjURI() {
		return objURI;
	}

	public void setObjURI(String objURI) {
		this.objURI = objURI;
	}

	public Double getSigVal() {
		return sigVal;
	}

	public void setSigVal(Double sigVal) {
		this.sigVal = sigVal;
	}

}
