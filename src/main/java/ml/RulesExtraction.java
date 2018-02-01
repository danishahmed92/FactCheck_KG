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
public class RulesExtraction {
    public static final String QUERY_VAR_SUBJECT = "s";
    public static final String QUERY_VAR_OBJECT = "o";
    public static final String QUERY_VAR_PREDICATE = "p";

    public static Boolean saveEntryToDB = true;
    public static Connection conn = Database.databaseInstance.conn;

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

        /* Step 3 (RULE #1) */
        Map<String, Double> semanticSubjectPropertiesMap = rule1SemanticSubjectProperties(subject, object, predicate);
        /* if provided resource has properties in sparql */
        if (semanticSubjectPropertiesMap.size() != 0) {
            extractedFeatures.setSemanticSubjectProperty(semanticSubjectPropertiesMap.keySet().toArray()[semanticSubjectPropertiesMap.size() - 1].toString(),
                    Double.parseDouble(semanticSubjectPropertiesMap.values().toArray()[semanticSubjectPropertiesMap.size() - 1].toString()));

            List<String> resourceAvailability = checkResourceAvailability(subjectUri, predicateUri);
            if (resourceAvailability.size() != 0) {
                /* Step 4 (RULE #2)*/
                Map<String, Integer> propertiesOfAllSubjSameObjMap = rule2RankedPropertiesOfAllSubjSameObj(subjectUri, predicateUri, objectUri);
                extractedFeatures.setPropertiesOfAllSubjSameObjMap(propertiesOfAllSubjSameObjMap);
                /* (RULE #2.1)*/
                extractedFeatures.setPropertiesValuesRankedMap(extractPropertyValuesFromTriple(propertiesOfAllSubjSameObjMap, subjectUri, predicateUri, objectUri));

                /* Step 5 (RULE #3)*/
                Map<String, Integer> propertiesOfAllObjSameSubjMap = rule3RankedPropertiesOfAllObjSameSubj(subjectUri, predicateUri, objectUri);
                extractedFeatures.setPropertiesOfAllObjSameSubjMap(propertiesOfAllObjSameSubjMap);

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

    public static Map<String, Map<String, Integer>> extractPropertyValuesFromTriple(Map<String, Integer> propertiesOfAllSubjSameObjMap, String subjectUri, String predicateUri, String objectUri) {
        Object[] propertyArray = propertiesOfAllSubjSameObjMap.keySet().toArray();
        int threshold = (int) Math.round(Math.sqrt(propertiesOfAllSubjSameObjMap.size()));
        Map<String, Map<String, Integer>> propertiesValuesRankedMap = new LinkedHashMap<>();
        for (int i = 0; i < threshold; i++) {
            String propertyUri = String.format("<%s>", propertyArray[i].toString());
            Map <String, Integer> propertyValuesMap = rule2_1PropertyValuesRanked(subjectUri, predicateUri, objectUri, propertyUri);
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

    public static Map<String, Double> rule1SemanticSubjectProperties(FactCheckResource subject, FactCheckResource object, Property predicate) throws JWNLException, IOException {
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

    public static List<String> checkResourceAvailability (String subjectUri, String predicateUri) {
        String query = Queries.getQueryCheckResourceAvailability(subjectUri, predicateUri);
        return Queries.execute(query, QUERY_VAR_OBJECT);
    }

    public static Map<String, Integer> rule2RankedPropertiesOfAllSubjSameObj(String subjectUri, String predicateUri, String objectUri) {
        String query = Queries.getQueryRankedPropertiesHiddenSubject(predicateUri, objectUri, subjectUri);
        return Queries.execFreq(query, QUERY_VAR_PREDICATE);
    }

    public static Map<String, Integer> rule2_1PropertyValuesRanked(String subjectUri, String predicateUri, String objectUri, String propertyUri) {
        String query = Queries.getQueryRankedPropertyValues(subjectUri, propertyUri, predicateUri, objectUri);
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
