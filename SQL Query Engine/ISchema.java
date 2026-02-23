import java.util.Map;

public interface ISchema {
    Map<Integer, String> getAttributes();
    String getName(int index);
    String getType(int index);
}