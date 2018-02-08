import static ml.RulesExtraction.getTriples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.hibernate.Session;

import config.Config;
import rdf.FactCheckResource;
import rdf.TripleExtractor;
import test.FactChecker;
import utils.HibernateUtils;

public class Main {

	public static Session session = HibernateUtils.getSessionFactory().openSession();

	public static File writeFileDR;
	public static FileWriter fileWriterDR;
	public static BufferedWriter bufferFileWriterDR;

	static {
		writeFileDR = new File("wrong_range_leader_det.tsv");
		try {
			fileWriterDR = new FileWriter(writeFileDR, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		bufferFileWriterDR = new BufferedWriter(fileWriterDR);
	}

	/*
	 * Configuration of Hibernate and test with DB
	 *
	 * For Training and Feature Extractions, execute main of RulesExtraction.Java
	 *
	 */
	public static void main(String[] args) throws IOException {
		session.beginTransaction();

		try {
			filesCrawler(Paths.get(Config.configInstance.testDataPath + "/wrong/range/leader"));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Processing finished.");
			bufferFileWriterDR.close();
			session.getTransaction().commit();
			session.close();
		}
	}

	/**
	 *
	 * @param path
	 *            - path to crawl accross rdf files for training
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
						// Check for empty content
						boolean cond1 = subjectUri.trim().length() == 0;
						boolean cond2 = predicateUri.trim().length() == 0;
						boolean cond3 = objectUri.trim().length() == 0;
						if (cond1 || cond2 || cond3) {
							return FileVisitResult.CONTINUE;
						}
						FactChecker factChecker = new FactChecker();
						String[] triple = { subjectUri, predicateUri, objectUri };
						double cfVal = factChecker.getFactCFValAdv(triple, session);

						String fileContent = fileName + "\t" + subjectUri + "\t" + predicateUri + "\t" + objectUri
								+ "\t" + cfVal + "\n";
						writeToFile("wrong_range_leader.txt", fileContent);
						List<String> formatedRes = factChecker.getFormattedResult();
						String detContent = fileName + "\t" + subjectUri + "\t" + predicateUri + "\t" + objectUri
								+ "\t" + formatedRes.get(0) + "\t" + formatedRes.get(1) + "\t" + formatedRes.get(2) + "\t" + formatedRes.get(3) + "\n";
						writeDetRes(detContent);

						System.out.println(
								fileName + "\t" + subjectUri + "\t" + predicateUri + "\t" + objectUri + "\t" + cfVal);
					} catch (IOException ignore) {
						// don't index files that can't be read.
						ignore.printStackTrace();
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	public static void writeDetRes(String content) throws IOException {
		fileWriterDR.append(content);
	}

	public static void writeToFile(String fileName, String content) throws IOException {
		File writeFile = new File(fileName);

		FileWriter fileWriter = new FileWriter(writeFile, true);
		BufferedWriter bufferFileWriter = new BufferedWriter(fileWriter);
		fileWriter.append(content);
		bufferFileWriter.close();
	}
}
