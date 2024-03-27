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

import relativity.instruments.data.polygon.types.PolygonQuote;
import relativity.instruments.types.MovementEnum;

import java.util.concurrent.ConcurrentHashMap;

public class QuotePriceMovement extends PriceMovement {
    public float bidHigh;
    public float bidLow;
    public float bidClose;
    public float bidOpen;
    public long bidVolume;

    public float askHigh;
    public float askLow;
    public float askClose;
    public float askOpen;
    public long askVolume;

    public ConcurrentHashMap<String, SimpleLimitOrder> buyOrders;
    public ConcurrentHashMap<String, SimpleLimitOrder> sellOrders;

    public ConcurrentHashMap<String, Long> bidPrices = new ConcurrentHashMap<>() {};
    public ConcurrentHashMap<String, Long> askPrices = new ConcurrentHashMap<>() {};

    public float spread; // This can be calculated from the close prices
    public float spreadPercent;

    public MovementEnum bidMovement;
    public MovementEnum askMovement;

    public ConcurrentHashMap<String, PolygonQuote> quotes = new ConcurrentHashMap<>();

    // https://www.quantstart.com/articles/high-frequency-trading-ii-limit-order-book/
    public float midPrice; // Average of ask and bid prices

    public float microPrice; // A version of VWAP - Is it different

    @Override
    public void reset() {
        super.reset();

        bidPrices.clear();
        askPrices.clear();
    }
}
