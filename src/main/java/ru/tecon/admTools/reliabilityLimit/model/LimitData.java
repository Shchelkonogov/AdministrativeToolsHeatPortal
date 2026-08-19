package ru.tecon.admTools.reliabilityLimit.model;

import java.io.Serializable;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * @author Maksim Shchelkonogov
 * 18.08.2026
 */
public class LimitData implements Serializable {

    private final UUID id;
    private final int parId;
    private final String parName;
    private final String techProcCode;
    private final int zone;
    private final String measureName;
    private Double min;
    private Double max;

    private boolean change;

    public LimitData(int parId, String parName, String techProcCode, int zone, String measureName, Double min, Double max) {
        this.id = UUID.randomUUID();
        this.parId = parId;
        this.parName = parName;
        this.techProcCode = techProcCode;
        this.zone = zone;
        this.measureName = measureName;
        this.min = min;
        this.max = max;
    }

    public UUID getId() {
        return id;
    }

    public int getParId() {
        return parId;
    }

    public String getParName() {
        return parName;
    }

    public String getTechProcCode() {
        return techProcCode;
    }

    public int getZone() {
        return zone;
    }

    public String getMeasureName() {
        return measureName;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public boolean isChange() {
        return change;
    }

    public void setChange(boolean change) {
        this.change = change;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LimitData.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("parName='" + parName + "'")
                .add("techProcCode='" + techProcCode + "'")
                .add("zone=" + zone)
                .add("measureName='" + measureName + "'")
                .add("min=" + min)
                .add("max=" + max)
                .add("change=" + change)
                .toString();
    }
}
