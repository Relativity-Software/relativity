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
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

import java.util.ArrayList;

public class InstrumentPricing {
    public Float price;
    public Float vwap;
    public Float vwapRatio;
    // Math.abs(price - vwap) / vwap

    public ArrayList<Float> trailingPrices = new ArrayList<>();
    public ArrayList<PriceMovement> minutePrices = new ArrayList<>();
    public InstrumentPriceStream priceStreams = new InstrumentPriceStream();
    public InstrumentPriceStream minutePriceStreams = new InstrumentPriceStream();

    public BarSeries tradeSeries = new BaseBarSeriesBuilder()
            .withName("second")
            .withMaxBarCount(200)
            .withNumTypeOf(DoubleNum .class)
            .build();

    public BarSeries minuteTradeSeries = new BaseBarSeriesBuilder()
            .withName("minute")
            .withMaxBarCount(200)
            .withNumTypeOf(DoubleNum .class)
            .build();

    public BarSeries quoteAskSeries = new BaseBarSeriesBuilder()
        .withName("quote_ask_second")
        .withMaxBarCount(200)
        .withNumTypeOf(DoubleNum .class)
        .build();

    public BarSeries quoteBidSeries = new BaseBarSeriesBuilder()
        .withName("quote_bid_second")
        .withMaxBarCount(200)
        .withNumTypeOf(DoubleNum .class)
        .build();

    public ArrayList<PriceMovement> quoteAskPrices = new ArrayList<>();
    public ArrayList<PriceMovement> quoteBidPrices = new ArrayList<>();
    public InstrumentPriceStream quoteAskPriceStreams = new InstrumentPriceStream();
    public InstrumentPriceStream quoteBidPriceStreams = new InstrumentPriceStream();

    public void reset() {
        trailingPrices.clear();
        minutePrices.clear();

        quoteAskPrices.clear();
        quoteBidPrices.clear();

        priceStreams.reset();
        minutePriceStreams.reset();
        quoteAskPriceStreams.reset();
        quoteBidPriceStreams.reset();

        tradeSeries = new BaseBarSeriesBuilder()
            .withName("second")
            .withMaxBarCount(200)
            .withNumTypeOf(DoubleNum .class)
            .build();
    }
}
