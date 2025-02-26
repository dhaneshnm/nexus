package ch.epfl.bluebrain.nexus.delta.sdk.model.schemas

import ch.epfl.bluebrain.nexus.delta.sourcing.config.AggregateConfig
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

/**
  * Configuration for the Schemas module.
  *
  * @param aggregate
  *   configuration of the underlying aggregate
  */
final case class SchemasConfig(aggregate: AggregateConfig, maxCacheSize: Long)

object SchemasConfig {
  implicit final val schemasConfigReader: ConfigReader[SchemasConfig] =
    deriveReader[SchemasConfig]
}
