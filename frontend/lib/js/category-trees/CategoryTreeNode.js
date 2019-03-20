// @flow

import React from "react";
import styled from "@emotion/styled";

import {
  type TreeNodeIdType,
  type InfoType,
  type DateRangeType,
  type NodeType
} from "../common/types/backend";
import { type DraggedNodeType } from "../standard-query-editor/types";
import { type SearchType } from "./reducer";

import { getConceptById } from "./globalTreeStoreHelper";
import Openable from "./Openable";
import CategoryTreeNodeTextContainer from "./CategoryTreeNodeTextContainer";
import { isInSearchResult } from "./selectors";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
`;

const StyledCategoryTreeNodeTextContainer = styled(
  CategoryTreeNodeTextContainer
)`
  display: inline-block;
`;

// Concept data that is necessary to display tree nodes. Includes additional infos
// for the tooltip as well as the id of the corresponding tree
type TreeNodeData = {
  label: string,
  description: string,
  active: boolean,
  matchingEntries: number,
  dateRange: DateRangeType,
  additionalInfos: Array<InfoType>,
  children: Array<TreeNodeIdType>,

  tree: TreeNodeIdType
};

type PropsType = {
  id: TreeNodeIdType,
  data: TreeNodeData,
  depth: number,
  open: boolean,
  search?: SearchType,
  onToggleOpen: () => void
};

const selectTreeNodeData = (concept: NodeType, tree: TreeNodeIdType) => ({
  label: concept.label,
  description: concept.description,
  active: concept.active,
  matchingEntries: concept.matchingEntries,
  dateRange: concept.dateRange,
  additionalInfos: concept.additionalInfos,
  children: concept.children,

  tree
});

class CategoryTreeNode extends React.Component<PropsType> {
  _onToggleOpen() {
    if (!this.props.data.children) return;

    this.props.onToggleOpen();
  }

  render() {
    const { id, data, depth, open, search } = this.props;
    const searching = search && search.searching;

    const render = searching
      ? isInSearchResult(id, data.children, search)
      : true;

    return (
      render && (
        <Root>
          <StyledCategoryTreeNodeTextContainer
            node={{
              id,
              label: data.label,
              description: data.description,
              matchingEntries: data.matchingEntries,
              dateRange: data.dateRange,
              additionalInfos: data.additionalInfos,
              hasChildren: !!data.children && data.children.length > 0
            }}
            createQueryElement={(): DraggedNodeType => {
              const { tables, selects } = getConceptById(data.tree);

              return {
                ids: [id],
                label: data.label,
                tables,
                selects,
                tree: data.tree
              };
            }}
            open={open}
            depth={depth}
            active={data.active}
            onTextClick={this._onToggleOpen.bind(this)}
            search={search}
          />
          {!!data.children && (open || search.allOpen) && (
            <div>
              {data.children.map((childId, i) => {
                const child = getConceptById(childId);

                return child ? (
                  <OpenableCategoryTreeNode
                    key={i}
                    id={childId}
                    data={selectTreeNodeData(child, data.tree)}
                    depth={this.props.depth + 1}
                    search={this.props.search}
                  />
                ) : null;
              })}
            </div>
          )}
        </Root>
      )
    );
  }
}

const OpenableCategoryTreeNode = Openable(CategoryTreeNode);

export default OpenableCategoryTreeNode;
