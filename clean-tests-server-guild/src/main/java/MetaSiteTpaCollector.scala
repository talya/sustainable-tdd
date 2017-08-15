import java.util.UUID

class MetaSiteTpaCollector(eventGenerator: EventGenerator) {

  def collectProvisionedTpas(tpas: Seq[TpaInstance]) = {
    tpas.collect { case tpa if tpa.state == TpaInstance.PROVISIONED => {
      eventGenerator.generateProvisionedEvents(Set(tpa.id))
      tpa.id
    }}
  }
}


case class TpaInstance(id: UUID, state: String)
object TpaInstance {
  val PROVISIONED = "provisioned"
  val TEMPLATE = "template"
}


