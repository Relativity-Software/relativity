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

import com.alibaba.fastjson2.annotation.JSONField;

public class PolygonQuote {
    @JSONField(name="ev")
    public String ev; // event type

    @JSONField(name="sym")
    public String sym; // symbol

    @JSONField(name="bx")
    public long bx; // bid exchange

    @JSONField(name="ax")
    public long ax; // ask exchange

    @JSONField(name="bp")
    public float bp; // bid price

    @JSONField(name="ap")
    public float ap; // ask price

    @JSONField(name="bs")
    public long bs; // bid size

    @JSONField(name="as")
    public long as; // ask size

    @JSONField(name="c")
    public int c; // condition

    @JSONField(name="i")
    public int[] i; // indicators

    @JSONField(name="t")
    public long t; // timestamp

    @JSONField(name="q")
    public long q; // sequence number

    public void reset() {
        sym = null;
        bx = 0;
        ax = 0;
        bp = 0;
        ap = 0;
        bs = 0;
        as = 0;
        c = 0;
        i = null;
        t = 0;
        q = 0;
    }
}
