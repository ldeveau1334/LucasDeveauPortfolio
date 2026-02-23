import java.io.*;
import java.util.*;

/**
 * this is the IO utility class
 */
public class IO {

    /**
     * Reads the table's data from a csv file
     *
     * Implement the following algorithm
     *
     * Open the csv file from the folder (corresponding to the tablename)
     *   For each line in the csv file
     *     Parse the line to get attribute values
     *     Create a new tuple with the schema of the table
     *     Set the tuple values to the attribute values
     *     Add the tuple to the table
     * Close file
     *
     * Return table
     * @param tablename the name of the table to read data for
     * @param schema the schema of the table
     * @param folder the folder where the CSV file is located
     * @return a table populated with the data from the CSV file
     */
    public static ITable readTable(String tablename, ISchema schema, String folder) {
        Table table = new Table(tablename, schema);
        try (BufferedReader br = new BufferedReader(new FileReader(folder + "/" + tablename + ".csv"))){
            String line;
            while ((line = br.readLine()) != null){
                line = line.trim();
                if (line.isEmpty()){
                    continue;
                }
                String[] values = line.split(",");
                if (values.length != schema.getAttributes().size()){
                    continue;
                }
                Tuple tuple = new Tuple(schema);
                for (int i = 0; i < values.length; i++){
                    tuple.setValue(i, values[i].trim());
                }
                table.addTuple(tuple);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return table;
    }

    /**
     * Writes the tables' data to a csv file
     *
     * Implement the following algorithm
     *
     * Open the csv file from the folder (corresponding to the tablename)
     * Clear all file content
     * For each tuple in table
     *   Write the tuple values to the file in csv format
     *
     * @param table the table whose data is to be written
     * @param folder the folder where the CSV file should be written
     */
    public static void writeTable(ITable table, String folder) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(folder + "/" + table.getName() + ".csv"))){
            for (ITuple tuple : table.getTuples()){
                Object[] vals = tuple.getValues();
                for (int i = 0; i < vals.length; i++){
                    bw.write(vals[i].toString());
                    if (i != vals.length - 1){
                        bw.write(",");
                    }
                }
                bw.newLine();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Prints the table to console (mainly used to print the output of the select query)
     *
     * Implements the following algorithm
     *
     * Print the attribute names from the schema as tab separated values
     * For each tuple in the table
     *   Print the values in tab separated format
     *
     *
     * @param table the table to print
     * @param schema the schema of the table, used to print attribute names
     */
    public static void printTable(ITable table, ISchema schema){
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < schema.getAttributes().size(); i++){
            headers.add(schema.getName(i));
        }
        System.out.println(String.join("\t", headers));
        for (ITuple tuple : table.getTuples()){
            Object[] values = tuple.getValues();
            List<String> row = new ArrayList<>();
            for (Object value : values) {
                row.add(value == null ? "" : value.toString());
            }
            System.out.println(String.join("\t", row));
        }
    }


    /**
     * Writes a tuple to a csv file
     *
     * Implements the following algorithm
     *
     * Open the csv file from the folder (corresponding to the tablename)
     * Append the tuple (as array of strings) in the csv format to the file
     *
     * @param tableName the name of the table file to which the tuple should be appended
     * @param values the values of the tuple to write
     * @param folder the folder where the table file is located
     */
    public static void writeTuple(String tableName, Object[] values, String folder) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(folder + "/" + tableName + ".csv", true))){
            for (int i = 0; i < values.length; i++){
                bw.write(values[i].toString());
                if (i != values.length - 1){
                    bw.write(",");
                }
            }
            bw.newLine();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Reads and parses the schema, creates schema objects and (empty) tables and adds them to the provided database
     * The schema is stored in a text file:
     *
     * Implements the following algorithm
     *
     * Open the schema file
     * For each line
     *   Parse the line to get the table name, attribute names and attribute types
     *   Create an attribute map of (index, att_name:att_type) pairs
     *   For each attribute
     *     Store the index and name:type pair in the map (index represents the position of attribute in the schema)
     *   Create a new schema object with this attribute map
     *   Add the schema object to the database
     *   Create a new table object with the table name and the schema object
     *   Add the table to the database
     *
     * @param schemaFileName the name of the schema file
     * @param folderName the folder where the schema file is located
     * @param db the database object to which tables and schemas will be added
     */
    public static void readSchema(String schemaFileName, String folderName, Database db) {
        try (BufferedReader br = new BufferedReader(new FileReader(folderName + "/" + schemaFileName))){
            String line;
            while ((line = br.readLine()) != null){
                String tableName = line.substring(0, line.indexOf('(')).trim();
                String attributesString = line.substring(line.indexOf('(') + 1, line.indexOf(')'));
                String[] attributeList = attributesString.split(",");
                Map<Integer, String> attrMap = new HashMap<>();
                for (int i = 0; i < attributeList.length; i++){
                    attrMap.put(i, attributeList[i].trim());
                }
                Schema schema = new Schema(attrMap);
                db.addSchema(schema);
                Table table = new Table(tableName, schema);
                db.addTable(table);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}