package ru.tinkoff.gatling.amqp.examples

import com.rabbitmq.client.BuiltinExchangeType
import io.gatling.core.Predef._
import ru.tinkoff.gatling.amqp.javaapi.AmqpDsl._
import ru.tinkoff.gatling.amqp.javaapi.protocol._

class AmqpGatlingTest extends Simulation {

  val testQueue = new AmqpQueue("test_queue",true,false,false, java.util.Map.of())
  val testExchange = new AmqpExchange("test_exchange", BuiltinExchangeType.TOPIC,true,false, java.util.Map.of())


  setUp(
    scenario("Test Scenario")
      .exec(
        amqp("Test publish queue exchange").publish
          .queueExchange("test_queue")
          .textMessage("Publish queue test message")
          .priority(0)
          .messageId("1")
          .correlationId("1")
          .contentEncoding("text/plain")
          .contentType("text")
          .asScala(),
      )
      .exec(
        amqp("Test publish topic exchange").publish
          .topicExchange("test_exchange","routingKey")
          .textMessage("Publish topic test message")
          .priority(0)
          .messageId("2")
          .asScala(),
      )
      .exec(
        amqp("Test publish direct exchange").publish
          .directExchange("test_exchange", "routingKey")
          .textMessage("Publish direct test message")
          .priority(0)
          .messageId("3")
          .replyTo("test_queue")
          .appId("appId")
          .asScala(),
      )
      .exec(
        amqp("Test request reply queue exchange").requestReply
          .queueExchange("test_queue")
          .replyExchange("test_queue")
          .textMessage("RR queue test message")
          .priority(0)
          .messageId("4")
          .contentEncoding("text/plain")
          .contentType("text")
          .asScala(),
      )
      .exec(
        amqp("Test request reply topic exchange").requestReply
          .topicExchange("test_exchange", "routingKey")
          .replyExchange("test_queue")
          .textMessage("RR topic test message")
          .priority(0)
          .messageId("5")
          .asScala(),
      )
      .exec(
        amqp("Test request reply direct exchange").requestReply
          .directExchange("test_exchange", "routingKey")
          .replyExchange("test_queue")
          .textMessage("RR direct test message")
          .priority(0)
          .messageId("6")
          .replyTo("test_queue")
          .appId("appId")
          .check(
            simpleCheck(msg => msg.messageId.matches("6"))
          )
          .asScala(),
      )
      .inject(atOnceUsers(1)),
  ).protocols(
    amqp
      .connectionFactory(
        rabbitmq
          .host("localhost")
          .port(5672)
          .username("rabbitmq")
          .password("rabbitmq")
          .vhost("/")
          .build(),
      )
      .declare(testExchange)
      .declare(testQueue)
      .bindQueue(testQueue, testExchange,"routingKey", java.util.Map.of())
      .replyTimeout(60000)
      .consumerThreadsCount(8)
      .usePersistentDeliveryMode()
      .matchByMessageId()
      .protocol(),
  ).maxDuration(20)
}