package utils;

import org.apache.jena.base.Sys;
import test.ConfidenceProvider;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Util {
    /**
     *
     * @param wordSimilarityMap arrange this map list according to score of values (lowest to highest)
     * @return sorted map
     */
    public static Map<String, Double> sortMapByValue(Map<String, Double> wordSimilarityMap) {
        return wordSimilarityMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     *
     * @param arr elements
     * @return mean amongst array elements
     */
    public static double mean(Object[] arr) {
        double sum = 0.0;
        for(Object i : arr)
            sum += Double.parseDouble(i.toString());
        return sum / arr.length;
    }

    /**
     *
     * @param arr elements
     * @return variance amongst array elements
     */
    public static double variance(Object[] arr) {
        double mean = mean(arr);
        double temp = 0;
        for(Object i :arr)
            temp += (Double.parseDouble(i.toString()) - mean) * (Double.parseDouble(i.toString()) - mean);
        return temp / (arr.length - 1);
    }

    /**
     *
     * @param arr elements
     * @return standard deviation amongst array elements
     */
    public static double standardDeviation(Object[] arr) {
        return Math.sqrt(variance(arr));
    }

    /**
     *
     * @param confidenceValueList list of confidence values of triples that you need to evaluate
     * @param threshold threshold that you calculated while training your dataset
     */
    public static void evaluateFactsFromListOfConfidence(ArrayList<String> confidenceValueList, Double threshold) {
        int falseCount = 0;
        int trueCount = 0;
        Double cv;
        Double diff;
        for (int i = 0; i < confidenceValueList.size(); i++) {
            cv = Double.parseDouble(confidenceValueList.get(i));
            if (cv <= threshold) {
                if (cv < -1.0)
                    cv = -1.0;
                diff = Math.abs(threshold - cv);
                System.out.println("False:\t" + cv + "\t" + diff + "\t" + ((diff+0.2)*100));
                falseCount++;
            } else {
                if (cv > 1.0)
                    cv = 1.0;
                diff = Math.abs(cv - (threshold));
                System.out.println("True:\t" + cv + "\t" + diff + "\t" + ((diff+0.8)*100));
                trueCount++;
            }
        }
        System.out.println("false count:\t" + falseCount);
        System.out.println("true count:\t" + trueCount);
        System.out.println();
    }

    public static void main(String[] args) throws IOException {

        try {
            BufferedReader brC = new BufferedReader(new FileReader("correct_award.txt"));
            BufferedReader brW = new BufferedReader(new FileReader("wrong_range_award.txt"));

            String lineC;
            ArrayList<String> confidenceValueC = new ArrayList<>();
            while ((lineC = brC.readLine()) != null) {
                String[] splitLine = lineC.split("\\s+");
                confidenceValueC.add(splitLine[4]);
            }

            String lineW;
            ArrayList<String> confidenceValueW = new ArrayList<>();
            while ((lineW = brW.readLine()) != null) {
                String[] splitLine = lineW.split("\\s+");
                confidenceValueW.add(splitLine[4]);
            }

            Double threshold = ConfidenceProvider.getConfidenceThreshold("correct_award_train_threshold.txt", "wrong_range_award_train_threshold.txt");

            System.out.println("wrong/range/award");
            evaluateFactsFromListOfConfidence(confidenceValueC, threshold);

            System.out.println("correct/award");
            evaluateFactsFromListOfConfidence(confidenceValueW, threshold);
        } catch (IOException ignore) {
            // don't index files that can't be read.
            ignore.printStackTrace();
        }
    }
}