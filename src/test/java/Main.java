import az.code.annotations.Column;
import az.code.annotations.Entity;
import az.code.annotations.Id;
import az.code.exceptions.NoMappableFieldsFound;
import az.code.mapper.Manager;

public class Main {
    public static void main(String[] args) throws NoMappableFieldsFound, Exception {
        Test test = new Test();
        test.id = 1;
        Manager.merge(test);
    }
}

@Entity(name = "asda")
class Test {
    @Id
    Integer id;
    @Column(name = "asa")
    Integer asa;
}