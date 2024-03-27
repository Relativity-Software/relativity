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

package relativity.instruments;

import relativity.events.EventService;
import relativity.events.types.InstrumentAnalysisEvent;
import relativity.events.types.InstrumentPriceChangeEvent;
import relativity.instruments.data.polygon.types.PolygonQuote;
import relativity.instruments.types.Instrument;
import relativity.instruments.data.polygon.types.PolygonTrade;
import relativity.workers.ThreadPool;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.concurrent.*;

import org.tinylog.Logger;

public class EquityQuoteAndTradeProcessor {
    private final String symbol;
    private final InstrumentManager instrumentManager;
    private final EventService eventService;
    private PriceMovement tradeAggregate = new PriceMovement();
    private PriceMovement lastTradeAggregate = new PriceMovement();
    private PriceMovement previousTradeAggregate = new PriceMovement();

    private QuotePriceMovement quoteAggregate = new QuotePriceMovement();
    private QuotePriceMovement lastQuoteAggregate = new QuotePriceMovement();
    private QuotePriceMovement previousQuoteAggregate = new QuotePriceMovement();

    private ThreadPool pool;
    private InstrumentStatisticsService instrumentStatisticsService;

    private long currentAggregatedSecond;
    private long currentAggregatedMillisecond;
    final Object tradeLock = new Object();
    final Object quoteLock = new Object();

    public EquityQuoteAndTradeProcessor(
        String symbol,
        InstrumentManager instrumentManager,
        ThreadPool pool,
        EventService eventService,
        InstrumentStatisticsService instrumentStatisticsService
    ) {
        this.symbol = symbol;
        this.instrumentManager = instrumentManager;
        this.eventService = eventService;
        this.pool = pool;
        this.instrumentStatisticsService = instrumentStatisticsService;

        tradeAggregate.symbol = symbol;
        lastTradeAggregate.symbol = symbol;

        quoteAggregate.symbol = symbol;
        lastQuoteAggregate.symbol = symbol;

        currentAggregatedMillisecond = System.currentTimeMillis();
        currentAggregatedSecond = (long) Math.floor(currentAggregatedMillisecond / 1000);

        // TODO: Determine if every equity doing calculations precisely at every second works
        int wait = (int) (1000 - (currentAggregatedMillisecond - (currentAggregatedSecond * 1000)));

        this.pool.scheduleAtFixedRate(() -> {
            if (!(tradeAggregate.hasBeenReset)/* && tradesReceived*/) {
                try {
                    updateCalculationWithExistingAggregate();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, wait, 1000, TimeUnit.MILLISECONDS);
    }

    public void aggregateTrade(PolygonTrade trade) {
        if (!trade.sym.equalsIgnoreCase(symbol)) {
            Logger.warn("Trade for " + trade.sym + " is in the wrong processor " + symbol);

            return;
        }

        synchronized (tradeLock){
            if (tradeAggregate.hasBeenReset) {
                initializeTradeAggregate(trade);

                return;
            }

//            long tradeTimeInSeconds = trade.t / 1000;
//
//            if (currentAggregatedSecond != tradeTimeInSeconds) {
//                // Attempt to update previous bars
//                Security security = securityManager.getSecurity(lastTradeAggregate.symbol);
//
//                long diff = currentAggregatedSecond - tradeTimeInSeconds;
//                long index = security.pricing.tradeSeries.getBarCount() - diff;
//
////                Logger.info("Index: " + index + " Diff: " + diff + " Time: " + trade.t + " Current: " + currentAggregatedSecond + " Trade: " + tradeTimeInSeconds + " Symbol: " + trade.sym);
//
////                Bar bar = security.pricing.tradeSeries.getBar((int) index);
////                bar.addTrade(DecimalNum.valueOf(trade.p), DecimalNum.valueOf(trade.p));
//
//                return;
//            }

            // For now, we are adding all trades to the aggregate
            // even if not in the same second. This will still average
            // out the data we need

            addTradeToAggregate(trade, tradeAggregate, false);
        }
    }

    public void aggregateQuote(PolygonQuote quote) {
        if (!quote.sym.equalsIgnoreCase(symbol)) {
            Logger.warn("Received quote for wrong symbol: " + quote.sym + " instead of " + symbol);

            return;
        }

        synchronized(quoteLock) {
            if (quoteAggregate.hasBeenReset) {
                initializeQuoteAggregate(quote);
            }

            addQuoteToAggregate(quote, quoteAggregate);
        }
    }

    private void updateCalculationWithExistingAggregate() throws ExecutionException, InterruptedException {
        // Immediately reset the current trade aggregate and close off data
        // Might use a lock here
        synchronized (tradeLock) {
            long currentTime = System.nanoTime();
            lastTradeAggregate.high = tradeAggregate.high;
            lastTradeAggregate.low = tradeAggregate.low;
            lastTradeAggregate.open = tradeAggregate.open;
            lastTradeAggregate.close = tradeAggregate.close;
            lastTradeAggregate.volume = tradeAggregate.volume;
            lastTradeAggregate.prices = tradeAggregate.prices;
            lastTradeAggregate.time = tradeAggregate.time;
            lastTradeAggregate.endTime = System.currentTimeMillis();
            lastTradeAggregate.endTimeNano = System.nanoTime();
            lastTradeAggregate.hasBeenReset = false;

            long endTime = System.nanoTime();
            long lengthOfTime = endTime - currentTime;

            if (lengthOfTime > 500) {
//                Logger.info("Update Values " + lengthOfTime + " ns");
            }

            // Reset this as quickly as possible
            tradeAggregate.reset();
        }

        synchronized (quoteLock) {
            lastQuoteAggregate.bidHigh = quoteAggregate.bidHigh;
            lastQuoteAggregate.bidLow = quoteAggregate.bidLow;
            lastQuoteAggregate.bidOpen = quoteAggregate.bidOpen;
            lastQuoteAggregate.bidClose = quoteAggregate.bidClose;
            lastQuoteAggregate.bidVolume = quoteAggregate.bidVolume;
            lastQuoteAggregate.askHigh = quoteAggregate.askHigh;
            lastQuoteAggregate.askLow = quoteAggregate.askLow;
            lastQuoteAggregate.askOpen = quoteAggregate.askOpen;
            lastQuoteAggregate.askClose = quoteAggregate.askClose;
            lastQuoteAggregate.askVolume = quoteAggregate.askVolume;
            lastQuoteAggregate.time = quoteAggregate.time;
            lastQuoteAggregate.endTime = System.currentTimeMillis();
            lastQuoteAggregate.endTimeNano = System.nanoTime();

            quoteAggregate.reset();
        }

        pool.runAsync(() -> {
            try {
                eventService.processEvent(new InstrumentPriceChangeEvent(lastTradeAggregate));
                Instrument instrument = instrumentManager.getInstrument(lastTradeAggregate.symbol);
                instrumentStatisticsService.addLatestPriceToInstrument(instrument, lastTradeAggregate, lastQuoteAggregate);

                eventService.processEvent(new InstrumentAnalysisEvent(instrument, lastTradeAggregate));
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void initializeTradeAggregate(PolygonTrade trade) {
        tradeAggregate.high = trade.p;
        tradeAggregate.low = trade.p;
        tradeAggregate.open = trade.p;
        tradeAggregate.close = trade.p;
        tradeAggregate.volume = trade.s;
        tradeAggregate.time = System.currentTimeMillis();
        tradeAggregate.hasBeenReset = false;
        tradeAggregate.prices.add(trade.p);

        currentAggregatedMillisecond = tradeAggregate.time + 50;
        currentAggregatedSecond = currentAggregatedMillisecond / 1000;
    }

    private void initializeQuoteAggregate(PolygonQuote quote) {
        quoteAggregate.hasBeenReset = false;

        quoteAggregate.symbol = quote.sym;
        quoteAggregate.bidOpen = quote.bp;
        quoteAggregate.askOpen = quote.ap;
        quoteAggregate.bidHigh = quote.bp;
        quoteAggregate.askHigh = quote.ap;
        quoteAggregate.bidLow = quote.bp;
        quoteAggregate.askLow = quote.ap;
        quoteAggregate.bidClose = quote.bp;
        quoteAggregate.askClose = quote.ap;
        quoteAggregate.bidVolume = quote.bs;
        quoteAggregate.askVolume = quote.as;

        quoteAggregate.time = System.currentTimeMillis();

        currentAggregatedMillisecond = quoteAggregate.time + 50;
        currentAggregatedSecond = currentAggregatedMillisecond / 1000;
    }

    private void addTradeToAggregate(@NotNull PolygonTrade trade, @NotNull PriceMovement aggregate, boolean minute) {
        long startTime = System.nanoTime();
        int tradeSeconds = new Date(trade.t).getSeconds();
        int aggregateSeconds = new Date(currentAggregatedMillisecond).getSeconds();

        // TODO: check if price is outlandishily different from the previous prices
        if (
            tradeSeconds != aggregateSeconds &&
            tradeSeconds - aggregateSeconds < 0 &&
            tradeSeconds != 0 &&
            aggregateSeconds != 59
        ) {
            if (aggregateSeconds - tradeSeconds > 1) {
                Logger.warn("Trade time does not match aggregate time. Trade: " + tradeSeconds + " Aggregate: " + aggregateSeconds);
            }

            // TODO: figure out how to update a previous bar

            // Only add to volume
            aggregate.volume += trade.s;

            return;
        }

        aggregate.close = trade.p;

        if (aggregate.high < trade.p) {
            aggregate.high = trade.p;
        }

        if (aggregate.low > trade.p) {
            aggregate.low = trade.p;
        }

        aggregate.volume += trade.s;
//        aggregate.prices.add(trade.p);
//        aggregate.trades.put(trade.i, trade);

        long endTime = System.nanoTime();
//        Logger.info("AddTradeCalc " + ((endTime - startTime)) + " ns");

        /**** Extra processing ******/
//            aggregate.prices.add(trade.p);
//            aggregate.prices[aggregate.prices.length] = trade.p;
//
//            if (!aggregate.priceLadder.containsKey(trade.p())) {
//                aggregate.priceLadder.put(trade.p(), trade.s());
//
//                return;
//            }
//
//            Integer priceVolume = aggregate.priceLadder.get(trade.p());
//            aggregate.priceLadder.put(trade.p(), priceVolume + trade.s());
    }

    public void addQuoteToAggregate(@NotNull PolygonQuote quote, @NotNull QuotePriceMovement aggregate) {
        long startTime = System.nanoTime();
        int quoteSeconds = new Date(quote.t).getSeconds();
        int aggregateSeconds = new Date(currentAggregatedMillisecond).getSeconds();

        if (
            quoteSeconds != aggregateSeconds &&
            quoteSeconds - aggregateSeconds < 0 &&
            quoteSeconds != 0 &&
            aggregateSeconds != 59
        ) {
//            if (aggregateSeconds - quoteSeconds > 1) {
//                Logger.warn("Quote time does not match aggregate time. Quote: " + quoteSeconds + " Aggregate: " + aggregateSeconds);
//            }

            // TODO: figure out how to update a previous bar

            // Only add to volume
            aggregate.askVolume += quote.as;
            aggregate.bidVolume += quote.bs;

            return;
        }

        aggregate.askClose = quote.ap;
        aggregate.bidClose = quote.bp;

        if (aggregate.askHigh < quote.ap) {
            aggregate.askHigh = quote.ap;
        }

        if (aggregate.askLow > quote.ap) {
            aggregate.askLow = quote.ap;
        }

        if (aggregate.bidHigh < quote.bp) {
            aggregate.bidHigh = quote.bp;
        }

        if (aggregate.bidLow > quote.bp) {
            aggregate.bidLow = quote.bp;
        }

        aggregate.askVolume += quote.as;
        aggregate.bidVolume += quote.bs;

        // Populate the price ladders for Order book
        if (aggregate.bidPrices.containsKey(quote.bp)) {
            aggregate.bidPrices.put(String.valueOf(quote.bp), aggregate.bidPrices.get(quote.bp) + quote.bs);
        } else {
            aggregate.bidPrices.put(String.valueOf(quote.bp), quote.bs);
        }

        if (aggregate.askPrices.containsKey(quote.ap)) {
            aggregate.askPrices.put(String.valueOf(quote.ap), aggregate.askPrices.get(quote.ap) + quote.as);
        } else {
            aggregate.askPrices.put(String.valueOf(quote.ap), quote.as);
        }
    }
}

