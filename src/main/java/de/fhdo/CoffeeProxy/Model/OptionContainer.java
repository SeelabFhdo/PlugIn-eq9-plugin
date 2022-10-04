package de.fhdo.CoffeeProxy.Model;

public class OptionContainer {
    private CoffeeOption data;

    public OptionContainer(CoffeeOption data) {
        this.data = data;
    }

    public CoffeeOption getData() {
        return data;
    }

    public void setData(CoffeeOption data) {
        this.data = data;
    }
}
