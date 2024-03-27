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

package relativity.instruments.types;

import relativity.instruments.PriceMovement;

public class InstrumentQuoteStatistics {
    public InstrumentTrend bidTrend = new InstrumentTrend();
    public InstrumentTrend askTrend = new InstrumentTrend();
    public InstrumentIndicators indicators = new InstrumentIndicators();
    public InstrumentVolume bidVolume = new InstrumentVolume();
    public InstrumentVolume askVolume = new InstrumentVolume();
    public InstrumentVolatility volatility = new InstrumentVolatility();
    public PriceMovement bidMovement = new PriceMovement();
    public PriceMovement askMovement = new PriceMovement();

    public SpreadStatistics spread = new SpreadStatistics();
    public SpreadStatistics last5secondSpread = new SpreadStatistics();
    public SpreadStatistics last10secondSpread = new SpreadStatistics();
    public SpreadStatistics last30secondSpread = new SpreadStatistics();

    public class SpreadStatistics {
        public float buyRatio;
        public float sellRatio;
        public float spread;
        public float spreadPercent;
    }
}
