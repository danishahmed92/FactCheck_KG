package test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import utils.Util;

/**
 * Class to implement methods that will help provide a confidence value for a
 * collection of provided rules
 * 
 * @author Nikit
 *
 */
public class ConfidenceProvider {
	public static int ruleLim = 5;

	// Method to accept a testRule object and return count of records fetched
	/**
	 * Method to accept a list of TestRule objects and calculate the total
	 * significance value of those rules
	 * 
	 * @param rules
	 *            - List of TestRule objects
	 * @return computed significance value
	 */
	public static double getConfidenceValue(List<TestRule> rules) {
		double cfVal = 0;
		int startInd = 0;
		int endIndex = ruleLim;
		int ruleSize = rules.size();
		// Since the query seems too large, divide it into fragments
		if (ruleSize > ruleLim) {
			
			while (true) {
				if (startInd >= ruleSize)
					break;
				if (endIndex > ruleSize)
					endIndex = ruleSize;
				//System.out.println("Calculating confidence value from: "+startInd+" to "+endIndex+" out of "+ruleSize);
				cfVal += executeResultQuery(generateQuery(rules.subList(startInd, endIndex)));
				startInd += ruleLim;
				endIndex += ruleLim;
			}
		} else {
			cfVal = executeResultQuery(generateQuery(rules));
		}
		return cfVal;
	}

	// Query Generator
	/**
	 * Method to generate a SPARQL query to calculate total significance value for a
	 * list of rules.
	 * 
	 * @param rules
	 *            - list of TestRule
	 * @return - Generate Query String
	 */
	public static String generateQuery(List<TestRule> rules) {
		StringBuilder queryStr = new StringBuilder();
		Map<Integer, TestRule> indexMap = getIndexMap(rules);
		StringBuilder calcStat = new StringBuilder();
		StringBuilder selStat = new StringBuilder();
		StringBuilder bindStat = new StringBuilder();
		TestRule tempRule;
		for (int i = 1; i <= rules.size(); i++) {
			tempRule = indexMap.get(i);
			// calc part formation
			calcStat.append(tempRule.getCalcPhrase(i));
			calcStat.append(" ");
			if (i != rules.size()) {
				calcStat.append("+");
			}
			// select statement formation
			selStat.append(" { ").append(tempRule.getSelQuery(i)).append(" } ");
			// bind statement formation
			bindStat.append(tempRule.getBindPhrase(i));
		}
		queryStr.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ");
		queryStr.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ");
		queryStr.append("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ");
		queryStr.append("PREFIX fn: <http://www.w3.org/2005/xpath-functions#> ");
		queryStr.append(" SELECT ")/* .append(calcStat) */.append(" ?cf WHERE { ").append(selStat).append(bindStat)
				.append(" ").append(" BIND( ").append(calcStat).append(" as ?cf1) . BIND(str(?cf1) as ?cf) .")
				.append(" } LIMIT 1");
		return queryStr.toString();
	}

	// index map generator
	/**
	 * Method to map a list of TestRule against its index
	 * 
	 * @param rules
	 *            - list of TestRule
	 * @return - indexed Map
	 */
	public static Map<Integer, TestRule> getIndexMap(List<TestRule> rules) {
		Map<Integer, TestRule> indexMap = new HashMap<>();
		int i = 1;
		for (TestRule entry : rules) {
			indexMap.put(i++, entry);
		}
		return indexMap;
	}

	// Query Executor
	/**
	 * Method to execute a SPARQL query on SPARQL remote server to calculate and
	 * return confidence value
	 * 
	 * @param queryStr
	 *            - Query String to execute
	 * @return - confidence value
	 */
	public static double executeResultQuery(String queryStr) {
		double res = 0;
		Query query = QueryFactory.create(queryStr);
		// Remote execution.
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)) {
			// Set the DBpedia specific timeout.
			((QueryEngineHTTP) qexec).addParam("timeout", "3000000");

			// Execute.
			ResultSet rs = qexec.execSelect();
			RDFNode tempNode;
			while (rs.hasNext()) {
				tempNode = rs.next().get("cf");
				res = Double.parseDouble(tempNode.toString());
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(queryStr);
		}
		return res;
	}

	public static void main(String[] args) throws IOException {
		try {
			BufferedReader brC = new BufferedReader(new FileReader("correct_award_train_threshold.txt"));
			BufferedReader brW = new BufferedReader(new FileReader("wrong_range_award_train_threshold.txt"));

			String lineC;
			ArrayList<String> confidenceValueC = new ArrayList<>();
			while ((lineC = brC.readLine()) != null) {
				String[] splitLine = lineC.split("\\s+");
				confidenceValueC.add(splitLine[4]);
			}

			String lineW;
			ArrayList<String> confidenceValueW = new ArrayList<>();
			while ((lineW = brW.readLine()) != null) {
				String[] splitLine = lineW.split("\\s+");
				confidenceValueW.add(splitLine[4]);
			}
			Double threshold = getConfidenceThreshold("correct_award_train_threshold.txt", "wrong_range_award_train_threshold.txt");
			System.out.println(threshold);
			double correctBoost = getAboveAverageCount(confidenceValueC.toArray(), Util.mean(confidenceValueC.toArray())) / (double) confidenceValueC.size();
			double wrongBoost = getBelowAverageCount(confidenceValueW.toArray(), Util.mean(confidenceValueW.toArray())) / (double) confidenceValueW.size();
			System.out.println("correct set boost:\t" + correctBoost);
			System.out.println("wrong set boost:\t" + wrongBoost);
			System.out.println(getPercentThresholdToDisplay(correctBoost, wrongBoost, threshold));
		} catch (IOException ignore) {
			// don't index files that can't be read.
			ignore.printStackTrace();
		}
	}

	/**
	 * This threshold will be displayed to user.
	 * Any Value less than this is a false fact,
	 * all above are counted as true.
	 * @param correctMatchRatio num of elements that were above mean
	 * @param wrongMatchRatio num of elements that were below mean
	 * @param threshold threshold that you calculated
	 * @return percentage of threshold
	 */
	public static Double getPercentThresholdToDisplay(Double correctMatchRatio, Double wrongMatchRatio, Double threshold) {
		Double diff;
		if (correctMatchRatio > wrongMatchRatio)
			diff = correctMatchRatio - wrongMatchRatio;
		else
			diff = wrongMatchRatio - correctMatchRatio;

		Double thresholdDiff = Math.abs(diff + threshold);
		return ((diff + thresholdDiff + 0.0001)*100);
	}

	/**
	 * Calculate 1st step value of threshold by using mean and standard deviation of both correct and wrong CV's
	 * First calculate mean and standard deviation individually
	 * then find centroid of individual sets using the values you got above
	 * last, take average of both sets
	 *
	 * @param correctFile path of correct file that has 20% of train data
	 * @param wrongFile path of wrong file that has 20% of train data
	 * @return threshold
	 */
	public static Double getConfidenceThreshold(String correctFile, String wrongFile) {
		try {
			BufferedReader brC = new BufferedReader(new FileReader(correctFile));
			BufferedReader brW = new BufferedReader(new FileReader(wrongFile));

			String lineC;
			ArrayList<String> confidenceValueC = new ArrayList<>();
			while ((lineC = brC.readLine()) != null) {
				String[] splitLine = lineC.split("\\s+");
				confidenceValueC.add(splitLine[4]);
			}

			String lineW;
			ArrayList<String> confidenceValueW = new ArrayList<>();
			while ((lineW = brW.readLine()) != null) {
				String[] splitLine = lineW.split("\\s+");
				confidenceValueW.add(splitLine[4]);
			}

			Double correctMean = Util.mean(confidenceValueC.toArray());
			Double correctStandardDeviation = Util.standardDeviation(confidenceValueC.toArray());

			Double wrongMean = Util.mean(confidenceValueW.toArray());
			Double wrongStandardDeviation = Util.standardDeviation(confidenceValueW.toArray());

			Double correctSDMidPoint = (correctMean + (correctMean + correctStandardDeviation)) / 2;
			Double wrongSDMidPoint = (wrongMean + (wrongMean + wrongStandardDeviation)) / 2;

			return (correctSDMidPoint + wrongSDMidPoint) / 2;
		}catch (IOException io) {
			io.printStackTrace();
		}
		return 0.0;
	}

	/**
	 * To be called by correct data set of CV
	 * @param arr array of confidence values of correct train dataset
	 * @param mean mean of above array
	 * @return count int of matching above average
	 */
	public static int getAboveAverageCount(Object[] arr, Double mean) {
		int count = 0;
		for (Object anArr : arr) {
			if (Double.parseDouble(String.valueOf(anArr)) >= mean)
				count++;
		}
		return count;
	}

	/**
	 * To be called by wrong data set of CV
	 * @param arr array of confidence values of wrong train dataset
	 * @param mean mean of above array
	 * @return count of matching below average
	 */
	public static int getBelowAverageCount(Object[] arr, Double mean) {
		int count = 0;
		for (Object anArr : arr) {
			if (Double.parseDouble(String.valueOf(anArr)) <= mean)
				count++;
		}
		return count;
	}

}
