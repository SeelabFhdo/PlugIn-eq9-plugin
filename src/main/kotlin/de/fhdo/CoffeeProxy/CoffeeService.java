package de.fhdo.CoffeeProxy;

import de.fhdo.CoffeeProxy.Model.AvailableProgramsContainer;
import de.fhdo.CoffeeProxy.Model.OptionContainer;
import de.fhdo.CoffeeProxy.Model.PowerState;
import de.fhdo.CoffeeProxy.Model.SelectableProgramContainer;
import retrofit2.Call;
import retrofit2.http.*;

public interface CoffeeService {

    @GET("settings/BSH.Common.Setting.PowerState")
    Call<PowerState> fetchPowerState();

    @PUT("settings/BSH.Common.Setting.PowerState")
    Call<Void> setPowerState(@Body PowerState powerState);

    @GET("programs/available")
    Call<AvailableProgramsContainer> fetchAvailablePrograms();

    @GET("programs/selected")
    Call<SelectableProgramContainer> fetchSelectedProgram();

    @PUT("programs/selected")
    Call<SelectableProgramContainer> setSelectedProgram(@Body SelectableProgramContainer program);

    @PUT("programs/active")
    Call<SelectableProgramContainer> setActiveProgram(@Body SelectableProgramContainer program);

    @DELETE("programs/active")
    Call<Void> deleteActiveProgram();

    @GET("programs/available/{programId}")
    Call<SelectableProgramContainer> fetchProgramInfo(@Path("programId") String programId);

    @PUT("programs/selected/options/{optionKey}")
    Call<Void> setOption(@Body OptionContainer option, @Path("optionKey") String optionKey);
}
