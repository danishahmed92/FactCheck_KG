import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import config.Config;
import ml.ExtractedFeatures;
import org.apache.jena.rdf.model.Property;
import org.hibernate.Session;

import provider.PersistenceProvider;
import rdf.FactCheckResource;
import rdf.TripleExtractor;
import rita.wordnet.jwnl.JWNLException;
import test.FactChecker;
import utils.HibernateUtils;

import static ml.ExtractedFeatures.subjectUri;
import static ml.RulesExtraction.getTriples;

public class Main {

	public static Session session = HibernateUtils.getSessionFactory().openSession();
	/*
	 * Configuration of Hibernate and test with DB
	 *
	 * For Training and Feature Extractions, execute main of RulesExtraction.Java
	 *
	 */
	public static void main(String[] args) throws IOException {
		session.beginTransaction();

		try {
			filesCrawler(Paths.get(Config.configInstance.testDataPath + "/wrong/range/award"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		session.getTransaction().commit();
		session.close();
	}

	/**
	 *
	 * @param path - path to crawl accross rdf files for training
	 * @throws IOException
	 */
	public static void filesCrawler(final Path path) throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					System.out.println(file.getFileName().toString());
					try {
						String fileName = String.valueOf(file.getFileName());
						TripleExtractor tripleExtractor = getTriples(String.valueOf(file.toAbsolutePath()));

						String subjectUri = FactCheckResource.getDBpediaUri(tripleExtractor.subject);
						String predicateUri = tripleExtractor.predicate.getURI();
						String objectUri = FactCheckResource.getDBpediaUri(tripleExtractor.object);

						FactChecker factChecker = new FactChecker();
						String[] triple = { subjectUri, predicateUri, objectUri };
						double cfVal = factChecker.getFactCFVal(triple, session);

						String fileContent = fileName + "\t"
                                + subjectUri + "\t"
                                + predicateUri + "\t"
                                + objectUri + "\t"
                                + cfVal
                                + "\n";
						writeToFile("wrong_range_award.txt", fileContent);

						System.out.println(fileName + "\t"
                                + subjectUri + "\t"
                                + predicateUri + "\t"
                                + objectUri + "\t"
                                + cfVal);
					} catch (IOException ignore) {
						// don't index files that can't be read.
						ignore.printStackTrace();
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	public static void writeToFile(String fileName, String content) throws IOException {
        File writeFile = new File(fileName);

        FileWriter fileWriter = new FileWriter(writeFile, true);
        BufferedWriter bufferFileWriter  = new BufferedWriter(fileWriter);
        fileWriter.append(content);
        bufferFileWriter.close();
    }
}
