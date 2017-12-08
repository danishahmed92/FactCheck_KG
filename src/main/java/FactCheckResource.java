import org.apache.jena.rdf.model.*;

import java.util.HashMap;
import java.util.Map;

public class FactCheckResource {
    private Resource resource;
    private Model model;
    private Property labelProperty = ResourceFactory.createProperty(Constants.RDF_SCHEMA_NAMESPACE + "label");

    public Map<String, String> langLabels = new HashMap<String, String>();

    public FactCheckResource(Resource resource, Model model) {
        this.resource = resource;
        this.model = model;

        setLangLabels();
    }

    private void setLangLabels() {
        NodeIterator nodeIterator = this.model.listObjectsOfProperty(this.resource, this.labelProperty);
        while (nodeIterator.hasNext()) {
            RDFNode rdfNode = nodeIterator.nextNode();
            this.langLabels.put(rdfNode.asLiteral().getLanguage(), rdfNode.asLiteral().getLexicalForm());
        }
    }
}
