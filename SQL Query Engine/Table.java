import java.util.List;

/**
 * A table has a name, a schema and a list of tuples
 */
public class Table implements ITable {
    private String name;
    private List<ITuple> tuples;
    private ISchema schema;

    /**
     * constructor
     * @param name the name of the table
     * @param schema the schema defining the structure of tuples in the table
     */
    public Table(String name, ISchema schema) {
        this.name = name;
        this.schema = schema;
        this.tuples = new java.util.ArrayList<>();
    }

    /**
     * Returns the table name
     * @return the table name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * adds a tuple to the table
     * @param tuple the tuple to be added to the table
     */
    @Override
    public void addTuple(ITuple tuple) {
        tuples.add(tuple);
    }

    /**
     * Returns the list of tuples
     * @return a list of tuples representing the table's data
     */
    @Override
    public List<ITuple> getTuples() {
        return tuples;
    }

    /**
     * Returns the table schema
     * @return the schema describing the structure of the table
     */
    @Override
    public ISchema getSchema() {
        return schema;
    }
}
