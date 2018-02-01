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
	//Method to accept a testRule object and return count of records fetched
	public static double getConfidenceValue(List<TestRule> rules) {
		double cfVal = 0;
		cfVal = executeResultQuery(generateQuery(rules));
		return cfVal;
	}
	//Query Generator
	public static String generateQuery(List<TestRule> rules) {
		StringBuilder queryStr = new StringBuilder();
		Map<Integer, TestRule> indexMap = getIndexMap(rules);
		StringBuilder calcStat = new StringBuilder();
		StringBuilder selStat = new StringBuilder();
		StringBuilder bindStat = new StringBuilder();
		TestRule tempRule;
		for(int i=1;i<=rules.size();i++) {
			tempRule = indexMap.get(i);
			//calc part formation
			calcStat.append(tempRule.getCalcPhrase(i));
			calcStat.append(" ");
			if(i!=rules.size()) {
				calcStat.append("+");
			}
			//select statement formation
			selStat.append(" { ").append(tempRule.getSelQuery(i)).append(" } ");
			//bind statement formation
			bindStat.append(tempRule.getBindPhrase(i));
		}
		
		queryStr.append(" SELECT ").append(calcStat).append(" as ?cf { ").append(selStat).append(bindStat).append(" } ");
		return queryStr.toString();
	}
	//index map generator
	public static Map<Integer, TestRule> getIndexMap(List<TestRule> rules){
		Map<Integer, TestRule> indexMap = new HashMap<>();
		int i=1;
		for(TestRule entry : rules) {
			indexMap.put(i++, entry);
		}
		return indexMap;
	}
	//Query Executor
	public static double executeResultQuery(String queryStr) {
		double res = 0;
		Query query = QueryFactory.create(queryStr);
		// Remote execution.
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)) {
			// Set the DBpedia specific timeout.
			((QueryEngineHTTP) qexec).addParam("timeout", "10000");

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
