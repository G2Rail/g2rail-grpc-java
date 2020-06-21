package com.g2rail.grpc.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.g2rail.*;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SuggestionsClient {
    private static final Logger logger = Logger.getLogger(SuggestionsClient.class.getName());

    private final ManagedChannel channel;
    private final SuggestionsGrpc.SuggestionsBlockingStub blockingStub;
    private final SuggestionsGrpc.SuggestionsStub asyncStub;
    private final String apiKey;
    private final String apiSecret;
    public SuggestionsClient(String host, int port, String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        channel = ManagedChannelBuilder.forAddress(host, port)
//                .intercept(new AuthenticationInterceptor(apiKey, apiSecret))
                .usePlaintext()
                .build();

        blockingStub = SuggestionsGrpc.newBlockingStub(channel);
        asyncStub = SuggestionsGrpc.newStub(channel);
    }

    public SuggestionsClient(String host, int port, String apiKey, String apiSecret, SslContext sslContext) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        channel = NettyChannelBuilder.forAddress(host, port)
                .sslContext(sslContext)
                .build();

        blockingStub = SuggestionsGrpc.newBlockingStub(channel);
        asyncStub = SuggestionsGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public SuggestionsResponse query(String query) {
        logger.info("load suggestions started");

        SuggestionRequest request = SuggestionRequest.newBuilder()
                .setQuery(query)
                .build();

        try {
            SuggestionsResponse suggestionsResponse = blockingStub
                    .withDeadlineAfter(200, TimeUnit.SECONDS)
                    .withInterceptors(new AuthenticationInterceptor(apiKey, new MessageSignature(apiKey, apiSecret, request)))
                    .query(request);

            return suggestionsResponse;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return SuggestionsResponse.newBuilder().build();
        }
    }
}
