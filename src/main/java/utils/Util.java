package utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Util {
    public static Map<String, Double> sortMapByValue(Map<String, Double> wordSimilarityMap) {
        return wordSimilarityMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}