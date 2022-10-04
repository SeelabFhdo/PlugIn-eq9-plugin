package de.fhdo.CoffeeProxy;


import de.fhdo.CoffeeProxy.Authentication.TokenAuthenticator;
import de.fhdo.CoffeeProxy.Sse.SseCoffeeCallbackHandler;
import de.fhdo.CoffeeProxy.Sse.SseCoffeeClient;
import de.fhdo.CoffeeProxy.Model.*;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CoffeeProxy implements SseCoffeeCallbackHandler {

    private final CoffeeService coffeeService;
    private HashMap<String, Object> activeOptions;
    private HashMap<String, SelectableProgram> programInfos;
    private List<String> selectablePrograms;

    private SseCoffeeClient sseClient;
    private SseCoffeeCallbackHandler nestedCallbackHandler;

    public CoffeeProxy(String haId, String clientSecret, String refreshToken) {

        activeOptions = new HashMap<>();
        programInfos = new HashMap<>();
        var authenticator = new TokenAuthenticator(refreshToken, clientSecret);

        sseClient = new SseCoffeeClient(authenticator, this, haId);
        OkHttpClient okHttpClient = new OkHttpClient.Builder().authenticator(authenticator).build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.home-connect.com/api/homeappliances/" + haId + "/")
                .addConverterFactory(GsonConverterFactory.create()).client(okHttpClient).build();
        coffeeService = retrofit.create(CoffeeService.class);
    }

    public void setNestedCallbackHandler(SseCoffeeCallbackHandler handler) {
        this.nestedCallbackHandler = handler;
    }

    public boolean isOn() throws IOException {
        var key = "BSH.Common.Setting.PowerState";
        var option = getOptionValue(key);
        var isOn = false;
        if (option == null) {
            isOn = coffeeService.fetchPowerState().execute().body().isOn();
            activeOptions.put(key, isOn ? "On" : "Standby");
        } else {
            isOn = option.toString().equals("On");
        }
        return isOn;
    }

    public void setOn(boolean on) throws IOException {
        coffeeService.setPowerState(new PowerState(on)).execute();
    }

    @Override
    public void handleOptionsChanged(List<CoffeeOption> options) {
        for (var option : options) {
            activeOptions.put(option.getKey(), option.getValue());
        }
        if (nestedCallbackHandler != null) {
            nestedCallbackHandler.handleOptionsChanged(options);
        }
    }

    private SelectableProgram getProgramInfo(String key) throws IOException {
        var program = programInfos.get(key);
        if (program == null || program.getOptions() == null || program.getOptions().isEmpty()) {
            program = coffeeService.fetchProgramInfo(key).execute().body().getData();
            programInfos.put(key, program);
        }
        return program;
    }

    public void update() throws IOException {
        activeOptions.clear();
        sseClient.reconnect();
        if (isOn()) {
            var selectedProgram = coffeeService.fetchSelectedProgram().execute().body().getData();
            handleOptionsChanged(selectedProgram.getOptions());
            activeOptions.put("BSH.Common.Root.SelectedProgram", selectedProgram.getKey());
        }
    }

    public List<String> getAvailableBeverages() throws IOException {
        if (selectablePrograms == null || selectablePrograms.isEmpty()) {
            selectablePrograms = coffeeService.fetchAvailablePrograms().execute().body().getPrograms();
        }
        return selectablePrograms.stream().map(this::getLastIdComponent).collect(Collectors.toList());
    }

    public void setBeverage(String beverage) throws IOException {
        if (selectablePrograms == null) {
            // Used to fetch programs again
            getAvailableBeverages();
        }
        for (String availableBeverage : selectablePrograms) {
            if (availableBeverage.endsWith(beverage)) {
                var program = new SelectableProgram();
                program.setKey(availableBeverage);
                coffeeService.setSelectedProgram(new SelectableProgramContainer(program)).execute();
                break;
            }
        }
    }

    public String getBeverage() {
        var value = getOptionValue("BSH.Common.Root.SelectedProgram");
        return value != null ? getLastIdComponent(value.toString()) : "";
    }

    public void start() throws IOException {
        var program = new SelectableProgram();
        program.setKey(activeOptions.get("BSH.Common.Root.SelectedProgram").toString());
        coffeeService.setActiveProgram(new SelectableProgramContainer(program)).execute();
    }

    public void stop() throws IOException {
        coffeeService.deleteActiveProgram().execute();
    }

    public boolean isStarted() {
        return activeOptions.get("BSH.Common.Root.ActiveProgram") != null;
    }

    public int getProgress() {
        var value = activeOptions.get("BSH.Common.Option.ProgramProgress");
        return value != null ? (int) (double) Double.valueOf(value.toString()) : 0;
    }

    public int getFillQuantity() {
        var value = activeOptions.get("ConsumerProducts.CoffeeMaker.Option.FillQuantity");
        return value != null ? (int) (double) Double.valueOf(value.toString()) : 0;
    }

    public void setFillQuantity(int value) throws IOException {
        var key = "ConsumerProducts.CoffeeMaker.Option.FillQuantity";
        var programInfo = getProgramInfo(activeOptions.get("BSH.Common.Root.SelectedProgram").toString());
        var optionInfo = programInfo.getOption(key);
        var option = new CoffeeOption();
        option.setKey(key);
        option.setUnit("ml");
        var min = optionInfo.getConstraints().getMin();
        var max = optionInfo.getConstraints().getMax();
        var stepSize = optionInfo.getConstraints().getStepSize();
        if (value < min) {
            value = min;
        } else if (value > max) {
            value = max;
        }
        value = (int) (stepSize * (Math.round(value / (double) stepSize)));
        option.setValue(value);
        coffeeService.setOption(new OptionContainer(option), key).execute();
    }

    public String getBeanAmount() {
        var value = getOptionValue("ConsumerProducts.CoffeeMaker.Option.BeanAmount");
        return value != null ? value.toString() : "";
    }

    public void setBeanAmount(String value) throws IOException {
        var key = "ConsumerProducts.CoffeeMaker.Option.BeanAmount";
        var option = new CoffeeOption();
        option.setKey(key);
        option.setValue("ConsumerProducts.CoffeeMaker.EnumType.BeanAmount." + value);
        coffeeService.setOption(new OptionContainer(option), key).execute();
    }

    public String getCoffeeTemperature() {
        var value = getOptionValue("ConsumerProducts.CoffeeMaker.Option.CoffeeTemperature");
        return value != null ? value.toString() : "";
    }

    public void setCoffeeTemperature(String value) throws IOException {
        var key = "ConsumerProducts.CoffeeMaker.Option.CoffeeTemperature";
        var option = new CoffeeOption();
        option.setKey(key);
        option.setValue("ConsumerProducts.CoffeeMaker.EnumType.CoffeeTemperature." + value);
        coffeeService.setOption(new OptionContainer(option), key).execute();
    }

    public Object getOptionValue(String key) {
        var option = activeOptions.get(key);
        if (option != null) {
            return getLastIdComponent(option.toString());
        }
        return null;
    }

    private String getLastIdComponent(String id) {
        if (id == null) {
            return null;
        }
        var components = id.split("\\.");
        return components.length != 0 ? components[components.length - 1] : null;
    }
}
