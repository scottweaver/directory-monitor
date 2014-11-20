package dirmon

/**
 * Created by scott on 11/20/14.
 */

import scala.concurrent.duration._

class TestDirectoryMonitor extends BaseActorTest {

  val d1f1 = FSFile("company1-0.txt", "company1", 0L)
  val d1f1changed = FSFile("company1-0.txt", "company1", 3L)
  val d1f2 = FSFile("company1-1.txt", "company1", 1L)
  val d1f3 = FSFile("company1-3.txt", "company1", 2L)
  val dir1 = FSDirectory("dir1", List(d1f1, d1f2))
  val dir1Changed = FSDirectory("dir1", List(d1f1changed, d1f2))
  val dir1_2 = FSDirectory("dir1", List(d1f1, d1f2, d1f3))


  "for the first time through when there is not a current state for a directory, make that the current state." in {
    within(500 millis) {
      val dirMonActor = DirectoryMonitor.actorRef(testActor, testActor)
      dirMonActor ! dir1
      expectNoMsg()
    }
  }

  "A DirectoryMonitor" should {
    "send notifications when a new file is added to a directory" in {

      within(500 millis) {
        val dirMonActor = DirectoryMonitor.actorRef(testActor, testActor)
        dirMonActor ! (dir1)
        dirMonActor ! (dir1_2)
        expectMsg(FileWasCreated(dir1_2, d1f3))
        expectMsg(CustomerLastActive("company1", 2L))
      }

    }

    "send notifications when a file is changed in a directory." in {
      within(500 millis) {
        val dirMonActor = DirectoryMonitor.actorRef(testActor, testActor)
        dirMonActor ! dir1
        dirMonActor ! dir1Changed
        expectMsg(FileWasChanged(dir1Changed, d1f1, d1f1changed))
        expectMsg(CustomerLastActive("company1", 3L))
      }
    }

  }
}