package fw

import java.util.UUID

import core.UserIdRetriever


class FwBasedUserIdRetriever(aspects: RequestAspectStore) extends UserIdRetriever {

  override def maybeUserInSession = aspects.getAspect(classOf[SecurityRequestAspect]).getWixSession map {_.userGuid}

}


trait RequestAspectStore {
  def getAspect[T](clazz: Class[T]): T
}

trait SecurityRequestAspect {
  def getWixSession: Option[WixSession]
}

case class WixSession(userGuid: UUID, registrationDate: Int)
