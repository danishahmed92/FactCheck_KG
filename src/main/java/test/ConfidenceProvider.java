package test;

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

public class ConfidenceProvider {
	public static int ruleLim = 120;

	// Method to accept a testRule object and return count of records fetched
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
				.append(" ").append(" BIND( ").append(calcStat).append(" as ?cf1) . BIND(str(?cf1) as ?cf) .").append(" } LIMIT 1");
		return queryStr.toString();
	}

	// index map generator
	public static Map<Integer, TestRule> getIndexMap(List<TestRule> rules) {
		Map<Integer, TestRule> indexMap = new HashMap<>();
		int i = 1;
		for (TestRule entry : rules) {
			indexMap.put(i++, entry);
		}
		return indexMap;
	}

	// Query Executor
	public static double executeResultQuery(String queryStr) {
		double res = 0;
		System.out.println(queryStr);
		Query query = QueryFactory.create(queryStr);
		// Remote execution.
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)) {
			// Set the DBpedia specific timeout.
			((QueryEngineHTTP) qexec).addParam("timeout", "30000");

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
		}
		return res;
	}

}
