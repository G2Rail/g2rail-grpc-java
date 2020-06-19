package com.g2rail.grpc.service;

import com.grpc.g2rail.*;
import com.grpc.g2rail.OnlineSolutionsGrpc.OnlineSolutionsBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.g2rail.DownloadRequest;
import io.grpc.g2rail.OnlineTicketsGrpc;
import io.grpc.g2rail.TicketsResponse;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OnlineTicketsClient {
    private static final Logger logger = Logger.getLogger(OnlineTicketsClient.class.getName());

    private final ManagedChannel channel;
    private final OnlineTicketsGrpc.OnlineTicketsBlockingStub blockingStub;
    private final OnlineTicketsGrpc.OnlineTicketsStub asyncStub;
    private final String apiKey;
    private final String apiSecret;
    public OnlineTicketsClient(String host, int port, String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        channel = ManagedChannelBuilder.forAddress(host, port)
//                .intercept(new AuthenticationInterceptor(apiKey, apiSecret))
                .usePlaintext()
                .build();

        blockingStub = OnlineTicketsGrpc.newBlockingStub(channel);
        asyncStub = OnlineTicketsGrpc.newStub(channel);
    }

    public OnlineTicketsClient(String host, int port, String apiKey, String apiSecret, SslContext sslContext) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        channel = NettyChannelBuilder.forAddress(host, port)
                .sslContext(sslContext)
                .build();

        blockingStub = OnlineTicketsGrpc.newBlockingStub(channel);
        asyncStub = OnlineTicketsGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public TicketsResponse download(String orderId) {
        logger.info("downloading started");

        DownloadRequest request = DownloadRequest.newBuilder()
                .setOrderId(orderId)
                .build();

        try {
            TicketsResponse ticketsResponse = blockingStub
                    .withDeadlineAfter(200, TimeUnit.SECONDS)
                    .withInterceptors(new AuthenticationInterceptor(apiKey, new MessageSignature(apiKey, apiSecret, request)))
                    .download(request);

            return ticketsResponse;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return TicketsResponse.newBuilder().build();
        }
    }
}
