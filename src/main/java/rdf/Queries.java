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
    public static final String CHECK_RESOURCE_AVAILABILITY = "SELECT ?o WHERE { " +
            " %s %s ?o " +
            "}";

    public static final String RULE_1 = "" +
            "SELECT distinct ?p ?freq WHERE {\n" +
            "    {SELECT distinct ?p (count(?o) as ?freq) WHERE { \n" +
            "        ?s ?p ?o .\n" +
            "        FILTER (?s = ?sub) {\n" +
            "            SELECT ?sub WHERE { ?sub %s %s }\n" +
            "        } .\n" +
            "    } group by ?p }\n" +
            "} \n" +
            "group by ?p ?freq\n" +
            "order by desc(?freq)";

    public static final String RULE_1_GRANULAR = "" +
            "SELECT ?o (count(?o) as ?freq) WHERE { \n" +
            "        ?s ?p ?o .\n" +
            "        FILTER (?s = ?sub) {\n" +
            "            SELECT ?sub WHERE { ?sub %s %s }\n" +
            "        } .\n" +
            "        FILTER (?p = %s) .\n" +
            "        FILTER (strlen(?o) <= 500) .\n" +
            "    }\n" +
            "group by ?o\n" +
            "order by desc(?freq)";

    public static final String RULE_2 = "" +
            "SELECT distinct ?p ?freq WHERE {\n" +
            "    {SELECT distinct ?p (count(?s) as ?freq) WHERE { \n" +
            "        ?s ?p ?o .\n" +
            "        FILTER (?s = ?obj) {\n" +
            "            SELECT ?obj WHERE { %s %s ?obj }\n" +
            "        } .\n" +
            "    } group by ?p }\n" +
            "} \n" +
            "group by ?p ?freq\n" +
            "order by desc(?freq)\n" +
            "limit 100";

    public static final String RULE_2_GRANULAR = "" +
            "SELECT ?o (count(?o) as ?freq) WHERE {\n" +
            "?subj %s ?o .\n" +
            "filter (?subj = ?obj) {\n" +
            "SELECT ?obj WHERE { \n" +
            "        ?s %s ?obj .\n" +
            "        FILTER (?s = ?sub) {\n" +
            "            SELECT ?sub WHERE { ?sub %s %s }\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "  FILTER (strlen(?o) <= 500) .\n" +
            "} group by ?o\n" +
            "order by desc(?freq)\n" +
            "limit 100";

    public static final String RULE_3 = "" +
            "SELECT distinct ?p ?freq WHERE {\n" +
            "    {SELECT distinct ?p (count(?o) as ?freq) WHERE { \n" +
            "        ?s ?p ?o .\n" +
            "        FILTER (?s = ?sub) {\n" +
            "            SELECT ?sub WHERE { ?sub %s ?obj }\n" +
            "        } .\n" +
            "    } group by ?p }\n" +
            "} \n" +
            "group by ?p ?freq\n" +
            "order by desc(?freq)\n";

    public static final String RULE_3_GRANULAR = "" +
            "SELECT ?o (count(?o) as ?freq) WHERE { \n" +
            "        ?s ?p ?o .\n" +
            "        FILTER (?s = ?sub) {\n" +
            "            SELECT ?sub WHERE { ?sub %s ?obj }\n" +
            "        } .\n" +
            "        FILTER (?p = %s)\n" +
            "        FILTER (strlen(?o) <= 500) .\n" +
            "    }\n" +
            "group by ?o\n" +
            "order by desc(?freq)\n" +
            "limit 100";

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

    public static final String GET_RANKED_PROPERTY_VALUES_OF_SUBJECT = "" +
            "SELECT ?o (COUNT(?o) AS ?freq) WHERE { \n" +
            "        ?s ?p ?o .\n" +
            "        FILTER (?s = ?sub) {\n" +
            "            SELECT ?sub WHERE { ?sub %s %s }\n" +
            "        } .\n" +
            "        FILTER (?p = %s)\n" +
            "    }\n" +
            "GROUP BY ?o\n" +
            "ORDER BY DESC(?freq)";

    /**
     *
     * @param predicateUri for rule 1
     * @param objectUri for rule 2
     * @return query string needed for rule 1
     */
    public static String getRule1(String predicateUri, String objectUri) {
        return String.format(RULE_1, predicateUri, objectUri);
    }

    /**
     *
     * @param predicateUri predicate uri
     * @param objectUri object uri
     * @param propertyUri to get value of and get freq
     * @return query string needed for rule 1
     */
    public static String getRule1Granular(String predicateUri, String objectUri, String propertyUri) {
        return String.format(RULE_1_GRANULAR, predicateUri, objectUri, propertyUri);
    }

    /**
     *
     * @param predicateUri for rule 2
     * @param subjectUri for rule 2
     * @return query string needed for rule 2
     */
    public static String getRule2(String subjectUri, String predicateUri) {
        return String.format(RULE_2, subjectUri, predicateUri);
    }

    /**
     *
     * @param predicateUri predicate uri
     * @param objectUri object uri
     * @param propertyUri to get value of and get freq
     * @return query string needed for rule 2
     */
    public static String getRule2Granular(String propertyUri, String predicateUri, String objectUri) {
        return String.format(RULE_2_GRANULAR, propertyUri, predicateUri, predicateUri, objectUri);
    }

    /**
     *
     * @param predicateUri predicate uri
     * @return query for rule 3
     */
    public static String getRule3(String predicateUri) {
        return String.format(RULE_3, predicateUri);
    }

    /**
     *
     * @param predicateUri predicate uri
     * @param propertyUri to get value of and get freq
     * @return query string needed for rule 3
     */
    public static String getRule3Granular(String predicateUri, String propertyUri) {
        return String.format(RULE_3_GRANULAR, predicateUri, propertyUri);
    }

    /**
     *
     * @param subjectUri subject uri
     * @param predicateUri predicaate uri
     * @return query if entries exist between provided subject and object
     */
    public static String getQueryCheckResourceAvailability(String subjectUri, String predicateUri) {
        return String.format(Queries.CHECK_RESOURCE_AVAILABILITY, subjectUri, predicateUri);
    }

    /*public static String getQueryRankedPropertiesHiddenSubject(String predicateUri, String objectUri, String subjectUri) {
        return String.format(Queries.GET_RANKED_PROPERTIES_HIDDEN_SUBJECT,
                predicateUri,
                objectUri,
                subjectUri,
                subjectUri);
    }

    public static String getQueryRankedPropertiesHiddenObject(String subjectUri, String predicateUri, String objectUri) {
        return String.format(Queries.GET_RANKED_PROPERTIES_HIDDEN_OBJECT,
                subjectUri,
                predicateUri,
                objectUri,
                objectUri);
    }

    public static String getQueryRankedObjectHiddenProperties(String subjectUri, String objectUri, String predicateUri) {
        return String.format(Queries.GET_RANKED_OBJECTS_HIDDEN_PROPERTIES,
                subjectUri,
                objectUri,
                predicateUri,
                predicateUri,
                subjectUri,
                predicateUri);
    }

    public static String getQueryRankedPropertyValues(String predicateUri, String objectUri, String propertyUri) {
        return String.format(Queries.GET_RANKED_PROPERTY_VALUES_OF_SUBJECT,
                predicateUri,
                objectUri,
                propertyUri);
    }*/

    /**
     * provided query execute it and get value of specific column
     * only works if needed column count is 1
     * @param queryString query
     * @param column required col
     * @return list of results
     */
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

    /**
     * provided query execute it and get value of specific column
     * only works if needed column count is 1 ALONG WITH ?freq
     * @param queryString query
     * @param column required col
     * @return Map of results along with freq
     */
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
