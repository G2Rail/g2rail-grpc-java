package com.g2rail.grpc.service;

import com.grpc.g2rail.OnlineSolutionsResponse;
import com.grpc.g2rail.RailwaySolution;
import io.github.cdimascio.dotenv.Dotenv;
import io.grpc.g2rail.OnlineConfirmationResponse;
import io.grpc.g2rail.OnlineOrderResponse;
import io.grpc.g2rail.TicketsResponse;

import java.util.List;

public class Main {
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
        } while(solutions.stream().anyMatch(railwaySolution -> railwaySolution.getLoading()));

        OnlineOrderClient orderClient = new OnlineOrderClient(host, port, apiKey, apiSecret);
        String bookingCode = solutions.get(0).getSolutions(0).getSections(0).getOffers(1).getServices(0).getBookingCode();
        asyncKey = orderClient.book(bookingCode);

        OnlineOrderResponse onlineOrder;
        do {
            Thread.sleep(2000);
            System.out.println("Loading "+asyncKey);
            onlineOrder = orderClient.queryAsyncOnlineOrder(asyncKey);
        } while (onlineOrder.getLoading());
        System.out.println(onlineOrder);

        OnlineConfirmationClient confirmationClient = new OnlineConfirmationClient(host, port, apiKey, apiSecret);
        String orderId = onlineOrder.getId();
        asyncKey = confirmationClient.confirm(orderId);

        OnlineConfirmationResponse onlineConfirmation;
        do {
            Thread.sleep(2000);
            System.out.println("Loading "+asyncKey);
            onlineConfirmation = confirmationClient.queryAsyncOnlineConfirmation(asyncKey);
        } while (onlineConfirmation.getLoading());
        System.out.println(onlineConfirmation);

        OnlineTicketsClient ticketsClient = new OnlineTicketsClient(host, port, apiKey, apiSecret);
        TicketsResponse tickets = ticketsClient.download(orderId);

        System.out.println(tickets);
        client.shutdown();
        orderClient.shutdown();
        confirmationClient.shutdown();
        ticketsClient.shutdown();
    }
}
