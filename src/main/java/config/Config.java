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

    public String wordNetDict;

    public String dbHost;
    public String dbPort;
    public String database;
    public String dbUser;
    public String dbPassword;

    public String trainDataPath;
    public String testDataPath;

    /**
     * reading configuration from factcheck.ini
     * and set variables that are gloably required
     * @throws IOException
     */
    public Config() throws IOException {
        configIni = new Ini(new File(Constants.RESOURCE_DIRECTORY + Constants.CONFIG_FILE));

        stopWordsFile = configIni.get("data", "stopWordsFile");
        wordNetDict = configIni.get("data", "wordNetDict");
        trainDataPath = configIni.get("data", "train");
        testDataPath = configIni.get("data", "test");

        dbHost = configIni.get("mysql", "dbHost");
        dbPort = configIni.get("mysql", "port");
        database = configIni.get("mysql", "database");
        dbUser = configIni.get("mysql", "dbUser");
        dbPassword = configIni.get("mysql", "dbPassword");
    }

    /**
     * load stopwords from fileName mentioned in config ini
     * @throws IOException
     */
    public void loadStopWords() throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(stopWordsFile));
        String word;
        while ((word = in.readLine()) != null) {
            stopWordsArray.add(word.trim());
        }
    }
}
