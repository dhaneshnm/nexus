package ch.epfl.bluebrain.nexus.delta.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Route
import ch.epfl.bluebrain.nexus.delta.kernel.utils.UUIDF
import ch.epfl.bluebrain.nexus.delta.rdf.Vocabulary.contexts
import ch.epfl.bluebrain.nexus.delta.sdk._
import ch.epfl.bluebrain.nexus.delta.sdk.generators.ProjectGen
import ch.epfl.bluebrain.nexus.delta.sdk.model.{Label, ServiceAccountConfig}
import ch.epfl.bluebrain.nexus.delta.sdk.model.acls.AclAddress
import ch.epfl.bluebrain.nexus.delta.sdk.model.identities.Identity.{Anonymous, Authenticated, Group, Subject, User}
import ch.epfl.bluebrain.nexus.delta.sdk.model.identities.{Caller, Identity, ServiceAccount}
import ch.epfl.bluebrain.nexus.delta.sdk.model.projects.ProjectCountsCollection.ProjectCount
import ch.epfl.bluebrain.nexus.delta.sdk.model.quotas.QuotasConfig
import ch.epfl.bluebrain.nexus.delta.sdk.testkit._
import ch.epfl.bluebrain.nexus.delta.sdk.utils.RouteHelpers
import ch.epfl.bluebrain.nexus.delta.service.quotas.QuotasImpl
import ch.epfl.bluebrain.nexus.delta.utils.RouteFixtures
import ch.epfl.bluebrain.nexus.testkit._
import org.scalatest.matchers.should.Matchers
import org.scalatest.{CancelAfterFailure, Inspectors, OptionValues}

import java.util.UUID

class QuotasRoutesSpec
    extends RouteHelpers
    with Matchers
    with CirceLiteral
    with CirceEq
    with CancelAfterFailure
    with IOFixedClock
    with IOValues
    with OptionValues
    with TestMatchers
    with Inspectors
    with RouteFixtures {

  private val uuid                  = UUID.randomUUID()
  implicit private val uuidF: UUIDF = UUIDF.fixed(uuid)

  private val asAlice = addCredentials(OAuth2BearerToken(alice.subject))
  private val asBob   = addCredentials(OAuth2BearerToken(bob.subject))

  private val org     = Label.unsafe("org")
  private val project = ProjectGen.project("org", "project", uuid = uuid, orgUuid = uuid)

  private val (_, projects) = {
    implicit val subject: Subject = Identity.Anonymous
    ProjectSetup.init(List(org), List(project)).accepted
  }

  private val identities =
    IdentitiesDummy(
      Caller(alice, Set(alice, Anonymous, Authenticated(realm), Group("group", realm))),
      Caller(bob, Set(bob))
    )

  private val acls = AclSetup
    .init(
      (Anonymous, AclAddress.Root, Set(Permissions.events.read)),
      (bob, AclAddress.Project(project.ref), Set(Permissions.quotas.read))
    )
    .accepted

  implicit private val config            = QuotasConfig(Some(5), Some(10), enabled = true, Map.empty)
  implicit private val serviceAccountCfg = ServiceAccountConfig(ServiceAccount(User("internal", Label.unsafe("sa"))))

  private val projectsCounts = ProjectsCountsDummy(project.ref -> ProjectCount.emptyEpoch)

  private val quotas = new QuotasImpl(projects, projectsCounts)

  private val routes = Route.seal(new QuotasRoutes(identities, acls, projects, quotas).routes)

  "The Quotas route" when {

    "fetching quotas" should {

      "succeed" in {
        Get(s"/v1/quotas/org/project") ~> asBob ~> routes ~> check {
          status shouldEqual StatusCodes.OK
          response.asJson shouldEqual
            json"""{"@context": "${contexts.quotas}", "@type": "Quota", "resources": 5, "events": 10}"""
        }
      }

      "fail without quotas/read permissions" in {
        Get(s"/v1/quotas/org/project") ~> asAlice ~> routes ~> check {
          response.status shouldEqual StatusCodes.Forbidden
          response.asJson shouldEqual jsonContentOf("errors/authorization-failed.json")
        }
      }
    }
  }
}
