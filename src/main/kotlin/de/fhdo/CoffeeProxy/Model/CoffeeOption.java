package de.fhdo.CoffeeProxy.Model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CoffeeOption {
    private String key;
    private Object value;
    private String unit;
    @SerializedName("default")
    private Object defaultValue;
    private Constraints constraints;

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public class Constraints {
        private List<String> allowedValues;
        private int min;
        private int max;
        @SerializedName("stepsize")
        private int stepSize;

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public int getStepSize() {
            return stepSize;
        }

        public List<String> getAllowedValues() {
            return allowedValues;
        }
    }
}
