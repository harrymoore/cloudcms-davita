/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.notifications;

import javax.jms.Session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;

@Configuration
@EnableJms
public class SqsConfig {
	@Value("${notifications.aws.accessKey}")
	private String awsAccessKey;

	@Value("${notifications.aws.region}")
	private String awsRegion;

	@Value("${notifications.aws.secretKey}")
	private String awsSecretKey;

	@Value("${notifications.aws.queueUrl}")
	private String awsQueueUrl;

	// @Bean(name = "invalidationQueueUrl")
	// public String invalidationQueueUrl() {
	// 	return awsQueueUrl;
	// }

	@Bean(name = "invalidationQueueName")
	@Primary
	public String invalidationQueueName() {
		return awsQueueUrl.substring(1+awsQueueUrl.lastIndexOf("/"));
	}

	@Bean
	public AmazonSQSClient amazonSQSClient() {
		return new AmazonSQSClient(getAwsCredentials());
	}

	private AWSCredentials getAwsCredentials() {
		return new BasicAWSCredentials(awsAccessKey, awsSecretKey);
	}

	private AWSCredentialsProvider getAwsCredentialsProvider() {
		return new AWSCredentialsProvider() {
			@Override
			public AWSCredentials getCredentials() {
				return getAwsCredentials();
			}

			@Override
			public void refresh() {
			}
		};
	}

	@Bean
	public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
		final SQSConnectionFactory connectionFactory = SQSConnectionFactory.builder()
				.withRegion(Region.getRegion(Regions.fromName(awsRegion)))
				.withAWSCredentialsProvider(getAwsCredentialsProvider()).build();
		final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setDestinationResolver(new DynamicDestinationResolver());
		factory.setConcurrency("3-10");
		factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
		return factory;
	}
}