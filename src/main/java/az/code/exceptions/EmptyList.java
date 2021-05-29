package az.code.exceptions;

public class EmptyList extends Exception{
    public EmptyList() {
        super("Given list can't be empty.");
    }
}
