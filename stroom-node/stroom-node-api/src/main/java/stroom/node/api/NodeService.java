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
 *
 */

package stroom.node.api;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>
 * Class to manage nodes.
 * </p>
 */
public interface NodeService  {

    String getBaseEndpointUrl(String nodeName);

    boolean isEnabled(String nodeName);

    int getPriority(String nodeName);

    List<String> findNodeNames(FindNodeCriteria criteria);

    /**
     * Call out to the specified node using the rest request defined by fullPath and
     * responseBuilderFunc, of id nodeName is this node then use localSupplier.
     */
    default <T_RESP> T_RESP remoteRestCall(final String nodeName,
                                   final Class<T_RESP> responseType,
                                   final String fullPath,
                                   final Supplier<T_RESP> localSupplier,
                                   final Function<Invocation.Builder, Response> responseBuilderFunc) {

        return remoteRestCall(
                nodeName,
                fullPath,
                localSupplier,
                responseBuilderFunc,
                response ->
                        response.readEntity(responseType));

    }

    <T_RESP> T_RESP remoteRestCall(final String nodeName,
                                          final String fullPath,
                                          final Supplier<T_RESP> localSupplier,
                                          final Function<Invocation.Builder, Response> responseBuilderFunc,
                                          final Function<Response, T_RESP> responseMapper);

}
