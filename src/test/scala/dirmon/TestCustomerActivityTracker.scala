package dirmon

import akka.testkit.TestActorRef
import scala.concurrent.duration._

/**
 * Created by Scott T Weaver on 11/20/14.
 *
 */
class TestCustomerActivityTracker extends BaseActorTest{

  var rightNow = 0L
  var aMinuteBefore =  0L
  var lessThanAMinute =  0L
  val activityTracker = TestActorRef[CustomerActivityTacker](CustomerActivityTacker.props(List("customer1", "customer2"), 1000, testActor))

  "CustomerActivityTracker" should {
    "record the last time a customer did something" in {
      within(500 millis) {
        activityTracker ! CustomerLastActive("customer1", 500L)
        activityTracker ! CustomerLastActive("customer2", rightNow)
        expectNoMsg
      }
      activityRecordFor(activityTracker, "customer1") should be (500L)
      activityRecordFor(activityTracker, "customer2") should be (rightNow)
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

  def activityRecordFor(actorRef: TestActorRef[CustomerActivityTacker], customer: String): Long = {
    // Good for testing, incredibly bad for anything else.
    actorRef.underlyingActor.activityRecord(customer)
  }

  override def beforeEach {
    rightNow = System.currentTimeMillis()
    aMinuteBefore =  rightNow - 1000
    lessThanAMinute =  rightNow - 500
  }

}
