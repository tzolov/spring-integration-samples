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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Christian Tzolov
 */
@Configuration
public class CRaCAdapterAutoConfiguration {

    private static Log logger = LogFactory.getLog(CRaCAdapterAutoConfiguration.class);

    @Bean
    public CRaCAdapter cracAdapter(ConfigurableApplicationContext applicationContext) {
        logger.info("The CRaC (Coordinated Restore at Checkpoint) is activated!" +
                " CRaC compliant JDK is required to create/resolve checkpoints");
        return new CRaCAdapter(applicationContext);
    }
}
