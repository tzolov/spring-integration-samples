/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.integration.samples.dsl.kafka;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.3
 */
@SpringBootApplication
@EnableConfigurationProperties(KafkaAppProperties.class)
public class Application implements SmartLifecycle {

	private boolean running = false;

	private AtomicBoolean stopRequested = new AtomicBoolean(true);

	private ReentrantLock lock = new ReentrantLock();

	private Condition appRunningCondition = lock.newCondition();

	private Condition appStoppedCondition = lock.newCondition();

	private int count = 100;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}

	@Autowired
	private IntegrationFlowContext flowContext;

	@Autowired
	private KafkaProperties kafkaProperties;

	@Autowired
	private KafkaAppProperties appProperties;

	@Autowired
	private KafkaGateway kafkaGateway;

	@MessagingGateway
	public interface KafkaGateway {

		@Gateway(requestChannel = "toKafka.input")
		void sendToKafka(String payload, @Header(KafkaHeaders.TOPIC) String topic);

		@Gateway(replyChannel = "fromKafka", replyTimeout = 10000)
		Message<?> receiveFromKafka();

	}

	@Bean
	public IntegrationFlow toKafka(KafkaTemplate<?, ?> kafkaTemplate, KafkaAppProperties properties) {
		return f -> f
				.handle(Kafka.outboundChannelAdapter(kafkaTemplate)
						.messageKey(properties.getMessageKey()));
	}

	@Bean
	public IntegrationFlow fromKafkaFlow(ConsumerFactory<?, ?> consumerFactory, KafkaAppProperties properties) {
		return IntegrationFlow
				.from(Kafka.messageDrivenChannelAdapter(consumerFactory, properties.getTopic()))
				.channel(c -> c.queue("fromKafka"))
				.get();
	}

	/*
	 * Boot's autoconfigured KafkaAdmin will provision the topics.
	 */

	@Bean
	public NewTopic topic(KafkaAppProperties properties) {
		return new NewTopic(properties.getTopic(), 1, (short) 1);
	}

	@Bean
	public NewTopic newTopic(KafkaAppProperties properties) {
		return new NewTopic(properties.getNewTopic(), 1, (short) 1);
	}

	public void addAnotherListenerForTopics(String... topics) {
		Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
		// change the group id so we don't revoke the other partitions.
		consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG,
				consumerProperties.get(ConsumerConfig.GROUP_ID_CONFIG) + "x");
		IntegrationFlow flow = IntegrationFlow
				.from(Kafka.messageDrivenChannelAdapter(
						new DefaultKafkaConsumerFactory<String, String>(consumerProperties), topics))
				.channel("fromKafka")
				.get();
		this.flowContext.registration(flow).register();
	}

	//
	// Application SmartLifecycle
	//
	@Override
	public void start() {

		stopRequested.set(false);

		System.out.println("Sending 10 messages...");

		for (int i = 0; i < count; i++) {
			String message = "foo" + i;
			System.out.println("Send to Kafka: " + message);
			conditionalOperation(() -> kafkaGateway.sendToKafka(message, this.appProperties.getTopic()));
		}

		for (int i = 0; i < count; i++) {
			conditionalOperation(() -> {
				sleep(300);
				Message<?> received = kafkaGateway.receiveFromKafka();
				System.out.println(received);
			});	
		}

		conditionalOperation(() -> {
			System.out.println("Adding an adapter for a second topic and sending 10 messages...");
			this.addAnotherListenerForTopics(this.appProperties.getNewTopic());
		});

		for (int i = 0; i < count; i++) {
			String message = "bar" + i;
			System.out.println("Send to Kafka: " + message);
			conditionalOperation(() -> kafkaGateway.sendToKafka(message, this.appProperties.getNewTopic()));
		}
		for (int i = 0; i < count; i++) {
			conditionalOperation(() -> {
				sleep(300);
				Message<?> received = kafkaGateway.receiveFromKafka();
				System.out.println(received);
			});
		}
	}

	@Override
	public void stop() {
		stopRequested.set(true);
		try {
			lock.lock();
			while (this.running) {
				appStoppedCondition.await();
			}
		}
		catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
		finally {
			appRunningCondition.signalAll();
			lock.unlock();
		}
	}

	@Override
	public boolean isRunning() {
		return !this.stopRequested.get();
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	@FunctionalInterface
	public interface ConditionedFunction {
		void apply();
	}

	private void conditionalOperation(ConditionedFunction conditionedFunction) {
		try {
			lock.lock();
			while (stopRequested.get()) {
				appRunningCondition.await();
			}
			running = true;

			conditionedFunction.apply();
		}
		catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
		finally {
			running = false;
			appStoppedCondition.signalAll();
			lock.unlock();

		}
	}
}
