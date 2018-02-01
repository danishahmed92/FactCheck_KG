package ml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

import config.Config;
import config.Database;
import context.Similarity;
import context.WordNet;
import rdf.FactCheckResource;
import rdf.Queries;
import rdf.TripleExtractor;
import rita.wordnet.jwnl.JWNLException;
import utils.Util;

import javax.persistence.criteria.CriteriaBuilder;

/*
 * Step 1:
 * read rdf file and extract triples
 * Get labels of subject and object
 *
 * Step 2:
 * Get filtered alternative labels (only those that are close enough to actual labels)
 *
 * step 3 (RULE #1):
 * get other properties from same subject that closely matches with object according to semantics
 * select only the property with maximum score
 *
 * Step 4 (RULE #2):
 * get all the properties in ranked order comparing with all the subjects that share same object
 * (RULE #2.1)
 * Those properties that are generic and have high rank,
 * get values count of those properties based on subject, and is shared by other subjects having same object
 *
 * Step 5 (RULE #3):
 * get all properties in ranked order that are relevant to object,
 * but is shared by all subjects also sharing same object
 *
 * Step 6 (RULE #4)
 * get all objects in ranked order that are relevant to subject,
 * and is also shared by other subjects having same property
 *
 * Step 7:
 * Compute confidence value for each extracted data value
 *
 * Step 8:
 * Store result retrieved (according to threshold) in database
 *
 *
 *
 * */
enum RuleNumber {
    RULE_1(1), RULE_2(2), RULE_3(3);
    private int value;
    private RuleNumber(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}

public class RulesExtraction {
    public static final String QUERY_VAR_SUBJECT = "s";
    public static final String QUERY_VAR_OBJECT = "o";
    public static final String QUERY_VAR_PREDICATE = "p";

    public static Boolean saveEntryToDB = true;
    public static Connection conn = Database.databaseInstance.conn;

    public static Map<String, Map<String, Map<String, Integer>>> queryCache = new LinkedHashMap<>();
    public static String currentQuery = "";

    public static void main(String[] args) {
        try {
            filesCrawler(Paths.get(Config.configInstance.trainDataPath + "/correct/award"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void filesCrawler(final Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    System.out.println(file.getFileName().toString());
                    try {
                        saveEntryToDB = true;
                        ExtractedFeatures extractedFeatures = extractRulesForRDFFile(Config.configInstance.trainDataPath + "/correct/award/" + file.getFileName().toString());

                        TimeUnit.SECONDS.sleep(1);
                        if (saveEntryToDB)
                            Database.saveExtractedFeaturesObjToDB(extractedFeatures, conn, "award", file.getFileName().toString());
                    } catch (IOException ignore) {
                        // don't index files that can't be read.
                    } catch (JWNLException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            System.out.println(path.getFileName().toString());
            try {
                ExtractedFeatures extractedFeatures = extractRulesForRDFFile(Config.configInstance.trainDataPath + "/correct/award/" + path.getFileName().toString());
                if (saveEntryToDB)
                    Database.saveExtractedFeaturesObjToDB(extractedFeatures, conn, "award", path.getFileName().toString());
            } catch (JWNLException e) {
                e.printStackTrace();
            }
        }
    }

    public static ExtractedFeatures extractRulesForRDFFile(String fileName) throws IOException, JWNLException {
        /* Step 1 */
        TripleExtractor tripleExtractor = getTriples(fileName);
        FactCheckResource subject = tripleExtractor.subject;
        Property predicate = tripleExtractor.predicate;
        FactCheckResource object = tripleExtractor.object;

        String subjectUri = String.format("<%s>", FactCheckResource.getDBpediaUri(subject));
        String predicateUri = String.format("<%s>", predicate.getURI());
        String objectUri = String.format("<%s>", FactCheckResource.getDBpediaUri(object));

        /* Step 1, 2 */
        ExtractedFeatures extractedFeatures = new ExtractedFeatures(subject, predicate, object);

        /* Step 3 (RULE #0) */
        Map<String, Double> semanticSubjectPropertiesMap = rule0SemanticSubjectProperties(subject, object, predicate);
        /* if provided resource has properties in sparql */
        if (semanticSubjectPropertiesMap.size() != 0) {
            extractedFeatures.setSemanticSubjectProperty(semanticSubjectPropertiesMap.keySet().toArray()[semanticSubjectPropertiesMap.size() - 1].toString(),
                    Double.parseDouble(semanticSubjectPropertiesMap.values().toArray()[semanticSubjectPropertiesMap.size() - 1].toString()));

            List<String> resourceAvailability = checkResourceAvailability(subjectUri, predicateUri);
            if (resourceAvailability.size() != 0) {
                /* Step 4 (RULE #1)*/

                Map<String, Integer> subjectsPropertiesMap = rule1SubjectsPropertiesIntersection(predicateUri, objectUri);
                if (!queryCache.containsKey(currentQuery)) {
                    extractedFeatures.setRule1SubjectsPropertiesMap(subjectsPropertiesMap);
                    Map<String, Map<String, Integer>> subjectsPropertiesValuesMap = extractPropertyValues(RuleNumber.RULE_1, subjectsPropertiesMap, predicateUri, objectUri);
                    queryCache.put(currentQuery, subjectsPropertiesValuesMap);
                    extractedFeatures.setRule1PropertiesValuesMap(subjectsPropertiesValuesMap);
                } else {
                    extractedFeatures.setRule1PropertiesValuesMap(queryCache.get(currentQuery));
                }

                /* Step 5 (RULE #2)*/
                Map<String, Integer> objectsPropertiesMap = rule2ObjectsPropertiesIntersection(subjectUri, predicateUri);
                if (!queryCache.containsKey(currentQuery)) {
                    extractedFeatures.setRule2ObjectsPropertiesMap(objectsPropertiesMap);
                    Map<String, Map<String, Integer>> objectsPropertiesValuesMap = extractPropertyValues(RuleNumber.RULE_2, subjectsPropertiesMap, predicateUri, objectUri);
                    queryCache.put(currentQuery, objectsPropertiesValuesMap);
                    extractedFeatures.setRule2PropertiesValuesMap(objectsPropertiesValuesMap);
                } else {
                    extractedFeatures.setRule2PropertiesValuesMap(queryCache.get(currentQuery));
                }

                /*Map<String, Integer> propertiesRankedMap = rule3PropertiesRanked(predicateUri);
                if (!queryCache.containsKey(currentQuery)) {
                    extractedFeatures.setRule3PropertiesRankedMap(propertiesRankedMap);
                    Map<String, Map<String, Integer>> propertiesValuesMap = extractPropertyValues(RuleNumber.RULE_3, subjectsPropertiesMap, predicateUri, objectUri);
                    queryCache.put(currentQuery, propertiesValuesMap);
                    extractedFeatures.setRule3PropertiesValuesMap(propertiesValuesMap);
                } else {
                    extractedFeatures.setRule3PropertiesValuesMap(queryCache.get(currentQuery));
                }*/

                /* Step 6 (RULE #4)*/
                Map<String, Integer> objOfAllSubjSamePropertyMap = rule4RankedObjOfAllSubjSameProperty(subjectUri, predicateUri, objectUri);
                extractedFeatures.setObjOfAllSubjSamePropertyMap(objOfAllSubjSamePropertyMap);

                System.out.println();
                return extractedFeatures;
            } else {
                saveEntryToDB = false;
                System.out.println("Resource unavailable w.r.t object in Dbpedia sparql: " + subjectUri + ",\t" + predicateUri);
                System.out.println();
            }
        } else {
            saveEntryToDB = false;
            System.out.println("Resource unavailable in Dbpedia sparql: " + subjectUri);
            System.out.println();
        }
        return null;
    }

    public static Map<String, Double> rule0SemanticSubjectProperties(FactCheckResource subject, FactCheckResource object, Property predicate) throws JWNLException, IOException {
        String objectLabel = object.langLabelsMap.get("en");
        String predicateLabel = predicate.getLocalName();
        String subjectLabel = subject.langLabelsMap.get("en");

        /* Get best synonyms for property (predicate) label */
        List<String> predicateSynonyms = new ArrayList<>();
        predicateSynonyms = WordNet.getNTopSynonyms(predicateLabel, 5);
        Map<String, Double> synonymsWeight = new HashMap<>();
        for (String syn : predicateSynonyms) {
            double totalWeight = Similarity.getSemanticSimilarity(objectLabel, syn, predicateLabel);
            synonymsWeight.put(syn, totalWeight);
        }
        String query = String.format(Queries.ALL_PREDICATES_OF_SUBJECT, "<" + FactCheckResource.getDBpediaUri(subject) + ">");
        List<String> results = Queries.execute(query, "predicate");
        System.out.println(results);
        Map<String, Double> propertySimilarityMap = new HashMap<String, Double>();
        for (String property : results) {
            int index = property.lastIndexOf('/') + 1;
            property = property.substring(index, property.length());
            property = property.replaceAll("\\d+", "").replaceAll("(.)([A-Z])", "$1 $2");
            if (property.contains("#") || property.equals(predicateLabel))
                continue;

            double propertySynonymScore = 0;
            double propertyPredicateScore = (Similarity.getSemanticSimilarity(objectLabel, property, predicateLabel)
                    + Similarity.getSemanticSimilarity(subjectLabel, property, predicateLabel)) / 2;

            for (String synonym : predicateSynonyms) {
                propertySynonymScore = propertySynonymScore
                        + ((Similarity.getSemanticSimilarity(objectLabel, property, synonym)
                            + Similarity.getSemanticSimilarity(subjectLabel, property, synonym)) / 2) * synonymsWeight.get(synonym);
            }
            double similarityScore = (propertyPredicateScore + (propertySynonymScore / predicateSynonyms.size())) / 2;

            if (similarityScore == 0 || Double.isNaN(similarityScore))
                continue;
            propertySimilarityMap.put(property, similarityScore);
        }
        return Util.sortMapByValue(propertySimilarityMap);
    }

    public static Map<String, Map<String, Integer>> extractPropertyValues(RuleNumber ruleNumber, Map<String, Integer> propertyFreqMap, String predicateUri, String objectUri) {
        Object[] propertyArray = propertyFreqMap.keySet().toArray();

        int threshold = (int) Math.round(Math.sqrt(propertyFreqMap.size()));
        Map<String, Map<String, Integer>> propertiesValuesRankedMap = new LinkedHashMap<>();

        for (int i = 0; i < threshold; i++) {
            String propertyUri = String.format("<%s>", propertyArray[i].toString());
            Map <String, Integer> propertyValuesMap = null;
            switch (ruleNumber) {
                case RULE_1:
                    propertyValuesMap = rule1_1PropertyValuesFreq(predicateUri, objectUri, propertyUri);
                    break;
                case RULE_2:
                    propertyValuesMap = rule2_1PropertyValuesFreq(predicateUri, objectUri, propertyUri);
                    break;
                case RULE_3:
                    propertyValuesMap = rule3_1PropertyValuesFreq(predicateUri, propertyUri);
                    break;
            }

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Map <String, Integer> reducedPropertyValuesMap = new LinkedHashMap<>();
            if (propertiesValuesRankedMap.get(propertyArray[i].toString()) != null)
                reducedPropertyValuesMap = propertiesValuesRankedMap.get(propertyArray[i].toString());

            Iterator it = propertyValuesMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                String property = (String) pair.getKey();
                int index = property.lastIndexOf('/') + 1;
                property = property.substring(index, property.length());
                index = property.lastIndexOf(':') + 1;
                property = property.substring(index, property.length());
                if (property.contains("#")
                        || property.matches(".*\\d+.*")
                        || property.equals(predicateUri)
                        || ((int)pair.getValue() == 1)) {
                    it.remove();
                    continue;
                }
                reducedPropertyValuesMap.put(pair.getKey().toString(), Integer.parseInt(pair.getValue().toString()));
                it.remove(); // avoids a ConcurrentModificationException
            }
            propertiesValuesRankedMap.put(propertyArray[i].toString(), reducedPropertyValuesMap);
        }
        return propertiesValuesRankedMap;
    }

    public static List<String> checkResourceAvailability (String subjectUri, String predicateUri) {
        String query = Queries.getQueryCheckResourceAvailability(subjectUri, predicateUri);
        return Queries.execute(query, QUERY_VAR_OBJECT);
    }

    public static Map<String, Integer> rule1SubjectsPropertiesIntersection(String predicateUri, String objectUri) {
        String query = Queries.getRule1(predicateUri, objectUri);
        currentQuery = query;
        return Queries.execFreq(query, QUERY_VAR_PREDICATE);
    }

    public static Map<String, Integer> rule1_1PropertyValuesFreq(String predicateUri, String objectUri, String propertyUri) {
        String query = Queries.getRule1Granular(predicateUri, objectUri, propertyUri);
        return Queries.execFreq(query, QUERY_VAR_OBJECT);
    }

    public static Map<String, Integer> rule2ObjectsPropertiesIntersection(String subjectUri, String predicateUri) {
        String query = Queries.getRule2(subjectUri, predicateUri);
        currentQuery = query;
        return Queries.execFreq(query, QUERY_VAR_PREDICATE);
    }

    public static Map<String, Integer> rule2_1PropertyValuesFreq(String predicateUri, String objectUri, String propertyUri) {
        String query = Queries.getRule2Granular(propertyUri, predicateUri, objectUri);
        return Queries.execFreq(query, QUERY_VAR_OBJECT);
    }

    public static Map<String, Integer> rule3PropertiesRanked(String predicateUri) {
        String query = Queries.getRule3(predicateUri);
        currentQuery = query;
        return Queries.execFreq(query, QUERY_VAR_PREDICATE);
    }

    public static Map<String, Integer> rule3_1PropertyValuesFreq(String predicateUri, String propertyUri) {
        String query = Queries.getRule3Granular(predicateUri, propertyUri);
        return Queries.execFreq(query, QUERY_VAR_OBJECT);
    }

    public static Map<String, Integer> rule3RankedPropertiesOfAllObjSameSubj(String subjectUri, String predicateUri, String objectUri) {
        String query = Queries.getQueryRankedPropertiesHiddenObject(subjectUri, predicateUri, objectUri);
        return Queries.execFreq(query, QUERY_VAR_PREDICATE);
    }

    public static Map<String, Integer> rule4RankedObjOfAllSubjSameProperty(String subjectUri, String predicateUri, String objectUri) {
        String query = Queries.getQueryRankedObjectHiddenProperties(subjectUri, objectUri, predicateUri);
        return Queries.execFreq(query, QUERY_VAR_OBJECT);
    }

    public static TripleExtractor getTriples(String fileName) throws FileNotFoundException {
        Model model = ModelFactory.createDefaultModel();
        model.read(new FileInputStream(fileName), null, "TTL");

        TripleExtractor tripleExtractor = new TripleExtractor(model);
        tripleExtractor.parseStatements();

        return tripleExtractor;
    }
}
