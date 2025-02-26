package ch.epfl.bluebrain.nexus.delta.plugins.storage.storages.operations.remote.client

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes.`text/plain(UTF-8)`
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import akka.util.ByteString
import ch.epfl.bluebrain.nexus.delta.plugins.storage.ConfigFixtures
import ch.epfl.bluebrain.nexus.delta.plugins.storage.files.model.Digest.NotComputedDigest
import ch.epfl.bluebrain.nexus.delta.plugins.storage.storages.operations.AkkaSourceHelpers
import ch.epfl.bluebrain.nexus.delta.plugins.storage.storages.operations.StorageFileRejection.{FetchFileRejection, MoveFileRejection}
import ch.epfl.bluebrain.nexus.delta.plugins.storage.storages.operations.remote.RemoteStorageDocker._
import ch.epfl.bluebrain.nexus.delta.plugins.storage.storages.operations.remote.client.model.RemoteDiskStorageFileAttributes
import ch.epfl.bluebrain.nexus.delta.sdk.http.{HttpClient, HttpClientConfig}
import ch.epfl.bluebrain.nexus.delta.sdk.http.HttpClientError.HttpClientStatusError
import ch.epfl.bluebrain.nexus.delta.sdk.model.ComponentDescription.ServiceDescription
import ch.epfl.bluebrain.nexus.delta.sdk.model.{Label, Name}
import ch.epfl.bluebrain.nexus.delta.sdk.model.identities.AuthToken
import ch.epfl.bluebrain.nexus.testkit.IOValues
import monix.execution.Scheduler
import org.scalatest.DoNotDiscover
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext

@DoNotDiscover
class RemoteStorageClientSpec
    extends TestKit(ActorSystem("RemoteStorageClientSpec"))
    with AnyWordSpecLike
    with Matchers
    with IOValues
    with AkkaSourceHelpers
    with Eventually
    with ConfigFixtures {

  implicit val ec: ExecutionContext = system.dispatcher

  "A RemoteStorage client" should {

    implicit val sc: Scheduler                = Scheduler.global
    implicit val httpConfig: HttpClientConfig = httpClientConfig
    implicit val httpClient: HttpClient       = HttpClient()
    implicit val cred: Option[AuthToken]      = None
    val content                               = "file content"
    val source                                = Source(content.map(c => ByteString(c.toString)))
    val attributes                            = RemoteDiskStorageFileAttributes(
      location = s"file:///app/$BucketName/nexus/my/file.txt",
      bytes = 12,
      digest = digest,
      mediaType = `text/plain(UTF-8)`
    )

    val client = new RemoteDiskStorageClient(RemoteStorageEndpoint)

    "fetch the service description" in eventually {
      client.serviceDescription.accepted shouldEqual ServiceDescription(Name.unsafe("remoteStorage"), "1.5.1")
    }

    "check if a bucket exists" in {
      client.exists(BucketName).accepted
      val error = client.exists(Label.unsafe("other")).rejectedWith[HttpClientStatusError]
      error.code == StatusCodes.NotFound
    }

    "create a file" in {
      client.createFile(BucketName, Uri.Path("my/file.txt"), source).accepted shouldEqual attributes
    }

    "get a file" in {
      consume(client.getFile(BucketName, Uri.Path("my/file.txt")).accepted) shouldEqual content
    }

    "fail to get a file that does not exist" in {
      client.getFile(BucketName, Uri.Path("my/file3.txt")).rejectedWith[FetchFileRejection.FileNotFound]
    }

    "get a file attributes" in eventually {
      client.getAttributes(BucketName, Uri.Path("my/file.txt")).accepted shouldEqual attributes
    }

    "move a file" in {
      client.moveFile(BucketName, Uri.Path("my/file.txt"), Uri.Path("other/file.txt")).accepted shouldEqual
        attributes.copy(
          location = s"file:///app/$BucketName/nexus/other/file.txt",
          digest = NotComputedDigest
        )
    }

    "fail to move a file that does not exist" in {
      client
        .moveFile(BucketName, Uri.Path("my/file.txt"), Uri.Path("other/file.txt"))
        .rejectedWith[MoveFileRejection.FileNotFound]

    }
  }
}
