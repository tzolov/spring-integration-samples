/*
 * Copyright 2023-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.crac;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;

import org.springframework.context.ConfigurableApplicationContext;

public class CRaCAdapter implements Resource {

    private ConfigurableApplicationContext applicationContext;

    public CRaCAdapter(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        Core.getGlobalContext().register(this);
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        System.out.println(">>>>>>>>>>>>>>>>>>>> beforeCheckpoint [Start]");
        this.applicationContext.stop();
        System.out.println(">>>>>>>>>>>>>>>>>>>> beforeCheckpoint [Complete]");
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        System.out.println(">>>>>>>>>>>>>>>>>>>> afterRestore [Start]");
        this.applicationContext.start();
        Thread.sleep(20000);
        System.out.println(">>>>>>>>>>>>>>>>>>>> afterRestore [Complete]");
    }
}
