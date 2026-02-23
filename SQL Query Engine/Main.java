import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

    }

    /**
     * Runs the given query on the database
     *
     * Implements the following algorithm
     *
     * Determine the type of query (from select, insert or delete)
     * If select query
     *   Select data
     *   Print results
     * Else if insert query
     *   Insert data
     * Else if delete is given
     *   Delete data
     *
     * @param query the inputted query in a string
     * @param db the name of the database for the query
     */
    public static void runQuery(String query, Database db) {
        try {
            query = query.trim().toLowerCase();
            System.out.println("Current query we are inserting: " + query);
            if (query.startsWith("select")){
                ITable result = db.selectData(query);
                removeDuplicateHeaderRows(result);
                IO.printTable(result, result.getSchema());
            }else if (query.startsWith("insert")){
                db.insertData(query);
            }else if (query.startsWith("delete")){
                db.deleteData(query);
            }else{
                System.out.println("Unknown query type.");
            }

        } catch (InvalidQueryException e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * helper function that removes duplicate header rows from a table if they exist. (this was a problem for me)
     *
     * @param table the table to clean
     */
    private static void removeDuplicateHeaderRows(ITable table) {
        if (table == null || table.getTuples().isEmpty()){
            return;
        }
        ISchema schema = table.getSchema();
        StringBuilder headerSignature = new StringBuilder();
        for (int i = 0; i < schema.getAttributes().size(); i++){
            headerSignature.append(schema.getName(i));
            if (i != schema.getAttributes().size() - 1){
                headerSignature.append(",");
            }
        }
        String headerString = headerSignature.toString().toLowerCase();
        List<ITuple> toRemove = new ArrayList<>();
        for (ITuple tuple : table.getTuples()){
            Object[] values = tuple.getValues();
            StringBuilder tupleSignature = new StringBuilder();
            for (int i = 0; i < values.length; i++){
                tupleSignature.append(values[i] == null ? "" : values[i].toString());
                if (i != values.length - 1){
                    tupleSignature.append(",");
                }
            }
            String tupleString = tupleSignature.toString().toLowerCase();
            if (tupleString.equals(headerString)){
                toRemove.add(tuple);
            }
        }
        table.getTuples().removeAll(toRemove);
    }
}
