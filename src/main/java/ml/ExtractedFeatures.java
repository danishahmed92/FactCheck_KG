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

    public Map<String, Integer> rule1SubjectsPropertiesMap;
    public Map<String, Map<String, Integer>> rule1PropertiesValuesMap;

    public Map<String, Integer> rule2ObjectsPropertiesMap;
    public Map<String, Map<String, Integer>> rule2PropertiesValuesMap;

    public Map<String, Integer> rule3PropertiesRankedMap;
    public Map<String, Map<String, Integer>> rule3PropertiesValuesMap;

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

    public void setRule1SubjectsPropertiesMap (Map<String, Integer> propertyMap) {
        rule1SubjectsPropertiesMap = propertyMap;
    }

    public void setRule1PropertiesValuesMap (Map<String, Map<String, Integer>> propertiesValuesMap) {
        rule1PropertiesValuesMap = propertiesValuesMap;
    }

    public void setRule2PropertiesValuesMap (Map<String, Map<String, Integer>> propertiesValuesMap) {
        rule2PropertiesValuesMap = propertiesValuesMap;
    }

    public void setRule2ObjectsPropertiesMap (Map<String, Integer> propertyMap) {
        rule2ObjectsPropertiesMap = propertyMap;
    }

    public void setRule3PropertiesValuesMap (Map<String, Map<String, Integer>> propertiesValuesMap) {
        rule3PropertiesValuesMap = propertiesValuesMap;
    }

    public void setRule3PropertiesRankedMap (Map<String, Integer> propertyMap) {
        rule3PropertiesRankedMap = propertyMap;
    }

    public void setObjOfAllSubjSamePropertyMap (Map<String, Integer> objectMap) {
        objOfAllSubjSamePropertyMap = objectMap;
    }
}
