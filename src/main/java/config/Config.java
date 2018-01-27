package config;

import org.ini4j.Ini;
import utils.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Config {
    public static Config configInstance;

    static {
        try {
            configInstance = new Config();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Ini configIni;
    public ArrayList<String> stopWordsArray = new ArrayList<>();
    private String stopWordsFile;

    private String trainDataPath;
    private String testDataPath;

    public Config() throws IOException {
        configIni = new Ini(new File(Constants.RESOURCE_DIRECTORY + Constants.CONFIG_FILE));

        stopWordsFile = configIni.get("data", "stopWordsFile");
        trainDataPath = configIni.get("data", "train");
        testDataPath = configIni.get("data", "test");
    }

    public void loadStopWords() throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(stopWordsFile));
        String word;
        while ((word = in.readLine()) != null) {
            stopWordsArray.add(word.trim());
        }
    }
}
