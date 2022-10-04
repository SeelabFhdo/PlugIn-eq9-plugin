package de.fhdo.CoffeeProxy.Authentication;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface OauthService {
    @FormUrlEncoded
    @POST("security/oauth/token")
    Call<TokenResponse> refreshToken(@Field("refresh_token") String refreshToken,
            @Field("client_secret") String client_secret, @Field("grant_type") String grant_type);
}
