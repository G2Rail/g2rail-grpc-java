package com.g2rail.grpc.service;

import com.grpc.g2rail.OnlineSolutionsAsyncQueryRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.g2rail.*;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OnlineOrderClient {
    private static final Logger logger = Logger.getLogger(OnlineSolutionsClient.class.getName());

    private final ManagedChannel channel;
    private final OnlineOrdersGrpc.OnlineOrdersBlockingStub blockingStub;
    private final OnlineOrdersGrpc.OnlineOrdersStub asyncStub;
    private final String apiKey;
    private final String apiSecret;
    public OnlineOrderClient(String host, int port, String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        channel = ManagedChannelBuilder.forAddress(host, port)
//                .intercept(new AuthenticationInterceptor(apiKey, apiSecret))
                .usePlaintext()
                .build();

        blockingStub = OnlineOrdersGrpc.newBlockingStub(channel);
        asyncStub = OnlineOrdersGrpc.newStub(channel);
    }

    public OnlineOrderClient(String host, int port, String apiKey, String apiSecret, SslContext sslContext) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        channel = NettyChannelBuilder.forAddress(host, port)
                .sslContext(sslContext)
                .build();

        blockingStub = OnlineOrdersGrpc.newBlockingStub(channel);
        asyncStub = OnlineOrdersGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public String book(String bookingCode) {
        logger.info("Booking started");

        BookRequest request = BookRequest.newBuilder()
                .addAllSections(Arrays.asList(bookingCode))
                .addAllPassengers(Arrays.asList(Passenger.newBuilder()
                        .setGender(Passenger.Gender.male)
                        .setFirstName("Qinwen")
                        .setLastName("SHI")
                        .setPassport("E12341813")
                        .setPhone("+8527892123")
                        .setEmail("wen@g2rail.com")
                        .setBirthdate("1986-06-01")
                        .build()))
                .build();
        System.out.println(request);

        try {
            AsyncKeyResponse asyncKeyResponse = blockingStub
                    .withDeadlineAfter(200, TimeUnit.SECONDS)
                    .withInterceptors(new AuthenticationInterceptor(apiKey, new MessageSignature(apiKey, apiSecret, request)))
                    .book(request);
            logger.info("book result will be at " + asyncKeyResponse.getAsyncKey());
            return asyncKeyResponse.getAsyncKey();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return "";
        }
    }

    public OnlineOrderResponse queryAsyncOnlineOrder(String asyncKey) {
        logger.info("search started");

        OnlineOrderAsyncQueryRequest request = OnlineOrderAsyncQueryRequest.newBuilder()
                .setAsyncKey(asyncKey)
                .build();

        try {
            OnlineOrderResponse response = blockingStub
                    .withDeadlineAfter(200, TimeUnit.SECONDS)
                    .withInterceptors(new AuthenticationInterceptor(apiKey, new MessageSignature(apiKey, apiSecret, request)))
                    .queryAsyncOnlineOrder(request);

            return response;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return OnlineOrderResponse.newBuilder().build();
        }
    }
}
