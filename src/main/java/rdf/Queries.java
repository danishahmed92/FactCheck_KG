package rdf;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class Queries {
    public static final String ALL_PREDICATES_OF_SUBJECT = "SELECT DISTINCT ?predicate WHERE {" +
            "   %s ?predicate [] " +
            "}";

    public static List<String> execute(String queryString) {
        Query query = QueryFactory.create(queryString);
        List<String> result = new ArrayList<>();

        // Remote execution.
        try ( QueryExecution qExec = QueryExecutionFactory.sparqlService(Constants.SPARQL_END_POINT, query) ) {
            // Set the DBpedia specific timeout.
            ((QueryEngineHTTP)qExec).addParam("timeout", "10000") ;

            ResultSet rs = qExec.execSelect();
            while ( rs.hasNext() ) {
                QuerySolution soln = rs.nextSolution();

                RDFNode predicate = soln.get("predicate");
                result.add(predicate.toString());
            }
            qExec.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
