import context.Similarity;
import context.WordNet;
import ml.labelsFiltration;
import org.apache.jena.rdf.model.*;
import rdf.FactCheckResource;
import rdf.Queries;
import rdf.TripleExtractor;
import rita.wordnet.jwnl.JWNLException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {
        Model model = ModelFactory.createDefaultModel();
//        model.read(new FileInputStream("leader_00000.ttl"), null, "TTL");
//        model.read(new FileInputStream("spouse_00024.ttl"), null, "TTL");
        model.read(new FileInputStream("Einstein.ttl"), null, "TTL");

        TripleExtractor tripleExtractor = new TripleExtractor(model);
        tripleExtractor.parseStatements();

        FactCheckResource subject = tripleExtractor.subject;
        Property predicate = tripleExtractor.predicate;
        FactCheckResource object = tripleExtractor.object;

        String objectLabel = object.langLabelsMap.get("en");
        String predicateLabel = predicate.getLocalName();
        String subjectLabel = subject.langLabelsMap.get("en");

        System.out.println("Subject:\t" + subjectLabel);
        System.out.println("Predicate:\t" + predicateLabel);
        System.out.println("Object:\t" + objectLabel);

        System.out.println();
        System.out.println("Object sameAs size:\t" + tripleExtractor.object.langAltLabelsMap.get("en").size());
        System.out.println("Object sameAs:\t" + tripleExtractor.object.langAltLabelsMap.get("en"));
        System.out.println();

        /* Get authentic labels with variety */
        System.out.println("Authentic label variants of object:");
        Set<String> objectLabelVariants = labelsFiltration.altLabelVariantsSimilarityBased(object, "en");
        System.out.println(objectLabelVariants);
        System.out.println();

        /* Get best synonyms for property (predicate) label */
        List<String> predicateSynonyms = new ArrayList<>();
        try {
            predicateSynonyms = WordNet.getNTopSynonyms(predicateLabel, 5);
            System.out.println();
            System.out.println("best synonyms for predicate " + predicateLabel + ":");
            System.out.println(predicateSynonyms);
            System.out.println();
        } catch (JWNLException e) {
            e.printStackTrace();
        }

        System.out.println("Jaccard similarity of synonyms w.r.t object label: (Actual word = " + predicateLabel + ")");
        for (String syn : predicateSynonyms) {
            double totalWeight = Similarity.getSemanticSimilarity(objectLabel, syn, predicateLabel);
            System.out.println(syn + "\t\tsemantic score:" + totalWeight);
        }
        System.out.println();

        /* Get all properties of subject */
        String query = String.format(Queries.ALL_PREDICATES_OF_SUBJECT, "<" + FactCheckResource.getDBpediaUri(subject) + ">");
        List<String> results = Queries.execute(query, "predicate");
        System.out.println("List all properties of Subject:");
        System.out.println(results);

        Set<String> objectSetString = new HashSet<String>(Arrays.asList(objectLabel.split(" ")));
        System.out.println();
//        System.out.println("properties of subjects as words:");
        Map<String, Double> propertySimilarityMap = new HashMap<String, Double>();
        for (String property : results) {
            int index = property.lastIndexOf('/') + 1;
            property = property.substring(index, property.length());
            property = property.replaceAll("\\d+", "").replaceAll("(.)([A-Z])", "$1 $2");
            if (property.contains("#") || property.equals(predicateLabel))
                continue;

            double propertyPredicateScore = 0;
            double propertySynonymScore = 0;

            propertyPredicateScore = Similarity.getSemanticSimilarity(objectLabel, property, predicateLabel);
            for (String synonym : predicateSynonyms) {
                propertySynonymScore = Similarity.getSemanticSimilarity(objectLabel, property, synonym);
            }
            double similarityScore = propertyPredicateScore + propertySynonymScore;

            if (similarityScore == 0 || Double.isNaN(similarityScore))
                continue;
            propertySimilarityMap.put(property, similarityScore);
        }

//        propertySimilarityMap = labelsFiltration.sortMapSimilarity(propertySimilarityMap);


        /* Get all subject */
        query = String.format(Queries.PREDICATE_OBJECT_FIXED,
                "<" + predicate.getURI() + ">",
                "<" + FactCheckResource.getDBpediaUri(object) + ">");
        results.clear();
        results = Queries.execute(query, "subject");
        System.out.println("");
        System.out.println("List all Subjects:");
        System.out.println(results);
    }
}
