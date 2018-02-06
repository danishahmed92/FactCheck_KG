package test;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

/**
 * Class to provide interface for retrieving confidence value of facts
 * 
 * @author Nikit
 *
 */
public class FactChecker {
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
		/*
		 * String[] case3 = {"U",triple[1],"K"}; String[] case4 = {"K",triple[1],"U"};
		 */
		List<TestRule> rules = new ArrayList<>();
		rules.addAll(RuleProvider.fetchRuleData(triple[0], case1, session));
		rules.addAll(RuleProvider.fetchRuleData(triple[2], case2, session));
		/*
		 * rules.addAll(RuleProvider.fetchRuleData(triple[0], case3, session));
		 * rules.addAll(RuleProvider.fetchRuleData(triple[2], case4, session));
		 */
		cfVal = ConfidenceProvider.getConfidenceValue(rules);
		return cfVal / 4;
	}

}
