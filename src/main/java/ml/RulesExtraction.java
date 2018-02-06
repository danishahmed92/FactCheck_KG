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
import provider.PersistenceProvider;
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
    RULE_1(1), RULE_2(2), RULE_3_SUB(31), RULE_3_OBJ(32);
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


    /**
     *
     * @param path - path to crawl accross rdf files for training
     * @throws IOException
     */
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
//                        if (saveEntryToDB)
//                            Database.saveExtractedFeaturesObjToDB(extractedFeatures, conn, "award", file.getFileName().toString());
                    } catch (IOException ignore) {
                        // don't index files that can't be read.
                    	ignore.printStackTrace();
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
//                if (saveEntryToDB)
//                    Database.saveExtractedFeaturesObjToDB(extractedFeatures, conn, "award", path.getFileName().toString());
            } catch (JWNLException e) {
                e.printStackTrace();
            }
        }
    }

    public static Map<String, Integer> setThresholdMap(Map<String, Integer> map) {
        int threshold;
        int counter = 0;

        threshold = (int) Math.sqrt(map.size());
        Map<String, Integer> thresholdMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (counter < threshold) {
                thresholdMap.put(entry.getKey(), entry.getValue());
            }
            counter++;
        }
        return thresholdMap;
    }

    /**
     *
     * @param fileName from given file extract features based on rules
     * @return
     * @throws IOException
     * @throws JWNLException
     */
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
//        Map<String, Double> semanticSubjectPropertiesMap = rule0SemanticSubjectProperties(subject, object, predicate);
        /* if provided resource has properties in sparql */
//        if (semanticSubjectPropertiesMap.size() != 0) {
//            extractedFeatures.setSemanticSubjectProperty(semanticSubjectPropertiesMap.keySet().toArray()[semanticSubjectPropertiesMap.size() - 1].toString(),
//                    Double.parseDouble(semanticSubjectPropertiesMap.values().toArray()[semanticSubjectPropertiesMap.size() - 1].toString()));

            List<String> resourceAvailability = checkResourceAvailability(subjectUri, predicateUri);
            if (resourceAvailability.size() != 0) {
                /* Step 4 (RULE #1)*/
                Map<String, Integer> subjectsPropertiesMap = rule1SubjectsPropertiesIntersection(predicateUri, objectUri);
                Map<String, Integer> subPropMap = setThresholdMap(subjectsPropertiesMap);
                subjectsPropertiesMap.clear();

                String[] arrRule1 = {"U", predicate.getURI(), FactCheckResource.getDBpediaUri(object)};
                if (!queryCache.containsKey(currentQuery)) {
                    extractedFeatures.setRule1SubjectsPropertiesMap(subPropMap);
                    Map<String, Map<String, Integer>> subjectsPropertiesValuesMap = extractPropertyValues(RuleNumber.RULE_1, subPropMap, predicateUri, objectUri);
                    queryCache.put(currentQuery, subjectsPropertiesValuesMap);
                    extractedFeatures.setRule1PropertiesValuesMap(subjectsPropertiesValuesMap);

                    PersistenceProvider.persistRules(arrRule1, subPropMap, subjectsPropertiesValuesMap);
                } /*else {
                    extractedFeatures.setRule1PropertiesValuesMap(queryCache.get(currentQuery));
                    PersistenceProvider.persistRules(arrRule1, subPropMap, queryCache.get(currentQuery));
                }*/
                System.out.println("Rule 1 finished");

                /* Step 5 (RULE #2)*/
                Map<String, Integer> objectsPropertiesMap = rule2ObjectsPropertiesIntersection(subjectUri, predicateUri);
                Map<String, Integer> objPropMap = setThresholdMap(objectsPropertiesMap);
                objectsPropertiesMap.clear();

                String[] arrRule2 = {FactCheckResource.getDBpediaUri(subject), predicate.getURI(), "U"};
                if (!queryCache.containsKey(currentQuery)) {
                    extractedFeatures.setRule2ObjectsPropertiesMap(objPropMap);
                    Map<String, Map<String, Integer>> objectsPropertiesValuesMap = extractPropertyValues(RuleNumber.RULE_2, objPropMap, predicateUri, objectUri);
                    queryCache.put(currentQuery, objectsPropertiesValuesMap);
                    extractedFeatures.setRule2PropertiesValuesMap(objectsPropertiesValuesMap);

                    PersistenceProvider.persistRules(arrRule2, objPropMap, objectsPropertiesValuesMap);
                } /*else {
                    extractedFeatures.setRule2PropertiesValuesMap(queryCache.get(currentQuery));

                    PersistenceProvider.persistRules(arrRule2, objPropMap, queryCache.get(currentQuery));
                }*/
                System.out.println("Rule 2 finished");

                /* Step 6 (RULE #3)*/
                Map<String, Integer> propertiesSubjectRankedMap = rule3SubjectPropertiesRanked(predicateUri);
                Map<String, Integer> propertiesObjectRankedMap = rule3ObjectPropertiesRanked(predicateUri);
                Map<String, Integer> propertiesSubRankedMap = setThresholdMap(propertiesSubjectRankedMap);
                Map<String, Integer> propertiesObjRankedMap = setThresholdMap(propertiesObjectRankedMap);
                propertiesSubjectRankedMap.clear();
                propertiesObjectRankedMap.clear();
                System.out.println("Rule 3 Sub threshold:\t" + propertiesSubRankedMap.size());
                System.out.println("Rule 3 Obj threshold:\t" + propertiesObjRankedMap.size());

                if (!queryCache.containsKey(currentQuery)) {
                    extractedFeatures.setRule3SubPropertiesRankedMap(propertiesSubRankedMap);
                    Map<String, Map<String, Integer>> subPropertiesValuesMap = extractPropertyValues(RuleNumber.RULE_3_SUB, propertiesSubRankedMap, predicateUri, objectUri);
                    queryCache.put(currentQuery, subPropertiesValuesMap);
                    extractedFeatures.setRule3SubPropertiesValuesMap(subPropertiesValuesMap);

                    String[] arrRule3Sub = {"U", predicate.getURI(), "K"};
                    PersistenceProvider.persistRules(arrRule3Sub, propertiesSubRankedMap, subPropertiesValuesMap);

                    extractedFeatures.setRule3ObjPropertiesRankedMap(propertiesObjRankedMap);
                    Map<String, Map<String, Integer>> objPropertiesValuesMap = extractPropertyValues(RuleNumber.RULE_3_OBJ, propertiesObjRankedMap, predicateUri, objectUri);
                    queryCache.put(currentQuery, objPropertiesValuesMap);
                    extractedFeatures.setRule3ObjPropertiesValuesMap(objPropertiesValuesMap);

                    String[] arrRule3Obj = {"K", predicate.getURI(), "U"};
                    PersistenceProvider.persistRules(arrRule3Obj, propertiesObjRankedMap, objPropertiesValuesMap);
                }
                System.out.println("Rule 3 finished");

                System.out.println();
                return extractedFeatures;
            } else {
                saveEntryToDB = false;
                System.out.println("Resource unavailable w.r.t object in Dbpedia sparql: " + subjectUri + ",\t" + predicateUri);
                System.out.println();
            }
//        } else {
//            saveEntryToDB = false;
//            System.out.println("Resource unavailable in Dbpedia sparql: " + subjectUri);
//            System.out.println();
//        }
        return null;
    }

    /**
     *
     * Used for rule 0
     *
     * @param subject as resource
     * @param object as resource
     * @param predicate as property
     * @return map of properties along with freq count
     * @throws JWNLException
     * @throws IOException
     */
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

    /**
     *
     * @param ruleNumber pass rule number for which you need to get all values of certain properties
     * @param propertyFreqMap properties that needs to be parsed along with freq
     * @param predicateUri input predicate
     * @param objectUri object uri
     * @return
     */
    public static Map<String, Map<String, Integer>> extractPropertyValues(RuleNumber ruleNumber, Map<String, Integer> propertyFreqMap, String predicateUri, String objectUri) {
        Object[] propertyArray = propertyFreqMap.keySet().toArray();

        Map<String, Map<String, Integer>> propertiesValuesRankedMap = new LinkedHashMap<>();

        for (int i = 0; i < propertyFreqMap.size(); i++) {
            String propertyUri = String.format("<%s>", propertyArray[i].toString());
            Map <String, Integer> propertyValuesMap = null;
            switch (ruleNumber) {
                case RULE_1:
                    propertyValuesMap = rule1_1PropertyValuesFreq(predicateUri, objectUri, propertyUri);
                    break;
                case RULE_2:
                    propertyValuesMap = rule2_1PropertyValuesFreq(predicateUri, objectUri, propertyUri);
                    break;
                case RULE_3_SUB:
                    propertyValuesMap = rule3_1SubjectPropertyValuesFreq(predicateUri, propertyUri);
                    break;
                case RULE_3_OBJ:
                    propertyValuesMap = rule3_1ObjectPropertyValuesFreq(predicateUri, propertyUri);
                    break;
            }

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int threshold = (int) Math.sqrt(propertyValuesMap.size());
            int counter = 0;
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
                        /*|| ((int)pair.getValue() == 1)*/) {
                    it.remove();
                    continue;
                }
                if (counter < threshold) {
                    reducedPropertyValuesMap.put(pair.getKey().toString(), Integer.parseInt(pair.getValue().toString()));
                    counter++;
                }
                it.remove(); // avoids a ConcurrentModificationException
            }

            propertiesValuesRankedMap.put(propertyArray[i].toString(), reducedPropertyValuesMap);
        }
        return propertiesValuesRankedMap;
    }

    /**
     * Check if sparql has results for given subject and predicate
     *
     * @param subjectUri subject
     * @param predicateUri object
     * @return
     */
    public static List<String> checkResourceAvailability (String subjectUri, String predicateUri) {
        String query = Queries.getQueryCheckResourceAvailability(subjectUri, predicateUri);
        return Queries.execute(query, QUERY_VAR_OBJECT);
    }

    /**
     * Rule 1: intersection of properties of all subjects that have predicate
     * @param predicateUri predicate url
     * @param objectUri object url
     * @return
     */
    public static Map<String, Integer> rule1SubjectsPropertiesIntersection(String predicateUri, String objectUri) {
        String query = Queries.getRule1(predicateUri, objectUri);
        currentQuery = query;
        return Queries.execFreq(query, QUERY_VAR_PREDICATE);
    }

    /**
     * get deeper values of properties extracted from rule 1
     * @param predicateUri predicate url
     * @param objectUri obj url
     * @param propertyUri property url
     * @return
     */
    public static Map<String, Integer> rule1_1PropertyValuesFreq(String predicateUri, String objectUri, String propertyUri) {
        String query = Queries.getRule1Granular(predicateUri, objectUri, propertyUri);
        return Queries.execFreq(query, QUERY_VAR_OBJECT);
    }

    /**
     * Rule 2: intersection of object properties
     * @param subjectUri
     * @param predicateUri
     * @return
     */
    public static Map<String, Integer> rule2ObjectsPropertiesIntersection(String subjectUri, String predicateUri) {
        String query = Queries.getRule2(subjectUri, predicateUri);
        currentQuery = query;
        return Queries.execFreq(query, QUERY_VAR_PREDICATE);
    }

    /**
     * get deeper values of properties extracted from rule 2
     * @param predicateUri predicate url
     * @param objectUri obj url
     * @param propertyUri property url
     * @return
     */
    public static Map<String, Integer> rule2_1PropertyValuesFreq(String predicateUri, String objectUri, String propertyUri) {
        String query = Queries.getRule2Granular(propertyUri, predicateUri, objectUri);
        return Queries.execFreq(query, QUERY_VAR_OBJECT);
    }

    /**
     * Rule: ?s p ?o
     * @param predicateUri predicate url
     * @return
     */
    public static Map<String, Integer> rule3PropertiesRanked(String predicateUri) {
        String query = Queries.getRule3(predicateUri);
        currentQuery = query;
        return Queries.execFreq(query, QUERY_VAR_PREDICATE);
    }

    /**
     * get deeper values of properties extracted from rule 3
     * @param predicateUri predicate url
     * @param propertyUri property url
     * @return
     */
    public static Map<String, Integer> rule3_1PropertyValuesFreq(String predicateUri, String propertyUri) {
        String query = Queries.getRule3Granular(predicateUri, propertyUri);
        return Queries.execFreq(query, QUERY_VAR_OBJECT);
    }

    /**
     * Rule: ?s p ?o
     * @param predicateUri predicate url
     * @return
     */
    public static Map<String, Integer> rule3SubjectPropertiesRanked(String predicateUri) {
        String query = Queries.getRule3Sub(predicateUri);
        currentQuery = query;
        return Queries.execFreq(query, QUERY_VAR_PREDICATE);
    }

    /**
     * get deeper values of properties extracted from rule 3
     * @param predicateUri predicate url
     * @param propertyUri property url
     * @return
     */
    public static Map<String, Integer> rule3_1SubjectPropertyValuesFreq(String predicateUri, String propertyUri) {
        String query = Queries.getRule3SubGranular(predicateUri, propertyUri);
        return Queries.execFreq(query, QUERY_VAR_OBJECT);
    }

    /**
     * Rule: ?s p ?o
     * @param predicateUri predicate url
     * @return
     */
    public static Map<String, Integer> rule3ObjectPropertiesRanked(String predicateUri) {
        String query = Queries.getRule3Obj(predicateUri);
        currentQuery = query;
        return Queries.execFreq(query, QUERY_VAR_PREDICATE);
    }

    /**
     * get deeper values of properties extracted from rule 3
     * @param predicateUri predicate url
     * @param propertyUri property url
     * @return
     */
    public static Map<String, Integer> rule3_1ObjectPropertyValuesFreq(String predicateUri, String propertyUri) {
        String query = Queries.getRule3ObjGranular(predicateUri, propertyUri);
        return Queries.execFreq(query, QUERY_VAR_OBJECT);
    }

    /**
     * extraction of rdf files on to triple format
     * @param fileName rdf file
     * @return
     * @throws FileNotFoundException
     */
    public static TripleExtractor getTriples(String fileName) throws FileNotFoundException {
        Model model = ModelFactory.createDefaultModel();
        model.read(new FileInputStream(fileName), null, "TTL");

        TripleExtractor tripleExtractor = new TripleExtractor(model);
        tripleExtractor.parseStatements();

        return tripleExtractor;
    }
}
