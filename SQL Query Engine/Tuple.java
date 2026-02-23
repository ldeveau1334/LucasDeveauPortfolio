import java.util.Map;

/**
 * A tuple is an ordered collection of Objects and their associated types (Integer, Double or String)
 * Objects are stored in an array while types are stored in a map of (index, type)
 *
 */
public class Tuple implements ITuple {
    private Object[] values;
    private Map<Integer, Class<?>> typeMap;

    /**
     * The constructor receives a schema and creates the object array and typemap (representing the tuple)
     * @param schema the schema defining the structure and types of the tuple's attributes
     */
    public Tuple(ISchema schema) {
        int size = schema.getAttributes().size();
        values = new Object[size];
        typeMap = new java.util.HashMap<>();
        for (Map.Entry<Integer, String> entry : schema.getAttributes().entrySet()){
            int index = entry.getKey();
            String type = entry.getValue().split(":")[1];
            if (type.equals("Integer")){
                typeMap.put(index, Integer.class);
            }else if (type.equals("Double")){
                typeMap.put(index, Double.class);
            }else{
                typeMap.put(index, String.class);
            }
        }
    }

    /**
     * Stores the value at the given index in the (tuple) object
     * The value is converted from the object to its actual class from the typemap
     * @param index the index where the value should be stored
     * @param value the value to store at the specified index
     */
    @Override
    public void setValue(int index, Object value) {
        Class<?> type = typeMap.get(index);
        if (type == Integer.class){
            values[index] = Integer.parseInt(value.toString());
        }else if (type == Double.class){
            values[index] = Double.parseDouble(value.toString());
        }else{
            values[index] = value.toString();
        }
    }

    /**
     * Returns the value at a given index from the tuple object
     * @param index the index of the value to retrieve
     * @return the value at the specified index, cast to the appropriate type
     * @param <T> the expected type of the returned value
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(int index) {
        return (T) values[index];
    }

    /**
     * Returns the tuple as an array of Objects
     * @return an array of Objects representing the tuple's values
     */
    @Override
    public Object[] getValues() {
        return values;
    }

    /**
     * Sets the tuple values to the provided ones
     * @param values an array of values to set in the tuple
     */
    @Override
    public void setValues(Object[] values) {
        for (int i = 0; i < values.length; i++){
            setValue(i, values[i]);
        }
    }
}