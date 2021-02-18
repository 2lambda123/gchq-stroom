package stroom.security.impl;

import stroom.security.shared.DocumentPermissions;

import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

public class DocumentPermissionResourceImpl implements DocumentPermissionResource {

    private final DocumentPermissionServiceImpl documentPermissionService;
    private final DocumentTypePermissions documentTypePermissions;

    @Inject
    public DocumentPermissionResourceImpl(final DocumentPermissionServiceImpl documentPermissionService,
                                          final DocumentTypePermissions documentTypePermissions) {
        this.documentPermissionService = documentPermissionService;
        this.documentTypePermissions = documentTypePermissions;
    }

    @Override
    public Response getPermissionForDocType(final String docType) {
        final List<String> permissions = documentTypePermissions.getPermissions(docType);
        return Response.ok(permissions).build();
    }

    @Override
    public Response getPermissionsForDocumentForUser(final String docUuid,
                                                     final String userUuid) {
        final Set<String> permissions =
                documentPermissionService.getPermissionsForDocumentForUser(docUuid, userUuid);
        return Response.ok(permissions).build();
    }

    @Override
    public Response addPermission(final String docUuid,
                                  final String userUuid,
                                  final String permissionName) {
        documentPermissionService.addPermission(docUuid, userUuid, permissionName);
        return Response.noContent().build();
    }

    @Override
    public Response removePermission(final String docUuid,
                                     final String userUuid,
                                     final String permissionName) {
        documentPermissionService.removePermission(docUuid, userUuid, permissionName);
        return Response.noContent().build();
    }

    @Override
    public Response removePermissionForDocumentForUser(final String docUuid,
                                                       final String userUuid) {
        documentPermissionService.clearDocumentPermissionsForUser(docUuid, userUuid);
        return Response.noContent().build();
    }

    @Override
    public Response getPermissionsForDocument(final String docUuid) {
        final DocumentPermissions permissions = documentPermissionService.getPermissionsForDocument(docUuid);
        return Response.ok(permissions).build();
    }

    @Override
    public Response clearDocumentPermissions(final String docUuid) {
        documentPermissionService.clearDocumentPermissions(docUuid);
        return Response.noContent().build();
    }
}
