package de.fhdo.CoffeeProxy.Model;

public class PowerState {
    private PowerStateData data;

    public PowerState(boolean on) {
        data = new PowerStateData(on);
    }

    public boolean isOn() {
        return data.isOn();
    }

    private class PowerStateData {
        private String key = "BSH.Common.Setting.PowerState";
        private String value;

        public PowerStateData(boolean on) {
            value = on ? "BSH.Common.EnumType.PowerState.On" : "BSH.Common.EnumType.PowerState.Standby";
        }

        public boolean isOn() {
            return value.equals("BSH.Common.EnumType.PowerState.On");
        }
    }
}
