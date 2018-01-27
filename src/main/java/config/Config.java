package config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Config {
    public static ArrayList<String> stopWordsArray = new ArrayList<>();

    public static void loadStopWords() throws IOException {
        BufferedReader in = new BufferedReader(new FileReader("stop_words.txt"));
        String word;
        while ((word = in.readLine()) != null) {
            stopWordsArray.add(word.trim());
        }
    }
}
