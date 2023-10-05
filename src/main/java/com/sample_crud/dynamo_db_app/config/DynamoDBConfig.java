package com.sample_crud.dynamo_db_app.config;

import org.springframework.context.annotation.Bean;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableDynamoDBRepositories(basePackages = "com.sample_crud.dynamo_db_app.repository")
public class DynamoDBConfig {
    @Value("${amazon.dynamodb.endpoint}")
    String endpoint;
    @Value("${amazon.aws.accesskey}")
    String accesskey;
    @Value("${amazon.aws.secretkey}")
    String secretkey;
    @Value("${amazon.aws.region}")
    String region;

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        return AmazonDynamoDBClientBuilder
                .standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(endpoint,region))
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(accesskey,secretkey)))
                .build();
    }

}