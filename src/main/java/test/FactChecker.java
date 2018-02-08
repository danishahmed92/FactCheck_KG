package test;

import java.io.FileNotFoundException;
import java.nio.file.FileVisitResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import rdf.FactCheckResource;
import rdf.TripleExtractor;

import static ml.RulesExtraction.getTriples;

/**
 * Class to provide interface for retrieving confidence value of facts
 * 
 * @author Nikit
 *
 */
public class FactChecker {
	/**
	 * Map to store cached confidence values against unique key
	 */
	public static Map<String, Double> cfValCacheMap;
	/**
	 * Map to store formatted confidence values against unique key
	 */
	public static Map<String, String> resCache;
	/**
	 * List to store all formatted values
	 */
	private List<String> formattedResult;
	static {
		cfValCacheMap = new HashMap<>();
		resCache = new HashMap<>();
	}
	
	public FactChecker() {
		formattedResult = new ArrayList<>();
	}
	/**
	 * Method to retrieve confidence value of a triple
	 * 
	 * @param triple
	 *            - triple to be tested
	 * @param session
	 *            - hibernate session instance
	 * @return - total confidence value
	 */
	public double getFactCFVal(String[] triple, Session session) {
		double cfVal = 0;
		String[] case1 = { "U", triple[1], triple[2] };
		String[] case2 = { triple[0], triple[1], "U" };

		String[] case3 = { "U", triple[1], "K" };
		String[] case4 = { "K", triple[1], "U" };

		List<TestRule> rules = new ArrayList<>();
		rules.addAll(RuleProvider.fetchRuleData(triple[0], case1, session));
		rules.addAll(RuleProvider.fetchRuleData(triple[2], case2, session));

		rules.addAll(RuleProvider.fetchRuleData(triple[0], case3, session));
		rules.addAll(RuleProvider.fetchRuleData(triple[2], case4, session));
		if(rules.size()>0)
			cfVal = ConfidenceProvider.getConfidenceValue(rules);
		else
			cfVal = -10;
		return cfVal;
	}
	
	/**
	 * Method to calculate initial confidence value for a triple. Also, cache the values to avoid repeated queries.
	 * @param triple - triple to be evaluated
	 * @param session - hibernate session
	 * @return - initial confidence value
	 */
	public Double getFactCFValAdv(String[] triple, Session session) {
		Double cfVal = 0d;
		int cacheCount = 0;
		String[] case1 = { "U", triple[1], triple[2] };
		String[] case2 = { triple[0], triple[1], "U" };

		String[] case3a = { "U", triple[1], "K" };
		String[] case3b = { "K", triple[1], "U" };
		//Loop through all the cases and check if cached
		String[][] caseArr = {case1, case2, case3a, case3b};
		String[] sourceArr = {triple[0], triple[2],triple[0], triple[2]};
		Double tempVal;
		String tempKey;
		String tempRes;
		Double[] cfVals = new Double[4];
		for(int i=0;i<caseArr.length;i++) {
			//get cached value
			tempRes = sourceArr[i];
			tempKey = getCNJStr(tempRes, caseArr[i]);
			tempVal = cfValCacheMap.get(tempKey);
			if(tempVal == null) {
				tempVal = getCaseCFVal(tempRes , caseArr[i], tempKey, session);
			}else {
				cacheCount++;
				formattedResult.add(resCache.get(tempKey));
			}
			/*if(tempVal == -10) {
				cfVal = -10d;
				break;
			}*/
			cfVals[i] = tempVal; 
			cfVal+=tempVal;
		}
		System.out.println("Cache called for fact: "+cacheCount+" times.");
		//return cfVal;
		//Calculate normalized value
		double nVal = 0;
		int totCount = 0;
		for(String entry: formattedResult) {
			if(!entry.equalsIgnoreCase("NA")) {
				nVal+=Double.valueOf(entry);
				totCount++;
			}
		}
		if(totCount==0)
			nVal = -10;
		return nVal/totCount;
	}
	/**
	 * Method to return a unique key for a particular query combination
	 * @param resource - resource string
	 * @param arr - triple pattern array
	 * @return - unique conjoined string
	 */
	public static String getCNJStr(String resource, String[] arr) {
		String res = new String();
		res+=resource;
		for(String entry: arr) {
			res+=entry;
		}
		return res;
	}
	/**
	 * Method to fetch sum of significance values using sparql
	 * @param source - subject of the triple
	 * @param caseTriple - triple pattern
	 * @param tempKey - unique key
	 * @param session - hibernate session
	 * @return - total significance value
	 */
	public double getCaseCFVal(String source, String[] caseTriple, String tempKey, Session session) {
		double cfVal;
		List<TestRule> rules = new ArrayList<>();
		rules.addAll(RuleProvider.fetchRuleData(source, caseTriple, session));
		String frmStrRes;
		if(rules.size()>0) {
			cfVal = ConfidenceProvider.getConfidenceValue(rules);
			frmStrRes = String.valueOf(cfVal);
		}
		else {
			cfVal = -0.54;
			frmStrRes = "NA";
		}
		formattedResult.add(frmStrRes);
		cfValCacheMap.put(tempKey, cfVal);
		resCache.put(tempKey, frmStrRes);
		return cfVal;
	}
	
	//Getter and Setter
	public List<String> getFormattedResult() {
		return formattedResult;
	}
	public void setFormattedResult(List<String> formattedResult) {
		this.formattedResult = formattedResult;
	}

	/**
	 *
	 * @param confidenceValueList list of confidence values of triples that you need to evaluate
	 * @param threshold threshold that you calculated while training your dataset
	 */
	public static void evaluateFactsFromListOfConfidence(ArrayList<String> confidenceValueList
			, Double threshold, Double correctBoost, Double wrongBoost) {
		int falseCount = 0;
		int trueCount = 0;
		Double cv;
		Double diff;
		for (int i = 0; i < confidenceValueList.size(); i++) {
			cv = Double.parseDouble(confidenceValueList.get(i));
			if (cv <= threshold) {
				if (cv < -1.0)
					cv = -1.0;
				diff = Math.abs(threshold - cv);
				System.out.println("False:\t" + cv + "\t" + diff + "\t" + ((diff+wrongBoost)*100));
				falseCount++;
			} else {
				if (cv > 1.0)
					cv = 1.0;
				diff = Math.abs(cv - (threshold));
				System.out.println("True:\t" + cv + "\t" + diff + "\t" + ((diff+correctBoost)*100));
				trueCount++;
			}
		}
		System.out.println("false count:\t" + falseCount);
		System.out.println("true count:\t" + trueCount);
		System.out.println();
	}

	public static void evaluateFactFromConfidence(Double confidenceValue,
												  Double threshold, Double correctBoost, Double wrongBoost) {
		Double evaluationValue;
		if (confidenceValue <= threshold) {
			if (confidenceValue < -1.0)
				confidenceValue = -1.0;
			evaluationValue = Math.abs(threshold - confidenceValue);
			evaluationValue = (evaluationValue + wrongBoost) * 100;
			System.out.println("Threshold:\t" + ConfidenceProvider.getPercentThresholdToDisplay(correctBoost, wrongBoost, threshold));
			System.out.println("Fact evaluated as:\tFalse");
			System.out.println("Matching percentage:\t" + evaluationValue + "%");
		} else {
			if (confidenceValue > 1.0)
				confidenceValue = 1.0;
			evaluationValue = Math.abs(confidenceValue - (threshold));
			evaluationValue = (evaluationValue + correctBoost) * 100;
			System.out.println("Threshold:\t" + ConfidenceProvider.getPercentThresholdToDisplay(correctBoost, wrongBoost, threshold));
			System.out.println("Fact evaluated as:\tTrue");
			System.out.println("Matching percentage:\t" + evaluationValue + "%");
		}
	}

	public static void evaluateFactFromFile(String fileName, Session session) throws FileNotFoundException {
		TripleExtractor tripleExtractor = getTriples(fileName);

		String subjectUri = FactCheckResource.getDBpediaUri(tripleExtractor.subject);
		String predicateUri = tripleExtractor.predicate.getURI();
		String objectUri = FactCheckResource.getDBpediaUri(tripleExtractor.object);

		// Check for empty content
		boolean cond1 = subjectUri.trim().length() == 0;
		boolean cond2 = predicateUri.trim().length() == 0;
		boolean cond3 = objectUri.trim().length() == 0;
		if (cond1 || cond2 || cond3) {
			System.out.println("Invalid File.");
			return;
		}

		FactChecker factChecker = new FactChecker();
		String[] triple = { subjectUri, predicateUri, objectUri };
		double cfVal = factChecker.getFactCFValAdv(triple, session);

		Double threshold = ConfidenceProvider.getConfidenceThreshold("correct_award_train_threshold.txt", "wrong_range_award_train_threshold.txt");
		Double correctBoost = ConfidenceProvider.getCorrectBoost("correct_award_train_threshold.txt");
		Double wrongBoost = ConfidenceProvider.getWrongBoost("wrong_range_award_train_threshold.txt");

		evaluateFactFromConfidence(cfVal, threshold, correctBoost, wrongBoost);
	}
	
	

}
