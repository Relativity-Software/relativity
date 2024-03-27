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

package relativity.instruments.signals;

import relativity.brokers.types.Position;
import relativity.brokers.types.PositionTypeEnum;

public class MarketValueService {
    public static boolean isMarketValueAbove(Position position, float price, float takeProfitRatio) {
        if (takeProfitRatio < 1) {
            takeProfitRatio += 1;
        }

        boolean isLong = position.type == PositionTypeEnum.LONG;

        float thresholdValue = isLong
            ? position.purchasedValue * takeProfitRatio
            : position.purchasedValue - (position.purchasedValue * (takeProfitRatio - 1));

//        Logger.info(position.symbol + " Market Value: " + position.marketValue + " Threshold Value: " + thresholdValue + " Unrealized Profit: " + position.unrealizedProfit);

        // TODO: recalculate market value here?
        return isLong
            ? position.marketValue > thresholdValue
            : position.marketValue < thresholdValue;
    }

    public static boolean isMarketValueBelow(Position position, float price, float stopLossRatio) {
        boolean isLong = position.type == PositionTypeEnum.LONG;

        float thresholdValue = isLong
            ? position.purchasedValue * stopLossRatio
            : (position.purchasedValue - (position.purchasedValue * stopLossRatio)) + position.purchasedValue;

        // TODO: recalculate market value here?
        return isLong
            ? position.marketValue < thresholdValue
            : position.marketValue > thresholdValue;
    }

    public static Float getTakeProfitThreshold(Position position, float takeProfitRatio) {
        if (takeProfitRatio < 1) {
            takeProfitRatio += 1;
        }

        boolean isLong = position.type == PositionTypeEnum.LONG;

        float thresholdValue = isLong
            ? position.purchasedValue * takeProfitRatio
            : position.purchasedValue - (position.purchasedValue * (takeProfitRatio - 1));

        return thresholdValue;
    }

    public static Float getStopLossThreshold(Position position, float stopLossRatio) {
        boolean isLong = position.type == PositionTypeEnum.LONG;

        float thresholdValue = isLong
            ? position.purchasedValue * stopLossRatio
            : (position.purchasedValue - (position.purchasedValue * stopLossRatio)) + position.purchasedValue;

        return thresholdValue;
    }
}
