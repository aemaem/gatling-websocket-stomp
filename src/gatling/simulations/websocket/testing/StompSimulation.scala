package websocket.testing

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import org.apache.commons.lang3.{RandomStringUtils, RandomUtils}

/**
  * Sample performance test with Gatling and STOMP over Websocket.
  */
class StompSimulation extends Simulation {

  val httpConfig: HttpProtocolBuilder = http
    .baseURL("http://localhost:8080")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Gatling")
    .wsBaseURL("ws://localhost:8080")

  val serverId: String = RandomUtils.nextInt(100, 1000).toString
  val sessionId: String = RandomStringUtils.randomAlphanumeric(8)
  val transport = "websocket"

  val scenario1: ScenarioBuilder = scenario("WebSocket")
    .exec(http("Open home page").get("/"))
    .pause(1)
    .exec(ws("Open websocket")
      .open(s"/gs-guide-websocket/$serverId/$sessionId/$transport")
    )
    .pause(1)
    .exec(ws("Connect via STOMP")
      .sendText("[\"CONNECT\\naccept-version:1.1,1.0\\nheart-beat:10000,10000\\n\\n\\u0000\"]")
      .check(wsAwait.within(10).until(1).regex(".*CONNECTED.*"))
    )
    .pause(1)
    .exec(ws("Subscribe")
      .sendText("[\"SUBSCRIBE\\nid:sub-0\\ndestination:/topic/greetings\\n\\n\\u0000\"]")
    )
    .pause(1)
    .repeat(10, "i") {
      exec(ws("Send message")
        .sendText("[\"SEND\\ndestination:/app/hello\\ncontent-length:15\\n\\n{\\\"name\\\":\\\"Sepp\\\"}\\u0000\"]")
        .check(wsAwait.within(10).until(1).regex("MESSAGE\\\\ndestination:\\/topic\\/greetings\\\\ncontent-type:application\\/json;charset=UTF-8\\\\nsubscription:sub-0\\\\nmessage-id:[\\w\\d-]*\\\\ncontent-length:\\d*\\\\n\\\\n\\{\\\\\"content\\\\\":\\\\\"Hello, Sepp!\\\\\"\\}\\\\u0000"))
      ).pause(1)
    }
    .exec(ws("Close WS").close)

  setUp(scenario1.inject(atOnceUsers(1)).protocols(httpConfig))
}
