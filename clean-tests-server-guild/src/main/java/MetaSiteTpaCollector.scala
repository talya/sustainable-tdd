import java.util.UUID

class MetaSiteTpaCollector(provisioningHandler: ProvisioningHandler) {

  def collectProvisionedTpas(tpas: Seq[TpaInstance]) = {
    tpas.collect { case tpa if tpa.state == TpaInstance.PROVISIONED => {
      provisioningHandler.handleEventsOf(Set(tpa.id))
      tpa.id
    }}
  }
}


case class TpaInstance(id: UUID, state: String)
object TpaInstance {
  val PROVISIONED = "provisioned"
  val TEMPLATE = "template"
}


