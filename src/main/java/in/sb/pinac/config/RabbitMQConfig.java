package in.sb.pinac.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitMQConfig {

    @Value("${whatsapp.rabbitmq.queue:whatsapp.incoming.messages}")
    private String queueName;

    @Value("${whatsapp.rabbitmq.exchange:whatsapp.direct.exchange}")
    private String exchangeName;

    @Value("${whatsapp.rabbitmq.routing-key:whatsapp.message.key}")
    private String routingKey;

    @Bean
    public Queue whatsappQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", exchangeName + ".dlx")
                .withArgument("x-dead-letter-routing-key", routingKey + ".dlq")
                .build();
    }

    @Bean
    public DirectExchange whatsappExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Binding whatsappBinding(Queue whatsappQueue, DirectExchange whatsappExchange) {
        return BindingBuilder.bind(whatsappQueue).to(whatsappExchange).with(routingKey);
    }

    // Dead Letter Queue for failed / retry messages
    @Bean
    public Queue whatsappDeadLetterQueue() {
        return QueueBuilder.durable(queueName + ".dlq").build();
    }

    @Bean
    public DirectExchange whatsappDeadLetterExchange() {
        return new DirectExchange(exchangeName + ".dlx");
    }

    @Bean
    public Binding whatsappDeadLetterBinding(Queue whatsappDeadLetterQueue, DirectExchange whatsappDeadLetterExchange) {
        return BindingBuilder.bind(whatsappDeadLetterQueue).to(whatsappDeadLetterExchange).with(routingKey + ".dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
