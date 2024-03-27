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

public class PolygonTrade {
    @JSONField(name="ev")
    public String ev; // event type

    @JSONField(name="sym")
    public String sym; // symbol

    @JSONField(name="i")
    public String i; // trade ID

    @JSONField(name="x")
    public Number x; // exchange ID

    @JSONField(name="p")
    public float p; // price

    @JSONField(name="s")
    public int s; // size

    @JSONField(name="c")
    public int[] c; // conditions

    @JSONField(name="t")
    public long t; // timestamp

    @JSONField(name="q")
    public long q; // sequence number

    @JSONField(name="z")
    public long z; // tape

    @JSONField(name="trfi")
    public long trfi; // trade reporting facility ID

    @JSONField(name="trft")
    public long trft; // trade reporting facility timestamp

    public void reset() {
        ev = null;
        sym = null;
        i = null;
        x = null;
        p = 0;
        s = 0;
        c = null;
        t = 0;
        q = 0;
        z = 0;
        trfi = 0;
        trft = 0;
    }
}
