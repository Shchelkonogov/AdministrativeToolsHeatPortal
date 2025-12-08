package ru.tecon.admTools.specificModel.model.additionalModel;

import ru.tecon.admTools.specificModel.Utils;
import ru.tecon.admTools.specificModel.model.Condition;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Дополнительные данные для данных перечислимых параметров
 */
public class EnumerateData implements Serializable, Additional {

    private List<ParamCondition> conditions = new ArrayList<>();

    public EnumerateData() {
    }

    public void addCondition(int enumCode, String propValue, int conditionID, String conditionName) {
        this.conditions.add(new ParamCondition(enumCode, propValue, conditionID, conditionName));
    }

    public List<ParamCondition> getConditions() {
        return conditions;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EnumerateData.class.getSimpleName() + "[", "]")
                .add("conditions=" + conditions)
                .toString();
    }

    public class ParamCondition {

        private int enumCode;
        private String propValue;

        private Condition condition;

        private boolean edited = false;

        private ParamCondition(int enumCode, String propValue, int conditionID, String conditionName) {
            this.enumCode = enumCode;
            this.propValue = propValue;
            condition = new Condition(conditionID, conditionName);
        }

        public int getEnumCode() {
            return enumCode;
        }

        public Condition getCondition() {
            return condition;
        }

        public boolean isEdited() {
            return edited;
        }

        public String getPropValue() {
            return propValue;
        }

        public void setConditionSer(String condSer) throws IOException, ClassNotFoundException {
            if (condSer != null) {
                this.condition = (Condition) Utils.fromString(condSer);
            }
        }

        public String getConditionSer() {
            return condition.getName();
        }

        public void setEdited(boolean edited) {
            this.edited = edited;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ParamCondition.class.getSimpleName() + "[", "]")
                    .add("enumCode=" + enumCode)
                    .add("propValue='" + propValue + "'")
                    .add("condition=" + condition)
                    .add("edited=" + edited)
                    .toString();
        }
    }
}
