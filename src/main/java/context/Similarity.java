package context;

import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.Jaccard;
import info.debatty.java.stringsimilarity.Levenshtein;
import info.debatty.java.stringsimilarity.NGram;
import utils.StringFilters;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Similarity {
    public String string1;
    public String string2;
    protected Boolean applyStringFilters;

    private Cosine cosine;
    private Jaccard jaccard;
    private NGram nGram;

    public Similarity(String string1, String string2, Boolean applyStringFilters) throws IOException {
        init();

        this.string1 = string1;
        this.string2 = string2;
        this.applyStringFilters = applyStringFilters;

        if (applyStringFilters)
            applyStringFilters();
    }

    public void applyStringFilters() throws IOException {
//        to lower case
        this.string1 = StringFilters.toLower(this.string1);
        this.string2 = StringFilters.toLower(this.string2);

//        removing stop words
        this.string1 = StringFilters.removeStopWords(this.string1);
        this.string2 = StringFilters.removeStopWords(this.string2);

        this.string1 = this.string1.trim();
        this.string2 = this.string2.trim();
    }

    public void init()  {
        cosine = new Cosine();
        jaccard = new Jaccard();
        nGram = new NGram();
    }

    public double cosineSimilarity() {
        return this.cosine.similarity(this.string1, this.string2);
    }

    public double jaccardSimilarity() {
        return this.jaccard.similarity(this.string1, this.string2);
    }

    public double nGramSimilarity() {
        return this.nGram.similarity(this.string1, this.string2);
    }

    public static double jaccardSimilarity(Set<String> stringSet1, Set<String> stringSet2) {
        Set<String> intersection = new HashSet<>(stringSet1);
        Set<String> union = new HashSet<>(stringSet1);

        intersection.retainAll(stringSet2);
        union.addAll(stringSet2);

        return (double) intersection.size() / union.size();
    }

    public static double getDefBasedSimilarity(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        Set<String> word1DefSet = new HashSet<String>(Arrays.asList(WordNet.getWordDefinition(word1.toLowerCase()).split(" ")));
        Set<String> word2DefSet = new HashSet<String>(Arrays.asList(WordNet.getWordDefinition(word2.toLowerCase()).split(" ")));

        return Similarity.jaccardSimilarity(word1DefSet, word2DefSet);
    }

    /* Compare predicate label with object label based on Jaccard and cosine similarity
     * then from the best synonyms of predicate, get definition of predicate and all syns
     * compute Jaccard similarity based on definition and merge both (weight + semantic) scores
     * */
    public static double getSemanticSimilarity(String objectRelationalPhrase, String synWord, String word) throws IOException {
        Similarity similarity = new Similarity(objectRelationalPhrase, synWord, true);
        Set<String> objSet = new HashSet<String>(Arrays.asList(similarity.string1.split(" ")));
        Set<String> synSet = new HashSet<String>(Arrays.asList(similarity.string2.split(" ")));

        double weight = (Similarity.jaccardSimilarity(objSet, synSet)
                + similarity.cosineSimilarity()
                + new Levenshtein().similarity(similarity.string1, similarity.string2)) / 3;
        double definitionSimilarity = Similarity.getDefBasedSimilarity(word, similarity.string2);

        return (weight + definitionSimilarity) / 2;
    }
}
