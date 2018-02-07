package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

/**
 * Class to provide interface for retrieving confidence value of facts
 * 
 * @author Nikit
 *
 */
public class FactChecker {
	
	public static Map<String, Double> cfValCacheMap;
	static {
		cfValCacheMap = new HashMap<>();
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
		Double[] cfVals = new Double[4];
		for(int i=0;i<caseArr.length;i++) {
			//get cached value
			tempKey = getCNJStr(caseArr[i]);
			tempVal = cfValCacheMap.get(tempKey);
			if(tempVal == null) {
				tempVal = getCaseCFVal(sourceArr[i], caseArr[i], session);
				cfValCacheMap.put(tempKey, tempVal);
			}else {
				cacheCount++;
			}
			/*if(tempVal == -10) {
				cfVal = -10d;
				break;
			}*/
			cfVals[i] = tempVal; 
			cfVal+=tempVal;
		}
		System.out.println("Cache called for fact: "+cacheCount+" times.");
		return cfVal;
	}
	
	public static String getCNJStr(String[] arr) {
		String res = new String();
		for(String entry: arr) {
			res+=entry;
		}
		return res;
	}
	
	public double getCaseCFVal(String source, String[] caseTriple, Session session) {
		double cfVal;
		List<TestRule> rules = new ArrayList<>();
		rules.addAll(RuleProvider.fetchRuleData(source, caseTriple, session));
		if(rules.size()>0)
			cfVal = ConfidenceProvider.getConfidenceValue(rules);
		else
			cfVal = -0.54;
		return cfVal;
	}

}
