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

import java.util.ArrayList;

public class Account {
    public String id;

    public String name;
    public String accountId;
    public Float marketValue;

    public Float buyingPower;
    public String currency;

    public Float cashBalance;
    public Float balance;
    public Float marginBalance;
    public Float marginPercentage;
    public Float dayTradingBuyingPower;
    public Float regulationTBuyingPower;
    public Float initialMargin;
    public Float maintenanceMargin;
    public Float lastMaintenanceMargin;
    public Float longMarketValue;
    public Float shortMarketValue;
    public String status; // Convert to an enum
    public long createdAt;
    public long updatedAt;
    public int dayTradeCount = 0;
    public Float outstandingMarginBalance;
    public String userId;

    public Object balancesLock = new Object();
    // TODO: These data structures should be concurrent
    public ArrayList<Position> positions = new ArrayList<>();
    public ArrayList<Position> orders = new ArrayList<>();
}
