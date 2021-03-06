package provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;

import bean.KgPred;
import bean.KgPredMap;
import bean.KgRule;
import bean.KgRuleMap;
import bean.KgRulePropvalBucket;
import bean.TriplePatternMap;
import utils.Constants;
import utils.HibernateUtils;

/**
 * Class providing methods to process the extracted data from DBpedia and
 * persist them as rules into the database using hibernate API
 * 
 * @author Nikit
 *
 */
public class PersistenceProvider {
	/**
	 * Method to persist all the rules related to the particular triple pattern into
	 * the database
	 * 
	 * @param triplePattern
	 *            - String array consisting values of Subject, Predicate and Object
	 *            respectively
	 * @param predFreq
	 *            - frequency map of all the related predicates for the given triple
	 * @param predRuleFreqMap
	 *            - frequency map of all the property values also mapped to the
	 *            predicate they belong to
	 * @return count of records inserted to the database
	 */
	public static int persistRules(String[] triplePattern, Map<String, Integer> predFreq,
			Map<String, Map<String, Integer>> predRuleFreqMap) {
		Session session = HibernateUtils.getSessionFactory().openSession();
		session.beginTransaction();
		// Predicate Map against its label/URI
		Map<String, KgPred> predMap = new HashMap<>();
		Map<String, List<KgRule>> predRuleMap = new HashMap<>();
		Map<String, Map<String, List<KgRulePropvalBucket>>> ruleBucket = new HashMap<>();
		int insCount = 0;
		// triplePattern - Save the triple pattern and fetch id
		TriplePatternMap triplePatternMap = new TriplePatternMap(triplePattern[0], triplePattern[1], triplePattern[2]);
		// predFreq - Create a map of Pred objects against their labels
		KgPred tempPred;
		Integer tempFreq;
		Map<String, Integer> propFreqMap;
		Map<String, Set<String>> predPropLabelsMap = new HashMap<>();
		String tempWord;
		KgRule tempRule;
		Set<String> propLabels;
		// Insert preds one by one to map
		for (String predLabel : predRuleFreqMap.keySet()) {
			tempFreq = predFreq.get(predLabel);
			tempPred = new KgPred(predLabel, predLabel, tempFreq);
			predMap.put(predLabel, tempPred);

			propFreqMap = predRuleFreqMap.get(predLabel);
			for (String propVal : propFreqMap.keySet()) {
				tempWord = fetchPropLabel(propVal);
				tempFreq = propFreqMap.get(propVal);
				propLabels = predPropLabelsMap.get(predLabel);
				// Create Rule obj
				if (predRuleMap.containsKey(predLabel) && checkEntry(tempWord, propLabels)) {
					// in case of similar looking propval just add to the frequency to current rule
					// and create a map of list of rulebuckets against a rule
					System.out.println(predLabel + "\t" + propVal + "\t" + predRuleMap);
					tempRule = fetchRule(predLabel, propVal, predRuleMap);
					tempRule.setRlPropfreq(tempRule.getRlPropfreq() + tempFreq);
					// Put current information in a rule bucket
					// Map rule_buckets to rule
					setRuleToBucket(predLabel, propVal, tempRule, ruleBucket);
				} else {
					// put the word in set
					propLabels = new HashSet<>();
					propLabels.add(tempWord);
					predPropLabelsMap.put(predLabel, propLabels);
					// Create rule objects and map them to pred label
					tempRule = new KgRule(propVal, tempFreq);
					setRuleToMap(predLabel, tempRule, predRuleMap);
				}
			}
		}

		// Save all the objects and fetch the ids
		// Save triplePatternMap
		// Save predMap
		// Save predRuleMap
		// Save ruleBucket

		KgPredMap kgPredMap;
		KgRuleMap kgRuleMap;
		session.save(triplePatternMap);
		List<KgRule> tempRuleList;
		for (String pred : predMap.keySet()) {
			// Save the pred
			tempPred = predMap.get(pred);
			session.save(tempPred);
			tempRuleList = predRuleMap.get(pred);
			if (tempRuleList == null)
				continue;
			for (KgRule ruleObj : tempRuleList) {
				System.out.println("Saving Rule:\t" + ruleObj.getRlPropval());
				// save the rule
				session.save(ruleObj);
				// Map rules to the triple
				kgRuleMap = new KgRuleMap(ruleObj, triplePatternMap);
				// Map rules to the predicate
				kgPredMap = new KgPredMap(tempPred, ruleObj);
				// Save rules to triple map
				session.save(kgRuleMap);
				// Save rules to predicate map
				session.save(kgPredMap);
				Map<String, List<KgRulePropvalBucket>> valBucketMap = ruleBucket.get(pred);
				List<KgRulePropvalBucket> tempBucketList = valBucketMap != null
						? valBucketMap.get(ruleObj.getRlPropval())
						: null;
				if (tempBucketList == null)
					continue;
				for (KgRulePropvalBucket bucket : ruleBucket.get(pred).get(ruleObj.getRlPropval())) {
					// save the bucket
					session.save(bucket);
				}
			}
		}

		session.getTransaction().commit();
		session.close();
		return insCount;

	}

	/**
	 * Method to set the repeating rule to its bucket
	 * 
	 * @param pred
	 *            - predicate of that rule
	 * @param propVal
	 *            - property value of the rule
	 * @param kgRule
	 *            - object of the rule itself
	 * @param ruleBucketMap
	 *            - map to store the bucket for future retrieval
	 */
	public static void setRuleToBucket(String pred, String propVal, KgRule kgRule,
			Map<String, Map<String, List<KgRulePropvalBucket>>> ruleBucketMap) {
		KgRulePropvalBucket kgRulePropvalBucket = new KgRulePropvalBucket(kgRule, propVal);
		Map<String, List<KgRulePropvalBucket>> bucketMap = ruleBucketMap.get(pred);
		if (bucketMap == null) {
			bucketMap = new HashMap<>();
			ruleBucketMap.put(pred, bucketMap);
			bucketMap.put(kgRule.getRlPropval(), new ArrayList<>());
		}

		List<KgRulePropvalBucket> ruleList = bucketMap.get(kgRule.getRlPropval());
		if (ruleList == null) {
			ruleList = new ArrayList<>();
			bucketMap.put(kgRule.getRlPropval(), ruleList);
		}

		// Add the rule to list
		ruleList.add(kgRulePropvalBucket);

	}

	/**
	 * Map a particular rule to its predicate in a given map structure
	 * 
	 * @param pred
	 *            - predicate of the rule
	 * @param kgRule
	 *            - rule object to be mapped
	 * @param predRuleMap
	 *            - map data structure to store the rule in
	 */
	public static void setRuleToMap(String pred, KgRule kgRule, Map<String, List<KgRule>> predRuleMap) {
		List<KgRule> ruleList = predRuleMap.get(pred);
		if (ruleList == null) {
			// Create new list
			ruleList = new ArrayList<>();
			// put the list in map
			predRuleMap.put(pred, ruleList);
		}

		// Add the rule to list
		ruleList.add(kgRule);

	}

	/**
	 * Method to fetch a particular rule from a give map based on its predicate and
	 * propVal
	 * 
	 * @param pred
	 *            - predicate of the rule
	 * @param propVal
	 *            - propVal of the rule
	 * @param predRuleMap
	 *            - map data structure to fetch from
	 * @return - Rule object that is retrieved from the map
	 */
	public static KgRule fetchRule(String pred, String propVal, Map<String, List<KgRule>> predRuleMap) {
		KgRule rule = null;
		for (KgRule entry : predRuleMap.get(pred)) {
			if (fetchPropLabel(entry.getRlPropval()).equalsIgnoreCase(fetchPropLabel(propVal)))
				return entry;
		}
		return rule;
	}

	/**
	 * Method to check for a particular String in a set of String
	 * 
	 * @param word
	 *            - entry to be searched for
	 * @param wordSet
	 *            - set of entries
	 * @return - true if found otherwise false
	 */
	public static boolean checkEntry(String word, Set<String> wordSet) {
		if (wordSet != null) {
			for (String entry : wordSet) {
				if (entry.equalsIgnoreCase(word))
					return true;
			}
		}
		return false;
	}

	/**
	 * Method to dissect a resource URI to fetch the last element
	 * 
	 * @param propVal
	 *            - resource URI
	 * @return - last element of the URI
	 */
	public static String fetchPropLabel(String propVal) {
		if(propVal.matches(Constants.URI_REGEX))
		{
			String[] words = propVal.split("[\\/]");
			return words[words.length - 1];
		}
		else
			return propVal;
	}

	/**
	 * Method to calculate the significance values throughout the rules and insert
	 * them into KgRuleSigvalMap
	 * 
	 * @return count of records inserted
	 */
	public static int calculateSigVals() {
		Session session = HibernateUtils.getSessionFactory().openSession();
		session.beginTransaction();
		// truncate the table
		Query query = session.createQuery("delete from KgRuleSigvalMap");
		query.executeUpdate();
		// insert the records
		StringBuilder insQuery = new StringBuilder();
		insQuery.append(" insert into kg_rule_sigval_map (rsm_rl_id, rsm_sigval) ");
		insQuery.append(" SELECT r1.rl_id, CAST(r1.rl_propfreq / (SELECT ");
		insQuery.append(" SUM(r2.rl_propfreq) FROM kg_rule r2, kg_rule_map rm2, triple_pattern_map tpm2 ");
		insQuery.append(" WHERE r2.rl_id = rm2.rm_rl_id AND rm2.rm_tpm_id = tpm2.tpm_id ");
		insQuery.append(" AND tpm1.TPM_ID = tpm2.TPM_ID) AS DECIMAL (10 , 8 )) AS sigVal ");
		insQuery.append(" FROM kg_rule r1, kg_rule_map rm1, triple_pattern_map tpm1 ");
		insQuery.append(" WHERE r1.rl_id = rm1.rm_rl_id AND rm1.rm_tpm_id = tpm1.tpm_id; ");
		query = session.createSQLQuery(insQuery.toString());
		int insCount = query.executeUpdate();
		session.getTransaction().commit();
		session.close();
		return insCount;
	}

}
