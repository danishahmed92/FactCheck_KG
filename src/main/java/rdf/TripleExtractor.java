package rdf;

import org.apache.jena.rdf.model.*;

public class TripleExtractor {

    private Model model;     // provide model after file has been read
    public FactCheckResource subject;
    public Property predicate;
    public FactCheckResource object;

    public TripleExtractor(Model model) {
        this.model = model;
    }

    public void parseStatements() {
        StmtIterator stmtIterator = this.model.listStatements();
        Resource subjectNode = null;
        RDFNode objectNode;

        while (stmtIterator.hasNext()) {
            Statement statement = stmtIterator.next();

            // look for starting node
            if (statement.getSubject().getURI().matches("^.*__[0-9]*$")) {
                subjectNode = statement.getSubject();
                this.predicate = statement.getPredicate();
                objectNode = statement.getObject();

                // check if object is resource and has edges, parse until you get Literal
                objectNode = getObject(statement, objectNode);
                continue;
            }
            if (statement.getObject().isResource()
                    && statement.getObject().asResource().getURI().equals(subjectNode.getURI())) {
                subjectNode = statement.getSubject();
                this.subject = new FactCheckResource(subjectNode.asResource(), model);
            }
        }
    }

    private RDFNode getObject(Statement statement, RDFNode objectNode) {
        if (objectNode.isLiteral()) {
            this.object = new FactCheckResource(statement.getSubject().asResource(), this.model);
            return objectNode;
        }

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
