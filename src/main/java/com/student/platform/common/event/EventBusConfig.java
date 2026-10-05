package com.student.platform.common.event;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnClass(RabbitTemplate.class)
public class EventBusConfig {

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EventConstants.EXCHANGE_DIRECT);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EventConstants.EXCHANGE_TOPIC);
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(EventConstants.EXCHANGE_FANOUT);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue userCreatedQueue() {
        return new Queue(EventConstants.QUEUE_USER_CREATED, true);
    }

    @Bean
    public Binding userCreatedBinding(Queue userCreatedQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(userCreatedQueue)
                .to(directExchange)
                .with(EventConstants.ROUTING_KEY_USER_CREATED);
    }

    @Bean
    public Queue exerciseCompletedQueue() {
        return new Queue(EventConstants.QUEUE_EXERCISE_COMPLETED, true);
    }

    @Bean
    public Binding exerciseCompletedBinding(Queue exerciseCompletedQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(exerciseCompletedQueue)
                .to(directExchange)
                .with(EventConstants.ROUTING_KEY_EXERCISE_COMPLETED);
    }

    @Bean
    public Queue qaAnsweredQueue() {
        return new Queue(EventConstants.QUEUE_QA_ANSWERED, true);
    }

    @Bean
    public Binding qaAnsweredBinding(Queue qaAnsweredQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(qaAnsweredQueue)
                .to(directExchange)
                .with(EventConstants.ROUTING_KEY_QA_ANSWERED);
    }

    @Bean
    public Queue knowledgeUpdatedQueue() {
        return new Queue(EventConstants.QUEUE_KNOWLEDGE_UPDATED, true);
    }

    @Bean
    public Binding knowledgeUpdatedBinding(Queue knowledgeUpdatedQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(knowledgeUpdatedQueue)
                .to(directExchange)
                .with(EventConstants.ROUTING_KEY_KNOWLEDGE_UPDATED);
    }
}
