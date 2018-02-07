package utils;

import org.apache.jena.base.Sys;

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

    public static void main(String[] args) throws IOException {
        System.out.println("wrong/range/award");
        try (BufferedReader br = new BufferedReader(new FileReader("wrong_range_spouse.txt"))) {
            String line;
            ArrayList<String> confidenceValue = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] splitLine = line.split("\\s+");
                confidenceValue.add(splitLine[4]);
            }

            System.out.println(confidenceValue);

            Double standardDeviation = standardDeviation(confidenceValue.toArray());
            Double variance = variance(confidenceValue.toArray());
            Double mean = mean(confidenceValue.toArray());
            System.out.println("mean:\t" + mean);
            System.out.println("standard deviation:\t" + standardDeviation);
            System.out.println("variance:\t" + variance);

            int falseCount = 0;
            int trueCount = 0;
            Double cv = 0.0;

            for (int i = 0; i < confidenceValue.size(); i++) {
                cv = Double.parseDouble(confidenceValue.get(i));
                if (cv <= -2.215678593775) {
                    falseCount++;
                } else {
                    trueCount++;
                }
            }
            System.out.println("false count:\t" + falseCount);
            System.out.println("true count:\t" + trueCount);
            System.out.println();
        }

        System.out.println("correct/award");
        try (BufferedReader br = new BufferedReader(new FileReader("correct_range_spouse.txt"))) {
            String line;
            ArrayList<String> confidenceValue = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] splitLine = line.split("\\s+");
                confidenceValue.add(splitLine[4]);
            }

            System.out.println(confidenceValue);

            Double standardDeviation = standardDeviation(confidenceValue.toArray());
            Double variance = variance(confidenceValue.toArray());
            Double mean = mean(confidenceValue.toArray());
            System.out.println("mean:\t" + mean);
            System.out.println("standard deviation:\t" + standardDeviation);
            System.out.println("variance:\t" + variance);

            int falseCount = 0;
            int trueCount = 0;
            Double cv = 0.0;

            for (int i = 0; i < confidenceValue.size(); i++) {
                cv = Double.parseDouble(confidenceValue.get(i));
                if (cv <= -2.1339) {
                    falseCount++;
                } else {
                    trueCount++;
                }
            }
            System.out.println("false count:\t" + falseCount);
            System.out.println("true count:\t" + trueCount);
        }


        /*System.out.println("calculating bias factor for non matching rules.");
        try {
            BufferedReader brC = new BufferedReader(new FileReader("correct_award_train_threshold.txt"));
            BufferedReader brW = new BufferedReader(new FileReader("wrong_range_award_train_threshold.txt"));

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

            ArrayList<Double> meanCV = new ArrayList<>();
            for (int i = 0; i < confidenceValueC.size(); i++) {
                meanCV.add(Double.parseDouble(confidenceValueC.get(i)) + Double.parseDouble(confidenceValueW.get(i)) / 2);
            }
            Double standardDeviation = standardDeviation(meanCV.toArray());
            Double variance = variance(meanCV.toArray());
            Double mean = mean(meanCV.toArray());
            System.out.println("mean:\t" + mean);
            System.out.println("standard deviation:\t" + standardDeviation);
            System.out.println("variance:\t" + variance);
        } catch (IOException ignore) {
            // don't index files that can't be read.
            ignore.printStackTrace();
        }*/
    }
}