package az.code.exceptions;

public class FieldlessClass extends Exception {
    public FieldlessClass() {
        super("This class does not have any fields.");
    }
}
