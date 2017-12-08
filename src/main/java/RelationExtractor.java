import org.apache.jena.rdf.model.*;

public class RelationExtractor {

    public Model model;     // provide model after file has been read

    public RelationExtractor(Model model) {
        this.model = model;
    }

    protected void parseStatements() {
        StmtIterator stmtIterator = this.model.listStatements();
        while (stmtIterator.hasNext()) {
            Statement statement = stmtIterator.next();

            // look for blank node
            if (statement.getSubject().getURI().matches("^.*__[0-9]*$")) {
                RDFNode subjectNode = statement.getSubject();
                Property predicate = statement.getPredicate();
                RDFNode objectNode = statement.getObject();

                // check if object is resource and has edges
                objectNode = getObject(statement, objectNode);
                System.out.println(predicate.getLocalName() + "\t" +
                        objectNode.asLiteral().getValue());
            }
        }
    }

    private RDFNode getObject(Statement statement, RDFNode objectNode) {
        if (objectNode.isLiteral())
            return objectNode;

        if (objectNode.isResource()) {
            // parse all statements again
            // if object URI becomes subject URI, it will either have literal or resource
            // if it's a literal, return object
            // else call this function again

            StmtIterator stmtIterator = this.model.listStatements();
            while (stmtIterator.hasNext()) {
                Statement stmt = stmtIterator.next();
                if (stmt.getSubject().getURI().equals(objectNode.asResource().getURI())) {
                    RDFNode objNode = stmt.getObject();

                    return getObject(stmt, objNode);
                }
            }
        }
        return null;
    }
}
