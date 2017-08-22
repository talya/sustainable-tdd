import java.util.Formatter.DateTime
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

trait RequestAspectStore {
  def getAspect[T](clazz: Class[T]): T
}

trait SecurityRequestAspect {
  def getWixSession: Option[WixSession]
}

case class WixSession(userGuid: UUID, registrationDate: Int)


class StatefulProvisioningHandler(eventNotifier: EventNotifier,
                                  eventsStateDao: UnacknowledgedEventsDao,
                                  biEventGenerator: BiEventGenerator,
                                  tpaTypeProviderFacade: TpaTypeProviderFacade,
                                  aspects: RequestAspectStore) extends ProvisioningHandler {

  override def handleEventsOf(ids: Set[UUID]): Unit = {
    eventsStateDao.addEventsWaitingForAck(ids)

    val idsToNotifyEventsOn = eventsStateDao.getEventsWaitingForAck() ++ ids

    idsToNotifyEventsOn.foreach {
      id => Try {
        if (tpaTypeProviderFacade.tpaIsInteresting(id))
          eventNotifier.notify(TpaProvisionedEvent(id))
        eventsStateDao.markAcknowledged(id)
        biEventGenerator.generateProvisionEvent(id, wasSuccessful = true, user = maybeUserInSession)
      } recover {
        case e =>
          biEventGenerator.generateProvisionEvent(id, wasSuccessful = false, user = maybeUserInSession)
      }
    }
  }

  private def maybeUserInSession = aspects.getAspect(classOf[SecurityRequestAspect]).getWixSession map {_.userGuid}
}


case class TpaProvisionedEvent(id: UUID)

trait EventNotifier {
  def notify(tpaProvisionEvent: TpaProvisionedEvent): Unit
}

