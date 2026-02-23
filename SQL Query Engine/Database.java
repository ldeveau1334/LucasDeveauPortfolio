import java.io.*;
import java.util.*;

/**
 * The main database class
 * Database as a list of tables, list of schemas and a folder name where the database is stored
 * Database is stored (on the disk) in the form of three csv files and schema text file
 */
class Database {
    private List<ITable> tables;
    private List<ISchema> schemas;
    private String folderName;


    /**
     * Constructor
     * Creates the empty tables and schema lists
     * Reads the schema file to add schemas to the database
     * Populates the database table (with the data read from the csv files)
     * @param folderName the name of the folder containing the database files
     * @param schemaFileName the name of the schema file to read
     */

    public Database(String folderName, String schemaFileName) {
        this.folderName = folderName;
        this.tables = new ArrayList<>();
        this.schemas = new ArrayList<>();

        IO.readSchema(schemaFileName, folderName, this);
        populateDB();
    }
    /**
     * Adds a table to the database
     * @param table the table to be added
     */
    public void addTable(ITable table) {
        tables.add(table);
    }

    /**
     * Adds a table schema to the database
     * @param schema the schema to be added
     */
    public void addSchema(ISchema schema) {
        schemas.add(schema);
    }

    /**
     * Return the list of tables in the database
     * @return a list of all tables currently in the database
     */
    public List<ITable> getTables() {
        return tables;
    }

    /**
     * Returns the list of schemas in the database
     * @return a list of all schemas associated with tables in the database
     */
    public List<ISchema> getSchemas() {
        return schemas;
    }

    /**
     * The list of tables in the database is initialized with empty tables in the constructor
     * An empty table has a name and an empty list of tuples
     * This method sets the empty table in the list to the one provided as a parameter
     * @param table the updated table to replace the existing one with the same name
     */
    public void updateTable(ITable table) {
        for (int i = 0; i < tables.size(); i++){
            if (tables.get(i).getName().equals(table.getName())){
                tables.set(i, table);
                return;
            }
        }
    }

    /**
     * Populates the database
     *
     * Implements the following algorithm
     *
     * For each table in the db (tables are initially empty)
     *   Get the table's data from the csv file (by calling the read table method)
     *   Update the table (by calling the udpate table method)
     */

    public void populateDB() {
        List<ITable> newTables = new ArrayList<>();
        for (ITable table : tables){
            ITable filledTable = IO.readTable(table.getName(), table.getSchema(), folderName);
            newTables.add(filledTable);
        }
        this.tables = newTables;
    }

    /**
     * Insert data into a table based upon the insert query
     * If the query is invalid throws an InvalidQueryException
     *
     * Implements the following algorithm
     *
     * Parse the insert into clause to get the table name, attribute name(s) and value(s)
     * If the query in not valid
     *   Throw an invalid query exception
     *   Exit
     * Create a new tuple with the schema of the table
     * Set the tuple values to the values from the query
     * Open the file corresponding to the table name
     * Append the tuple values (as comma separated values) to the end of the file
     *
     * @param query the SQL INSERT query string
     * @throws InvalidQueryException if the query is malformed or the table cannot be found
     */
    public void insertData(String query) throws InvalidQueryException {
        try {
            query = query.trim().replaceAll("\\s+", " ");
            if (!query.toLowerCase().startsWith("insert into")){
                throw new InvalidQueryException("Invalid INSERT syntax.");
            }
            int valuesIndex = query.toLowerCase().indexOf("values");
            if (valuesIndex == -1){
                throw new InvalidQueryException("Missing VALUES keyword.");
            }
            String tablePart = query.substring("insert into".length(), valuesIndex).trim();
            int parenIndex = tablePart.indexOf("(");
            String tableName = (parenIndex != -1) ? tablePart.substring(0, parenIndex).trim() : tablePart;
            String valuesPart = query.substring(valuesIndex + "values".length()).trim();
            if (!valuesPart.startsWith("(") || !valuesPart.endsWith(")")){
                throw new InvalidQueryException("Invalid VALUES format: must be enclosed in parentheses.");
            }
            valuesPart = valuesPart.substring(1, valuesPart.length() - 1).trim();
            String[] values = valuesPart.split(",");
            ITable table = null;
            for (ITable t : tables){
                if (t.getName().equalsIgnoreCase(tableName)){
                    table = t;
                    break;
                }
            }
            if (table == null){
                throw new InvalidQueryException("Table not found: " + tableName);
            }
            ISchema schema = table.getSchema();
            int expectedSize = schema.getAttributes().size();
            if (values.length != expectedSize){
                throw new InvalidQueryException("Invalid number of values: expected " + expectedSize + ", got " + values.length);
            }
            Tuple newTuple = new Tuple(schema);
            for (int i = 0; i < expectedSize; i++){
                String cleaned = values[i].trim();
                if (cleaned.startsWith("'") && cleaned.endsWith("'")){
                    cleaned = cleaned.substring(1, cleaned.length() - 1);
                }
                newTuple.setValue(i, cleaned);
            }
            table.addTuple(newTuple);
            IO.writeTable(table, folderName);
        //} catch (InvalidQueryException e){
        //    throw e;
        //} catch (NumberFormatException e){
        //    throw new InvalidQueryException("Type mismatch: " + e.getMessage());
        } catch (Exception e){
            throw new InvalidQueryException("Invalid insert operation: " + e.getMessage());
        }
    }


    /**
     * Selects data from a table (and returns it in the form of a results table)
     * If the query in not valid, throws an InvalidQueryException
     *
     * A query is valid if
     *
     * 1.	It has a select clause (select keyword followed by at least one attribute name)
     * 2.	It has a from clause (from keyword followed by a table name)
     * 3.	All the attribute names in the select clause are in the schema
     * 4.	The table name in the from clause is in the schema
     * 5.	All the attribute names in the where clause (if present) are in the schema
     * 6.	The attribute name in the order by clause (if present) is in the schema
     *
     * Implements the following algorithm
     *
     * Parse the query to get the select, from, where and order by clauses and the attribute and table names and condition
     * If the query is not valid
     *   Throw an invalid query exception
     *   Exit
     * Create a new results schema based with the attributes from the select clause
     * Create a new result table
     * For each tuple in the table
     *   If the tuple matches the where clause condition(s)
     *     Create a new results tuple using the result schema
     *     Set the results tuple values to the current tuple corresponding values
     *     Add the results tuple to the result table
     * Return results table
     *
     *
     * @param query the SQL SELECT query string
     * @return a table containing the results of the query
     * @throws InvalidQueryException if the query is invalid or processing fails
     */
    public ITable selectData(String query) throws InvalidQueryException {
        try {
            String normalized = query.trim().replaceAll("\\s+", " ");
            String lower = normalized.toLowerCase();
            if (!lower.contains("select") || !lower.contains("from")){
                throw new InvalidQueryException("Query must contain SELECT and FROM.");
            }
            int fromIndex = lower.indexOf("from");
            String selectPart = normalized.substring(6, fromIndex).trim();
            String rest = normalized.substring(fromIndex + 4).trim();
            String tableName;
            String whereClause = null;
            String orderByAttr = null;
            if (rest.toLowerCase().contains("where")){
                int whereIndex = rest.toLowerCase().indexOf("where");
                tableName = rest.substring(0, whereIndex).trim();
                String wherePart = rest.substring(whereIndex + 5).trim();
                if (wherePart.toLowerCase().contains("order by")){
                    int orderByIndex = wherePart.toLowerCase().indexOf("order by");
                    whereClause = wherePart.substring(0, orderByIndex).trim();
                    orderByAttr = wherePart.substring(orderByIndex + 8).trim();
                }else{
                    whereClause = wherePart.trim();
                }
            }else if (rest.toLowerCase().contains("order by")){
                int orderByIndex = rest.toLowerCase().indexOf("order by");
                tableName = rest.substring(0, orderByIndex).trim();
                orderByAttr = rest.substring(orderByIndex + 8).trim();
            }else{
                tableName = rest.trim();
            }
            ITable targetTable = null;
            ISchema targetSchema = null;
            for (ITable t : tables) {
                if (t.getName().equalsIgnoreCase(tableName)){
                    targetTable = t;
                    targetSchema = t.getSchema();
                    break;
                }
            }
            if (targetTable == null || targetSchema == null){
                throw new InvalidQueryException("Table not found: " + tableName);
            }
            Map<Integer, String> schemaMap = ((Schema) targetSchema).getAttributes();
            List<String> selectAttrs = new ArrayList<>();
            boolean selectAll = selectPart.equals("*");
            if (selectAll){
                for (String s : schemaMap.values()){
                    selectAttrs.add(s.split(":")[0]);
                }
            }else{
                for (String attr : selectPart.split(",")){
                    String trimmed = attr.trim();
                    boolean found = false;
                    for (String s : schemaMap.values()){
                        if (s.split(":")[0].equalsIgnoreCase(trimmed)){
                            found = true;
                            break;
                        }
                    }
                    if (!found){
                        throw new InvalidQueryException("Attribute in SELECT not in schema: " + trimmed);
                    }
                    selectAttrs.add(trimmed);
                }
            }
            String whereAttr = null;
            String whereValue = null;
            if (whereClause != null) {
                String[] parts = whereClause.split("=");
                if (parts.length != 2){
                    throw new InvalidQueryException("Malformed WHERE clause.");
                }
                whereAttr = parts[0].trim();
                whereValue = parts[1].trim();
                if (whereValue.startsWith("'") && whereValue.endsWith("'")){
                    whereValue = whereValue.substring(1, whereValue.length() - 1);
                }
                boolean found = false;
                for (String s : schemaMap.values()){
                    if (s.split(":")[0].equalsIgnoreCase(whereAttr)){
                        found = true;
                        break;
                    }
                }
                if (!found){
                    throw new InvalidQueryException("WHERE attribute not found: " + whereAttr);
                }
            }
            if (orderByAttr != null){
                boolean found = false;
                for (String s : schemaMap.values()){
                    if (s.split(":")[0].equalsIgnoreCase(orderByAttr.trim())){
                        found = true;
                        break;
                    }
                }
                if (!found){
                    throw new InvalidQueryException("ORDER BY attribute not found: " + orderByAttr);
                }
            }
            Map<Integer, String> resultSchemaMap = new HashMap<>();
            int col = 0;
            for (Map.Entry<Integer, String> entry : schemaMap.entrySet()){
                String name = entry.getValue().split(":")[0];
                String type = entry.getValue().split(":")[1];
                if (selectAttrs.contains(name)){
                    resultSchemaMap.put(col++, name + ":" + type);
                }
            }
            ISchema resultSchema = new Schema(resultSchemaMap);
            ITable resultTable = new Table("Result", resultSchema);
            int whereIndex = -1;
            if (whereAttr != null){
                for (Map.Entry<Integer, String> entry : schemaMap.entrySet()){
                    if (entry.getValue().split(":")[0].equalsIgnoreCase(whereAttr)){
                        whereIndex = entry.getKey();
                        break;
                    }
                }
            }
            for (ITuple tup : targetTable.getTuples()){
                boolean matches = true;
                if (whereIndex != -1){
                    Object val = tup.getValue(whereIndex);
                    matches = (val != null && val.toString().equalsIgnoreCase(whereValue));
                }
                if (matches){
                    ITuple newTup = new Tuple(resultSchema);
                    int newIndex = 0;
                    for (int i = 0; i < schemaMap.size(); i++){
                        String name = schemaMap.get(i).split(":")[0];
                        if (selectAttrs.contains(name)) {
                            newTup.setValue(newIndex++, tup.getValue(i));
                        }
                    }
                    resultTable.addTuple(newTup);
                }
            }
            if (orderByAttr != null){
                int orderIndex = -1;
                for (Map.Entry<Integer, String> entry : resultSchemaMap.entrySet()){
                    if (entry.getValue().split(":")[0].equalsIgnoreCase(orderByAttr.trim())) {
                        orderIndex = entry.getKey();
                        break;
                    }
                }
                int finalOrderIndex = orderIndex;
                resultTable.getTuples().sort((a, b) -> ((Comparable) a.getValue(finalOrderIndex)).compareTo(b.getValue(finalOrderIndex)));
            }
            removeDuplicateHeaderRows(resultTable);
            return resultTable;
        } catch (InvalidQueryException e){
            throw e;
        } catch (Exception e){
            throw new InvalidQueryException("Invalid select operation: " + e.getMessage());
        }
    }



    /**
     * Delete data from a table
     * If the query in not valid, throws an InvalidQueryException
     *
     * Implements the following algorithm
     *
     * Parse the query to get the from and where clauses
     * Parse the from clause to get the table name
     * If the query in not valid
     *   Throw an invalid query exception
     *   Exit
     * If where clause is not empty
     *   Parse the where clause to get the the condition
     *   For each tuple in the table
     *     If the where clause condition is true
     *       Remove the tuple from the table
     * Else
     *   For each tuple in the table
     *     Remove the tuple from the table
     * Write the table to the file
     *
     * @param query the SQL DELETE query string
     * @throws InvalidQueryException if the query is malformed or a table/column cannot be found
     */
    public void deleteData(String query) throws InvalidQueryException {
        try{
            query = query.trim();
            if (!query.toLowerCase().startsWith("delete")){
                throw new InvalidQueryException("Invalid DELETE query syntax.");
            }
            String fromPart = query.substring(query.toLowerCase().indexOf("from") + 4).trim();
            String wherePart = "";
            if (fromPart.toLowerCase().contains("where")){
                wherePart = fromPart.substring(fromPart.toLowerCase().indexOf("where") + 5).trim();
                fromPart = fromPart.substring(0, fromPart.toLowerCase().indexOf("where")).trim();
            }
            String tableName = fromPart.trim();
            ITable table = null;
            for (ITable t : tables){
                if (t.getName().equalsIgnoreCase(tableName)){
                    table = t;
                    break;
                }
            }
            if (table == null){
                throw new InvalidQueryException("Table not found.");
            }
            List<ITuple> toRemove = new ArrayList<>();
            ISchema schema = table.getSchema();
            if (!wherePart.isEmpty()){
                String[] conditionParts = wherePart.split("=");
                String left = conditionParts[0].trim();
                String right = conditionParts[1].trim();
                if (right.startsWith("'") && right.endsWith("'")){
                    right = right.substring(1, right.length() - 1);
                }
                int leftIndex = -1;
                for (Map.Entry<Integer, String> entry : schema.getAttributes().entrySet()){
                    if (schema.getName(entry.getKey()).equals(left)){
                        leftIndex = entry.getKey();
                        break;
                    }
                }
                for (ITuple tuple : table.getTuples()){
                    if (tuple.getValue(leftIndex).toString().equals(right)){
                        toRemove.add(tuple);
                    }
                }
            }else{
                toRemove.addAll(table.getTuples());
            }
            table.getTuples().removeAll(toRemove);
            IO.writeTable(table, folderName);
        } catch (Exception e){
            throw new InvalidQueryException("Invalid delete operation: " + e.getMessage());
        }
    }

    /**
     * helper function that removes duplicate header rows from a table if they exist. (this was a problem for me)
     *
     * @param table the table to clean
     */
    private void removeDuplicateHeaderRows(ITable table) {
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