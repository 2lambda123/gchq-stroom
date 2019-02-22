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

package stroom.receive.rules.impl;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import stroom.docref.DocRef;
import stroom.importexport.api.DocRefs;
import stroom.importexport.api.OldDocumentData;
import stroom.importexport.shared.ImportState;
import stroom.importexport.shared.ImportState.ImportMode;
import stroom.util.RestResource;
import stroom.util.string.EncodingUtil;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@Api(value = "ruleset - /v1")
@Path("/ruleset/v1")
@Produces(MediaType.APPLICATION_JSON)
public class ReceiveDataRuleSetResource implements RestResource {
    private final ReceiveDataRuleSetService ruleSetService;

    @Inject
    public ReceiveDataRuleSetResource(final ReceiveDataRuleSetService ruleSetService) {
        this.ruleSetService = ruleSetService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    @Timed
    @ApiOperation(
            value = "Submit a request for a list of doc refs held by this service",
            response = Set.class)
    public DocRefs listDocuments() {
        return new DocRefs(ruleSetService.listDocuments());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/import")
    @Timed
    @ApiOperation(
            value = "Submit an import request",
            response = DocRef.class)
    public DocRef importDocument(@ApiParam("DocumentData") final OldDocumentData documentData) {
        final ImportState importState = new ImportState(documentData.getDocRef(), documentData.getDocRef().getName());
        if (documentData.getDataMap() == null) {
            return ruleSetService.importDocument(documentData.getDocRef(), null, importState, ImportMode.IGNORE_CONFIRMATION);
        }
        final Map<String, byte[]> data = documentData.getDataMap().entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> EncodingUtil.asBytes(e.getValue())));
        return ruleSetService.importDocument(documentData.getDocRef(), data, importState, ImportMode.IGNORE_CONFIRMATION);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/export")
    @Timed
    @ApiOperation(
            value = "Submit an export request",
            response = OldDocumentData.class)
    public OldDocumentData exportDocument(@ApiParam("DocRef") final DocRef docRef) {
        final Map<String, byte[]> map = ruleSetService.exportDocument(docRef, true, new ArrayList<>());
        if (map == null) {
            return new OldDocumentData(docRef, null);
        }
        final Map<String, String> data = map.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> EncodingUtil.asString(e.getValue())));
        return new OldDocumentData(docRef, data);
    }
}