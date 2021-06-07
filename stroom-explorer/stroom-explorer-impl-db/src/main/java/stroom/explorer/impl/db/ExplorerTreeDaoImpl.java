package stroom.explorer.impl.db;

import stroom.db.util.JooqUtil;
import stroom.explorer.impl.ExplorerTreeDao;
import stroom.explorer.impl.ExplorerTreeNode;
import stroom.explorer.impl.ExplorerTreePath;
import stroom.explorer.impl.TreeModel;
import stroom.explorer.impl.db.jooq.tables.records.ExplorerNodeRecord;
import stroom.explorer.shared.ExplorerConstants;
import stroom.explorer.shared.ExplorerNode;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

import static stroom.explorer.impl.db.jooq.tables.ExplorerNode.EXPLORER_NODE;
import static stroom.explorer.impl.db.jooq.tables.ExplorerPath.EXPLORER_PATH;

class ExplorerTreeDaoImpl implements ExplorerTreeDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExplorerTreeDaoImpl.class);

    private final boolean orderIndexMatters;
    private final boolean removeReferencedNodes;
    private final ExplorerDbConnProvider explorerDbConnProvider;

    private final stroom.explorer.impl.db.jooq.tables.ExplorerPath p = EXPLORER_PATH.as("p");
    private final stroom.explorer.impl.db.jooq.tables.ExplorerPath p1 = EXPLORER_PATH.as("p1");
    private final stroom.explorer.impl.db.jooq.tables.ExplorerPath p2 = EXPLORER_PATH.as("p2");
    private final stroom.explorer.impl.db.jooq.tables.ExplorerNode n = EXPLORER_NODE.as("n");

    @Inject
    ExplorerTreeDaoImpl(final ExplorerDbConnProvider explorerDbConnProvider) {
        this.removeReferencedNodes = true;
        this.orderIndexMatters = false;
        this.explorerDbConnProvider = explorerDbConnProvider;


        try {
            final ExplorerTreeNode rootNode = new ExplorerTreeNode();
            rootNode.setName(ExplorerConstants.ROOT_DOC_REF.getName());
            rootNode.setType(ExplorerConstants.ROOT_DOC_REF.getType());
            rootNode.setUuid(ExplorerConstants.ROOT_DOC_REF.getUuid());
            createRoot(rootNode);
        } catch (final RuntimeException e) {
            // Ignore error as it is probably a duplicate entry constraint.
            LOGGER.debug(e.getMessage(), e);
        }
    }

    private boolean isPersistent(final ExplorerTreeNode entity) {
        return entity.getId() != null;
    }

    private void assertUpdate(final ExplorerTreeNode node) {
        if (!isPersistent(node)) {
            throw new IllegalArgumentException("Entity is not persistent! Use addChild() for " + node);
        }
    }

    private boolean isRemoveReferencedNodes() {
        return removeReferencedNodes;
    }

//    private void setRemoveReferencedNodes(boolean removeReferencedNodes) {
//        this.removeReferencedNodes = removeReferencedNodes;
//    }
//
//    private ExplorerTreePath getTreePath(ExplorerTreeNode node) {
//        JooqUtil.context(connectionProvider, context -> context
//            final List<ExplorerTreePath> result = create
//                    .selectFrom(p)
//                    .where(p.ANCESTOR.eq(node.getId()))
//                    .and(p.DESCENDANT.eq(node.getId()))
//                    .fetch()
//                    .stream()
//                    .map(r -> new ExplorerTreePath(
//                    r.getAncestor(), r.getDescendant(), r.getDepth(), r.getOrderIndex()))
//                    .collect(Collectors.toList());
//
//            if (result.size() <= 0) {
//                return null;
//            } else if (result.size() > 1) {
//                throw new IllegalStateException(
//                "Found more than one path for node " + node + ", paths are: " + result);
//            } else {
//                return result.get(0);
//            }
//
//        } catch (final SQLException e) {
//            LOGGER.error(e.getMessage(), e);
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }
//
//    private ExplorerTreeNode find(Integer id) {
//        JooqUtil.context(connectionProvider, context -> context
//            return create
//                    .selectFrom(n)
//                    .where(n.ID.eq(id))
//                    .fetchOptional()
//                    .map(r -> new ExplorerTreeNode(r.getId(), r.getType(), r.getUuid(), r.getName(), r.getTags()))
//                    .orElse(null);
//        } catch (final SQLException e) {
//            LOGGER.error(e.getMessage(), e);
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }

    private ExplorerTreeNode create(final ExplorerTreeNode node) {
        return JooqUtil.contextResult(explorerDbConnProvider, context -> {
            final int id = context
                    .insertInto(EXPLORER_NODE)
                    .set(EXPLORER_NODE.TYPE, node.getType())
                    .set(EXPLORER_NODE.UUID, node.getUuid())
                    .set(EXPLORER_NODE.NAME, node.getName())
                    .set(EXPLORER_NODE.TAGS, node.getTags())
                    .returning(EXPLORER_NODE.ID)
                    .fetchOne()
                    .getId();
            node.setId(id);
            return node;
        });
    }

    @Override
    public void update(final ExplorerTreeNode node) {
        assertUpdate(node);

        JooqUtil.context(explorerDbConnProvider, context -> context
                .update(EXPLORER_NODE)
                .set(EXPLORER_NODE.TYPE, node.getType())
                .set(EXPLORER_NODE.UUID, node.getUuid())
                .set(EXPLORER_NODE.NAME, node.getName())
                .set(EXPLORER_NODE.TAGS, node.getTags())
                .where(EXPLORER_NODE.ID.eq(node.getId()))
                .execute());
    }

    private ExplorerTreePath create(final ExplorerTreePath path) {
        JooqUtil.context(explorerDbConnProvider, context -> context
                .insertInto(EXPLORER_PATH)
                .set(EXPLORER_PATH.ANCESTOR, path.getAncestor())
                .set(EXPLORER_PATH.DESCENDANT, path.getDescendant())
                .set(EXPLORER_PATH.DEPTH, path.getDepth())
                .set(EXPLORER_PATH.ORDER_INDEX, path.getOrderIndex())
                .execute());
        return path;
    }

    private void update(final ExplorerTreePath path) {
        JooqUtil.context(explorerDbConnProvider, context -> context
                .update(EXPLORER_PATH)
                .set(EXPLORER_PATH.ANCESTOR, path.getAncestor())
                .set(EXPLORER_PATH.DESCENDANT, path.getDescendant())
                .set(EXPLORER_PATH.DEPTH, path.getDepth())
                .set(EXPLORER_PATH.ORDER_INDEX, path.getOrderIndex())
                .where(EXPLORER_PATH.ANCESTOR.eq(path.getAncestor()))
                .and(EXPLORER_PATH.DESCENDANT.eq(path.getDescendant()))
                .execute());
    }

//    private boolean isRoot(ExplorerTreeNode node) {
//        if (!isPersistent(node)) {
//            return false;
//        } else {
//            JooqUtil.context(connectionProvider, context -> context
//                final int count = create
//                        .selectCount()
//                        .from(p)
//                        .where(p.DESCENDANT.eq(node.getId()))
//                        .and(p.DEPTH.gt(0))
//                        .fetchOne(0, int.class);
//
//                return 0 == count;
//
//            } catch (final SQLException e) {
//                LOGGER.error(e.getMessage(), e);
//                throw new RuntimeException(e.getMessage(), e);
//            }
//        }
//    }

    @Override
    public ExplorerTreeNode createRoot(final ExplorerTreeNode root) {
        return addChild(null, root);
    }

    @Override
    public List<ExplorerTreeNode> getRoots() {
        return JooqUtil.contextResult(explorerDbConnProvider, context -> context
                .selectFrom(n)
                .whereNotExists(context
                        .selectOne()
                        .from(p)
                        .where(p.DESCENDANT.eq(n.ID).and(p.DEPTH.gt(0))))
                .fetch()
                .stream()
                .map(r -> new ExplorerTreeNode(r.getId(), r.getType(), r.getUuid(), r.getName(), r.getTags()))
                .collect(Collectors.toList()));
    }

    @Override
    public TreeModel createModel(final Function<String, String> iconUrlProvider) {
        final TreeModel treeModel = new TreeModel();

        final List<Integer> roots = JooqUtil.contextResult(explorerDbConnProvider, context -> context
                .select(p.ANCESTOR)
                .from(p)
                .where(p.DEPTH.eq(0))
                .andNotExists(
                        context
                                .select(DSL.val("x"))
                                .from(p2)
                                .where(p2.DESCENDANT.eq(p.DESCENDANT))
                                .and(p2.DEPTH.gt(0))
                )
                .fetch(p.ANCESTOR));
        if (roots.size() > 0) {
            final Map<Integer, ExplorerNode> nodeMap = JooqUtil.contextResult(explorerDbConnProvider, context -> context
                    .selectFrom(n)
                    .fetch()
                    .stream()
                    .collect(Collectors.toMap(ExplorerNodeRecord::getId, r ->
                            ExplorerNode
                                    .builder()
                                    .type(r.getType())
                                    .uuid(r.getUuid())
                                    .name(r.getName())
                                    .tags(r.getTags())
                                    .iconClassName(iconUrlProvider.apply(r.getType()))
                                    .build())));

            // Add the roots.
            roots.forEach(rootId -> {
                final ExplorerNode root = nodeMap.get(rootId);
                if (root != null) {
                    treeModel.add(null, root);
                }
            });

            // Add parents and children.
            JooqUtil.context(explorerDbConnProvider, context -> context
                    .select(p.ANCESTOR, p.DESCENDANT)
                    .from(p)
                    .where(p.DEPTH.eq(1))
                    .fetch()
                    .forEach(r -> {
                        final int ancestorId = r.value1();
                        final int descendantId = r.value2();
                        final ExplorerNode ancestor = nodeMap.get(ancestorId);
                        final ExplorerNode descendant = nodeMap.get(descendantId);
                        if (ancestor != null && descendant != null) {
                            treeModel.add(ancestor, descendant);
                        }
                    }));
        }

        return treeModel;
    }

    @Override
    public synchronized void removeAll() {
        JooqUtil.context(explorerDbConnProvider, context -> context
                .deleteFrom(p)
                .execute());

        JooqUtil.context(explorerDbConnProvider, context -> context
                .deleteFrom(n)
                .execute());
    }

    @Override
    public List<ExplorerTreeNode> getTree(final ExplorerTreeNode parent) {
        return JooqUtil.contextResult(explorerDbConnProvider, context -> context
                .selectFrom(n)
                .where(n.ID.in(context
                        .select(p.DESCENDANT)
                        .from(p)
                        .where(p.ANCESTOR.eq(parent.getId()))))
                .fetch()
                .stream()
                .map(r -> new ExplorerTreeNode(r.getId(), r.getType(), r.getUuid(), r.getName(), r.getTags()))
                .collect(Collectors.toList()));

    }

//    private int getChildCount(ExplorerTreeNode parent) {
//        JooqUtil.context(connectionProvider, context -> context
//            return create
//                    .selectCount()
//                    .from(p)
//                    .where(p.ANCESTOR.eq(parent.getId()))
//                    .and(p.DEPTH.eq(1))
//                    .fetchOne(0, int.class);
//        } catch (final SQLException e) {
//            LOGGER.error(e.getMessage(), e);
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }

    @Override
    public List<ExplorerTreeNode> getChildren(final ExplorerTreeNode parent) {
        return JooqUtil.contextResult(explorerDbConnProvider, context -> context
                .selectFrom(n)
                .where(n.ID.in(context
                        .select(p.DESCENDANT)
                        .from(p)
                        .where(p.ANCESTOR.eq(parent.getId()))
                        .and(p.DEPTH.eq(1))
                        .orderBy(p.ORDER_INDEX)))
                .fetch()
                .stream()
                .map(r -> new ExplorerTreeNode(r.getId(), r.getType(), r.getUuid(), r.getName(), r.getTags()))
                .collect(Collectors.toList()));
    }

//    private ExplorerTreeNode getRoot(ExplorerTreeNode node) {
//        if (node == null) {
//            throw new IllegalArgumentException("Can not read root for a null node!");
//        } else {
//            List<ExplorerTreeNode> path = getPath(node);
//            return path.size() > 0 ? path.get(0) : node;
//        }
//    }

    @Override
    public ExplorerTreeNode getParent(final ExplorerTreeNode child) {
        return JooqUtil.contextResult(explorerDbConnProvider, context -> {
            final List<ExplorerTreeNode> parents = context
                    .selectFrom(n)
                    .where(n.ID.in(context
                            .select(p.ANCESTOR)
                            .from(p)
                            .where(p.DESCENDANT.eq(child.getId()))
                            .and(p.DEPTH.eq(1))))
                    .fetch()
                    .stream()
                    .map(r -> new ExplorerTreeNode(r.getId(), r.getType(), r.getUuid(), r.getName(), r.getTags()))
                    .collect(Collectors.toList());

            if (parents.size() == 1) {
                return parents.get(0);
            } else if (parents.size() == 0) {
                return null;
            } else {
                throw new IllegalArgumentException("More than one parent found: " + parents);
            }
        });
    }

    @Override
    public List<ExplorerTreeNode> getPath(final ExplorerTreeNode node) {
        return JooqUtil.contextResult(explorerDbConnProvider, context -> {

            final Result<? extends Record> result = context
                    .select(n.ID, n.TYPE, n.UUID, n.NAME, n.TAGS, p.DEPTH)
                    .from(n.innerJoin(p).on(n.ID.eq(p.ANCESTOR)))
                    .where(p.DESCENDANT.eq(node.getId()).and(p.ANCESTOR.ne(node.getId())))
                    .orderBy(p.DEPTH.desc())
                    .fetch();

            ArrayList<ExplorerTreeNode> path = new ArrayList<>();
            for (Record r : result) {
                path.add(new ExplorerTreeNode(r.get(n.ID), r.get(n.TYPE), r.get(n.UUID), r.get(n.NAME), r.get(n.TAGS)));
            }
            return path;
        });
    }

//    private int getLevel(ExplorerTreeNode node) {
//        JooqUtil.context(connectionProvider, context -> context
//            return create
//                    .selectCount()
//                    .from(p)
//                    .where(p.DESCENDANT.eq(node.getId()))
//                    .fetchOne(0, int.class) - 1;
//        } catch (final SQLException e) {
//            LOGGER.error(e.getMessage(), e);
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }
//
//    private int size(ExplorerTreeNode parent) {
//       JooqUtil.context(connectionProvider, context -> context
//            return create
//                    .selectCount()
//                    .from(p)
//                    .where(p.ANCESTOR.eq(parent.getId()))
//                    .fetchOne(0, int.class);
//        } catch (final SQLException e) {
//            LOGGER.error(e.getMessage(), e);
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }
//
//    private boolean isLeaf(ExplorerTreeNode node) {
//        return getChildCount(node) <= 0;
//    }
//
//    private boolean isEqualToOrChildOf(ExplorerTreeNode child, ExplorerTreeNode parent) {
//        return Objects.equals(parent, child) || isChildOf(child, parent);
//    }
//
//    private boolean isChildOf(ExplorerTreeNode child, ExplorerTreeNode parent) {
//        if (Objects.equals(parent, child)) {
//            return false;
//        } else {
//            JooqUtil.context(connectionProvider, context -> context
//                final int count = create
//                        .selectCount()
//                        .from(p)
//                        .where(p.ANCESTOR.eq(parent.getId()))
//                        .and(p.DESCENDANT.eq(child.getId()))
//                        .fetchOne(0, int.class);
//
//                if (count > 1) {
//                    throw new IllegalStateException(
//                    "Ambiguous ancestor/descendant, found " + count +
//                    " paths for parent " + parent + " and child " + child);
//                } else {
//                    return count == 1;
//                }
//            } catch (final SQLException e) {
//                LOGGER.error(e.getMessage(), e);
//                throw new RuntimeException(e.getMessage(), e);
//            }
//        }
//    }

    @Override
    public ExplorerTreeNode addChild(final ExplorerTreeNode parent,
                                     final ExplorerTreeNode child) {
        return addChildAt(parent, child, -1);
    }

    private synchronized ExplorerTreeNode addChildAt(final ExplorerTreeNode parent,
                                                     final ExplorerTreeNode child,
                                                     final int position) {
        return addChild(parent, null, child, position);
    }

//    private synchronized ExplorerTreeNode addChildBefore(ExplorerTreeNode sibling, ExplorerTreeNode child) {
//        return addChild(null, sibling, child, -1);
//    }

    @Override
    public synchronized void remove(final ExplorerTreeNode node) {
        if (node != null && isPersistent(node)) {
            removeTree(node.getId());
        } else {
            throw new IllegalArgumentException("Node to remove is null or not persistent: " + node);
        }
    }

    @Override
    public void move(final ExplorerTreeNode node, final ExplorerTreeNode parent) {
        moveTo(node, parent, -1);
    }

    private synchronized void moveTo(final ExplorerTreeNode node,
                                     final ExplorerTreeNode parent,
                                     final int position) {
        move(node, parent, position, null);
    }

//    private synchronized void moveBefore(ExplorerTreeNode node, ExplorerTreeNode sibling) {
//        move(node, null, -1, sibling);
//    }
//
//    private synchronized void moveToBeRoot(ExplorerTreeNode child) {
//        move(child, null, -1, null);
//    }
//
//    private ExplorerTreeNode copy(ExplorerTreeNode node, ExplorerTreeNode parent) {
//        return copyTo(node, parent, -1);
//    }
//
//    private synchronized ExplorerTreeNode copyTo(ExplorerTreeNode node, ExplorerTreeNode parent, int position) {
//        return copy(node, parent, position, null);
//    }
//
//    private synchronized ExplorerTreeNode copyBefore(ExplorerTreeNode node, ExplorerTreeNode sibling) {
//        return copy(node, null, -1, sibling);
//    }
//
//    private synchronized ExplorerTreeNode copyToBeRoot(ExplorerTreeNode child) {
//        return copy(child, null, -1, null);
//    }
//
//    private List<ExplorerTreeNode> find(ExplorerTreeNode parent, Map<String, Object> criteria) {
//        JooqUtil.context(connectionProvider, context -> context
//
//            Condition condition = null;
//            if (parent != null) {
//                condition = and(p.ANCESTOR.eq(parent.getId()));
//            }
//
//            return create
//                    .select(n.ID, n.TYPE, n.UUID, n.NAME, n.TAGS)
//                    .from(n)
//                    .join(p).on(p.DESCENDANT.eq(n.ID))
//                    .where(condition)
//                    .fetch()
//                    .stream()
//                    .map(r -> new ExplorerTreeNode(
//                    r.component1(), r.component2(), r.component3(), r.component4(), r.component5()))
//                    .collect(Collectors.toList());
//
//        } catch (final SQLException e) {
//            LOGGER.error(e.getMessage(), e);
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }

    private boolean shouldCloseGapOnRemove() {
        return true;
    }

    private void removeTree(final Integer parentId) {
        boolean closeGap = shouldCloseGapOnRemove();
        List<ExplorerTreePath> pathSiblings = closeGap
                ? getAllTreePathSiblings(parentId)
                : null;
        Set<Integer> nodesToRemove = new HashSet<>();
        int orderIndex = -1;

        final List<ExplorerTreePath> paths = getPathsToRemove(parentId);
        for (ExplorerTreePath path : paths) {
            removePath(path);
            nodesToRemove.add(path.getDescendant());
            if (closeGap && path.getDepth() == 1 && Objects.equals(path.getDescendant(), parentId)) {
                assert orderIndex == -1 : "Found second path with depth = 1 where node is descendant: " + path;
                orderIndex = path.getOrderIndex();
            }
        }

        if (closeGap) {
            closeGap(pathSiblings, orderIndex);
        }

        for (final Integer id : nodesToRemove) {
            removeNode(id);
        }
    }

    private void removePath(final ExplorerTreePath path) {
        JooqUtil.context(explorerDbConnProvider, context -> context
                .deleteFrom(p)
                .where(p.ANCESTOR.eq(path.getAncestor()))
                .and(p.DESCENDANT.eq(path.getDescendant()))
                .execute());
    }

//    private void removeNode(ExplorerTreeNode nodeToRemove) {
//        removeNode(nodeToRemove.getId());
//    }

    private void removeNode(final Integer id) {
        if (isRemoveReferencedNodes()) {
            JooqUtil.context(explorerDbConnProvider, context -> context
                    .deleteFrom(n)
                    .where(n.ID.eq(id))
                    .execute());
        }
    }

    private List<ExplorerTreePath> getPathsToRemove(final Integer nodeId) {
        return JooqUtil.contextResult(explorerDbConnProvider, context -> context
                .selectFrom(p)
                .where(p.DESCENDANT.in(context
                        .select(p1.DESCENDANT)
                        .from(p1)
                        .where(p1.ANCESTOR.eq(nodeId))))
                .fetch()
                .stream()
                .map(r -> new ExplorerTreePath(r.getAncestor(), r.getDescendant(), r.getDepth(), r.getOrderIndex()))
                .collect(Collectors.toList()));
    }

    private ExplorerTreeNode addChild(final ExplorerTreeNode parent,
                                      final ExplorerTreeNode sibling,
                                      ExplorerTreeNode child,
                                      int orderIndex) {
        if (child == null) {
            throw new IllegalArgumentException("Node to add is null!");
        } else if (isPersistent(child) && exists(child)) {
            throw new IllegalArgumentException("Node is already part of tree: " + child);
        } else {
            assertInsertParameters(parent, sibling, orderIndex);
            boolean relatedNodeIsParent = parent != null;
            List<ExplorerTreePath> pathsToClone = new ArrayList<>();
            orderIndex = getPositionAndPathsToConnectSubTree(relatedNodeIsParent,
                    orderIndex,
                    parent,
                    sibling,
                    pathsToClone);
            if (!isPersistent(child)) {
                child = create(child);
            }

            insertSelfReference(child);
            clonePaths(child.getId(), 0, orderIndex, pathsToClone, relatedNodeIsParent);
            return child;
        }
    }

    private boolean exists(final ExplorerTreeNode node) {
        return JooqUtil.contextResult(explorerDbConnProvider, context -> {
            final int count = context
                    .selectCount()
                    .from(p)
                    .where(p.DESCENDANT.eq(node.getId()))
                    .fetchOne(0, int.class);
            return count > 0;
        });
    }

    private void insertSelfReference(final ExplorerTreeNode child) {
        final ExplorerTreePath newPath = new ExplorerTreePath(child.getId(), child.getId(), 0, -1);
        create(newPath);
    }

    private void move(final ExplorerTreeNode nodeToMove,
                      final ExplorerTreeNode newParent,
                      final int position,
                      final ExplorerTreeNode sibling) {
        assertCopyOrMoveParameters(nodeToMove, newParent, position, sibling);
        disconnectSubTree(nodeToMove);
        if (newParent != null || sibling != null) {
            List<ExplorerTreePath> childPaths = getPathsIntoSubtree(nodeToMove);
            connectSubTree(newParent, position, sibling, childPaths);
        }
    }

    private void disconnectSubTree(final ExplorerTreeNode node) {
        JooqUtil.context(explorerDbConnProvider, context -> {
            final List<ExplorerTreePath> pathsToRemove = context
                    .selectFrom(p)
                    .where(p.DESCENDANT.in(context
                            .select(p1.DESCENDANT)
                            .from(p1)
                            .where(p1.ANCESTOR.eq(node.getId()))))
                    .and(p.ANCESTOR.notIn(context
                            .select(p2.DESCENDANT)
                            .from(p2)
                            .where(p2.ANCESTOR.eq(node.getId()))))
                    .fetch()
                    .stream()
                    .map(r -> new ExplorerTreePath(r.getAncestor(), r.getDescendant(), r.getDepth(), r.getOrderIndex()))
                    .collect(Collectors.toList());

            List<ExplorerTreePath> pathSiblings = getAllTreePathSiblings(node.getId());
            int oldPosition = -1;

            for (final ExplorerTreePath path : pathsToRemove) {
                removePath(path);
                if (path.getDepth() == 1) {
                    oldPosition = path.getOrderIndex();
                }
            }

            closeGap(pathSiblings, oldPosition);
        });
    }

    private List<ExplorerTreePath> getPathsIntoSubtree(final ExplorerTreeNode parent) {
        return JooqUtil.contextResult(explorerDbConnProvider, context -> context
                .selectFrom(p)
                .where(p.ANCESTOR.eq(parent.getId()))
                .fetch()
                .stream()
                .map(r -> new ExplorerTreePath(r.getAncestor(), r.getDescendant(), r.getDepth(), r.getOrderIndex()))
                .collect(Collectors.toList()));
    }

//    private ExplorerTreeNode copy(
//    ExplorerTreeNode node, ExplorerTreeNode newParent, int position, ExplorerTreeNode sibling) {
//        assertCopyOrMoveParameters(node, newParent, position, sibling);
//        List<ExplorerTreePath> pathsToCopy = getSubTreePathsToCopy(node);
//        List<ExplorerTreePath> childPaths = new ArrayList<>();
//        ExplorerTreeNode copiedNode = copySubTree(pathsToCopy, node, childPaths);
//        if (newParent != null || sibling != null) {
//            connectSubTree(newParent, position, sibling, childPaths);
//        }
//
//        return copiedNode;
//    }
//
//    private List<ExplorerTreePath> getSubTreePathsToCopy(ExplorerTreeNode parent) {
//        JooqUtil.context(connectionProvider, context -> context
//            return create
//                    .selectFrom(p)
//                    .where(p.DESCENDANT.in(create
//                            .select(p2.DESCENDANT)
//                            .from(p2)
//                            .where(p2.ANCESTOR.eq(parent.getId()))))
//                    .and(p.ANCESTOR.in(create
//                            .select(p2.DESCENDANT)
//                            .from(p2)
//                            .where(p2.ANCESTOR.eq(parent.getId()))))
//                    .fetch()
//                    .stream()
//                    .map(r -> new ExplorerTreePath(
//                    r.getAncestor(), r.getDescendant(), r.getDepth(), r.getOrderIndex()))
//                    .collect(Collectors.toList());
//        } catch (final SQLException e) {
//            LOGGER.error(e.getMessage(), e);
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }
//
//    private ExplorerTreeNode copySubTree(
//    List<ExplorerTreePath> pathsToCopy, ExplorerTreeNode node, List<ExplorerTreePath> childPaths) {
//        Map<Integer, ExplorerTreeNode> cloneMap = new HashMap<>();
//        ExplorerTreeNode copiedNode = node;
//        Map<Integer, ExplorerTreeNode> mergedCloneMap = new HashMap<>();
//
//        for (final Entry<Integer, ExplorerTreeNode> entry : cloneMap.entrySet()) {
//            Integer originalId = entry.getKey();
//            ExplorerTreeNode clonedNode = entry.getValue();
//            ExplorerTreeNode mergedClonedNode = create(clonedNode);
//            mergedCloneMap.put(originalId, mergedClonedNode);
//            if (clonedNode == copiedNode) {
//                copiedNode = mergedClonedNode;
//            }
//        }
//
//        for (final ExplorerTreePath pathToCopy : pathsToCopy) {
//            ExplorerTreePath clonedPath = new ExplorerTreePath(pathToCopy.getAncestor(),
//            pathToCopy.getDescendant(), pathToCopy.getDepth(), pathToCopy.getOrderIndex());
//            ExplorerTreePath mergedClonedPath = create(clonedPath);
//            if (Objects.equals(pathToCopy.getAncestor(), node.getId())) {
//                childPaths.add(mergedClonedPath);
//            }
//        }
//
//        return copiedNode;
//    }

    private void connectSubTree(final ExplorerTreeNode newParent,
                                int position,
                                final ExplorerTreeNode sibling,
                                final List<ExplorerTreePath> childPaths) {
        boolean relatedNodeIsParent = newParent != null;
        List<ExplorerTreePath> pathsToClone = new ArrayList<>();
        position = getPositionAndPathsToConnectSubTree(relatedNodeIsParent, position, newParent, sibling, pathsToClone);

        for (final ExplorerTreePath childPath : childPaths) {
            clonePaths(childPath.getDescendant(), childPath.getDepth(), position, pathsToClone, relatedNodeIsParent);
        }
    }

    private int getPositionAndPathsToConnectSubTree(final boolean relatedNodeIsParent,
                                                    int position,
                                                    final ExplorerTreeNode parent,
                                                    final ExplorerTreeNode sibling,
                                                    final List<ExplorerTreePath> pathsToClone) {
        assert pathsToClone != null && pathsToClone.size() <= 0;

        assert !relatedNodeIsParent || parent != null;

        if (relatedNodeIsParent) {
            JooqUtil.context(explorerDbConnProvider, context -> {
                final List<ExplorerTreePath> paths = context
                        .selectFrom(p)
                        .where(p.DESCENDANT.eq(parent.getId()))
                        .fetch()
                        .stream()
                        .map(r -> new ExplorerTreePath(r.getAncestor(),
                                r.getDescendant(),
                                r.getDepth(),
                                r.getOrderIndex()))
                        .collect(Collectors.toList());
                pathsToClone.addAll(paths);
            });
        } else if (sibling != null) {
            position = JooqUtil.contextResult(explorerDbConnProvider, context -> {
                final List<ExplorerTreePath> paths = context
                        .selectFrom(p)
                        .where(p.DESCENDANT.eq(parent.getId()))
                        .and(p.DEPTH.gt(0))
                        .orderBy(p.DEPTH)
                        .fetch()
                        .stream()
                        .map(r -> new ExplorerTreePath(r.getAncestor(),
                                r.getDescendant(),
                                r.getDepth(),
                                r.getOrderIndex()))
                        .collect(Collectors.toList());
                pathsToClone.addAll(paths);

                if (pathsToClone.size() <= 0) {
                    throw new IllegalArgumentException("Sibling seems not to be a child but a root: " + sibling);
                }

                final int pos = pathsToClone.get(0).getOrderIndex();

                assert pos >= 0 : "Position of first path is not valid: " + pathsToClone;

                return pos;
            });
        }

        return position;
    }

    private void clonePaths(final Integer childId,
                            final int addToDepth,
                            final int position,
                            final List<ExplorerTreePath> pathsToClone,
                            final boolean isParentPaths) {
        for (final ExplorerTreePath pathToClone : pathsToClone) {
            final int depth = pathToClone.getDepth() + addToDepth + (isParentPaths
                    ? 1
                    : 0);
            final int newPosition = depth == 1
                    ? createGap(isParentPaths
                    ? pathToClone.getDescendant()
                    : pathToClone.getAncestor(), position)
                    : -1;
            final ExplorerTreePath newPath = new ExplorerTreePath(pathToClone.getAncestor(),
                    childId,
                    depth,
                    newPosition);
            create(newPath);
        }
    }

    private int createGap(final Integer parentId,
                          final int position) {
        if (!orderIndexMatters) {
            return 0;
        } else {
            List<ExplorerTreePath> children = getAllDirectTreePathChildren(parentId);
            if (position == -1) {
                return children.size();
            } else {
                for (int i = children.size() - 1; i >= position; --i) {
                    final ExplorerTreePath treePath = children.get(i);
                    treePath.setOrderIndex(i + 1);
                    update(treePath);
                }

                return position;
            }
        }
    }

    private void closeGap(final List<ExplorerTreePath> siblings,
                          final int removedPosition) {
        if (orderIndexMatters) {
            if (removedPosition >= 0) {
                for (int i = removedPosition; i < siblings.size(); ++i) {
                    final ExplorerTreePath pathSibling = siblings.get(i);
                    pathSibling.setOrderIndex(i);
                    update(pathSibling);
                }
            }

        }
    }

    private List<ExplorerTreePath> getAllDirectTreePathChildren(final Integer parentId) {
        return JooqUtil.contextResult(explorerDbConnProvider, context -> context
                .selectFrom(p)
                .where(p.ANCESTOR.eq(parentId))
                .and(p.DEPTH.eq(1))
                .orderBy(p.ORDER_INDEX)
                .fetch()
                .stream()
                .map(r -> new ExplorerTreePath(r.getAncestor(), r.getDescendant(), r.getDepth(), r.getOrderIndex()))
                .collect(Collectors.toList()));
    }

    private List<ExplorerTreePath> getAllTreePathSiblings(final Integer nodeId) {
        return JooqUtil.contextResult(explorerDbConnProvider, context -> context
                .selectFrom(p)
                .where(p.DEPTH.eq(1))
                .and(p.DESCENDANT.notEqual(nodeId))
                .and(p.ANCESTOR.in(context
                        .select(p2.ANCESTOR)
                        .from(p2)
                        .where(p2.DESCENDANT.eq(nodeId))
                        .and(p2.DEPTH.eq(1))))
                .orderBy(p.ORDER_INDEX)
                .fetch()
                .stream()
                .map(r -> new ExplorerTreePath(r.getAncestor(), r.getDescendant(), r.getDepth(), r.getOrderIndex()))
                .collect(Collectors.toList()));
    }

    @Override
    public ExplorerTreeNode findByUUID(final String uuid) {
        if (uuid == null) {
            return null;
        }

        return JooqUtil.contextResult(explorerDbConnProvider, context -> {
            final List<ExplorerTreeNode> list = context
                    .selectFrom(n)
                    .where(n.UUID.eq(uuid))
                    .fetch()
                    .stream()
                    .map(r -> new ExplorerTreeNode(r.getId(), r.getType(), r.getUuid(), r.getName(), r.getTags()))
                    .collect(Collectors.toList());

            if (list.size() == 0) {
                return null;
            }

            if (list.size() > 1) {
                throw new RuntimeException("Found more than 1 tree node with uuid=" + uuid);
            }

            return list.get(0);
        });
    }

    private void assertInsertParameters(final ExplorerTreeNode parent,
                                        final ExplorerTreeNode sibling,
                                        final int position) {
        assert parent == null || sibling == null;

        assert sibling == null || position == -1;

    }

    private void assertCopyOrMoveParameters(final ExplorerTreeNode node,
                                            final ExplorerTreeNode newParent,
                                            final int position,
                                            final ExplorerTreeNode sibling) {
        if (node != null && isPersistent(node)) {
            assertInsertParameters(newParent, sibling, position);
        } else {
            throw new IllegalArgumentException("Node to move is null or not persistent: " + node);
        }
    }
}
