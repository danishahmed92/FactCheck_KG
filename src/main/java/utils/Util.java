package utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Util {
    /**
     *
     * @param wordSimilarityMap
     * @return sorted map
     */
    public static Map<String, Double> sortMapByValue(Map<String, Double> wordSimilarityMap) {
        return wordSimilarityMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public static double mean(Object[] arr) {
        double sum = 0.0;
        for(Object i : arr)
            sum += Double.parseDouble(i.toString());
        return sum / arr.length;
    }

    public static double variance(Object[] arr) {
        double mean = mean(arr);
        double temp = 0;
        for(Object i :arr)
            temp += (Double.parseDouble(i.toString()) - mean) * (Double.parseDouble(i.toString()) - mean);
        return temp / (arr.length - 1);
    }

    public static double standardDeviation(Object[] arr) {
        return Math.sqrt(variance(arr));
    }
}