import java.io.IOException;

import org.hibernate.Session;

import test.FactChecker;
import utils.HibernateUtils;

public class Main {

	/*
	 * Configuration of Hibernate and test with DB
	 *
	 * For Training and Feature Extractions, execute main of RulesExtraction.Java
	 *
	 */
	public static void main(String[] args) throws IOException {
		Session session = HibernateUtils.getSessionFactory().openSession();
		session.beginTransaction();

		/*
		 * TriplePatternMap patternMap = new TriplePatternMap("dem2", "dem2", "dem2");
		 * session.save(patternMap);
		 */
		// session.update(student);//No need to update manually as it will be updated
		// automatically on transaction close.
		FactChecker factChecker = new FactChecker();
		String[] triple = { "http://dbpedia.org/resource/Henry_Dunant", "http://dbpedia.org/ontology/award",
				"http://dbpedia.org/resource/Nobel_Peace_Prize" };
		double cfVal = factChecker.getFactCFVal(triple, session);
		System.out.println("Total confidence value: "+cfVal);
		session.getTransaction().commit();
		session.close();
	}
}
