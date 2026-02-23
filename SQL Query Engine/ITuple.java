public interface ITuple {
    void setValue(int index, Object value);
    <T> T getValue(int index);
    Object[] getValues();

    void setValues(Object[] values);
}