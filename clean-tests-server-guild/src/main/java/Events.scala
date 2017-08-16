import java.util.UUID

trait EventGenerator {
  def generateProvisionedEvents(ids: Set[UUID]): Unit
}

class TheEventGenerator(eventNotifier: EventNotifier) extends EventGenerator {
  override def generateProvisionedEvents(ids: Set[UUID]): Unit =
    ids.foreach(id => eventNotifier.notify(TpaProvisionedEvent(id)))
}


case class TpaProvisionedEvent(id: UUID)

trait EventNotifier {
  def notify(tpaProvisionEvent: TpaProvisionedEvent): Unit
}
