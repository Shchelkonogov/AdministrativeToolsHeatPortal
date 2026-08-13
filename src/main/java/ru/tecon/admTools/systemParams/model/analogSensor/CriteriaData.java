package ru.tecon.admTools.systemParams.model.analogSensor;

import java.util.StringJoiner;

/**
 * @author Maksim Shchelkonogov
 * 12.08.2026
 */
public class CriteriaData {

    private int id;
    private String name;
    private boolean state;

    public CriteriaData(int id, String name, boolean state) {
        this.id = id;
        this.name = name;
        this.state = state;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CriteriaData.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("name='" + name + "'")
                .add("state=" + state)
                .toString();
    }
}
