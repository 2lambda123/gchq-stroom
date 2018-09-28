/*
 * Copyright 2018 Crown Copyright
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

import { findItem, itemIsInSubtree, iterateNodes } from "../../lib/treeUtils";
import {
  PipelineModelType,
  PipelineAsTreeType,
  PipelineElementType,
  ItemWithId,
  Tree,
  ElementDefinitionsByType,
  ElementDefinition,
  PipelineDataType,
  PipelinePropertyType,
  AddRemove,
  PipelineLinkType
} from "../../types";

export interface RecycleBinItem {
  recycleData?: PipelineElementType;
  element: ElementDefinition;
}

export function getBinItems(
  pipeline: PipelineModelType,
  elementsByType: ElementDefinitionsByType
): Array<RecycleBinItem> {
  const thisConfigStack = pipeline.configStack[pipeline.configStack.length - 1];

  return thisConfigStack.elements.remove.map(e => ({
    recycleData: e,
    element: elementsByType[e.type]
  }));
}

/**
 * This function takes the denormalised pipeline definition and creates
 * a tree like structure. The root element is the 'from' of the first link.
 * This tree structure can then be used to lay the elements out in the graphical display.
 *
 * @param {object} pipeline
 * @return {object} Tree like structure
 */
export function getPipelineAsTree(
  pipeline: PipelineModelType
): PipelineAsTreeType | undefined {
  const elements: {
    [id: string]: PipelineAsTreeType;
  } = {};

  // Put all the elements into an object, keyed on id
  pipeline.merged.elements.add.forEach(e => {
    elements[e.id] = {
      uuid: e.id,
      type: e.type,
      children: []
    };
  });

  // Create the tree using links
  pipeline.merged.links.add.filter(l => !!elements[l.from]).forEach(l => {
    elements[l.from].children.push(elements[l.to]);
  });

  // Figure out the root
  let rootId;

  // First, is there an element of type Source?
  if (elements.Source) {
    rootId = "Source";
  } else {
    // if a link doesn't have anything going to it then it may be a root.
    const rootLinks = pipeline.merged.links.add.filter(fromLink => {
      const toLinks = pipeline.merged.links.add.filter(
        l => fromLink.from === l.to
      );
      return toLinks.length === 0;
    });

    if (rootLinks.length === 0) {
      // Maybe there's only one thing and therefore no links?
      if (pipeline.merged.elements.add.length !== 0) {
        // If there're no links then we can use the first element.
        rootId = pipeline.merged.elements.add[0].id;
      } else {
        // If there are no elements then we can't have a root node.
        rootId = undefined;
      }
    } else {
      // Just pick the first potential root? (not sure about this)
      rootId = rootLinks[0].from;
    }
  }

  return rootId ? elements[rootId] : undefined;
}

export enum Orientation {
  horizontal,
  vertical
}

export interface PipelineLayoutInfo {
  horizontalPos: number;
  verticalPos: number;
}

export interface PipelineLayoutInfoById {
  [uuid: string]: PipelineLayoutInfo;
}

/**
 * This calculates the layout information for the elements in a tree where the tree
 * must be layed out in a graphical manner.
 * The result object only indicates horizontal and vertical positions as 1-up integers.
 * A further mapping is required to convert this to specific layout pixel information.
 *
 * @param {treeNode} asTree The pipeline information as a tree with UUID's and 'children' on nodes.
 * @return {object} An object with a key for each UUID in the tree. The values are objects with
 * the following properties {horizontalPos, verticalPos}. These position indicators are just
 * 1-up integer values that can then bo converted to specific layout information (pixel position, position in grid etc)
 */
export function getPipelineLayoutInformation(
  asTree: PipelineAsTreeType,
  orientation: Orientation = Orientation.horizontal
): PipelineLayoutInfoById {
  const layoutInformation: PipelineLayoutInfoById = {};

  let sidewayPosition = 1;
  let lastLineageLengthSeen = -1;
  iterateNodes(asTree, (lineage, node) => {
    const forwardPosition = lineage.length;

    if (forwardPosition <= lastLineageLengthSeen) {
      sidewayPosition += 1;
    }
    lastLineageLengthSeen = forwardPosition;

    switch (orientation) {
      case Orientation.horizontal:
        layoutInformation[node.uuid] = {
          horizontalPos: forwardPosition,
          verticalPos: sidewayPosition
        };
        break;
      case Orientation.vertical:
        layoutInformation[node.uuid] = {
          horizontalPos: sidewayPosition,
          verticalPos: forwardPosition
        };
        break;
      default:
        throw new Error(`Invalid Orientation value: ${Orientation}`);
    }
  });

  return layoutInformation;
}

/**
 * Use this function to retrieve all element names from the pipeline.
 *
 * @param {pipeline} pipeline The pipeline from with to extract names
 */
export function getAllElementNames(pipeline: PipelineModelType): Array<string> {
  const names: Array<string> = [];

  pipeline.merged.elements.add
    .map(e => e.id)
    .map(id => id.toLowerCase())
    .forEach(id => names.push(id));
  pipeline.configStack.forEach(cs => {
    cs.elements.add
      .map(e => e.id)
      .map(id => id.toLowerCase())
      .forEach(id => names.push(id));
  });

  return names;
}

/**
 * Utility function for replacing the last item in an array by using a given map function.
 * All the other items are left as they are.
 */
function mapLastItemInArray(input: any, mapFunc: (input: any) => any) {
  return input.map(
    (item: any, i: number, arr: Array<any>) =>
      i === arr.length - 1 ? mapFunc(item) : item
  );
}

/**
 * Adds a new entry to a pipeline for the chosen element definition.
 * Creates the link between the new element and the given parent.
 * The new element will be given the name as it's unique ID.
 *
 * @param {pipeline} pipeline The current total picture of the pipeline
 * @param {string} parentId The ID of the parent that the link is being added to.
 * @param {element} childDefinition The definiton of the new element.
 * @param {string} name The name to give to the new element.
 */
export function createNewElementInPipeline(
  pipeline: PipelineModelType,
  parentId: string,
  childDefinition: ElementDefinition,
  name: string
): PipelineModelType {
  const newElement = {
    id: name,
    type: childDefinition.type
  };
  const newLink = {
    from: parentId,
    to: name
  };

  return {
    ...pipeline,
    configStack: mapLastItemInArray(pipeline.configStack, stackItem => ({
      properties: stackItem.properties,
      elements: {
        ...stackItem.elements,
        add: stackItem.elements.add.concat([newElement])
      },
      links: {
        ...stackItem.links,
        add: stackItem.links.add.concat([newLink])
      }
    })),
    merged: {
      ...pipeline.merged,
      elements: {
        ...pipeline.merged.elements,
        add: pipeline.merged.elements.add.concat([newElement])
      },
      links: {
        ...pipeline.merged.links,
        add: pipeline.merged.links.add
          // add the new link
          .concat([newLink])
      }
    }
  };
}

/**
 * Adds or updates a property on the element of a pipeline.
 *
 * @param {pipeline} pipeline The current definition of the pipeline.
 * @param {string} element The name of the element to update.
 * @param {string} name The name of the property on the element to update
 * @param {string} propertyType The type of the property to update, one of boolean, entity, integer, long, or string.
 * @param {boolean|entity|integer|long|string} propertyValue The value to add or update
 */
export function setElementPropertyValueInPipeline(
  pipeline: PipelineModelType,
  element: string,
  name: string,
  propertyType: string,
  propertyValue: any
): PipelineModelType {
  // Create the 'value' property.
  const value = {
    boolean: null,
    entity: null,
    integer: null,
    long: null,
    string: null
  };
  value[propertyType.toLowerCase()] = propertyValue;

  const property = {
    element,
    name,
    value
  };

  return {
    ...pipeline,
    configStack: mapLastItemInArray(pipeline.configStack, stackItem =>
      addPropertyToStackItem(stackItem, property)
    ),
    merged: addPropertyToStackItem(pipeline.merged, property)
  };
}

function addPropertyToStackItem(
  stackItem: PipelineDataType,
  property: PipelinePropertyType
): PipelineDataType {
  const result = {
    ...stackItem,
    properties: {
      ...stackItem.properties,
      add: stackItem.properties.add
        .filter(
          p => !(p.element === property.element && p.name === property.name)
        )
        .concat([property])
    }
  };

  return result;
}

export function revertPropertyToParent(
  pipeline: PipelineModelType,
  element: string,
  name: string
): PipelineModelType {
  if (pipeline.configStack.length < 2) {
    throw new Error("This function requires a configStack with a parent");
  }
  if (element === undefined)
    throw new Error("This function requires an element name");
  if (name === undefined)
    throw new Error("This function requires a property name");

  const childAdd =
    pipeline.configStack[pipeline.configStack.length - 1].properties.add;
  const parentAdd =
    pipeline.configStack[pipeline.configStack.length - 2].properties.add;
  const mergedAdd = pipeline.merged.properties.add;

  const indexToRemove = childAdd.findIndex(addItem => addItem.name === name);
  const parentProperty = parentAdd.find(addItem => addItem.name === name)!;
  const indexToRemoveFromMerged = mergedAdd.findIndex(
    addItem => addItem.name === name
  );

  return {
    ...pipeline,
    configStack: mapLastItemInArray(pipeline.configStack, stackItem => ({
      ...stackItem,
      properties: {
        ...stackItem.properties,
        add: [
          // We want to remove the property from the child
          ...stackItem.properties.add.slice(0, indexToRemove),
          ...stackItem.properties.add.slice(indexToRemove + 1)
        ]
      }
    })),
    merged: {
      ...pipeline.merged,
      properties: {
        ...pipeline.merged.properties,
        add: [
          // We want to remove the property from the merged stack...
          ...pipeline.merged.properties.add.slice(0, indexToRemoveFromMerged),
          ...pipeline.merged.properties.add.slice(indexToRemoveFromMerged + 1),
          // ... and replace it with the paren'ts property
          parentProperty
        ]
      }
    }
  };
}

export function revertPropertyToDefault(
  pipeline: PipelineModelType,
  element: string,
  name: string
): PipelineModelType {
  if (pipeline.configStack.length < 2) {
    throw new Error("This function requires a configStack with a parent");
  }
  if (element === undefined)
    throw new Error("This function requires an element name");
  if (name === undefined)
    throw new Error("This function requires a property name");

  const childAdd =
    pipeline.configStack[pipeline.configStack.length - 1].properties.add;
  const mergedAdd = pipeline.merged.properties.add;
  const indexToRemoveFromMerged = mergedAdd.findIndex(
    addItem => addItem.name === name
  );

  // We might be removing something that has an override on a child, in which case we
  // need to make sure we remove the child add too.
  const indexToRemove = childAdd.findIndex(addItem => addItem.name === name);
  if (indexToRemove !== -1) {
    const propertyForRemove = childAdd.find(addItem => addItem.name === name);
    return {
      ...pipeline,
      configStack: mapLastItemInArray(pipeline.configStack, stackItem => ({
        ...stackItem,
        properties: {
          ...stackItem.properties,
          add: [
            // We want to remove the property from the child
            ...stackItem.properties.add.slice(0, indexToRemove),
            ...stackItem.properties.add.slice(indexToRemove + 1)
          ],
          remove: [
            ...stackItem.properties.remove,
            propertyForRemove // We add the property we found on the parent
          ]
        }
      })),
      merged: {
        ...pipeline.merged,
        properties: {
          ...pipeline.merged.properties,
          add: [
            // Merged shouldn't have this property at all, and it doesn't need a remove either
            ...pipeline.merged.properties.add.slice(0, indexToRemoveFromMerged),
            ...pipeline.merged.properties.add.slice(indexToRemoveFromMerged + 1)
          ]
        }
      }
    };
  }
  // If we don't have this property in the child then we need to get it from the parent:
  const propertyForRemove = getParentProperty(
    pipeline.configStack,
    element,
    name
  );
  return {
    ...pipeline,
    configStack: mapLastItemInArray(pipeline.configStack, stackItem => ({
      ...stackItem,
      properties: {
        ...stackItem.properties,
        add: stackItem.properties.add, // We don't have anything to change here
        remove: [
          ...stackItem.properties.remove,
          propertyForRemove // We add the property we found on the parent
        ]
      }
    })),
    // Just copy them over
    merged: {
      ...pipeline.merged,
      properties: {
        ...pipeline.merged.properties,
        add: [
          // Merged shouldn't have this property at all, and it doesn't need a remove either
          ...pipeline.merged.properties.add.slice(0, indexToRemoveFromMerged),
          ...pipeline.merged.properties.add.slice(indexToRemoveFromMerged + 1)
        ]
      }
    }
  };
}

/**
 * Reinstates an element into the pipeline that had previously been removed.
 *
 * @param {pipeline} pipeline The current definition of the pipeline.
 * @param {string} parentId The ID of the element that the new connection will be made from.
 * @param {object} recycleData {id, type} The identifying information for the element being re-instated
 */
export function reinstateElementToPipeline(
  pipeline: PipelineModelType,
  parentId: string,
  recycleData: PipelineElementType
): PipelineModelType {
  const newLink = {
    from: parentId,
    to: recycleData.id
  };

  return {
    ...pipeline,
    configStack: mapLastItemInArray(pipeline.configStack, stackItem => ({
      properties: stackItem.properties,
      elements: {
        add: stackItem.elements.add.concat([recycleData]),
        remove: stackItem.elements.remove.filter(
          (e: PipelineElementType) => e.id !== recycleData.id
        )
      },
      links: {
        ...stackItem.links,
        add: stackItem.links.add.concat([newLink])
      }
    })),
    merged: {
      ...pipeline.merged,
      elements: {
        add: pipeline.merged.elements.add.concat([recycleData]),
        remove: pipeline.merged.elements.remove.filter(
          e => e.id !== recycleData.id
        )
      },
      links: {
        ...pipeline.merged.links,
        add: pipeline.merged.links.add
          // add the new link
          .concat([newLink])
      }
    }
  };
}

/**
 * Used to delete an element from a pipeline.
 *
 * @param {pipeline} pipeline Pipeline definition before the deletion
 * @param {string} itemToDelete ID of the item to delete
 * @return The updated pipeline definition.
 */
export function removeElementFromPipeline(
  pipeline: PipelineModelType,
  itemToDelete: string
): PipelineModelType {
  // Get hold of our entry in the config stack
  const configStackThis = pipeline.configStack[pipeline.configStack.length - 1];

  // Find the element in the merged picture (it should be there)
  const elementFromMerged: PipelineElementType = pipeline.merged.elements.add.find(
    e => e.id === itemToDelete
  )!;
  const linkFromMerged: PipelineLinkType = pipeline.merged.links.add.find(
    l => l.to === itemToDelete
  )!;

  // Find the element and link from our config stack (they may not be from our stack)
  const linkIsOurs = configStackThis.links.add.find(l => l.to === itemToDelete);

  // Determine the behaviour for removing the link, based on if it was ours or not
  let calcNewLinks: (
    links: AddRemove<PipelineLinkType>
  ) => AddRemove<PipelineLinkType>;
  if (linkIsOurs) {
    // we can simply filter out the add
    calcNewLinks = ourStackLinks => ({
      add: ourStackLinks.add.filter(l => l.to !== itemToDelete),
      remove: ourStackLinks.remove
    });
  } else {
    // we will need to shadow the link we inherited
    calcNewLinks = ourStackLinks => ({
      add: ourStackLinks.add,
      remove: ourStackLinks.remove.concat([linkFromMerged])
    });
  }

  return {
    ...pipeline,
    configStack: mapLastItemInArray(pipeline.configStack, stackItem => ({
      properties: stackItem.properties,
      elements: {
        add: stackItem.elements.add.filter(
          (e: PipelineElementType) => e.id !== itemToDelete
        ),
        remove: stackItem.elements.remove.concat([elementFromMerged])
      },
      links: calcNewLinks(stackItem.links)
    })),
    merged: {
      ...pipeline.merged,
      elements: {
        ...pipeline.merged.elements,
        add: pipeline.merged.elements.add.filter(e => e.id !== itemToDelete) // simply remove itemToDelete
      },
      links: {
        ...pipeline.merged.links,
        add: pipeline.merged.links.add.filter(l => l.to !== itemToDelete) // simply remove the link where to=itemToDelete
      }
    }
  };
}

/**
 * Use to check if a pipeline element can be moved onto the destination given.
 *
 * @param {pipeline} pipeline
 * @param {treeNode} pipelineAsTree
 * @param {string} itemToMove ID of the item to move
 * @param {string} destination ID of the destination
 * @return {boolean} Indicate if the move is valid.
 */

export function canMovePipelineElement(
  pipeline: PipelineModelType,
  pipelineAsTree: PipelineAsTreeType,
  itemToMove: string,
  destination: string
): boolean {
  const { node: itemToMoveNode } = findItem(pipelineAsTree, itemToMove)!;
  const { node: destinationNode } = findItem(pipelineAsTree, destination)!;

  // If either node cannot be found...bad times
  if (!itemToMoveNode || !destinationNode) {
    return false;
  }

  // If the item being dropped is a folder, and is being dropped into itself
  if (itemIsInSubtree(itemToMoveNode, destinationNode)) {
    return false;
  }
  if (
    !!itemToMoveNode.children &&
    itemToMoveNode.uuid === destinationNode.uuid
  ) {
    return false;
  }

  // Does this item appear in the destination folder already?
  return (
    destinationNode.children
      .map(c => c.uuid)
      .filter(u => u === itemToMoveNode.uuid).length === 0
  );
}

/**
 * Used to carry out the move of elements then return the updated pipeline definition.
 *
 * @param {pipeline} pipeline
 * @param {string} itemToMove Id of the element to move
 * @param {string} destination Id of the destination
 * @return The updated pipeline definition.
 */
export function moveElementInPipeline(
  pipeline: PipelineModelType,
  itemToMove: string,
  destination: string
): PipelineModelType {
  return {
    ...pipeline,
    configStack: pipeline.configStack,
    merged: {
      ...pipeline.merged,
      properties: pipeline.merged.properties,
      elements: pipeline.merged.elements,
      links: {
        ...pipeline.merged.links,
        add: pipeline.merged.links.add
          // Remove any existing link that goes into the moving item
          .filter(l => l.to !== itemToMove)
          // add the new link
          .concat([
            {
              from: destination,
              to: itemToMove
            }
          ])
      }
    }
  };
}

/**
 * Gets an array of all descendents of the pipeline element
 *
 * @param {pipeline} pipeline Pipeline definition
 * @param {string} parent The id of the parent
 */
export function getAllChildren(
  pipeline: PipelineModelType,
  parent: string
): Array<string> {
  let allChildren: Array<string> = [];

  const getAllChildren = (pipeline: PipelineModelType, element: string) => {
    const thisElementsChildren = pipeline.merged.links.add
      .filter(p => p.from === element)
      .map(p => p.to);
    allChildren = allChildren.concat(thisElementsChildren);
    for (const childIndex in thisElementsChildren) {
      getAllChildren(pipeline, thisElementsChildren[childIndex]);
    }
  };

  getAllChildren(pipeline, parent);

  return allChildren;
}

/**
 * Looks through the parents in the stack until it finds the first time this property has been set.
 * It'll return that value but if it's never been set it'll return undefined. It won't return a value
 * if the property exists in 'remove'.
 *
 * @param {configStack} stack The config stack for the pipeline
 * @param {string} elementId The elementId of the
 * @param {string} propertyName The name of the property to search for
 */
export function getParentProperty(
  stack: Array<PipelineDataType>,
  elementId: string,
  propertyName: string
) {
  const getFromParent = (index: number): PipelinePropertyType | undefined => {
    const property = stack[index].properties.add.find(
      (element: PipelinePropertyType) =>
        element.element === elementId && element.name === propertyName
    );
    const removeProperty = stack[index].properties.remove.find(
      (element: PipelinePropertyType) =>
        element.element === elementId && element.name === propertyName
    );
    if (property !== undefined) {
      // We return the first matching property we find.
      return property;
    }
    // If we haven't found one we might need to continue looking up the stack
    // We won't continue looking up the stack if we have a matching 'remove' property.
    if (index - 1 >= 0 && removeProperty === undefined) {
      return getFromParent(index - 1);
    }
    return undefined;
  };

  if (stack.length < 2) return undefined;
  return getFromParent(stack.length - 2);
}

/**
 * Gets the value of the child in an inherited element.
 *
 * @param {pipeline} pipeline The pipeline definition
 * @param {string} elementId The element id
 * @param {string} elementTypeName The name of the element type
 */
export function getChildValue(
  pipeline: PipelineModelType,
  elementId: string,
  elementTypeName: string
) {
  const elementPropertiesInChild = pipeline.configStack[
    pipeline.configStack.length - 1
  ].properties.add.filter(property => property.element === elementId);
  const childValue = elementPropertiesInChild.find(
    element => element.name === elementTypeName
  );
  return childValue;
}

/**
 * Gets the value of an element's property.
 *
 * @param {pipeline} pipeline The pipeline definition
 * @param {string} elementId The element id
 * @param {string} elementTypeName The name of the element type
 */
export function getElementValue(
  pipeline: PipelineModelType,
  elementId: string,
  elementTypeName: string
) {
  const elementProperties = pipeline.merged.properties.add.filter(
    property => property.element === elementId
  );
  const value = elementProperties.find(
    element => element.name === elementTypeName
  );
  return value;
}
