import org.apache.jena.rdf.model.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        Model model = ModelFactory.createDefaultModel();
        model.read(new FileInputStream("Einstein.ttl"), null, "TTL");

        RelationExtractor relationExtractor = new RelationExtractor(model);
        relationExtractor.parseStatements();
    }
}
