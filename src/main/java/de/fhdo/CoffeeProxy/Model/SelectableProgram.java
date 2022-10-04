package de.fhdo.CoffeeProxy.Model;

import java.util.List;

public class SelectableProgram {
    private String key;
    private List<CoffeeOption> options;

    public CoffeeOption getOption(String key) {
        return options.stream().filter(coffeeOption -> coffeeOption.getKey().equals(key)).findFirst().orElse(null);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<CoffeeOption> getOptions() {
        return options;
    }

    public void setOptions(List<CoffeeOption> options) {
        this.options = options;
    }
}
