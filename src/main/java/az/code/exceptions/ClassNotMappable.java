package az.code.exceptions;

public class ClassNotMappable extends Exception {
    public ClassNotMappable() {
        super("This class is not mappable because it is not annotated with @Entity annotation.");
    }
}
