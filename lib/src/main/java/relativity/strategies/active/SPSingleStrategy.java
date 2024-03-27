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

import relativity.brokers.types.PositionTypeEnum;
import relativity.strategies.BaseStrategy;
import relativity.strategies.StrategyHandlers;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;

public class SPSingleStrategy extends BaseStrategy {

    public SPSingleStrategy(String symbol) {
        super(symbol);

        name = "S&P Single Strategy";

        state.startedAt = LocalDateTime.now();

        config.takeProfitRatio = 1.10F;
        config.stopLossRatio = 0.998F; // This could be updated for each strategy based on ATR

        config.positionType = PositionTypeEnum.LONG;

        config.startTime = LocalTime.of(3, 0);
        config.endTime = LocalTime.of(18, 0);

        config.cancelEnterPositionOrderTimeThreshold = 30_000;
        config.maximumPositionDurationOfStrategy = 120_000;
        config.cancelOrderTimeThreshold = 30_000;

        config.balancePercentageToUse = 0.0166f;
        config.marginPercent = 0.0166f;

        config.dynamicPositionSizing = true;
        config.useAverageVolume = true;
        config.useTriggerOrders = false;
        config.exitImmediatelyIfEntryOrderNotFilled = false;

        config.handlers = new ArrayList<>(Arrays.asList(
            StrategyHandlers.CHECK_ITERATION,
            StrategyHandlers.CHECK_EXITED,

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

    public void initialize() {

    }
}
