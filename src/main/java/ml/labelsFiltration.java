package ml;

import config.Config;
import context.Similarity;
import rdf.FactCheckResource;
import utils.Util;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class labelsFiltration {
    private static String mainLabel;
    private static Map<String, Double> wordSimilarityMap;
    private static Set<String> similarWords;

    /*
     * Keep track of least similar words,
     * if the words are similar, there is already a high probability that to be compared label will match
     * but if you store less similar labels, then you have variety of labels to be compared with which are also authentic.
     *
     * No need to normalize the sum of Jaccard and cosine, as comparision is also made without normalization
     *
     * Sqrt approach for how many words to be stored
     * */
    public static Set<String> altLabelVariantsSimilarityBased(FactCheckResource resource, String lang) throws IOException {
        mainLabel = resource.langLabelsMap.get(lang);
        Set<String> altLabels = resource.langAltLabelsMap.get(lang);
        similarWords = new HashSet<String>();
        wordSimilarityMap = new HashMap<String, Double>();

        getWordSimilarityScore(altLabels);
        Map<String, Double> sortedWordSimilarity = Util.sortMapByValue(wordSimilarityMap);

        int mostDiffWordsCount = (int) Math.sqrt(wordSimilarityMap.size());
        int mostRelevantWordsCount = (int) Math.sqrt(mostDiffWordsCount);

        getNWordVariants(mostDiffWordsCount, mostRelevantWordsCount, sortedWordSimilarity);
        return similarWords;
    }

    public static void getWordSimilarityScore(Set<String> altLabels) throws IOException {
        for (String altLabel : altLabels) {
            Similarity similarity = new Similarity(mainLabel, altLabel, true);
            if (wordSimilarityMap.containsKey(similarity.string2))
                continue;
            wordSimilarityMap.put(similarity.string2, (similarity.cosineSimilarity() + similarity.jaccardSimilarity()));
        }
    }

    public static void getNWordVariants(int mostDiffWordsCount, int mostRelevantWordsCount, Map<String, Double> sortedWordSimilarity) {
        int counter = 0;
        for (String label : sortedWordSimilarity.keySet()) {
            if (counter <= mostDiffWordsCount)
                similarWords.add(label);
            if (sortedWordSimilarity.keySet().size() - counter <= mostRelevantWordsCount)
                similarWords.add(label);
            counter++;
        }
    }
}
