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

        System.out.println("Subject:\t" + subject.langLabelsMap.get("en"));
        System.out.println("Predicate:\t" + predicate.getLocalName());
        System.out.println("Object:\t" + object.langLabelsMap.get("en"));

        System.out.println();
        System.out.println("Object sameAs size:\t" + tripleExtractor.object.langAltLabelsMap.get("en").size());
        System.out.println("Object sameAs:\t" + tripleExtractor.object.langAltLabelsMap.get("en"));
        System.out.println();

        /* Get authentic labels with variety */
        System.out.println("Authentic label variants:");
        labelsFiltration.altLabelVariantsSimilarityBased(object, "en");
        System.out.println();

        /* Get best synonyms for property (predicate) label */
        List<String> predicateSynonyms = new ArrayList<>();
        try {
            predicateSynonyms = WordNet.getNTopSynonyms(predicate.getLocalName(), 5);
            System.out.println();
            System.out.println("best synonyms for predicate " + predicate.getLocalName() + ":");
            System.out.println(predicateSynonyms);
            System.out.println();
        } catch (JWNLException e) {
            e.printStackTrace();
        }

        System.out.println("Jaccard similarity of synonyms w.r.t object label: (Actual word = " + predicate.getLocalName() + ")");
        for (String syn : predicateSynonyms) {
            double totalWeight = Similarity.getSemanticSimilarity(object.langLabelsMap.get("en"), syn, predicate.getLocalName());
            System.out.println(syn + "\t\tsemantic score:" + totalWeight);
        }
        System.out.println();

        /* Get all properties of subject */
        String query = String.format(Queries.ALL_PREDICATES_OF_SUBJECT, "<" + FactCheckResource.getDBpediaUri(subject) + ">");
        List<String> results = Queries.execute(query);
        System.out.println("List all properties of Subject:");
        System.out.println(results);

        Set<String> objectSetString = new HashSet<String>(Arrays.asList(object.langLabelsMap.get("en").split(" ")));
        System.out.println();
        System.out.println("properties of subjects as words:");
        for (String property : results) {
            int index = property.lastIndexOf('/') + 1;
            property = property.substring(index, property.length());
            property = property.replaceAll("\\d+", "").replaceAll("(.)([A-Z])", "$1 $2");
            if (property.contains("#"))
                continue;
            Set<String> propertySetString = new HashSet<String>(Arrays.asList(property.split(" ")));

//            Similarity similarity = new Similarity(object.langLabelsMap.get("en"), property, true);
//            System.out.println(property + ": \t\t" + "Jaccard set:" + Similarity.jaccardSimilarity(objectSetString, propertySetString));
            System.out.println(property);
        }
    }
}
