package dirmon

import akka.actor.Props
import scala.concurrent.duration._

/**
 * Created by scott on 11/20/14.
 */
class TestDirectoryIterator extends BaseActorTest {

  "DirectoryIterator" should {
    "send an FSDirectory to the directoryProcessor actor for each directory name sent via the IterateDirectories message" in {
      val dirItr = system.actorOf(Props(classOf[DirectoryIterator], testActor))

      within(500 millis) {
        dirItr ! IterateDirectories(List("test_data/dir1", "test_data/dir2"))
        expectMsg(FSDirectory("dir1", List(FSFile("company1-1416429810487.txt", "company1", 1416429848000L),
                                           FSFile("company2-1416429810487.txt", "company2", 1416429848000L))))
        expectMsg(FSDirectory("dir2", List(FSFile("company3-1416433122734.txt", "company3", 1416429848000L))))
        expectNoMsg
      }

    }
  }

}
