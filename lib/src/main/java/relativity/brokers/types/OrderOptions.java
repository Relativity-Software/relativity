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

package relativity.brokers.types;

import java.util.UUID;

public class OrderOptions {
    public String symbol;
    public Float quantity;
    public OrderSideEnum orderSide = OrderSideEnum.BUY;
    public Float limitPrice;
    public boolean limit = true;
    public String accountId;
    public String userId;
    public Float marginPercentage;
    public OrderTypeEnum orderType;
    public OrderIntentEnum intent;
    public String reason;
    public OrderTriggeredByEnum triggeredBy;
    public String strategyName;
    public UUID strategyId;
    public OrderTimeInForceEnum timeInForce;
    public OrderSideEnum side;

    public LegLimitPrice takeProfit = new LegLimitPrice();
    public LegLimitPrice stopLoss = new LegLimitPrice();

    public class LegLimitPrice {
        public Float limitPrice;
    }

    // TODO: Determine how to add these here
//    takeProfit?: {
//        limitPrice: number
//    }
//    stopLoss?: {
//        limitPrice: number
//    }
}
