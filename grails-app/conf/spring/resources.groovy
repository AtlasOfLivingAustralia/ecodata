// Place your Spring DSL code here
beans = {
//    //comment this out for local development
//    amqConnectionFactory(org.apache.activemq.ActiveMQConnectionFactory) {
//        println("[JMS] Initialising connection factory with brokerURL : " + grailsApplication.config.brokerURL)
//        brokerURL = grailsApplication.config.brokerURL
//    }
//
//    jmsConnectionFactory(org.springframework.jms.connection.CachingConnectionFactory){
//        targetConnectionFactory = amqConnectionFactory
//        sessionCacheSize="10"
//    }
//
//    destination(org.apache.activemq.command.ActiveMQQueue){
//        physicalName = grailsApplication.config.queueName
//    }
//
//    jmsTemplate(org.springframework.jms.core.JmsTemplate){
//        connectionFactory = jmsConnectionFactory
//        defaultDestination = destination
//    }
}
