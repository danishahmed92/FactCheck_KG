package test;

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
	public String getSelQuery(int index) {
		StringBuilder queryStr = new StringBuilder();
		queryStr.append("SELECT count(1) as ?cVal").append(index);
		queryStr.append("WHERE { <").append(subjURI).append("> ");
		queryStr.append(" <").append(predURI).append("> ");
		queryStr.append(" <").append(objURI).append("> . }");
		return queryStr.toString();
	}

	// Get bind phrase
	public String getBindPhrase(int index) {
		StringBuilder queryStr = new StringBuilder();
		queryStr.append(" bind( if(?cVal").append(index).append("> 0, 1, -1) as ?result").append(index).append(" ) .");
		return queryStr.toString();
	}
	// Get calc phrase
	public String getCalcPhrase(int index) {
		StringBuilder queryStr = new StringBuilder();
		queryStr.append(" (xsd:float(?result").append(index).append(")*").append(sigVal).append(") ");
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
