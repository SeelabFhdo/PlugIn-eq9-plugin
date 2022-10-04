package de.fhdo.CoffeeProxy.Authentication;

import java.io.IOException;
import java.time.Instant;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TokenAuthenticator implements Authenticator {
    private String refreshToken;
    private String accessToken;
    private String clientSecret;
    private Instant expiryDate;
    private OauthService oauthService;

    public TokenAuthenticator(String refreshToken, String clientSecret) {
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.home-connect.com")
                .addConverterFactory(GsonConverterFactory.create()).build();
        this.oauthService = retrofit.create(OauthService.class);
    }

    public synchronized String getAccessToken() throws IOException {
        if (expiryDate == null || expiryDate.isBefore(Instant.now())) {
            System.out.println("refreshing token");
            var response = oauthService.refreshToken(refreshToken, clientSecret, "refresh_token").execute();
            if (!response.isSuccessful()) {
                throw new IOException("Error while fetching accesstoken");
            }
            expiryDate = Instant.now().plusSeconds(response.body().getExpiresIn() - 30);
            accessToken = response.body().getAccessToken();
        }
        return accessToken;
    }

    @Override
    public Request authenticate(Route route, Response httpResponse) throws IOException {
        return httpResponse.request().newBuilder().header("Authorization", "Bearer " + getAccessToken()).build();
    }
}
