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

package relativity.strategies.listeners;

import relativity.brokers.MarketHoursService;
import relativity.brokers.paper.PositionManager;
import relativity.events.EventService;
import relativity.instruments.PriceMovement;
import relativity.instruments.InstrumentManager;
import relativity.instruments.types.MarketHoursEnum;
import relativity.instruments.types.Instrument;
import org.tinylog.Logger;

import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractStrategyListener {
    int preMarketMinuteVolumeMin = 5000;
    int preMarketVolumeMin = 1500;
    int minuteVolumeMin = 10000;
    int volumeMin = 3000;

    boolean secondListenerActive = true;
    boolean minuteListenerActive = true;

    ConcurrentHashMap<String, String> minuteMovers = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, SecondMovers> secondMovers = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, FastMover> fastMovers = new ConcurrentHashMap<>();

    // TODO: Set Market start and end time for this listener

    MarketHoursEnum marketHours = MarketHoursEnum.PRE;

    public PositionManager positionManager;
    public InstrumentManager instrumentManager;

    public EventService eventService;

    class FastMover {
        public String symbol;
        public long time;
        public float price;
        public float volume;

        public FastMover(String symbol, long time, float price, float volume) {
            this.symbol = symbol;
            this.time = time;
            this.price = price;
            this.volume = volume;
        }
    }

    class SecondMovers {
        public String symbol;
        public long time;

        public SecondMovers(String symbol, long time) {
            this.symbol = symbol;
            this.time = time;
        }
    }

    public void initializeTimes() {
        // Set the market hours class member
        MarketHoursService marketHoursService = new MarketHoursService();
        marketHours = marketHoursService.getMarketHours();

        Logger.info("Market Hours: " + marketHours);
    }

    public boolean minute(Instrument instrument, PriceMovement priceMovement) {
        long currentTime = System.nanoTime();
        long fiveMinutesAgo = currentTime - (1_000_000_000 * 60 * 5);

        return false;
    }

    public boolean second(Instrument instrument, PriceMovement priceMovement) {

        long currentTime = System.nanoTime();
        long fifteenSecondsAgo = currentTime - (1_000_000_000 * 15);
        long thirtySecondsAgo = currentTime - (1_000_000_000 * 15);

        int volumeThreshold = marketHours == MarketHoursEnum.MARKET
            ? volumeMin
            : preMarketVolumeMin;

        return false;
    }
}
