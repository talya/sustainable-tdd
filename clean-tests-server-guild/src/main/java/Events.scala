import java.util.UUID

trait EventGenerator {
  def generateProvisionedEvent(i: UUID): Unit
}
