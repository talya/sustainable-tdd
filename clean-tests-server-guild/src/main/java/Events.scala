import java.util.UUID

import scala.util.Try

trait EventGenerator {
  def generateProvisionedEvents(ids: Set[UUID]): Unit
}


trait EventsStateDao {
  def markSuccessful(id: UUID): Unit = ???
  def addEventsWaitingForAck(ids: Set[UUID]): Unit = ???
  def eventsNotAcked(): Set[UUID] = ???
}

class StatefulEventGenerator(eventNotifier: EventNotifier, eventsStateDao: EventsStateDao) extends EventGenerator {
  override def generateProvisionedEvents(ids: Set[UUID]): Unit = {
    eventsStateDao.eventsNotAcked().foreach(id => eventNotifier.notify(TpaProvisionedEvent(id)))

    eventsStateDao.addEventsWaitingForAck(ids)

    ids.foreach(id => Try {
      eventNotifier.notify(TpaProvisionedEvent(id))
      eventsStateDao.markSuccessful(id)
    })
  }
}


case class TpaProvisionedEvent(id: UUID)

trait EventNotifier {
  def notify(tpaProvisionEvent: TpaProvisionedEvent): Unit
}
