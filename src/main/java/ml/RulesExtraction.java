package ml;

import rdf.Queries;

import java.util.Map;

public class RulesExtraction {
    public static final String QUERY_VAR_SUBJECT = "s";
    public static final String QUERY_VAR_OBJECT = "o";
    public static final String QUERY_VAR_PREDICATE = "p";

    public static String getQueryRankedPropertiesHiddenSubject(String predicateUri, String objectUri, String subjectUri) {
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

    public static void main(String[] args) {
        /*String query = getQueryRankedPropertiesHiddenSubject("<http://dbpedia.org/ontology/award>",
                "<http://dbpedia.org/resource/Nobel_Prize_in_Physics>",
                "<http://dbpedia.org/resource/Albert_Einstein>");
        Map<String, Integer> propertyFreqMap = Queries.execFreq(query, QUERY_VAR_PREDICATE);*/

        /*String query = getQueryRankedPropertiesHiddenObject("<http://dbpedia.org/resource/Albert_Einstein>",
                "<http://dbpedia.org/ontology/award>",
                "<http://dbpedia.org/resource/Nobel_Prize_in_Physics>");
        Map<String, Integer> propertyFreqMap = Queries.execFreq(query, QUERY_VAR_PREDICATE);*/

        String query = getQueryRankedObjectHiddenProperties("<http://dbpedia.org/resource/Albert_Einstein>",
                "<http://dbpedia.org/resource/Nobel_Prize_in_Physics>",
                "<http://dbpedia.org/ontology/award>");
        Map<String, Integer> propertyFreqMap = Queries.execFreq(query, QUERY_VAR_OBJECT);
    }
}
