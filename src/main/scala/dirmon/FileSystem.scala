package dirmon

import java.io.File


/**
 * Created by scott on 11/19/14.
 */
class DirList {

}

sealed trait FSObject {
  def name: String
}

case class FSFile(name: String, customer: String) extends FSObject

case class FSDirectory(name: String, files: List[FSFile]) extends FSObject

object FSDirectory {

  def forDirectory(directory: File): FSDirectory = {
    val files = for {
      file <- directory.listFiles().toList
      matcher = """([\w\s]+)-\d+\.txt""".r.findFirstMatchIn(file.getName)
      if matcher != None
    } yield {
      matcher match {
        case Some(m) => FSFile(m.group(0), m.group(1))
        case None    => FSFile("", "") // This will never happen due to the guard above
      }
    }

    FSDirectory(directory.getName, files)
  }

  def forDirectories(directories: List[File]): List[FSDirectory] = {
    directories.map { case dir => forDirectory(dir)}
  }
}