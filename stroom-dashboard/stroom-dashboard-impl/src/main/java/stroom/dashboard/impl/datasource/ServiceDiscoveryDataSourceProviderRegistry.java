/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.dashboard.impl.datasource;

import stroom.datasource.api.v2.DataSourceProvider;
import stroom.docref.DocRef;
import stroom.security.api.SecurityContext;
import stroom.servicediscovery.api.ExternalService;
import stroom.servicediscovery.api.ServiceDiscoverer;

import java.util.Optional;
import javax.inject.Provider;
import javax.ws.rs.client.Client;

class ServiceDiscoveryDataSourceProviderRegistry {

    private final SecurityContext securityContext;
    private final ServiceDiscoverer serviceDiscoverer;
    private final Provider<Client> clientProvider;

    //    @Inject
    ServiceDiscoveryDataSourceProviderRegistry(final SecurityContext securityContext,
                                               final ServiceDiscoverer serviceDiscoverer,
                                               final Provider<Client> clientProvider) {
        this.securityContext = securityContext;
        this.serviceDiscoverer = serviceDiscoverer;
        this.clientProvider = clientProvider;
    }

    /**
     * Gets a valid instance of a {@link DataSourceProvider} by querying service discovery
     *
     * @param docRefType The docRef type to get a data source provider for
     * @return A remote data source provider that can handle docRefs of the passed type. Will return
     * an empty optional for two reasons:
     * There may be no services that can handle the passed docRefType.
     * The service has no instances that are up and enabled.
     * <p>
     * The returned {@link DataSourceProvider} should be used and then thrown away, not cached or held.
     */
    private Optional<DataSourceProvider> getDataSourceProvider(final String docRefType) {

        return ExternalService.getExternalService(docRefType)
                .flatMap(serviceDiscoverer::getServiceInstance)
//                .filter(ServiceInstance::isEnabled) //not available until curator 2.12
                .flatMap(serviceInstance -> {
                    String address = serviceInstance.buildUriSpec();
                    return Optional.of(new RemoteDataSourceProvider(securityContext, () -> address, clientProvider));
                });
    }

    /**
     * Gets a valid instance of a {@link RemoteDataSourceProvider} by querying service discovery
     *
     * @param dataSourceRef The docRef to get a data source provider for
     * @return A remote data source provider that can handle the passed docRef. Will return
     * an empty optional for two reasons:
     * There may be no services that can handle the passed docRefType.
     * The service has no instances that are up and enabled.
     */
    public Optional<DataSourceProvider> getDataSourceProvider(final DocRef dataSourceRef) {
        return Optional.ofNullable(dataSourceRef)
                .map(DocRef::getType)
                .flatMap(this::getDataSourceProvider);
    }
}
