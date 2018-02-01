import java.io.IOException;

import org.hibernate.Session;

import bean.TriplePatternMap;
import utils.HibernateUtils;

public class Main {

    public static void main(String[] args) throws IOException {
    	Session session = HibernateUtils.getSessionFactory().openSession();
        session.beginTransaction();
 
        TriplePatternMap patternMap = new TriplePatternMap("dem2", "dem2", "dem2");
        session.save(patternMap);
        //session.update(student);//No need to update manually as it will be updated automatically on transaction close.
        session.getTransaction().commit();
        session.close();
    }
}
