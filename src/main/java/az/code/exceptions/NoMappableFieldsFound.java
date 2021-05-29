package az.code.exceptions;

public class NoMappableFieldsFound extends Throwable {
    public NoMappableFieldsFound() {
        super("This class does not have any mappable fields.");
    }
}
