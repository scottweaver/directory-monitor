package dirmon

import akka.actor.Props
import akka.testkit.TestActorRef
import scala.concurrent.duration._

/**
 * Created by Scott T Weaver on 11/20/14.
 *
 */
class TestCustomerActivityTracker extends BaseActorTest{

  val rightNow = System.currentTimeMillis()
  val aMinuteBefore =  rightNow - 1000
  val lessThanAMinute =  rightNow - 500
  val activityTracker = TestActorRef(Props(classOf[CustomerActivityTacker], List("customer1", "customer2"), testActor))

  "CustomerActivityTracker" should {
    "record the last time a customer did something" in {
      within(500 millis) {
        activityTracker ! CustomerLastActive("customer1", 500L)
        activityTracker ! CustomerLastActive("customer2", rightNow)
        expectNoMsg
      }
      activityRecordFor("customer1") should be (500L)
      activityRecordFor("customer2") should be (rightNow)
    }

    "notify us if a customer is overdue for activity" in {
      within(500 millis) {
        activityTracker ! CustomerLastActive("customer1", aMinuteBefore)
        activityTracker ! CustomerLastActive("customer2", lessThanAMinute)
        activityTracker ! CheckForActivity
        expectMsg(CustomerOverdue("customer1", aMinuteBefore))
        expectNoMsg
      }
    }

    "be fine when customers have been active within a minute of their last activity" in {
      within(500 millis) {
        activityTracker ! CustomerLastActive("customer1", lessThanAMinute)
        activityTracker ! CustomerLastActive("customer2", lessThanAMinute)
        activityTracker ! CheckForActivity
        expectNoMsg
      }
    }


  }

  def activityRecordFor(customer: String): Long = {
    // Good for testing, incredibly bad for anything else.
    activityTracker.underlyingActor.asInstanceOf[CustomerActivityTacker].activityRecord(customer)
  }

}
