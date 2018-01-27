package context;

import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.Jaccard;
import info.debatty.java.stringsimilarity.NGram;
import utils.StringFilters;

import java.io.IOException;

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
}
