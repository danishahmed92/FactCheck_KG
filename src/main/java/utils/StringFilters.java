package utils;

import config.Config;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;

public class StringFilters {
    public static String toLower(String string) {
        return string.toLowerCase();
    }

    public static String removeSpecialCharacters(String string) {
        return string.replaceAll("[^\\p{L}\\p{Nd}]+", "");
    }

    public static String removeStopWords(String string) throws IOException {
        StandardTokenizer stdToken = new StandardTokenizer();
        stdToken.setReader(new StringReader(string));

//        Lucene Filtering
        TokenStream tokenStream;
        tokenStream = new StopFilter(new ASCIIFoldingFilter(stdToken), EnglishAnalyzer.getDefaultStopSet());
        tokenStream.reset();

//        filtering from stop_words.txt file
        CharTermAttribute token = tokenStream.getAttribute(CharTermAttribute.class);
        String filteredString = "";
        while (tokenStream.incrementToken()) {
            if (Config.stopWordsArray.contains(token.toString()))
                continue;
            filteredString = filteredString + token.toString() + " ";
        }
        tokenStream.close();

        return filteredString;
    }
}
