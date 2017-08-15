import java.util.UUID

import scala.util.Try

trait EventGenerator {
  def generateProvisionedEvents(ids: Set[UUID]): Unit
}


trait EventsStateDao {
  def addAll(ids: Set[UUID]): Unit = ???
}

class TheEventGenerator(eventNotifier: EventNotifier, eventsStateDao: EventsStateDao) extends EventGenerator {
  override def generateProvisionedEvents(ids: Set[UUID]): Unit = {
    Try {
      ids.foreach(id => eventNotifier.notify(TpaProvisionedEvent(id)))
    }
    eventsStateDao.addAll(ids)
  }
}


case class TpaProvisionedEvent(id: UUID)

trait EventNotifier {
  def notify(tpaProvisionEvent: TpaProvisionedEvent): Unit
}
