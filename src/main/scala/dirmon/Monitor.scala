package dirmon

import akka.actor.{ActorLogging, ActorSystem, Actor, Props}
import scala.concurrent.duration._

/**
 * Created by scott on 11/19/14.
 */
class MonitorBoot extends App {

}

class Monitor(delay: Int, directories: List[String]) {

  val system = ActorSystem("FSMonitor")


}

class DirectoryMonitor extends Actor with ActorLogging {
  def receive: Receive = {
    case (dirNow: FSDirectory, dirPrevOpt : Option[FSDirectory]) => {
      dirPrevOpt match {
        case Some(dirPrev) => {
          dirNow.files diff dirPrev.files foreach((f) => {
            dirPrev.files.find(_.name == f.name) match {
              case Some(prevFile) => log.info(s"Existing file ${f.name} from customer ${f.customer} in directory ${dirNow.name} was changed at ${f.lastChanged}.")
              case None           => log.info(s"New file ${f.name} from customer ${f.customer} added to directory ${dirNow.name}")
            }
          })
        }
        case None => // Since there is no previous directory, we really tell if anything has changed.
      }
    }
  }
}