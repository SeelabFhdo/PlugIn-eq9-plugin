package de.fhdo.CoffeeProxy.Sse;

import de.fhdo.CoffeeProxy.Model.CoffeeOption;

import java.util.List;


public interface SseCoffeeCallbackHandler {
    void handleOptionsChanged(List<CoffeeOption> options);
}
