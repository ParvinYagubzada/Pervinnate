package az.code.mapper;

import az.code.exceptions.NoMappableFieldsFound;

import java.util.List;

public interface IManager {

    <T> T find(int id, Class<T> reference) throws Exception, NoMappableFieldsFound;

    <T> T remove(T object) throws Exception, NoMappableFieldsFound;

    <T> void merge(T object) throws Exception, NoMappableFieldsFound;

    <T> List<T> getObjects(int limit, Class<T> reference) throws Exception, NoMappableFieldsFound;
}
