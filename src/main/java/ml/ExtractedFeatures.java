package ml;

import org.apache.jena.rdf.model.Property;
import rdf.FactCheckResource;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class ExtractedFeatures implements Serializable {
    public static String subjectUri;
    public static String predicateUri;
    public static String objectUri;

    public static String subjectLabel;
    public static String predicateLabel;
    public static String objectLabel;

    public static Set<String> objectLabelVariants;
    public static Set<String> subjectLabelVariants;

    public String semanticSubjectProperty;
    public double semanticSubjectPropertyWeight;

    public Map<String, Integer> propertiesOfAllSubjSameObjMap;
    public Map<String, Map<String, Integer>> propertiesValuesRankedMap;
    public Map<String,Integer> propertiesOfAllObjSameSubjMap;
    public Map<String,Integer> objOfAllSubjSamePropertyMap;

    public ExtractedFeatures(FactCheckResource subject, Property predicate, FactCheckResource object) throws IOException {
        subjectUri = String.format("<%s>", FactCheckResource.getDBpediaUri(subject));
        predicateUri = String.format("<%s>", predicate.getURI());
        objectUri = String.format("<%s>", FactCheckResource.getDBpediaUri(object));

        objectLabel = object.langLabelsMap.get("en");
        predicateLabel = predicate.getLocalName();
        subjectLabel = subject.langLabelsMap.get("en");

        objectLabelVariants = labelsFiltration.altLabelVariantsSimilarityBased(object, "en");
        subjectLabelVariants = labelsFiltration.altLabelVariantsSimilarityBased(subject, "en");
    }

    public void setSemanticSubjectProperty (String property, double score) {
        semanticSubjectProperty = property;
        semanticSubjectPropertyWeight = score;
    }

    public void setPropertiesOfAllSubjSameObjMap (Map<String, Integer> propertyMap) {
        propertiesOfAllSubjSameObjMap = propertyMap;
    }

    public void setPropertiesValuesRankedMap (Map<String, Map<String, Integer>> propertiesValuesMap) {
        propertiesValuesRankedMap = propertiesValuesMap;
    }

    public void setPropertiesOfAllObjSameSubjMap (Map<String, Integer> propertyMap) {
        propertiesOfAllObjSameSubjMap = propertyMap;
    }

    public void setObjOfAllSubjSamePropertyMap (Map<String, Integer> objectMap) {
        objOfAllSubjSamePropertyMap = objectMap;
    }
}
