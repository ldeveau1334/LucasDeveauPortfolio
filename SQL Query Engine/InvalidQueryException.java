/**
 * Custom exception
 */
public class InvalidQueryException extends Exception {
    public InvalidQueryException() {
        super();
    }

    public InvalidQueryException(String message) {
        super(message);
    }
}
