package dirmon

import java.io.File


/**
 * Created by scott on 11/19/14.
 */

sealed trait FSObject {
  def name: String
}

case class FSFile(name: String, customer: String, lastChanged: Long) extends FSObject

case class FSDirectory(name: String, files: List[FSFile]) extends FSObject

object FSDirectory {

  def apply(directoryByName: String) : FSDirectory = {
    apply(new File(directoryByName))
  }

  def apply(directory: File): FSDirectory = {
    val files = for {
      file <- directory.listFiles().toList
      matcher = """([\w\s]+)-\d+\.txt""".r.findFirstMatchIn(file.getName)
      if matcher != None
    } yield {
      matcher match {
        case Some(m) => FSFile(m.group(0), m.group(1), file.lastModified())
        case None    => FSFile("", "", 0) // This will never happen due to the guard above
      }
    }

    FSDirectory(directory.getName, files)
  }

  def forDirectories(directories: List[File]): List[FSDirectory] = {
    directories.map { case dir => FSDirectory(dir)}
  }

  def forDirectoryNames(directories: List[String]): List[FSDirectory] = {
    forDirectories( directories.map{case fname => new File(fname)})
  }
}