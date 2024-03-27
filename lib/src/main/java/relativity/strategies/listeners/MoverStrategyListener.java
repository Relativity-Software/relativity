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

//import accounts.types.Account;
import relativity.brokers.types.Position;
import relativity.brokers.types.PositionTypeEnum;
import relativity.events.types.ActivateStrategyEvent;
import relativity.instruments.PriceMovement;
import relativity.instruments.types.MarketHoursEnum;
import relativity.instruments.types.MovementEnum;
import relativity.instruments.types.Instrument;
import org.tinylog.Logger;

public class MoverStrategyListener extends AbstractStrategyListener {

    public boolean second(Instrument instrument, PriceMovement priceMovement) {
        long currentTimeMillis = System.currentTimeMillis();
        long fifteenSecondsAgo = currentTimeMillis - (1000 * 15);
        long thirtySecondsAgo = currentTimeMillis - (1000 * 15);

        int volumeThreshold = marketHours == MarketHoursEnum.MARKET
            ? volumeMin
            : preMarketVolumeMin;

        int liquidityThreshold = marketHours == MarketHoursEnum.MARKET
            ? 40_000
            : 20_000;

        Position latestPosition = positionManager.getLatestPosition(instrument.symbol);
        FastMover fastMover = fastMovers.get(instrument.symbol);
        Long lastVolume = instrument.pricing.priceStreams.volume.get(instrument.pricing.priceStreams.volume.size() - 1);

        /*** Temporary logging for debugging ***/
//        if (
//            (
//                security.statistics.movement.percentChange > 0.0025 &&
//                priceMovement.close > 7 /*&&
//                priceMovement.volume > volumeThreshold */
//            )
//        ) {
//            Logger.info(security.symbol + " Mover: " + String.format("%.4f", security.statistics.movement.percentChange) + " " + security.statistics.movement.movement + " Volume: " + priceMovement.volume + " Close: " + priceMovement.close + " Liquidity " + security.statistics.volume.average * priceMovement.close);
//        }

        if (
            (
                (
                  fastMover != null &&
                  (
                    fastMover.volume <= lastVolume ||
                    lastVolume / fastMover.volume >= 0.8
                  )
                ) ||
                (
                    instrument.statistics.volume.average != null &&
                    instrument.statistics.volume.average * priceMovement.close > liquidityThreshold &&
                    priceMovement.volume >= instrument.statistics.volume.average * 2
                )
            ) &&
            instrument.pricing.priceStreams.volume.size() > 10 &&
            instrument.statistics.movement.movement != null &&
            priceMovement.volume > volumeThreshold &&
//            priceMovement.close > 7 &&
            (
                !secondMovers.containsKey(instrument.symbol) ||
                (
                    secondMovers.containsKey(instrument.symbol) &&
                    secondMovers.get(instrument.symbol).time < fifteenSecondsAgo
                )
            ) &&
            (
              latestPosition == null ||
              latestPosition.closedAt < thirtySecondsAgo
            ) &&
            Math.abs(instrument.statistics.movement.percentChange) > 0.003 /*&&
            security.statistics.volatility.atrPercentage < 1.5*/
        ) {
            if (fastMover == null) {
                fastMover = new FastMover(instrument.symbol, System.currentTimeMillis(), priceMovement.close, lastVolume);
            }

            fastMover.time = System.currentTimeMillis();
            fastMover.price = priceMovement.close;
            fastMover.volume = lastVolume;

            fastMovers.put(instrument.symbol, fastMover);
            secondMovers.put(instrument.symbol, new SecondMovers(instrument.symbol, fastMover.time));

            Logger.info(instrument.symbol + " Mover! Liquidity: " + instrument.statistics.volume.average * priceMovement.close +
                " Average: " + instrument.statistics.volume.average + " Volume: " + priceMovement.volume +
                " Close: " + priceMovement.close + " Movement: " + instrument.statistics.movement.movement +
                " " + String.format("%.4f", instrument.statistics.movement.percentChange) + " ATR: " + String.format("%.4f", instrument.statistics.volatility.atr) +
                " ATR Percentage: " + String.format("%.4f", instrument.statistics.volatility.atrPercentage) +
                " buyRatio: " + String.format("%.4f", instrument.quoteStatistics.spread.buyRatio) +
                " sellRatio: " + String.format("%.4f", instrument.quoteStatistics.spread.sellRatio) +
                " bidMovement: " + instrument.quoteStatistics.bidMovement.movement + " askMovement: " + instrument.quoteStatistics.askMovement.movement);

            eventService.processEvent(new ActivateStrategyEvent(
                "MoverStrategy",
            instrument,
                instrument.statistics.movement.movement == MovementEnum.UP
                    ? PositionTypeEnum.LONG
                    : PositionTypeEnum.SHORT
            ));

            return true;
        }

        return false;
    }
}
