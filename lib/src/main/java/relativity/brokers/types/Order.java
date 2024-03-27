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

import relativity.instruments.types.Instrument;

import java.util.ArrayList;
import java.util.UUID;

public class Order {
    public String id;
    public Instrument instrument;
    public UUID strategyId;
    public String strategyName;
    public String accountId;
    public String symbol;
    public String reason;
    public OrderTimeInForceEnum timeInForce;
    public Float quantity;
    public Float filledQuantity;
    public Long filledAt;
    public Float stopPrice;
    public Float limitPrice;
    public Float filledAveragePrice;
    public OrderStatusEnum status;
    public OrderTypeEnum type;
    public Float trailAmount;
    public boolean extendedHours = false;
    public Long submittedAt;
    public Long expiredAt;
    public Long canceledAt;
    public Long failedAt;
    public Long createdAt;
    public Long updatedAt;
    public Float cashBalance;
    public OrderIntentEnum intent;
    public Float marginBalance;
    public Float marketValue;
    public OrderClassEnum orderClass;
    public Order parentOrder;
    public OrderSideEnum side;
    public Float trailPercent;
    public OrderTriggeredByEnum triggeredBy;
    public String userId;
    public ArrayList<Position> positions = new ArrayList<>();
    public ArrayList<Order> childOrders = new ArrayList<>();
    public ArrayList<Order> peerOrders = new ArrayList<>();
    public ArrayList<OrderFill> orderFills = new ArrayList<>();

    public Object raw;
}
