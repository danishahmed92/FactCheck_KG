package utils;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

/**
 * Class to provide hibernate utils such as static Hibernate SessionFactory
 * 
 * @author Nikit
 *
 */
@SuppressWarnings("deprecation")
public class HibernateUtils {

	private static final SessionFactory sessionFactory;

	static {
		try {
			sessionFactory = new AnnotationConfiguration().configure().buildSessionFactory();

		} catch (Throwable ex) {
			System.err.println("Session Factory could not be created." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	/**
	 * Method to return session factory
	 * 
	 * @return - Hibernate Session Factory
	 */
	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}

}
