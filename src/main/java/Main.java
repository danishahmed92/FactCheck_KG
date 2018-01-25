import org.apache.jena.rdf.model.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        Model model = ModelFactory.createDefaultModel();
        model.read(new FileInputStream("Einstein.ttl"), null, "TTL");

        RelationExtractor relationExtractor = new RelationExtractor(model);
        relationExtractor.parseStatements();

        System.out.println("Subject:\t" + relationExtractor.subject.langLabelsMap.get("en"));
        System.out.println("Predicate:\t" + relationExtractor.predicate.getLocalName());
        System.out.println("Object:\t" + relationExtractor.object.langLabelsMap.get("en"));

        System.out.println();
        System.out.println("Object sameAs:\t" + relationExtractor.object.langAltLabelsMap.get("en"));
    }
}
