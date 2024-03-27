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

package relativity.workers;

import java.util.concurrent.*;

public class ThreadPool {
    public ScheduledExecutorService pool;
    public ExecutorService fixedThreadPool;

    public ThreadPool() {
//        pool = Executors.newScheduledThreadPool(16, new AffinityThreadFactory("bg", SAME_CORE, DIFFERENT_SOCKET, ANY));
        pool = Executors.newScheduledThreadPool(15);
//        fixedThreadPool = Executors.newFixedThreadPool(15);
    }

    public void execute(Runnable task) {
        pool.execute(task);
    }

    public void shutdown() {
        pool.shutdown();
    }

    public void scheduleAtFixedRate(Runnable task, int initialDelay, int period, TimeUnit unit) {
        pool.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public void schedule(Callable task, int initialDelay, TimeUnit unit) {
        pool.schedule(task, initialDelay, unit);
    }

    public void bindExecuteToThread(Runnable task) {
//        pool.
    }

    public CompletableFuture runAsync(Runnable task) {
        // TODO: How to supply our own executor/thread pool here
        return CompletableFuture.runAsync(task);
    }
}
