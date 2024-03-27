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

import relativity.instruments.types.MovementEnum;
import relativity.instruments.data.polygon.types.PolygonTrade;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractPriceMovement {
    public float high;
    public float low;
    public float close;
    public float open;
    public long volume;
    public String symbol;
    public long time;
    public long endTime;
    public long endTimeNano;
    public long analysisTime;
    public float change;
    public float percentChange;
    public MovementEnum movement;

    public ArrayList<Float> prices = new ArrayList<>(200);
    public ConcurrentHashMap<String, PolygonTrade> trades = new ConcurrentHashMap<>();
    public Boolean hasBeenReset = true;

    // TODO: change the toString method to something more human readable
    public String toString() {
        return high + " " + low + " " + open + " " + close + " " + volume;
    }

    public void reset() {
//        long startTime = System.nanoTime();
        prices.clear();
        trades.clear();
//        long endTime = System.nanoTime();

//        high = null;
//        low = null;
//        open = null;
//        close = null;
//        volume = null;
//        time = null;
//        socketTime = null;
//        analysisTime = null;
//        percentChange = null;
        symbol = null;
        movement = null;

        hasBeenReset = true;


//        Logger.info("Reset " + (endTime - startTime) + " ns");
    }
}
