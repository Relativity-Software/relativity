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

package relativity.strategies.active;

import relativity.instruments.types.Instrument;
import relativity.strategies.MultiBaseStrategy;
import relativity.workers.ThreadPool;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public class SPEnsembleStrategy extends MultiBaseStrategy {
    public HashSet<String> symbols = new HashSet<>();
    public HashSet<String> spSymbols = new HashSet<>();

    Comparator<Instrument> hourComparator = (instrument1, instrument2) -> compare(instrument1, instrument2, 61);

    Comparator<Instrument> thirtyMinutesComparator = (instrument1, instrument2) -> compare(instrument1, instrument2, 31);

    Comparator<Instrument> fifteenMinutesComparator = (instrument1, instrument2) -> compare(instrument1, instrument2, 16);

    public ConcurrentSkipListSet<Instrument> rankedHourList = new ConcurrentSkipListSet<>(hourComparator);
    public ConcurrentSkipListSet<Instrument> rankedThirtyMinuteList = new ConcurrentSkipListSet<>(thirtyMinutesComparator);
    public ConcurrentSkipListSet<Instrument> rankedFifteenMinuteList = new ConcurrentSkipListSet<>(fifteenMinutesComparator);

    public ConcurrentHashMap<String, SPSingleStrategy> activeStrategies = new ConcurrentHashMap<>();

    public HashSet<String> topTenHour = new HashSet<>();
    public HashSet<String> bottomTenHour = new HashSet<>();
    public HashSet<String> topTenThirtyMinutes = new HashSet<>();
    public HashSet<String> bottomTenThirtyMinutes = new HashSet<>();
    public HashSet<String> topTenFifteenMinutes = new HashSet<>();
    public HashSet<String> bottomTenFifteenMinutes = new HashSet<>();

    private ThreadPool pool;

    public SPEnsembleStrategy(ThreadPool pool) {
        this.pool = pool;
    }

    public void initialize() {
        resort();

        LocalTime now = LocalTime.now();
        int secondsTillNextMinute = 60 - now.getSecond();
        int minute = now.getMinute();
        int minutesTillFifteen = 15 - 1 - (minute % 15); // Subtract one minute initially due to already being in minute

        this.pool.scheduleAtFixedRate(
            () -> {},
        secondsTillNextMinute + (minutesTillFifteen * 60) + 10, // 10 extra seconds to allow for processing
        15 * 60, TimeUnit.SECONDS
        );
    }

    public void resort() {
        rankedHourList.clear();
        rankedThirtyMinuteList.clear();
        rankedFifteenMinuteList.clear();

        // Re-add updated securities to each list
        for (String symbol : spSymbols) {
            rankedHourList.add(instrumentManager.getInstrument(symbol));
            rankedThirtyMinuteList.add(instrumentManager.getInstrument(symbol));
            rankedFifteenMinuteList.add(instrumentManager.getInstrument(symbol));
        }
    }

    public int compare(Instrument instrument1, Instrument instrument2, int size) {
        float instrument1Last = instrument1.pricing.minutePriceStreams.close.getLast();
        float instrument2Last = instrument2.pricing.minutePriceStreams.close.getLast();

        if(instrument1Last == 0 || instrument2Last == 0) {
            // Handle divide by zero
        }

        float instrument1Difference = instrument1Last - instrument1.pricing.minutePriceStreams.close.get(instrument1.pricing.minutePriceStreams.close.size() - size);
        float instrument2Difference = instrument2Last - instrument2.pricing.minutePriceStreams.close.get(instrument2.pricing.minutePriceStreams.close.size() - size);

        return Float.compare(instrument1Difference / instrument1Last, instrument2Difference / instrument2Last);
    }

    public void check() {
        // TODO: This should possibly run every 15 minutes 10 seconds
        resort();
        setTopTen();
        setBottomTen();
    }

    public void setTopTen() {
        HashSet<String> hourTopTen = getFirst10(rankedHourList);
        HashSet<String> thirtyMinutesTopTen = getFirst10(rankedThirtyMinuteList);
        HashSet<String> fifteenMinutesTopTen = getFirst10(rankedFifteenMinuteList);

        addStrategiesNotInList(hourTopTen, topTenHour);
        removeStrategiesNotInList(hourTopTen, topTenHour);

        // Set the new Set
        topTenHour = hourTopTen;

        addStrategiesNotInList(thirtyMinutesTopTen, topTenThirtyMinutes);
        removeStrategiesNotInList(thirtyMinutesTopTen, topTenThirtyMinutes);

        topTenThirtyMinutes = thirtyMinutesTopTen;

        addStrategiesNotInList(fifteenMinutesTopTen, topTenFifteenMinutes);
        removeStrategiesNotInList(fifteenMinutesTopTen, topTenFifteenMinutes);

        topTenThirtyMinutes = thirtyMinutesTopTen;
    }

    public void setBottomTen() {
        HashSet<String> hourBottomTen = getLast10(rankedHourList);
        HashSet<String> thirtyMinutesBottomTen = getLast10(rankedThirtyMinuteList);
        HashSet<String> fifteenMinutesBottomTen = getLast10(rankedFifteenMinuteList);

        addStrategiesNotInList(hourBottomTen, bottomTenHour);
        removeStrategiesNotInList(hourBottomTen, bottomTenHour);

        bottomTenHour = hourBottomTen;

        addStrategiesNotInList(thirtyMinutesBottomTen, bottomTenThirtyMinutes);
        removeStrategiesNotInList(thirtyMinutesBottomTen, bottomTenThirtyMinutes);

        bottomTenThirtyMinutes = thirtyMinutesBottomTen;

        addStrategiesNotInList(fifteenMinutesBottomTen, bottomTenFifteenMinutes);
        removeStrategiesNotInList(fifteenMinutesBottomTen, bottomTenFifteenMinutes);

        bottomTenFifteenMinutes = fifteenMinutesBottomTen;
    }

    public HashSet<String> getFirst10(ConcurrentSkipListSet<Instrument> set) {
        Iterator<Instrument> iterator = set.iterator();
        HashSet<String> first10 = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            if (!iterator.hasNext()) {
                break;
            }

            first10.add(iterator.next().symbol);
        }

        return first10;
    }

    public HashSet<String> getLast10(ConcurrentSkipListSet<Instrument> set) {
        Iterator<Instrument> iterator = set.descendingIterator();
        HashSet<String> last10 = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            if (!iterator.hasNext()) {
                break;
            }

            last10.add(iterator.next().symbol);
        }

        return last10;
    }

    public void addStrategiesNotInList(HashSet<String> newList, HashSet<String> oldList) {
        for (String symbol : newList) {
            if (!oldList.contains(symbol)) {
                addStrategy(symbol);
            }
        }
    }

    public void removeStrategiesNotInList(HashSet<String> newList, HashSet<String> oldList) {
        for (String symbol : oldList) {
            if (!newList.contains(symbol)) {
                removeStrategy(activeStrategies.get(symbol));
            }
        }
    }

    public void addStrategy(String symbol) {
        if (activeStrategies.containsKey(symbol)) {
            return;
        }

        SPSingleStrategy strategy = new SPSingleStrategy(symbol);
        strategy.initialize();
        activeStrategies.put(symbol, strategy);
    }
}
