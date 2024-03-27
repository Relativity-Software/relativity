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

import relativity.instruments.signals.MomentumService;
import relativity.instruments.types.Instrument;
import relativity.instruments.signals.RSIDown;
import relativity.instruments.signals.RSIUp;
import relativity.workers.ThreadPool;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.tinylog.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class InstrumentStatisticsService {

    public ThreadPool pool;

    public void addLatestPriceToInstrument(Instrument instrument, PriceMovement price, QuotePriceMovement quotePrice) throws ExecutionException, InterruptedException {
        // TODO: Get average price
//        Logger.info("Adding price to security");

        try{
            instrument.pricing.tradeSeries.addBar(
                    new BaseBar(Duration.ofSeconds(1), Instant.ofEpochMilli(price.endTime).atZone(ZoneId.systemDefault()),
                            price.open, price.high, price.low, price.close, price.volume));
        } catch (Exception e){
            int lastIndex = instrument.pricing.tradeSeries.getEndIndex();
            Bar lastBar = instrument.pricing.tradeSeries.getBar(lastIndex);
            Logger.warn("Last Bar: " + lastBar.getEndTime() + " " + lastBar.getClosePrice() + " " + lastBar.getVolume());
            Logger.warn("Price Time: " + price.time + " Open: " + price.open + " High: " + price.high + " Low: " + price.low + " Close: " + price.close + " Volume: " + price.volume);
            Logger.warn(e.getMessage());
            Logger.warn("error inserting bar (double value) : " + instrument.symbol + ":" + price.time + " current time: " + System.currentTimeMillis());
        }

        instrument.pricing.trailingPrices.add(price.close);
        instrument.pricing.price = price.close;

        instrument.pricing.priceStreams.high.add(price.high);
        instrument.pricing.priceStreams.low.add(price.low);
        instrument.pricing.priceStreams.open.add(price.open);
        instrument.pricing.priceStreams.close.add(price.close);
        instrument.pricing.priceStreams.volume.add(price.volume);
        instrument.pricing.priceStreams.time.add(price.time);

        if (instrument.pricing.priceStreams.high.size() > 200) {
            if (instrument.pricing.trailingPrices.size() >= 200) {
                instrument.pricing.trailingPrices.subList(0, 100).clear();
            }

            instrument.pricing.priceStreams.high.subList(0, 100).clear();
            instrument.pricing.priceStreams.low.subList(0, 100).clear();
            instrument.pricing.priceStreams.open.subList(0, 100).clear();
            instrument.pricing.priceStreams.close.subList(0, 100).clear();
            instrument.pricing.priceStreams.volume.subList(0, 100).clear();
            instrument.pricing.priceStreams.time.subList(0, 100).clear();
        }

        addLatestQuoteToInstrument(instrument, quotePrice);

        updateInstrumentStatistics(instrument, false);
        long nanoTime = System.nanoTime();
        long millis = System.currentTimeMillis();

        price.analysisTime = nanoTime;
        instrument.updatedAt = nanoTime;

//        long lengthOfTimeFromAggregate = (millis - price.time);
        long lengthOfTime = (nanoTime - price.endTimeNano) / 1000;


        // if greater than 500 ms, log it
        if (lengthOfTime > 250_000) {
            Logger.info(instrument.symbol + " Tick to Trade: " + lengthOfTime / 1000 + " ms");
        }

//        Logger.info(security.symbol + " Aggregate Analysis " + lengthOfTimeFromAggregate + " ms");
    }

    public Instrument addLatestMinutePriceToInstrument(Instrument instrument, PriceMovement price) throws ExecutionException, InterruptedException {

        instrument.pricing.minutePriceStreams.high.add(price.high);
        instrument.pricing.minutePriceStreams.low.add(price.low);
        instrument.pricing.minutePriceStreams.open.add(price.open);
        instrument.pricing.minutePriceStreams.close.add(price.close);
        instrument.pricing.minutePriceStreams.volume.add(price.volume);
        instrument.pricing.minutePriceStreams.time.add(price.time);

        int size = instrument.pricing.minutePriceStreams.high.size();
        if (size > 200) {
            instrument.pricing.minutePriceStreams.high.subList(0, 100).clear();
            instrument.pricing.minutePriceStreams.low.subList(0, 100).clear();
            instrument.pricing.minutePriceStreams.open.subList(0, 100).clear();
            instrument.pricing.minutePriceStreams.close.subList(0, 100).clear();
            instrument.pricing.minutePriceStreams.volume.subList(0, 100).clear();
            instrument.pricing.minutePriceStreams.time.subList(0, 100).clear();
        }

        updateInstrumentStatistics(instrument, true);

        instrument.updatedAt = System.nanoTime();

        return instrument;
    }

    public void updateInstrumentStatistics(Instrument instrument, Boolean minute) throws ExecutionException, InterruptedException {
//        synchronized (security) {
        try {
            long startTime = System.nanoTime();
            CompletableFuture<Void> future = CompletableFuture.allOf(
                pool.runAsync(() -> calculateDayHighAndLow(instrument)),
                pool.runAsync(() -> calculateLatestHighAndLow(instrument)),
                pool.runAsync(() -> calculateRSI(instrument)),
                pool.runAsync(() -> calculateVolumeMetrics(instrument)),
                pool.runAsync(() -> calculateTrends(instrument)),
                pool.runAsync(() -> calculateVolatility(instrument)),
                pool.runAsync(() -> calculateMovement(instrument, 10))
            );

            future.get();

            long endTime = System.nanoTime();
            long lengthOfTime = (endTime - startTime) / 1000;
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }

//            if (lengthOfTime < 50) {
//                Logger.info(security.symbol + " Analysis " + lengthOfTime + " µs");
//            }
//        }
    }

    public void updateInstrumentQuoteStatistics(Instrument instrument, Boolean minute) throws ExecutionException, InterruptedException {
        try {
            // TODO: bid trend
            // TODO: ask trend
            // TODO: volume

            if (instrument.pricing.quoteBidPriceStreams.volume.size() > 0) {
                Long bidVolume = instrument.pricing.quoteBidPriceStreams.volume.getLast();
                Long askVolume = instrument.pricing.quoteAskPriceStreams.volume.getLast();

                if (bidVolume > 0 && askVolume > 0) {
                    instrument.quoteStatistics.spread.buyRatio = (float) bidVolume / askVolume;
                    instrument.quoteStatistics.spread.sellRatio = (float) askVolume / bidVolume;

//                    Logger.info(security.symbol + " Spread Buy Ratio: " + security.quoteStatistics.spread.buyRatio + " Sell Ratio: " + security.quoteStatistics.spread.sellRatio + " Bid Volume: " + bidVolume + " Ask Volume: " + askVolume + " open " + security.pricing.priceStreams.open.getLast() + " close " + security.pricing.priceStreams.close.getLast());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void calculateDayHighAndLow(Instrument instrument) {

    }

    public void calculateLatestHighAndLow(Instrument instrument) {

    }

    public void calculateRSI(Instrument instrument) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(instrument.pricing.tradeSeries);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 14);

        instrument.statistics.indicators.rsi = rsiIndicator.getValue(instrument.pricing.tradeSeries.getEndIndex()).floatValue();
        instrument.statistics.indicators.rsiValues.add(instrument.statistics.indicators.rsi);

        // TODO: Add an indicator RSI is in consolidation or within a range
        instrument.statistics.indicators.rsiUp = RSIUp.run(instrument, 2, 14);
        instrument.statistics.indicators.rsiDown = RSIDown.run(instrument, 2, 14);

//        Logger.info(security.symbol + " RSI up: " + security.statistics.indicators.rsiUp + " RSI down: " + security.statistics.indicators.rsiDown + " RSI: " + security.statistics.indicators.rsi);

//        Logger.info(security.symbol + ":RSI:" + security.statistics.indicators.rsi);
    }

    public void calculateStandardDeviation(Instrument instrument) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(instrument.pricing.tradeSeries);
        StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePrice, 14);
        SMAIndicator sma = new SMAIndicator(closePrice, 14);

        instrument.statistics.volatility.standardDeviation = standardDeviation.getValue(instrument.pricing.tradeSeries.getEndIndex()).floatValue();
        instrument.statistics.volatility.standardDeviationPercentage = (instrument.statistics.volatility.standardDeviation / sma.getValue(instrument.pricing.tradeSeries.getEndIndex()).floatValue()) * 100;
        // TODO: do this also for the minute values
    }

    public void calculateAverageTrueRange(Instrument instrument) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(instrument.pricing.tradeSeries);
        ATRIndicator atr = new ATRIndicator(instrument.pricing.tradeSeries, 14);

        instrument.statistics.volatility.atr = atr.getValue(instrument.pricing.tradeSeries.getEndIndex()).floatValue();
        instrument.statistics.volatility.atrPercentage = (instrument.statistics.volatility.atr / instrument.pricing.price) * 100;
    }

    public void calculateVolumeMetrics(Instrument instrument) {
//        security.pricing.tradeSeries.addTrade()
        if (instrument.pricing.priceStreams.volume.size() > 0) {
//            long startTime = System.nanoTime();
             Float sum = 0.0F;

             for (Long volume : instrument.pricing.priceStreams.volume) {
                 sum += volume;
             }

             instrument.statistics.volume.average = sum / instrument.pricing.priceStreams.volume.size();
             instrument.statistics.volume.minuteAverage = sum / (instrument.pricing.priceStreams.volume.size() / 60);

             sum = 0.0F;

             for (Long volume : instrument.pricing.quoteBidPriceStreams.volume) {
                 sum += volume;
             }
             instrument.quoteStatistics.bidVolume.average = sum / instrument.pricing.quoteBidPriceStreams.volume.size();

             sum = 0.0F;

             for (Long volume : instrument.pricing.quoteAskPriceStreams.volume) {
                sum += volume;
             }

             instrument.quoteStatistics.askVolume.average = sum / instrument.pricing.quoteAskPriceStreams.volume.size();
//             security.quoteStatistics.askVolume.average = security.pricing.quoteAskPriceStreams.volume.stream().mapToLong(Long::longValue).average().orElse(0);
//            long endTime = System.nanoTime();
//            Logger.info("(Average) Took " + ((endTime - startTime) / 1000) + " µs ");

//            startTime = System.nanoTime();
            List<Long> sortedVolume = new ArrayList<>(instrument.pricing.priceStreams.volume);
            Collections.sort(sortedVolume);
//            endTime = System.nanoTime();
//            Logger.info("(Collection Sort) Took " + ((endTime - startTime) / 1000) + " µs ");

            instrument.statistics.volume.median = sortedVolume.get(sortedVolume.size() / 2);
            instrument.statistics.volume.minuteMedian = sortedVolume.get((sortedVolume.size() / 60) / 2);
        }
    }

    public void calculateTrends(Instrument instrument) {

    }

    public void calculateVolatility(Instrument instrument) {
        try {
            CompletableFuture<Void> future = CompletableFuture.allOf(
                pool.runAsync(() -> calculateAverageTrueRange(instrument)),
                pool.runAsync(() -> calculateStandardDeviation(instrument))
            );

            future.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
//            Logger.info(security.symbol +  " ATR: " + security.statistics.volatility.atr + " ATR Percentage: " + security.statistics.volatility.atrPercentage + " Standard Deviation: " + security.statistics.volatility.standardDeviation + " Standard Deviation %: " + security.statistics.volatility.standardDeviationPercentage);
    }

    public void calculateMovement(Instrument instrument, int numberOfPrices) {
        instrument.statistics.movement = MomentumService.getMovement(instrument, numberOfPrices);
        instrument.quoteStatistics.askMovement = MomentumService.getMovement(instrument.pricing.quoteAskPriceStreams.close, numberOfPrices);
        instrument.quoteStatistics.bidMovement = MomentumService.getMovement(instrument.pricing.quoteBidPriceStreams.close, numberOfPrices);

//        Logger.info(security.symbol + " Movement: " + priceMovement.movement.value);
    }

    public void addLatestQuoteToInstrument(Instrument instrument, QuotePriceMovement price) throws ExecutionException, InterruptedException {
        instrument.pricing.quoteAskPriceStreams.high.add(price.askHigh);
        instrument.pricing.quoteAskPriceStreams.low.add(price.askLow);
        instrument.pricing.quoteAskPriceStreams.open.add(price.askOpen);
        instrument.pricing.quoteAskPriceStreams.close.add(price.askClose);
        instrument.pricing.quoteAskPriceStreams.volume.add(price.askVolume);
        instrument.pricing.quoteAskPriceStreams.time.add(price.time);

        instrument.pricing.quoteBidPriceStreams.high.add(price.bidHigh);
        instrument.pricing.quoteBidPriceStreams.low.add(price.bidLow);
        instrument.pricing.quoteBidPriceStreams.open.add(price.bidOpen);
        instrument.pricing.quoteBidPriceStreams.close.add(price.bidClose);
        instrument.pricing.quoteBidPriceStreams.volume.add(price.bidVolume);
        instrument.pricing.quoteBidPriceStreams.time.add(price.time);

        try{
            instrument.pricing.quoteAskSeries.addBar(new BaseBar(
                Duration.ofSeconds(1),
                Instant.ofEpochMilli(price.endTime).atZone(ZoneId.systemDefault()
            ), price.askOpen, price.askHigh, price.askLow, price.askClose, price.askVolume));

            instrument.pricing.quoteBidSeries.addBar(new BaseBar(
                Duration.ofSeconds(1),
                Instant.ofEpochMilli(price.endTime).atZone(ZoneId.systemDefault()
            ), price.bidOpen, price.bidHigh, price.bidLow, price.bidClose, price.bidVolume));
        } catch (Exception e){
            int lastIndex = instrument.pricing.quoteAskSeries.getEndIndex();
            Bar lastBar = instrument.pricing.quoteAskSeries.getBar(lastIndex);
            Logger.warn("Last Bar: " + lastBar.getEndTime() + " " + lastBar.getClosePrice() + " " + lastBar.getVolume());
            Logger.warn("Ask - Price Time: " + price.time + " Open: " + price.askOpen + " High: " + price.askHigh + " Low: " + price.askLow + " Close: " + price.askClose + " Volume: " + price.askVolume);
            Logger.warn("Bid - Price Time: " + price.time + " Open: " + price.bidOpen + " High: " + price.bidHigh + " Low: " + price.bidLow + " Close: " + price.bidClose + " Volume: " + price.bidVolume);
            Logger.warn(e.getMessage());
            Logger.warn("error inserting bar (double value) : " + instrument.symbol + ":" + price.time + " current time: " + System.currentTimeMillis());
        }

        int size = instrument.pricing.quoteAskPriceStreams.high.size();

        if (size > 200) {
            instrument.pricing.quoteAskPriceStreams.high.subList(0, 100).clear();
            instrument.pricing.quoteAskPriceStreams.low.subList(0, 100).clear();
            instrument.pricing.quoteAskPriceStreams.open.subList(0, 100).clear();
            instrument.pricing.quoteAskPriceStreams.close.subList(0, 100).clear();
            instrument.pricing.quoteAskPriceStreams.volume.subList(0, 100).clear();
            instrument.pricing.quoteAskPriceStreams.time.subList(0, 100).clear();

            instrument.pricing.quoteBidPriceStreams.high.subList(0, 100).clear();
            instrument.pricing.quoteBidPriceStreams.low.subList(0, 100).clear();
            instrument.pricing.quoteBidPriceStreams.open.subList(0, 100).clear();
            instrument.pricing.quoteBidPriceStreams.close.subList(0, 100).clear();
            instrument.pricing.quoteBidPriceStreams.volume.subList(0, 100).clear();
            instrument.pricing.quoteBidPriceStreams.time.subList(0, 100).clear();
        }

        updateInstrumentQuoteStatistics(instrument, false);

        instrument.updatedAt = System.nanoTime();
    }
}
