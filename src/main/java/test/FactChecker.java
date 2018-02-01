package test;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

public class FactChecker {
	
	public double getFactCFVal(String[] triple, Session session) {
		double cfVal = 0;
		String[] case1 = {"U",triple[1],triple[2]};
		String[] case2 = {triple[0],triple[1],"U"};
		List<TestRule> rules = new ArrayList<>();
		rules.addAll(RuleProvider.fetchRuleData(triple[0], case1, session));
		rules.addAll(RuleProvider.fetchRuleData(triple[2], case2, session));
		cfVal = ConfidenceProvider.getConfidenceValue(rules);
		return cfVal;
	}

}
