package service.propiedades.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "realtyhub.events";
    public static final String DEAD_LETTER_EXCHANGE = "realtyhub.events.dlx";

    public static final String RK_CONTRACT_SIGNED = "contract.signed";
    public static final String RK_CONTRACT_CANCELLED = "contract.cancelled";
    public static final String RK_ESTADO_CAMBIADO = "propiedad.estado_cambiado";

    public static final String QUEUE_CONTRACT_SIGNED = "propiedades.contract-signed";
    public static final String QUEUE_CONTRACT_SIGNED_DLQ = "propiedades.contract-signed.dlq";
    public static final String QUEUE_CONTRACT_CANCELLED = "propiedades.contract-cancelled";
    public static final String QUEUE_CONTRACT_CANCELLED_DLQ = "propiedades.contract-cancelled.dlq";

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Declarables contractQueues(TopicExchange eventsExchange, DirectExchange deadLetterExchange) {
        Queue signed = conDeadLetter(QUEUE_CONTRACT_SIGNED, QUEUE_CONTRACT_SIGNED_DLQ);
        Queue signedDlq = QueueBuilder.durable(QUEUE_CONTRACT_SIGNED_DLQ).build();
        Queue cancelled = conDeadLetter(QUEUE_CONTRACT_CANCELLED, QUEUE_CONTRACT_CANCELLED_DLQ);
        Queue cancelledDlq = QueueBuilder.durable(QUEUE_CONTRACT_CANCELLED_DLQ).build();

        return new Declarables(
                signed,
                signedDlq,
                cancelled,
                cancelledDlq,
                BindingBuilder.bind(signed).to(eventsExchange).with(RK_CONTRACT_SIGNED),
                BindingBuilder.bind(cancelled).to(eventsExchange).with(RK_CONTRACT_CANCELLED),
                BindingBuilder.bind(signedDlq).to(deadLetterExchange).with(QUEUE_CONTRACT_SIGNED_DLQ),
                BindingBuilder.bind(cancelledDlq).to(deadLetterExchange).with(QUEUE_CONTRACT_CANCELLED_DLQ));
    }

    private Queue conDeadLetter(String nombre, String nombreDlq) {
        return QueueBuilder.durable(nombre)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(nombreDlq)
                .build();
    }
}
