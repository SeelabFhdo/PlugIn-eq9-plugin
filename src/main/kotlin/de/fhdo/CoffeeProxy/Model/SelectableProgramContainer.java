package de.fhdo.CoffeeProxy.Model;

public class SelectableProgramContainer {
    private SelectableProgram data;

    public SelectableProgramContainer(SelectableProgram data) {
        this.data = data;
    }

    public SelectableProgramContainer() {
    }

    public SelectableProgram getData() {
        return data;
    }
}
