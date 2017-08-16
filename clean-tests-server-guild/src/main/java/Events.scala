import java.util.UUID

import scala.util.Try

trait ProvisioningHandler {
  def handleEventsOf(ids: Set[UUID]): Unit
}


trait UnacknowledgedEventsDao {
  def markAcknowledged(id: UUID): Unit
  def addEventsWaitingForAck(ids: Set[UUID]): Unit
  def getEventsWaitingForAck(): Set[UUID]
}

class StatefulProvisioningHandler(eventNotifier: EventNotifier, eventsStateDao: UnacknowledgedEventsDao) extends ProvisioningHandler {
  override def handleEventsOf(ids: Set[UUID]): Unit = {
    eventsStateDao.getEventsWaitingForAck().foreach(id => eventNotifier.notify(TpaProvisionedEvent(id)))

    eventsStateDao.addEventsWaitingForAck(ids)

    ids.foreach(id => Try {
      eventNotifier.notify(TpaProvisionedEvent(id))
      eventsStateDao.markAcknowledged(id)
    })
  }
}


case class TpaProvisionedEvent(id: UUID)

trait EventNotifier {
  def notify(tpaProvisionEvent: TpaProvisionedEvent): Unit
}
