import config.Config;
import context.Similarity;
import ml.labelsFiltration;
import org.apache.jena.rdf.model.*;
import rdf.FactCheckResource;
import rdf.TripleExtractor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        model.read(new FileInputStream("Einstein.ttl"), null, "TTL");

        TripleExtractor tripleExtractor = new TripleExtractor(model);
        tripleExtractor.parseStatements();

        FactCheckResource subject = tripleExtractor.subject;
        FactCheckResource object = tripleExtractor.object;

        System.out.println("Subject:\t" + subject.langLabelsMap.get("en"));
        System.out.println("Predicate:\t" + tripleExtractor.predicate.getLocalName());
        System.out.println("Object:\t" + object.langLabelsMap.get("en"));

        System.out.println();
        System.out.println("Object sameAs size:\t" + tripleExtractor.object.langAltLabelsMap.get("en").size());
        System.out.println("Object sameAs:\t" + tripleExtractor.object.langAltLabelsMap.get("en"));

        System.out.println();
        labelsFiltration.altLabelVariantsSimilarityBased(object, "en");
    }
}
