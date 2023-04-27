package ru.tecon.admTools.systemParams.model.struct;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Класс описывающий структуру
 * @author Maksim Shchelkonogov
 */
public class StructType implements Serializable, Comparable<StructType> {

    private int id;
    private String name;
    private String typeChar;
    private Integer parentID;

    public StructType() {
    }

    public StructType(int id, String name, String typeChar) {
        this.id = id;
        this.name = name;
        this.typeChar = typeChar;
    }

    public StructType(int id, String name, String typeChar, Integer parentID) {
        this.id = id;
        this.name = name;
        this.typeChar = typeChar;
        this.parentID = parentID;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTypeChar() {
        return typeChar;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTypeChar(String typeChar) {
        this.typeChar = typeChar;
    }

    public Integer getParentID() {
        return parentID;
    }

    public void setParentID(Integer parentID) {
        this.parentID = parentID;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StructType.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("name='" + name + "'")
                .add("typeChar='" + typeChar + "'")
                .add("parentID=" + parentID)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StructType that = (StructType) o;
        return id == that.id && Objects.equals(name, that.name) && Objects.equals(typeChar, that.typeChar) && Objects.equals(parentID, that.parentID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, typeChar, parentID);
    }

    @Override
    public int compareTo(StructType o) {
        return getName().compareTo(o.getName());
    }
}
