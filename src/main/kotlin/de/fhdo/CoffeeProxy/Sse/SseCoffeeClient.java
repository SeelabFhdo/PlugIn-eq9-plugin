package de.fhdo.CoffeeProxy.Sse;

import java.io.IOException;


import com.google.gson.Gson;
import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;


import de.fhdo.CoffeeProxy.Authentication.TokenAuthenticator;
import de.fhdo.CoffeeProxy.Model.SseOptionContainer;
import okhttp3.Request;
import okhttp3.Response;

public class SseCoffeeClient implements ServerSentEvent.Listener {
    private TokenAuthenticator tokenAuthenticator;
    private SseCoffeeCallbackHandler callbackHandler;
    private String haId;
    private ServerSentEvent serverSentEvent;
    private Gson gson;

    public SseCoffeeClient(TokenAuthenticator authenticator, SseCoffeeCallbackHandler callbackHandler, String haId) {

        this.gson = new Gson();
        this.tokenAuthenticator = authenticator;
        this.callbackHandler = callbackHandler;
        this.haId = haId;
    }

    public void reconnect() throws IOException {
        close();

        var url = "https://api.home-connect.com/api/homeappliances/" + haId + "/events";
        var request = new Request.Builder().url(url)
                .addHeader("Authorization", "Bearer " + tokenAuthenticator.getAccessToken()).build();
        OkSse okSse = new OkSse();
        this.serverSentEvent = okSse.newServerSentEvent(request, this);
    }

    public void close() {
        if (serverSentEvent != null) {
            serverSentEvent.close();
        }
    }

    @Override
    public void onOpen(ServerSentEvent sse, Response response) {
        System.out.println("Opened SSE connection...");
    }

    @Override
    public void onMessage(ServerSentEvent sse, String id, String event, String message) {
        if (event.equals("NOTIFY")) {
            var sseOptions = gson.fromJson(message, SseOptionContainer.class);
            callbackHandler.handleOptionsChanged(sseOptions.getItems());
        }
    }

    @Override
    public void onComment(ServerSentEvent sse, String comment) {
        System.out.println("Comment: " + comment);
    }

    @Override
    public boolean onRetryTime(ServerSentEvent sse, long milliseconds) {
        System.out.println("Retry time " + milliseconds);
        return false;
    }

    @Override
    public boolean onRetryError(ServerSentEvent sse, Throwable throwable, Response response) {
        throwable.printStackTrace();
        return false;
    }

    @Override
    public void onClosed(ServerSentEvent sse) {
        System.out.println("Closed SSE-Connection");
    }

    @Override
    public Request onPreRetry(ServerSentEvent sse, Request originalRequest) {
        System.out.println("Pre retry");
        return null;
    }
}
