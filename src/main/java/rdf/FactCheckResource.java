package rdf;

import org.apache.jena.rdf.model.*;
import utils.Constants;

import java.util.*;

public class FactCheckResource {
    enum NodeIteratorType{
        LABEL(0), ALT_LABEL(1), SAME_AS(2);

        private int type;
        private NodeIteratorType(int value){
            this.type = value;
        }
        public int getType() {
            return type;
        }
    }

    protected Resource resource;
    protected Model model;
    public String uri;
    private Property labelProperty = ResourceFactory.createProperty(Constants.RDF_SCHEMA_NAMESPACE + "label");
    private Property altLabelProperty = ResourceFactory.createProperty(Constants.SKOS_CORE_NAMESPACE + "altLabel");
    private Property owlSameAsProperty = ResourceFactory.createProperty(Constants.OWL_NAMESPACE + "sameAs");

    public Map<String, String> langLabelsMap = new HashMap<String, String>();
    public Map<String, Set<String>> langAltLabelsMap = new HashMap<String, Set<String>>();

    public List<Resource> owlSameAsList = new ArrayList<Resource>();     // currently only for en lang

    /**
     * give me FactCheck resource model
     * @param resource given jena resource
     * @param model jena model
     */
    FactCheckResource(Resource resource, Model model) {
        this.resource = resource;
        this.uri =resource.getURI();
        this.model = model;

        // set labels w.r.t language
        NodeIterator LabelNodeIterator = this.model.listObjectsOfProperty(this.resource, this.labelProperty);
        setLabelsMap(LabelNodeIterator, NodeIteratorType.LABEL);

        // set alternative labels w.r.t language
        NodeIterator altLabelNodeIterator = this.model.listObjectsOfProperty(this.resource, this.altLabelProperty);
        setLabelsMap(altLabelNodeIterator, NodeIteratorType.ALT_LABEL);

        // set sameAs resource list
        setOwlSameAsList();
    }

    /**
     *
     * @param nodeIterator go through all jena nodes that you got from rdf
     * @param type which property you need to extract?
     */
    private void setLabelsMap(NodeIterator nodeIterator, NodeIteratorType type) {
        while (nodeIterator.hasNext()) {
            RDFNode rdfNode = nodeIterator.nextNode();
            String lang = rdfNode.asLiteral().getLanguage();
            String label = rdfNode.asLiteral().getLexicalForm();

            switch (type) {
                case LABEL:
                    langLabelsMap.put(lang, label);
                    break;
                case ALT_LABEL:
                    if (!langAltLabelsMap.containsKey(lang))
                        langAltLabelsMap.put(lang, new HashSet<String>());
                    langAltLabelsMap.get(lang).add(label);
                    break;
                case SAME_AS:
                    if (!langAltLabelsMap.containsKey(lang))
                        langAltLabelsMap.put(lang, new HashSet<String>());
                    langAltLabelsMap.get(lang).add(label);
                    break;
            }
        }
    }

    /**
     * get sameAs owl property
     */
    private void setOwlSameAsList() {
        NodeIterator nodeIterator = model.listObjectsOfProperty(this.resource, this.owlSameAsProperty);
        while (nodeIterator.hasNext()) {
            RDFNode rdfNode = nodeIterator.nextNode();
            owlSameAsList.add(rdfNode.asResource());

            NodeIterator altLabelNodeIterator = this.model.listObjectsOfProperty(rdfNode.asResource(), this.altLabelProperty);
            setLabelsMap(altLabelNodeIterator, NodeIteratorType.SAME_AS);
        }
    }

    /**
     * filter out only dbpedia resource as sparql doesn't work on freebase
     * @param resource FactCheck resource
     * @return uri of resource
     */
    public static String getDBpediaUri(FactCheckResource resource) {
        String uri = resource.uri;
        if (uri.contains(Constants.DBPEDIA_URI))
            return uri;

        List<Resource> subjectSameAsList = resource.owlSameAsList;
        String subjectUri = "";

        for (Resource rsc : subjectSameAsList) {
            if (rsc.toString().contains(Constants.DBPEDIA_URI)) {
                subjectUri = rsc.toString();
//                break;
            }
        }
        return subjectUri;
    }
}
