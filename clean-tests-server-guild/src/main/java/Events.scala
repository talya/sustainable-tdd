import java.util.UUID

trait EventGenerator {
  def generateProvisionedEvents(ids: Set[UUID]): Unit
}

class TheEventGenerator extends EventGenerator {
  override def generateProvisionedEvents(ids: Set[UUID]): Unit = ???
}


case class TpaProvisionedEvent(id: UUID)

trait EventNotifier {
  def notify(tpaProvisionEvent: TpaProvisionedEvent): Unit
}
