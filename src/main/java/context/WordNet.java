package context;

import config.Config;
import rita.RiWordNet;
import rita.wordnet.jwnl.JWNLException;
import rita.wordnet.jwnl.wndata.IndexWord;
import rita.wordnet.jwnl.wndata.POS;
import rita.wordnet.jwnl.wndata.Synset;
import rita.wordnet.jwnl.wndata.Word;

import java.util.ArrayList;
import java.util.List;

public class WordNet {

    public static RiWordNet wordNet = new RiWordNet(Config.configInstance.wordNetDict);

    public static List<String> getNTopSynonyms(String word, int n) throws JWNLException {
        List<String> synonyms = new ArrayList<>();
        POS pos = getPartOfSpeech(word);

        IndexWord indexWord = wordNet.jwnlDict.getIndexWord(pos, word);
        if (indexWord != null) {
            Synset[] synsets = indexWord.getSenses();
            int bound = Math.min(n, synsets.length);
            for (int i = 0; i < bound; i++) {
                for (Word synsetWord : synsets[i].getWords()) {
                    String lemma = synsetWord.getLemma();
                    if (!lemma.equals(word) && !lemma.contains(" "))
                        synonyms.add(lemma);
                }
            }
        }
        return synonyms;
    }

    public static POS getPartOfSpeech(String word) {
        String partOfSpeech = wordNet.getBestPos(word);
        POS pos = POS.VERB;
        switch (partOfSpeech) {
            case "n":
                return POS.NOUN;
            case "v":
                return POS.VERB;
            case "a":
                return POS.ADJECTIVE;
            case "r":
                return POS.ADVERB;
        }
        return pos;
    }

    public static String getWordDefinition(String word) {
        String partOfSpeech = wordNet.getBestPos(word);
        if (partOfSpeech == null)
            return "";
        String gloss = wordNet.getGloss(word, partOfSpeech);
        return gloss.toLowerCase();
    }
}
