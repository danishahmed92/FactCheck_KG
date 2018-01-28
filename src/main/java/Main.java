import ml.labelsFiltration;
import org.apache.jena.rdf.model.*;
import rdf.FactCheckResource;
import rdf.Queries;
import rdf.TripleExtractor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        model.read(new FileInputStream("leader_00000.ttl"), null, "TTL");
//        model.read(new FileInputStream("spouse_00024.ttl"), null, "TTL");
//        model.read(new FileInputStream("Einstein.ttl"), null, "TTL");

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

        String query = String.format(Queries.ALL_PREDICATES_OF_SUBJECT, "<" + FactCheckResource.getDBpediaUri(subject) + ">");
        List<String> results = Queries.execute(query);
        System.out.println(results);
    }
}
