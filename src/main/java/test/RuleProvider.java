package test;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

public class RuleProvider {
	/**
	 * Query1 select pr.pr_label, rl.rl_propval, rsm.RSM_SIGVAL from kg_pred pr,
	 * kg_rule rl, kg_rule_sigval_map rsm where rl.rl_id in (select rlm.rm_rl_id
	 * from kg_rule_map rlm where rlm.rm_tpm_id in (select tpat.TPM_ID from
	 * triple_pattern_map tpat where tpat.TPM_OBJ = ' ' and tpat.TPM_PRED = ' ' and
	 * tpat.TPM_SUBJ=' ')) and pr.pr_id in (select prm.pm_pr_id from kg_pred_map prm
	 * where prm.pm_rl_id = rl.rl_id) and rsm.RSM_RL_ID = rl.rl_id;
	 * 
	 * Query2 select pr.pr_label, rpb.RPB_PROPVAL, rsm.RSM_SIGVAL from kg_pred pr,
	 * kg_rule_propval_bucket rpb, kg_rule rl, kg_rule_sigval_map rsm where rl.rl_id
	 * in (select rlm.rm_rl_id from kg_rule_map rlm where rlm.rm_tpm_id in (select
	 * tpat.TPM_ID from triple_pattern_map tpat where tpat.TPM_OBJ = ' ' and
	 * tpat.TPM_PRED = ' ' and tpat.TPM_SUBJ=' ')) and pr.pr_id in (select
	 * prm.pm_pr_id from kg_pred_map prm where prm.pm_rl_id = rl.rl_id) and
	 * rpb.RPB_RL_ID = rl.rl_id and rsm.RSM_RL_ID = rl.rl_id;
	 * 
	 * 
	 */

	public static StringBuilder query1;
	public static StringBuilder query2;
	static {
		query1 = new StringBuilder();
		query1.append(" select pr.pr_label, rl.rl_propval, rsm.RSM_SIGVAL from kg_pred pr, kg_rule");
		query1.append(" rl, kg_rule_sigval_map rsm where rl.rl_id in (select rlm.rm_rl_id from");
		query1.append(" kg_rule_map rlm where rlm.rm_tpm_id in (select tpat.TPM_ID from");
		query1.append(" triple_pattern_map tpat where tpat.TPM_OBJ = :obj and tpat.TPM_PRED = :pred and");
		query1.append(" tpat.TPM_SUBJ= :subj)) and pr.pr_id in (select prm.pm_pr_id from kg_pred_map prm");
		query1.append(" where prm.pm_rl_id = rl.rl_id) and rsm.RSM_RL_ID = rl.rl_id;");

		query2 = new StringBuilder();
		query2.append(" select pr.pr_label, rpb.RPB_PROPVAL, rsm.RSM_SIGVAL from kg_pred pr,");
		query2.append(" kg_rule_propval_bucket rpb, kg_rule rl, kg_rule_sigval_map rsm where rl.rl_id");
		query2.append(" in (select rlm.rm_rl_id from kg_rule_map rlm where rlm.rm_tpm_id in (select");
		query2.append(" tpat.TPM_ID from triple_pattern_map tpat where tpat.TPM_OBJ = :obj and");
		query2.append(" tpat.TPM_PRED = :pred and tpat.TPM_SUBJ= :subj)) and pr.pr_id in (select");
		query2.append(" prm.pm_pr_id from kg_pred_map prm where prm.pm_rl_id = rl.rl_id) and");
		query2.append(" rpb.RPB_RL_ID = rl.rl_id and rsm.RSM_RL_ID = rl.rl_id;");

	}

	// Method to run the query1 and query2 for a particular pattern and resource URI
	public static List<TestRule> fetchRuleData(String[] params, Session session) {
		List<TestRule> testRules  = new ArrayList<>();
		List<Object[]> dbRules = new ArrayList<>();
		dbRules.addAll(executeQuery(query1.toString(), params, session));
		dbRules.addAll(executeQuery(query2.toString(), params, session));
		TestRule tempRule;
		for(Object[] entry : dbRules) {
			tempRule = new TestRule(params[0], entry[0].toString(), entry[1].toString(), Double.parseDouble(entry[2].toString()));
			testRules.add(tempRule);
		}
		return testRules;
	}

	@SuppressWarnings("unchecked")
	public static List<Object[]> executeQuery(String queryStr, String[] params, Session session) {
		List<Object[]> res = null;
		Query query = session.createSQLQuery(queryStr).
				setParameter("subj", params[0]).
				setParameter("pred", params[0]).
				setParameter("obj", params[0]);
		res = query.list();
		return res;
	}

}
