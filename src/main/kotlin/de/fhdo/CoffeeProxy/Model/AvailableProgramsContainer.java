package de.fhdo.CoffeeProxy.Model;

import java.util.List;
import java.util.stream.Collectors;

public class AvailableProgramsContainer {
    private AvailableProgramsData data;

    public List<String> getPrograms() {
        return data.getPrograms().stream().map(program -> program.getKey()).collect(Collectors.toList());
    }

    private class AvailableProgramsData {

        private List<Program> programs;

        public List<Program> getPrograms() {
            return programs;
        }

        private class Program {
            private String key;

            public String getKey() {
                return key;
            }
        }
    }
}
