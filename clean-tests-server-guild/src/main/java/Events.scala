import java.util.UUID

import scala.util.Try

trait ProvisioningHandler {
  def handleEventsOf(ids: Set[UUID]): Unit
}

trait BiEventGenerator {
  def generateProvisionEvent(tpaId: UUID, wasSuccessful: Boolean): Unit
}


trait UnacknowledgedEventsDao {
  def markAcknowledged(id: UUID): Unit
  def addEventsWaitingForAck(ids: Set[UUID]): Unit
  def getEventsWaitingForAck(): Set[UUID]
}

trait TpaTypeProviderFacade {
  def tpaIsInteresting(id: UUID): Boolean
}

trait SecurityAspect

class StatefulProvisioningHandler(eventNotifier: EventNotifier,
                                  eventsStateDao: UnacknowledgedEventsDao,
                                  biEventGenerator: BiEventGenerator,
                                  tpaTypeProviderFacade: TpaTypeProviderFacade) extends ProvisioningHandler {

  override def handleEventsOf(ids: Set[UUID]): Unit = {
    eventsStateDao.addEventsWaitingForAck(ids)

    val idsToNotifyEventsOn = eventsStateDao.getEventsWaitingForAck() ++ ids

    idsToNotifyEventsOn.foreach {
      id => Try {
        if (tpaTypeProviderFacade.tpaIsInteresting(id))
          eventNotifier.notify(TpaProvisionedEvent(id))
        eventsStateDao.markAcknowledged(id)
        biEventGenerator.generateProvisionEvent(id, wasSuccessful = true)
      } recover {
        case e =>
          biEventGenerator.generateProvisionEvent(id, wasSuccessful = false)
      }
    }
  }
}


case class TpaProvisionedEvent(id: UUID)

trait EventNotifier {
  def notify(tpaProvisionEvent: TpaProvisionedEvent): Unit
}

