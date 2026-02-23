import java.util.List;

public interface ITable {
    String getName();
    void addTuple(ITuple tuple);
    List<ITuple> getTuples();
    ISchema getSchema();
}