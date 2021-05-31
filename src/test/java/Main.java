import az.code.annotations.Column;
import az.code.annotations.Entity;
import az.code.annotations.Id;
import az.code.exceptions.NoMappableFieldsFound;
import az.code.mapper.IManager;
import az.code.mapper.Manager;

import java.sql.Date;

public class Main {
    public static void main(String[] args) throws NoMappableFieldsFound, Exception {
        IManager manager = new Manager();
        Person person = new Person();
//        for (int i = 0; i < 30; i++) {
//            person.name = "Pervin" + i;
//            person.surname = "Yaqubzade";
//            person.data = "Yes";
//            person.birthdate = new Date(100, 8, 13);
//            manager.merge(person);
//        }
//        System.out.println(manager.find(6, Test.class));
//        for (Person data : manager.getObjects(15, Person.class)) {
//            System.out.println(manager.remove(data));
//        }
        System.out.println(manager.getObjects(-1, Person.class));
//        person.name = "Emil";
//        person.surname = "Ehmedli";
//        person.data = "Yes";
//        person.id=61;
//        person.birthdate = new Date(100, 8, 13);
////            manager.merge(person);
////        System.out.println(manager.remove(person));
//        manager.merge(person);

//        for (Person data : manager.getObjects(15, Person.class)) {
//            data.name = "TEST";
//            manager.merge(data);
//        }
    }
}

@Entity(name = "datas")
class Person {
    @Id
    @Column(name = "person_id")
    Integer id;
    @Column(name = "name")
    String name;
    @Column(name = "surname")
    String surname;
    String data;
    @Column(name = "birthday")
    Date birthdate;

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", data='" + data + '\'' +
                ", birthdate=" + birthdate +
                '}';
    }

    public Person() {
    }
}
@Entity(name = "salam")
class Test{
    @Id
    @Column(name = "person_id")
    Integer id;
}