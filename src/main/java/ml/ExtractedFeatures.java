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

    /**
     * sets triple urls, and labels
     * @param subject resource
     * @param predicate property
     * @param object resource
     * @throws IOException
     */
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

    /**
     * set semantic score for property with regard to synonym and other property comparision
     * @param property predicate property to match
     * @param score double value
     */
    public void setSemanticSubjectProperty (String property, double score) {
        semanticSubjectProperty = property;
        semanticSubjectPropertyWeight = score;
    }

    /**
     * set property freq map according to rule 1
     * @param propertyMap property map ranked in desc order
     */
    public void setRule1SubjectsPropertiesMap (Map<String, Integer> propertyMap) {
        rule1SubjectsPropertiesMap = propertyMap;
    }

    /**
     * all properties deep with freq
     * @param propertiesValuesMap map of property of property along with freq
     */
    public void setRule1PropertiesValuesMap (Map<String, Map<String, Integer>> propertiesValuesMap) {
        rule1PropertiesValuesMap = propertiesValuesMap;
    }


    /**
     * all properties deep with freq
     * @param propertiesValuesMap map of property of property along with freq
     */
    public void setRule2PropertiesValuesMap (Map<String, Map<String, Integer>> propertiesValuesMap) {
        rule2PropertiesValuesMap = propertiesValuesMap;
    }

    /**
     * set property freq map according to rule 2
     * @param propertyMap property map ranked in desc order
     */
    public void setRule2ObjectsPropertiesMap (Map<String, Integer> propertyMap) {
        rule2ObjectsPropertiesMap = propertyMap;
    }

    /**
     * all properties deep with freq
     * @param propertiesValuesMap map of property of property along with freq
     */
    public void setRule3PropertiesValuesMap (Map<String, Map<String, Integer>> propertiesValuesMap) {
        rule3PropertiesValuesMap = propertiesValuesMap;
    }

    /**
     * set property freq map according to rule 3
     * @param propertyMap property map ranked in desc order
     */
    public void setRule3PropertiesRankedMap (Map<String, Integer> propertyMap) {
        rule3PropertiesRankedMap = propertyMap;
    }
}
