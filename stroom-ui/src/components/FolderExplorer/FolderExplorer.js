import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { compose, withProps, branch, renderComponent } from 'recompose';
import { Loader, Grid, Header, Icon, Button } from 'semantic-ui-react';

import ThemedPopup from 'components/ThemedPopup';
import AppSearchBar from 'components/AppSearchBar';
import DocRefBreadcrumb from 'components/DocRefBreadcrumb';
import { findItem } from 'lib/treeUtils';
import { actionCreators } from './redux';
import { fetchDocInfo } from 'components/FolderExplorer/explorerClient';
import DndDocRefListingEntry from './DndDocRefListingEntry';
import withOpenDocRef from 'sections/RecentItems/withOpenDocRef';
import NewDocDialog from './NewDocDialog';
import DocRefInfoModal from 'components/DocRefInfoModal';
import withDocumentTree from './withDocumentTree';
import withSelectableItemListing from 'lib/withSelectableItemListing';

const {
  prepareDocRefCreation,
  prepareDocRefDelete,
  prepareDocRefCopy,
  prepareDocRefRename,
  prepareDocRefMove,
} = actionCreators;

const LISTING_ID = 'folder-explorer';

const enhance = compose(
  withDocumentTree,
  withOpenDocRef,
  connect(
    ({ folderExplorer: { documentTree }, selectableItemListings }, { folderUuid }) => ({
      folder: findItem(documentTree, folderUuid),
      selectableItemListing: selectableItemListings[LISTING_ID] || {},
    }),
    {
      prepareDocRefCreation,
      prepareDocRefDelete,
      prepareDocRefCopy,
      prepareDocRefRename,
      prepareDocRefMove,
      fetchDocInfo,
    },
  ),
  branch(({ folder }) => !folder, renderComponent(() => <Loader active>Loading folder</Loader>)),
  withSelectableItemListing(({ openDocRef, folder: { node: { children } } }) => ({
    listingId: LISTING_ID,
    items: children,
    allowMultiSelect: true,
    openItem: openDocRef
  })),
  withProps(({
    folder,
    prepareDocRefCreation,
    prepareDocRefDelete,
    prepareDocRefCopy,
    prepareDocRefRename,
    prepareDocRefMove,
    fetchDocInfo,
    selectableItemListing: { selectedItems, items },
  }) => {
    const actionBarItems = [
      {
        icon: 'file',
        onClick: () => prepareDocRefCreation(folder.node),
        tooltip: 'Create a Document',
      },
    ];

    const singleSelectedDocRef = selectedItems.length === 1 ? selectedItems[0] : undefined;
    const selectedDocRefUuids = selectedItems.map(d => d.uuid);

    if (selectedItems.length > 0) {
      if (singleSelectedDocRef) {
        actionBarItems.push({
          icon: 'info',
          onClick: () => fetchDocInfo(singleSelectedDocRef),
          tooltip: 'View Information about this document',
        });
        actionBarItems.push({
          icon: 'pencil',
          onClick: () => prepareDocRefRename(singleSelectedDocRef),
          tooltip: 'Rename this document',
        });
      }
      actionBarItems.push({
        icon: 'copy',
        onClick: d => prepareDocRefCopy(selectedDocRefUuids),
        tooltip: 'Copy selected documents',
      });
      actionBarItems.push({
        icon: 'move',
        onClick: () => prepareDocRefMove(selectedDocRefUuids),
        tooltip: 'Move selected documents',
      });
      actionBarItems.push({
        icon: 'trash',
        onClick: () => prepareDocRefDelete(selectedDocRefUuids),
        tooltip: 'Delete selected documents',
      });
    }

    return { actionBarItems };
  }),
);

const FolderExplorer = ({
  folder: { node },
  folderUuid,
  actionBarItems,
  openDocRef,
  enableShortcuts,
  disableShortcuts,
}) => (
  <React.Fragment>
    <Grid className="content-tabs__grid">
      <Grid.Column width={16}>
        <AppSearchBar />
      </Grid.Column>
      <Grid.Column width={11}>
        <Header as="h3">
          <Icon name="folder" />
          <Header.Content className="header">{node.name}</Header.Content>
          <Header.Subheader>
            <DocRefBreadcrumb docRefUuid={node.uuid} openDocRef={openDocRef} />
          </Header.Subheader>
        </Header>
      </Grid.Column>
      <Grid.Column width={5}>
        <span className="doc-ref-listing-entry__action-bar">
          {actionBarItems.map(({
 onClick, icon, tooltip, disabled,
}, i) => (
  <ThemedPopup
    key={i}
    trigger={
      <Button
        className="icon-button"
        circular
        onClick={onClick}
        icon={icon}
        disabled={disabled}
      />
              }
    content={tooltip}
  />
          ))}
        </span>
      </Grid.Column>
    </Grid>
    <div
      className="doc-ref-listing"
      tabIndex={0}
      onFocus={enableShortcuts}
      onBlur={disableShortcuts}
    >
      {node.children.map((docRef, index) => (
        <DndDocRefListingEntry
          key={docRef.uuid}
          index={index}
          listingId={LISTING_ID}
          docRefUuid={docRef.uuid}
          onNameClick={node => openDocRef(node)}
          openDocRef={openDocRef}
        />
      ))}
    </div>

    <DocRefInfoModal />
    <NewDocDialog />
  </React.Fragment>
);

const EnhanceFolderExplorer = enhance(FolderExplorer);

EnhanceFolderExplorer.contextTypes = {
  store: PropTypes.object,
};

EnhanceFolderExplorer.propTypes = {
  folderUuid: PropTypes.string.isRequired,
};

export default EnhanceFolderExplorer;
