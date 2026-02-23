import java.util.Map;

/**
 * the schema is stored as a map of (index, name:type) pairs
 */
public class Schema implements ISchema {

    private Map<Integer, String> attributes;

    /**
     * constructor
     * @param attributes a map where each key is an attribute index and each value is a "name:type" string
     */
    public Schema(Map<Integer, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * getter
     * @return a map of attribute indices to "name:type" strings
     */
    @Override
    public Map<Integer, String> getAttributes() {
        return attributes;
    }

    /**
     * splits the name:type to return the attribute name
     * @param index the index of the attribute
     * @return the name of the attribute at the specified index
     */
    @Override
    public String getName(int index) {
        String attribute = attributes.get(index);
        return attribute.split(":")[0];
    }

    /**
     * splits the name:type to return the attribute type
     * @param index the index of the attribute
     * @return the type of the attribute at the specified index (e.g., "Integer", "String", "Double")
     */
    @Override
    public String getType(int index) {
        String attribute = attributes.get(index);
        return attribute.split(":")[1];
    }
}
