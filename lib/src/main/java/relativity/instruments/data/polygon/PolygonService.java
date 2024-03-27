/*
 * Copyright (c) 2024. Relativity Software. All Rights Reserved.
 *
 * Licensed under the Functional Source License, Version 1.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the license at
 *
 * https://github.com/Relativity-Software/relativity/blob/main/LICENSE.md
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============================================================================
 */

package relativity.instruments.data.polygon;

import io.polygon.kotlin.sdk.rest.*;
import io.polygon.kotlin.sdk.rest.reference.*;
import io.polygon.kotlin.sdk.websocket.*;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class PolygonService {
    private String apiKey;
    public PolygonRestClient client;
    public PolygonWebSocketClient webSocket;
    public CountDownLatch webSocketLatch;

    public PolygonService(String apiKey) {
        this.apiKey = apiKey;

        client = new PolygonRestClient(this.apiKey);
    }

    public ArrayList<TickerDTO> getReferenceTickers() {
        ArrayList<TickerDTO> tickers = new ArrayList<>();

        SupportedTickersParameters params = new SupportedTickersParametersBuilder()
                .sortBy("ticker")
                .sortDescending(false)
                .market("stocks")
                .limit(1000)
                .build();

        client.getReferenceClient().listSupportedTickers(params)
            .asStream()
            .limit(50_000)
            .forEach((tickerDTO -> tickers.add(tickerDTO)));

        return tickers;
    }

    /**
     * Create a WebSocket connection to Polygon
     * Beware that this function could have a very
     * slow JSON deserialization method
     *
     * @param apiKey
     */
    public void createWebSocket(String apiKey) {
        if (webSocketLatch == null) {
            // Keeps the main process going
            webSocketLatch = new CountDownLatch(1);
        }

        if (webSocket != null) {
            webSocket.disconnectBlocking();
        }

        webSocket = new PolygonWebSocketClient(
            apiKey,
            Feed.RealTime.INSTANCE,
            Market.Stocks.INSTANCE,
            new PolygonWebSocketListener() {
                @Override
                public void onAuthenticated(@NotNull PolygonWebSocketClient polygonWebSocketClient) {
                    // This function is never hit
                }

                @Override
                public void onReceive(
                    @NotNull PolygonWebSocketClient polygonWebSocketClient,
                    @NotNull PolygonWebSocketMessage polygonWebSocketMessage
                ) {
                    if (polygonWebSocketMessage instanceof PolygonWebSocketMessage.StatusMessage) {
                        PolygonWebSocketMessage.StatusMessage statusMessage = (PolygonWebSocketMessage.StatusMessage) polygonWebSocketMessage;
                        Logger.info("Received message: " + statusMessage.getMessage());

                        return;
                    }

                    Logger.info("Received message: " + polygonWebSocketMessage);
                    if (polygonWebSocketMessage instanceof PolygonWebSocketMessage.StocksMessage) {
                        PolygonWebSocketMessage.StocksMessage stocksMessage = (PolygonWebSocketMessage.StocksMessage) polygonWebSocketMessage;
                        Logger.info("Received message: " + stocksMessage);
                    }
                }

                @Override
                public void onDisconnect(@NotNull PolygonWebSocketClient polygonWebSocketClient) {
                    Logger.info("Polygon disconnected");
                }

                @Override
                public void onError(@NotNull PolygonWebSocketClient polygonWebSocketClient, @NotNull Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        );

        Logger.info("Connecting to Polygon WebSocket...");
        webSocket.connectBlocking();

        List<PolygonWebSocketSubscription> subs = Collections.singletonList(
            new PolygonWebSocketSubscription(PolygonWebSocketChannel.Stocks.Trades.INSTANCE, "*")
        );

        webSocket.subscribeBlocking(subs);
    }

    public AggregatesDTO getHistoricPricing(String symbol, String timespan, int multiplier, String from, String to) {
        AggregatesParameters params = new AggregatesParametersBuilder()
            .ticker(symbol)
            .limit(50000)
            .fromDate(from)
            .toDate(to)
            .multiplier(multiplier)
            .timespan(timespan)
            .build();

        return client.getAggregatesBlocking(params);
    }

    public AggregatesDTO getHistoricPricing(String symbol, String timespan, int multiplier, String from) throws SocketTimeoutException {
        String to = LocalDateTime.now().plus(1, ChronoUnit.HOURS).format(DateTimeFormatter.ISO_LOCAL_DATE);

        AggregatesParameters params = new AggregatesParametersBuilder()
        .ticker(symbol)
        .limit(50000)
        .fromDate(from)
        .toDate(to)
        .multiplier(multiplier)
        .timespan(timespan)
        .build();

        return client.getAggregatesBlocking(params);
    }

    public AggregatesDTO getSecondPricingForDay(String symbol, String date) {
        return getHistoricPricing(symbol, "second", 1, date, date);
    }

    public AggregatesDTO getSecondPricingFirstFiveMinutes(String symbol, String date) throws SocketTimeoutException {
        // TODO: use timestamps from date
        return getHistoricPricing(symbol, "second", 1, date, date);
    }

    public AggregatesDTO getMinutePricingForDay(String symbol, String date) throws SocketTimeoutException {
        return getHistoricPricing(symbol, "minute", 1, date);
    }

    public AggregatesDTO getFiveMinutePricingForDay(String symbol, String date) {
        return getHistoricPricing(symbol, "minute", 5, date, date);
    }

    public List<Exchange> getExchanges() {
        return client
            .getReferenceClient()
            .getExchangesBlocking(new ExchangesParameters())
            .getResults();
    }

}
