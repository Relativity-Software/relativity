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

package relativity.strategies;

import relativity.brokers.MarketHoursService;
import relativity.brokers.types.Account;
import relativity.instruments.types.Instrument;
import org.tinylog.Logger;

public class StrategyPositionSizing {

    public static float calculatePositionSize(BaseStrategy strategy, Float price) {
        boolean extendedHours = MarketHoursService.isInExtendedHours();

        Instrument instrument = strategy.instrumentManager.getInstrument(strategy.symbol);
        Account account = strategy.accountManager.getAccount(strategy.accountId);

        if (account.cashBalance <= 100) {
            Logger.info("Not enough cash to place a trade");

            return 0;
        }

        float totalCash = account.cashBalance + strategy.accountManager.getCashBalanceFromPositionsAndOrders(account.id);
        float currentBuyingPower = strategy.accountManager.calculateBuyingPower(account.id);

        double cashToUse = extendedHours
            ? totalCash * strategy.config.balancePercentageToUsePreMarket
            : totalCash * strategy.config.balancePercentageToUse;

        if (account.cashBalance < cashToUse) {
            cashToUse = account.cashBalance - 30;
        }

        double marginBuyingPower = 0.0f;

        if (strategy.config.marginPercent > 0) {
            double marginToAdd = (account.marginBalance + account.outstandingMarginBalance) * strategy.config.marginPercent;

            marginBuyingPower += marginToAdd <= account.marginBalance
                ? marginToAdd
                : account.marginBalance - 30;
        }

        double totalBuyingPower = cashToUse + marginBuyingPower;

        if (totalBuyingPower > currentBuyingPower) {
            totalBuyingPower = currentBuyingPower - 30;

            if (totalBuyingPower < 1000) {
                Logger.info("Not enough buying power to place a trade");

                return 0;
            }
        }

        Logger.info("Current buying power " + currentBuyingPower + " Cash to use: " + cashToUse + " Margin to use: " + marginBuyingPower + " Total buying power: " + totalBuyingPower);

        return getSharesForBuyingPower(strategy, instrument, price, (float) totalBuyingPower);
    }

    public static float getSharesForBuyingPower(BaseStrategy strategy, Instrument instrument, float price, float totalBuyingPower) {
        float shares = totalBuyingPower / price;

        float averageVolume = instrument.statistics.volume.minuteAverage;

        Logger.info("Shares: " + shares + " Average volume " + averageVolume + " Price: " + price + " Total buying power: " + totalBuyingPower + " " + instrument.symbol);

        if (
            strategy.config.useAverageVolume &&
            averageVolume * 0.016 < shares
        ) {
            shares = averageVolume * 0.016f;
        }

        if (strategy.config.floorShares) {
            shares = Math.round(shares);
        }

        if (shares < 1) {
            Logger.info("Not enough buying power to place a trade");

            return 0;
        }

        Logger.info("Eventual shares: " + shares + " Price: " + price + " Total buying power: " + totalBuyingPower + " " + instrument.symbol);

        return shares;
    }
}
