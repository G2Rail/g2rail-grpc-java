package com.g2rail.grpc.service;

import com.grpc.g2rail.*;
import com.grpc.g2rail.OnlineSolutionsGrpc.OnlineSolutionsBlockingStub;
import com.grpc.g2rail.OnlineSolutionsGrpc.OnlineSolutionsStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.github.cdimascio.dotenv.Dotenv;

public class OnlineSolutionsClient {
    private static final Logger logger = Logger.getLogger(OnlineSolutionsClient.class.getName());

    private final ManagedChannel channel;
    private final OnlineSolutionsBlockingStub blockingStub;
    private final OnlineSolutionsStub asyncStub;
    private final String apiKey;
    private final String apiSecret;
    public OnlineSolutionsClient(String host, int port, String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        channel = ManagedChannelBuilder.forAddress(host, port)
//                .intercept(new AuthenticationInterceptor(apiKey, apiSecret))
                .usePlaintext()
                .build();

        blockingStub = OnlineSolutionsGrpc.newBlockingStub(channel);
        asyncStub = OnlineSolutionsGrpc.newStub(channel);
    }

    public OnlineSolutionsClient(String host, int port, String apiKey, String apiSecret, SslContext sslContext) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        channel = NettyChannelBuilder.forAddress(host, port)
                .sslContext(sslContext)
                .build();

        blockingStub = OnlineSolutionsGrpc.newBlockingStub(channel);
        asyncStub = OnlineSolutionsGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public String search() {
        logger.info("search started");

        SearchRequest request = SearchRequest.newBuilder()
                .setDate("2020-06-22")
                .setTime("07:00")
                .setFrom("BERLIN")
                .setTo("FRANKFURT")
                .setAdult(1)
                .setChild(0)
                .build();

        try {

            AsyncKeyResponse asyncKeyResponse = blockingStub
                    .withDeadlineAfter(200, TimeUnit.SECONDS)
                    .withInterceptors(new AuthenticationInterceptor(apiKey, new MessageSignature(apiKey, apiSecret, request)))
                    .search(request);
            logger.info("search result will be at " + asyncKeyResponse.getAsyncKey());
            return asyncKeyResponse.getAsyncKey();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return "";
        }
    }

    public OnlineSolutionsResponse queryAsyncOnlineSolutions(String asyncKey) {
        OnlineSolutionsAsyncQueryRequest request = OnlineSolutionsAsyncQueryRequest.newBuilder()
                .setAsyncKey(asyncKey)
                .build();
        try {
            OnlineSolutionsResponse onlineSolutionsResponse = blockingStub
                    .withDeadlineAfter(200, TimeUnit.SECONDS)
                    .withInterceptors(new AuthenticationInterceptor(apiKey, new MessageSignature(this.apiKey, this.apiSecret, request)))
                    .queryAsyncOnlineSolutions(request);
            logger.info("search result is " + onlineSolutionsResponse.toString());
            return onlineSolutionsResponse;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return OnlineSolutionsResponse.newBuilder().addRailwaySolutions(
                    RailwaySolution
                            .newBuilder()
                            .setLoading(false)
                            .build()
            ).build();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(System.getProperty("user.dir"));
        Dotenv dotenv = Dotenv.load();
        System.out.println("load succeed");
        String host = dotenv.get("HOST", "localhost");
        int port = Integer.parseInt(dotenv.get("PORT", "10541"));
        String apiKey = dotenv.get("API_KEY", "key");
        String apiSecret = dotenv.get("API_SECRET", "secret");
        System.out.println(host + apiKey + apiSecret);


        OnlineSolutionsClient client = new OnlineSolutionsClient(host, port, apiKey, apiSecret);
        String asyncKey = client
               .search();
        if (asyncKey.isEmpty()) {
            System.err.println("Search Failed");
            return;
        }

        List<RailwaySolution> solutions;
        do {
            Thread.sleep(2000);
            OnlineSolutionsResponse response = client.queryAsyncOnlineSolutions(asyncKey);
            solutions = response.getRailwaySolutionsList();
        }
        while(solutions.stream().anyMatch(railwaySolution -> railwaySolution.getLoading()));
        client.shutdown();
    }
}
