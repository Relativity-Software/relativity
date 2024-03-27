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

import relativity.instruments.types.Instrument;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class InstrumentManager {
    final ConcurrentHashMap<String, Instrument> instruments = new ConcurrentHashMap<>();
    private final ArrayList<Instrument> availableInstruments = new ArrayList<>(10_000);

    public void initializeInstrumentObjects() {
        for (int i = 0; i < 10_000; i++) {
            availableInstruments.add(new Instrument());
        }
    }

    public Instrument getInstrument(String symbol) {
        return instruments.computeIfAbsent(symbol, this::addInstrument);
    }

    public int getSecuritiesSize() {
        return instruments.size();
    }

    private synchronized Instrument addInstrument(String symbol) {
        Instrument instrument = !availableInstruments.isEmpty()
                ? availableInstruments.removeLast()
                : new Instrument();

        instrument.symbol = symbol;

        return instrument;
    }

    public Float getLatestTrailingPrice(String symbol) {
        Instrument instrument = getInstrument(symbol);

        return instrument.pricing.trailingPrices.get(instrument.pricing.trailingPrices.size() - 1);
    }

    public Float getBuyOrSellPrice(String symbol) {
        return this.getLatestTrailingPrice(symbol);
    }

    public Float getBuyOrSellPrice(String symbol, Float ratio) {
        return getLatestTrailingPrice(symbol) * ratio;
    }

    public Float getBuyOrSellPrice(String symbol, Float ratio, Float offset) {
        return getLatestTrailingPrice(symbol) * ratio + offset;
    }

    public boolean hasPricing(String symbol) {
        return getInstrument(symbol).pricing.trailingPrices.size() > 0;
    }

    public void resetPricingAndStatistics(String symbol) {
        Instrument instrument = getInstrument(symbol);
        instrument.pricing.trailingPrices.clear();
//        security.statistics.reset();
    }

    public void resetPricingAndStatisticsAll() {
        for (Instrument instrument : instruments.values()) {
            resetPricingAndStatistics(instrument.symbol);
        }
    }
}
