package com.g2rail.grpc.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.g2rail.*;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OnlineConfirmationClient {
    private static final Logger logger = Logger.getLogger(OnlineSolutionsClient.class.getName());

    private final ManagedChannel channel;
    private final OnlineConfirmationsGrpc.OnlineConfirmationsBlockingStub blockingStub;
    private final OnlineConfirmationsGrpc.OnlineConfirmationsStub asyncStub;
    private final String apiKey;
    private final String apiSecret;
    public OnlineConfirmationClient(String host, int port, String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        channel = ManagedChannelBuilder.forAddress(host, port)
//                .intercept(new AuthenticationInterceptor(apiKey, apiSecret))
                .usePlaintext()
                .build();

        blockingStub = OnlineConfirmationsGrpc.newBlockingStub(channel);
        asyncStub = OnlineConfirmationsGrpc.newStub(channel);
    }

    public OnlineConfirmationClient(String host, int port, String apiKey, String apiSecret, SslContext sslContext) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        channel = NettyChannelBuilder.forAddress(host, port)
                .sslContext(sslContext)
                .build();

        blockingStub = OnlineConfirmationsGrpc.newBlockingStub(channel);
        asyncStub = OnlineConfirmationsGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public String confirm(String orderId) {
        logger.info("Booking started");

        ConfirmRequest request = ConfirmRequest.newBuilder()
                .setOrderId(orderId)
                .build();
        System.out.println(request);

        try {
            AsyncKeyResponse asyncKeyResponse = blockingStub
                    .withDeadlineAfter(200, TimeUnit.SECONDS)
                    .withInterceptors(new AuthenticationInterceptor(apiKey, new MessageSignature(apiKey, apiSecret, request)))
                    .confirm(request);
            logger.info("confirm result will be at " + asyncKeyResponse.getAsyncKey());
            return asyncKeyResponse.getAsyncKey();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return "";
        }
    }

    public OnlineConfirmationResponse queryAsyncOnlineConfirmation(String asyncKey) {
        logger.info("confirmation async query started");

        OnlineConfirmationAsyncQueryRequest request = OnlineConfirmationAsyncQueryRequest.newBuilder()
                .setAsyncKey(asyncKey)
                .build();

        try {
            OnlineConfirmationResponse response = blockingStub
                    .withDeadlineAfter(200, TimeUnit.SECONDS)
                    .withInterceptors(new AuthenticationInterceptor(apiKey, new MessageSignature(apiKey, apiSecret, request)))
                    .queryAsyncOnlineConfirmation(request);

            return response;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return OnlineConfirmationResponse.newBuilder().build();
        }
    }
}
