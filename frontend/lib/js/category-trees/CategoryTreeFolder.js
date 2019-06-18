// @flow

import React from "react";
import styled from "@emotion/styled";

import type { NodeType, ConceptIdT } from "../api/types";

import { getConceptById } from "./globalTreeStoreHelper";
import Openable from "./Openable";
import CategoryTree from "./CategoryTree";
import CategoryTreeNodeTextContainer from "./CategoryTreeNodeTextContainer";
import { type SearchType } from "./reducer";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
`;

type PropsType = {
  depth: number,
  trees: Object,
  tree: NodeType,
  treeId: ConceptIdT,
  active: boolean,
  open?: boolean,
  onToggleOpen?: Function,
  search: SearchType,
  onLoadTree: (id: string) => void
};

const sumMatchingEntries = (children, initSum) => {
  return children.reduce((sum, treeId) => {
    const rootConcept = getConceptById(treeId);
    const rootMatchingEntries = rootConcept ? rootConcept.matchingEntries : 0;

    return rootMatchingEntries ? sum + rootMatchingEntries : sum;
  }, initSum);
};

const CategoryTreeFolder = (props: PropsType) => {
  const { tree, search } = props;
  const matchingEntries =
    !tree.children || !tree.matchingEntries
      ? null
      : sumMatchingEntries(tree.children, tree.matchingEntries);

  return (
    <Root>
      <CategoryTreeNodeTextContainer
        node={{
          id: props.treeId,
          label: props.tree.label,
          description: props.tree.description,
          matchingEntries: matchingEntries,
          dateRange: props.tree.dateRange,
          additionalInfos: props.tree.additionalInfos,
          children: props.tree.children
        }}
        createQueryElement={() => {
          // We don't have to implement this since CategoryTreeFolders should never be
          // dragged into the editor, hence they're 'active: false' and thus not draggable
        }}
        isTreeFolder
        open={props.open || false}
        depth={props.depth}
        active={props.active}
        onTextClick={props.onToggleOpen}
        search={search}
      />
      {props.open &&
        props.tree.children &&
        props.tree.children.map((childId, i) => {
          const tree = props.trees[childId];

          if (tree.detailsAvailable) {
            const rootConcept = getConceptById(childId);

            return (
              <CategoryTree
                key={i}
                id={childId}
                label={tree.label}
                tree={rootConcept}
                treeId={childId}
                loading={tree.loading}
                error={tree.error}
                depth={props.depth + 1}
                search={search}
                onLoadTree={props.onLoadTree}
              />
            );
          } else {
            return tree.children &&
              props.tree.children &&
              props.tree.children.length > 0 ? (
              <OpenableCategoryTreeFolder
                key={i}
                trees={props.trees}
                tree={tree}
                treeId={childId}
                openInitially={false}
                depth={props.depth + 1}
                active={tree.active}
                search={search}
                onLoadTree={props.onLoadTree}
              />
            ) : (
              <CategoryTreeFolder
                key={i}
                trees={props.trees}
                tree={tree}
                treeId={childId}
                openInitially={false}
                depth={props.depth + 1}
                active={tree.active}
                search={search}
                onLoadTree={props.onLoadTree}
              />
            );
          }
        })}
    </Root>
  );
};

const OpenableCategoryTreeFolder = Openable(CategoryTreeFolder);

export default OpenableCategoryTreeFolder;
