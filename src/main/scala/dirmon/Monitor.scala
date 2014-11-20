package dirmon

import akka.actor._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.Source
import java.io.File


/**
 * Created by Scott T Weaver on 11/19/14.
 *
 */
object MonitorBoot extends App {
  val monitor = Monitor.start(1000, 10000, "configs/directories.txt", "configs/customers.txt")
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


class Monitor(delay: Long, pastDueAt: Long, directoriesConfigFile: String, customersConfigFile : String ) {
  implicit val system = ActorSystem("FSMonitor")
  val changeLogger = ChangeLogger.actorRef
  val activityTracker = CustomerActivityTacker.actorRef(List("customer1", "customer2"), pastDueAt, changeLogger)
  val dirMon = DirectoryMonitor.actorRef(changeLogger, activityTracker)
  val dirItr = DirectoryIterator.actorRef(dirMon)

  import system.dispatcher
  val s1 = system.scheduler.schedule(0 millis, delay millis, dirItr, IterateDirectories(() => entries(directoriesConfigFile)))
  val s2 = system.scheduler.schedule(pastDueAt millis, pastDueAt millis, activityTracker, CheckForActivity)

  def stop() = {
    s1.cancel()
    s2.cancel()
  }

  def entries(configFilePath: String) : List[String] = {
    Source.fromFile(new File(configFilePath)).getLines.toList.map((s) => s)
  }
}

object Monitor {

  def start(delay: Long, pastDueAt: Long, directoriesConfigFile: String, customersConfigFile : String) = {
    new Monitor(delay, pastDueAt, directoriesConfigFile, customersConfigFile)
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
    case FileWasChanged(dir, oldFileVer, newFileVer) => log.info(s"The file ${oldFileVer.name} was changed at ${newFileVer.lastChanged} in directory ${dir.name}.")
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
  def props(directoryMonitor: ActorRef): Props = {
    Props(classOf[DirectoryIterator], directoryMonitor)
  }

  def actorRef(directoryMonitor: ActorRef)(implicit system: ActorSystem) = {
    system.actorOf(props(directoryMonitor))
  }
}
