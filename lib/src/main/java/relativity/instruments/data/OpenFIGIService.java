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

package relativity.instruments.data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class OpenFIGIService {
    HttpClient client = HttpClient.newHttpClient();
    String baseURL = "https://api.openfigi.com/v1";
    String apiKey;

    public enum MethodType {
        GET,
        POST
    }

    public void getMappings(String apiKey, ArrayList<Object> jobs) throws IOException, InterruptedException {
        // TODO: Iterate over jobs for JSON string

        HttpRequest request = createPOSTRequest("/mapping", "");
        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpRequest createGetRequest(String path) {
        return HttpRequest.newBuilder()
            .uri(URI.create(baseURL + path))
            .header("Content-Type", "application/json")
            .header("X-OPENFIGI-APIKEY", apiKey)
            .GET()
            .build();
    }

    public HttpRequest createPOSTRequest(String path, String body) {
        return HttpRequest.newBuilder()
            .uri(URI.create(baseURL + path))
            .header("Content-Type", "application/json")
            .header("X-OPENFIGI-APIKEY", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    }
}
