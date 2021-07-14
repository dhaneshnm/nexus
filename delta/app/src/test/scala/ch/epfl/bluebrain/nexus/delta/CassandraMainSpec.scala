package ch.epfl.bluebrain.nexus.delta

import ch.epfl.bluebrain.nexus.testkit.{ElasticSearchDocker, IOValues}
import ch.epfl.bluebrain.nexus.testkit.cassandra.CassandraDocker.CassandraSpec
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CassandraMainSpec
    extends AnyWordSpecLike
    with Matchers
    with IOValues
    with OptionValues
    with CassandraSpec
    with ElasticSearchDocker
    with MainBehaviors {

  override protected def flavour: String = "cassandra"

  override def beforeAll(): Unit = {
    super.beforeAll()
    commonBeforeAll()
  }

  override def afterAll(): Unit = {
    commonAfterAll()
    super.afterAll()
  }

}
