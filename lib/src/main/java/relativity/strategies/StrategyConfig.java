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

import relativity.brokers.types.PositionTypeEnum;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;

public class StrategyConfig {
    public Boolean splitOrderIntoLots = false;
    public Boolean useTriggerOrders = false;
    public Boolean enableCircuitBreakers = true;
    public Boolean exitImmediatelyIfEntryOrderNotFilled = false;
    public Boolean exitImmediatelyIfAccountBalanceNotSufficient = true;
    public Boolean dynamicPositionSizing = false;
    public Boolean trailingStopLoss = false;

    public PositionTypeEnum positionType = PositionTypeEnum.LONG;

    public String tradingType = "day";
    public String interval = "second";

    public LocalTime startTime;
    public LocalTime endTime;

    public int buyLockCount = 0;
    public int cancelOrderTimeThreshold = 30_000; // milliseconds
    public int cancelEnterPositionOrderTimeThreshold = 30_000; // milliseconds
    public int maximumPositionDurationOfStrategy = 120_000; // milliseconds

    public float takeProfitRatio;
    public float stopLossRatio;
    public float trailingStopLossRatio;
    public boolean stopLossRatioFreeze = false;
    public float marginPercent = 0;
    public float profitThresholdPercentage = 0.033f;
    public float lossThresholdPercentage = 0.04f;
    public int profitMax = 2_000;
    public float buyPriceDifferential = 0;
    public float balancePercentageToUsePreMarket = 0.15f;
    public float balancePercentageToUse = 0.15f;

    public boolean useAverageVolume = true;

    public boolean floorShares = true;

    public ArrayList<StrategyHandlers> handlers = new ArrayList<>(Arrays.asList(
            StrategyHandlers.CHECK_ITERATION,
            StrategyHandlers.CHECK_EXITED,

            StrategyHandlers.CHECK_ORDER_LOCK,
            StrategyHandlers.CHECK_PROFIT,

            StrategyHandlers.REPLACE_STOP_LOSS_ORDER,
            StrategyHandlers.STOP_LOSS,
            StrategyHandlers.REPLACE_TAKE_PROFIT_ORDER,
            StrategyHandlers.TAKE_PROFIT,
            StrategyHandlers.REPLACE_ENTER_POSITION_ORDER,

            StrategyHandlers.CHECK_OLD_ORDERS,
            StrategyHandlers.CHECK_TRADING_HOURS,
            StrategyHandlers.CHECK_CIRCUIT_BREAKER,
            StrategyHandlers.ENTER_POSITION
    ));
}
