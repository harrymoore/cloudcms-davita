/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.notifications;

import javax.jms.Session;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

@Configuration
@EnableJms
@ConditionalOnProperty(name="notifications.notificationType", havingValue="sqs")
public class SqsConfig {
    @Value("${notifications.aws.accessKey}")
    private String awsAccessKey;

    @Value("${notifications.aws.region}")
    private String awsRegion;

    @Value("${notifications.aws.secretKey}")
    private String awsSecretKey;

    @Value("${notifications.aws.queueUrl}")
    private String awsQueueUrl;

    @Value("${notifications.notificationType}")
    private String notificationsType;

    // @Bean(name = "invalidationQueueUrl")
    // public String invalidationQueueUrl() {
    // return awsQueueUrl;
    // }

    @Bean(name = "invalidationQueueName")
    // @Primary
    public String invalidationQueueName() {
        return awsQueueUrl.substring(1 + awsQueueUrl.lastIndexOf("/"));
    }

    @Bean(name = "notificationsType")
    public String notificationsType() {
        return notificationsType;
    }

    @Bean
    public AmazonSQS amazonSQSClient() {
        return AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(getAwsCredentials()))
                .build();
    }

    private AWSCredentials getAwsCredentials() {
        return new BasicAWSCredentials(awsAccessKey, awsSecretKey);
    }
    
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        final SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(), amazonSQSClient());
        final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDestinationResolver(new DynamicDestinationResolver());
        factory.setConcurrency("3-10");
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);

        return factory;
    }
}