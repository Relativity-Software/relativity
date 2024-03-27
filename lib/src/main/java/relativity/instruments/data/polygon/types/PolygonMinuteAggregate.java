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

package relativity.instruments.data.polygon.types;

public class PolygonMinuteAggregate {
    String ev;  // Event type
    String sym;  // Symbol
    long v;  // Volume
    long av;  // Accumulated volume
    float op;  // Open price
    float vw;  // VWAP
    float o;  // Open price
    float c;  // Close price
    float h;  // High price
    float l;  // Low price
    float a;  // Average price
    long z;  // Trades
    long s;  // Start timestamp
    long e;  // End timestamp
}
