package dirmon

/**
 * Created by scott on 11/19/14.
 */

import java.io.File

import org.scalatest.{Matchers, WordSpecLike}

class TestFileSystem extends WordSpecLike with Matchers {

  "FSDirectory" should {
    "should filter and create based on criteria" in {
      val dir = FSDirectory.forDirectory(new File("test_data/dir1"))
      dir.name should be ("dir1")
      dir.files.size should be (2)
      dir.files should contain (FSFile("company1-1416429810487.txt", "company1", 1416429848000L))
      dir.files should contain (FSFile("company2-1416429810487.txt", "company2", 1416429848000L))
    }

    "should filter and create based on criteria against multiple directories" in {
      val dirs = FSDirectory.forDirectories(List(new File("test_data/dir1"), new File("test_data/dir2")))
      dirs.size should be (2)
//      dirs should contain (FSDirectory("dir1", List(FSFile("company1-1416429810487.txt", "company1", 1416429848000L), FSFile("company2-1416429810487.txt", "company2", 1416429848000L))))
      dirs should contain (FSDirectory("dir2", List(FSFile("company3-1416433122734.txt", "company3", 1416429848000L))))
    }
  }
}