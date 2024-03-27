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

import java.util.ArrayList;

public class InstrumentIndicators {
    public Float rsi;
    public Float minuteRsi;
    public Boolean rsiUp;
    public Boolean rsiDown;
    public ArrayList<Float> rsiValues = new ArrayList<>();
    public ArrayList<Float> minuteRSIValues = new ArrayList<>();
    public Float vwap;
    public Float macd;
    public Float sma;
    public Float ema;
    public Float adx;
    public Float ppo;
    public Float pvo;
}
