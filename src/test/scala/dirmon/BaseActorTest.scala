package dirmon

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike, BeforeAndAfterEach}

/**
 * Created by scott on 9/17/14.
 */
abstract class BaseActorTest extends TestKit(ActorSystem("testSystem",
  ConfigFactory.parseString(BaseActorTest.config)))
   with DefaultTimeout
   with ImplicitSender
   with WordSpecLike
   with Matchers
   with BeforeAndAfterAll
   with BeforeAndAfterEach
{


  override def afterAll {
    shutdown()
  }

}

object BaseActorTest {
  // Define your test specific configuration here
  val config = """
    akka.loggers = ["akka.testkit.TestEventListener"]
    akka {
      loglevel = "INFO"
    }
               """
}
