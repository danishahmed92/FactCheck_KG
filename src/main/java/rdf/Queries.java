package rdf;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import utils.Constants;

import java.util.*;

public class Queries {
    public static final String ALL_PREDICATES_OF_SUBJECT = "SELECT DISTINCT ?predicate WHERE { " +
            " %s ?predicate [] " +
            "}";
    public static final String PREDICATE_OBJECT_FIXED = "SELECT DISTINCT ?subject WHERE { " +
            " ?subject  %s %s " +
            "}";

    public static final String GET_RANKED_PROPERTIES_HIDDEN_SUBJECT = "" +
            "SELECT ?p ?freq WHERE {\n" +
            "    {SELECT ?s ?p (COUNT(?o) AS ?freq) WHERE { \n" +
            "        ?s ?p ?o .\n" +
            "        FILTER (?s = ?sub) {\n" +
            "            SELECT ?sub WHERE { ?sub %s %s }\n" +
            "        } .\n" +
            "        FILTER (?p = ?pred && ?o = ?obj) {\n" +
            "            SELECT ?pred ?obj WHERE { %s ?pred ?obj }\n" +
            "        }\n" +
            "    }\n" +
            "    GROUP BY ?s ?p}\n" +
            "    FILTER (?s = %s)\n" +
            "} \n" +
            "GROUP BY ?p ?freq\n" +
            "ORDER BY DESC(?freq)";

    public static final String GET_RANKED_PROPERTIES_HIDDEN_OBJECT = "" +
            "SELECT ?p ?freq WHERE {\n" +
            "    {SELECT ?o ?p (COUNT(?s) AS ?freq) WHERE { \n" +
            "        ?s ?p ?o .\n" +
            "        FILTER (?o = ?obj) {\n" +
            "            SELECT ?obj WHERE { %s %s ?obj }\n" +
            "        } .\n" +
            "        FILTER (?p = ?pred && ?s = ?sub) {\n" +
            "            SELECT ?sub ?pred WHERE { ?sub ?pred %s }\n" +
            "        }\n" +
            "    }\n" +
            "    GROUP BY ?p ?o}\n" +
            "    FILTER (?o = %s)\n" +
            "}\n" +
            "GROUP BY ?p ?freq\n" +
            "ORDER BY DESC(?freq)";

    public static final String GET_RANKED_OBJECTS_HIDDEN_PROPERTIES = "" +
            "SELECT ?o ?freq WHERE {\n" +
            "    {SELECT ?o ?p (COUNT(?s) AS ?freq) WHERE { \n" +
            "        ?s ?p ?o .\n" +
            "        FILTER (?p = ?pred) {\n" +
            "            SELECT ?pred WHERE { %s ?pred %s }\n" +
            "        } .\n" +
            "        FILTER (?o = ?obj && ?s = ?sub) {\n" +
            "            SELECT ?sub ?obj WHERE { ?sub %s ?obj }\n" +
            "        }\n" +
            "    }\n" +
            "    GROUP BY ?o ?p}\n" +
            "    FILTER (?p = %s)\n" +
            "    FILTER (?o = ?ob) {\n" +
            "        SELECT ?ob WHERE { %s %s ?ob }\n" +
            "    }\n" +
            "}\n" +
            "GROUP BY ?o ?freq\n" +
            "ORDER BY DESC(?freq)";


    public static List<String> execute(String queryString, String column) {
        Query query = QueryFactory.create(queryString);
        List<String> result = new ArrayList<>();

        // Remote execution.
        try (QueryExecution qExec = QueryExecutionFactory.sparqlService(Constants.SPARQL_END_POINT, query)) {
            // Set the DBpedia specific timeout.
            ((QueryEngineHTTP) qExec).addParam("timeout", "10000");

            ResultSet rs = qExec.execSelect();
            while (rs.hasNext()) {
                QuerySolution soln = rs.nextSolution();

                RDFNode predicate = soln.get(column);
                result.add(predicate.toString());
            }
            qExec.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Map<String, Integer> execFreq(String queryString, String column) {
        Query query = QueryFactory.create(queryString);
        Map<String, Integer> result = new LinkedHashMap<>();

        // Remote execution.
        try (QueryExecution qExec = QueryExecutionFactory.sparqlService(Constants.SPARQL_END_POINT, query)) {
            ((QueryEngineHTTP) qExec).addParam("timeout", "10000");

            ResultSet rs = qExec.execSelect();
//            ResultSetFormatter.out(System.out, rs, query);
            while (rs.hasNext()) {
                QuerySolution soln = rs.nextSolution();

                String colValue = soln.get(column).toString();
                Integer freq = soln.getLiteral("freq").getInt();
                result.put(colValue, freq);
            }
            qExec.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
