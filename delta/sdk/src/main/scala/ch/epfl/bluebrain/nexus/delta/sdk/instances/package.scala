package ch.epfl.bluebrain.nexus.delta.sdk

import ch.epfl.bluebrain.nexus.delta.rdf.instances.{SecretInstances, TripleInstances, UriInstances}

package object instances
    extends OffsetJsonLdInstances
    with CredentialsInstances
    with TripleInstances
    with UriInstances
    with SecretInstances
