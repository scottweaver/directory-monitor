package dirmon

import akka.actor._
import scala.collection.mutable
import scala.concurrent.duration._


/**
 * Created by Scott T Weaver on 11/19/14.
 *
 */
object MonitorBoot extends App {
  val monitor = 5Monitor.start(1000, 10000, List("test_data_live/dir1"))
}

sealed trait FSMessage

case class FileWasChanged(inDir: FSDirectory, oldFileVersion: FSFile, newFileVersion: FSFile) extends FSMessage

case class FileWasCreated(inDir: FSDirectory, newFile: FSFile) extends FSMessage

case class FSDirectoryStateRecorded(dir: FSDirectory)

case class CustomerLastActive(customer: String, lastActive: Long)

case object CheckForActivity extends FSMessage

case class CustomerOverdue(customer: String, lastActive: Long) extends FSMessage

case class SetCurrentDirectoryState(dir: FSDirectory) extends FSMessage

case class IterateDirectories(directories: () => List[String]) extends FSMessage


class Monitor(delay: Long, pastDueAt: Long, directories: List[String]) {
  implicit val system = ActorSystem("FSMonitor")
  val changeLogger = ChangeLogger.actorRef
  val activityTracker = CustomerActivityTacker.actorRef(List("customer1", "customer2"), pastDueAt, changeLogger)
  val dirMon = DirectoryMonitor.actorRef(changeLogger, activityTracker)
  val dirItr = DirectoryIterator.actorRef(directories, dirMon)

  import system.dispatcher
  val s1 = system.scheduler.schedule(0 millis, delay millis, dirItr, IterateDirectories(() => directories))
  val s2 = system.scheduler.schedule(pastDueAt millis, pastDueAt millis, activityTracker, CheckForActivity)

  def stop() = {
    s1.cancel()
    s2.cancel()
  }
}

object Monitor {

  def start(delay: Long, pastDueAt: Long, directories: List[String]) = {
    new Monitor(delay, pastDueAt, directories)
  }

}

class DirectoryMonitor(changeLogger: ActorRef, customerActivity: ActorRef) extends Actor with ActorLogging {

  val directoryState = mutable.Map[String, FSDirectory]()

  def receive: Receive = {
    case dirNow: FSDirectory => {
      directoryState.get(dirNow.name) match {
        case Some(dirPrev) => {
          dirNow.files diff dirPrev.files foreach ((currFile) => {
            dirPrev.files.find(_.name == currFile.name) match {
              case Some(prevFile) => {
                changeLogger ! FileWasChanged(dirNow, prevFile, currFile)
                customerActivity ! CustomerLastActive(currFile.customer, currFile.lastChanged)
                directoryState += dirNow.name -> dirNow
              }
              case None => {
                changeLogger ! FileWasCreated(dirNow, currFile)
                customerActivity ! CustomerLastActive(currFile.customer, currFile.lastChanged)
                directoryState += dirNow.name -> dirNow
              }
            }
          })
        }
        case None => directoryState += dirNow.name -> dirNow // Since there is no previous directory, we really tell can't if anything has changed.
      }
    }
    case other => log.warning(s"DirectoryMonitor was unable to process message $other.")
  }
}

object DirectoryMonitor {
  def props(changeLogger: ActorRef, customerActivity: ActorRef): Props = {
    Props(classOf[DirectoryMonitor], changeLogger, customerActivity)
  }

  def actorRef(changeLogger: ActorRef, customerActivity: ActorRef)(implicit system: ActorSystem) = {
    system.actorOf(props(changeLogger, customerActivity))
  }
}

class ChangeLogger extends Actor with ActorLogging {
  def receive: Receive = {
    case FileWasChanged(dir, oldFileVer, newFileVer) => log.info(s"The file ${oldFileVer.name} was changed at ${newFileVer} in directory ${dir.name}.")
    case FileWasCreated(dir, file) => log.info(s"The file ${file.name} was created in directory ${dir.name}.")
    case CustomerOverdue(customer, lastActive) => log.warning(s"$customer is past due, last activity at $lastActive")
    case other => log.warning(s"ChangeLogger was unable to process message $other.")
  }
}

object ChangeLogger {
  def props: Props = {
    Props(classOf[ChangeLogger])
  }

  def actorRef(implicit system: ActorSystem) = {
    system.actorOf(props)
  }
}

class CustomerActivityTacker(customers: List[String], pastDueAt: Long, overDueNotifier: ActorRef) extends Actor with ActorLogging {
  val activityRecord = customers.foldLeft(mutable.Map[String, Long]()) { (m, s) => m + (s -> 0)}

  def receive: Receive = {
    case CustomerLastActive(customer, lastActive) => activityRecord += customer -> lastActive
    case CheckForActivity => {
      val now = System.currentTimeMillis
      activityRecord.foreach { case (customer, lastActive) => if (now - lastActive > pastDueAt) overDueNotifier ! CustomerOverdue(customer, lastActive)}

    }
    case other => log.warning(s"CustomerActivityTacker was unable to process message $other.")
  }
}

object CustomerActivityTacker {
  def props(customers: List[String], pastDueAt: Long, overDueNotifier: ActorRef): Props = {
    Props(classOf[CustomerActivityTacker], customers, pastDueAt, overDueNotifier)
  }

  def actorRef(customers: List[String], pastDueAt: Long, overDueNotifier: ActorRef)(implicit system: ActorSystem) = {
    system.actorOf(props(customers, pastDueAt, overDueNotifier))
  }
}


class DirectoryIterator(directoryProcessor: ActorRef) extends Actor {
  def receive: Receive = {
    case IterateDirectories(directories) => FSDirectory.forDirectoryNames(directories.apply()).foreach(directoryProcessor ! _)
  }
}

object DirectoryIterator {
  def props(directories: List[String], directoryMonitor: ActorRef): Props = {
    Props(classOf[DirectoryIterator], directoryMonitor)
  }

  def actorRef(directories: List[String], directoryMonitor: ActorRef)(implicit system: ActorSystem) = {
    system.actorOf(props(directories, directoryMonitor))
  }
}
