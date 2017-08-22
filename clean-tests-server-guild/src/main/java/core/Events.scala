package core

import java.util.UUID

import scala.util.Try

trait ProvisioningHandler {
  def handleEventsOf(ids: Set[UUID]): Unit
}

trait BiEventGenerator {
  def generateProvisionEvent(tpaId: UUID, wasSuccessful: Boolean, user: Option[UUID] = None): Unit
}


trait UnacknowledgedEventsDao {
  def markAcknowledged(id: UUID): Unit
  def addEventsWaitingForAck(ids: Set[UUID]): Unit
  def getEventsWaitingForAck(): Set[UUID]
}

trait TpaTypeProviderFacade {
  def tpaIsInteresting(id: UUID): Boolean
}


trait UserIdRetriever {
  def maybeUserInSession: Option[UUID]
}

class StatefulProvisioningHandler(eventNotifier: EventNotifier,
                                  eventsStateDao: UnacknowledgedEventsDao,
                                  biEventGenerator: BiEventGenerator,
                                  userIdRetriever: UserIdRetriever) extends ProvisioningHandler {

  override def handleEventsOf(ids: Set[UUID]): Unit = {
    eventsStateDao.addEventsWaitingForAck(ids)

    val idsToNotifyEventsOn = eventsStateDao.getEventsWaitingForAck() ++ ids

    idsToNotifyEventsOn.foreach {
      id => Try {
        eventNotifier.notify(TpaProvisionedEvent(id))
        eventsStateDao.markAcknowledged(id)
        biEventGenerator.generateProvisionEvent(id, wasSuccessful = true, user = userIdRetriever.maybeUserInSession)
      } recover {
        case e =>
          biEventGenerator.generateProvisionEvent(id, wasSuccessful = false, user = userIdRetriever.maybeUserInSession)
      }
    }
  }
}


case class TpaProvisionedEvent(id: UUID)

trait EventNotifier {
  def notify(tpaProvisionEvent: TpaProvisionedEvent): Unit
}

class InterestingTpaEventNotifier(tpaTypeProviderFacade: TpaTypeProviderFacade) extends EventNotifier {
  override def notify(tpaProvisionEvent: TpaProvisionedEvent): Unit =
    if (tpaTypeProviderFacade.tpaIsInteresting(tpaProvisionEvent.id)) actuallyNotify()

  private def actuallyNotify():Unit = {}
}

