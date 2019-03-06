// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { DragSource, type ConnectDragSource } from "react-dnd";

import { dndTypes } from "../common/constants";
import { ErrorMessage } from "../error-message";
import { nodeHasActiveFilters } from "../model/node";

import QueryNodeActions from "./QueryNodeActions";

import type { QueryNodeType, DraggedNodeType, DraggedQueryType } from "./types";

const Root = styled("div")`
  position: relative;
  width: 100%;
  margin: 0 auto;
  background-color: white;
  display: inline-block;
  padding: 7px;
  font-size: ${({ theme }) => theme.font.sm};
  cursor: pointer;
  text-align: left;
  border-radius: ${({ theme }) => theme.borderRadius};
  transition: border ${({ theme }) => theme.transitionTime};
  border: 1px solid ${({ theme }) => theme.col.grayMediumLight};
  &:hover {
    border: 1px solid ${({ theme }) => theme.col.blueGrayDark};
  }
`;

const Content = styled("p")`
  margin: 0;
  line-height: 1.2;
  word-break: break-word;
  font-size: ${({ theme }) => theme.font.md};
`;
const PreviousQueryLabel = styled("p")`
  margin: 0 0 3px;
  line-height: 1.2;
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;
  font-weight: 700;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;

type PropsType = {
  node: QueryNodeType,
  onDeleteNode: Function,
  onEditClick: Function,
  onExpandClick: Function,
  connectDragSource: Function,
  andIdx: number,
  orIdx: number,
  connectDragSource: ConnectDragSource
};

const QueryNode = (props: PropsType) => {
  const { node, connectDragSource, onExpandClick } = props;

  return (
    <Root ref={instance => connectDragSource(instance)}>
      <QueryNodeActions
        hasActiveFilters={nodeHasActiveFilters(node)}
        onEditClick={props.onEditClick}
        onDeleteNode={props.onDeleteNode}
        isExpandable={node.isPreviousQuery}
        onExpandClick={() => {
          if (!node.query) return;

          onExpandClick(node.query.groups, node.id);
        }}
        previousQueryLoading={node.loading}
        error={node.error}
      />
      {node.isPreviousQuery && (
        <PreviousQueryLabel>
          {T.translate("queryEditor.previousQuery")}
        </PreviousQueryLabel>
      )}
      {node.error ? (
        <ErrorMessage message={node.error} />
      ) : (
        <Content>
          <span>{node.label || node.id}</span>
          {node.description && <span> - {node.description}</span>}
        </Content>
      )}
    </Root>
  );
};

/**
 * Implements the drag source contract.
 */
const nodeSource = {
  beginDrag(props: PropsType): DraggedNodeType | DraggedQueryType {
    // Return the data describing the dragged item
    // NOT using `...node` since that would also spread `children` in.
    // This item may stem from either:
    // 1) A concept (dragged from CategoryTreeNode)
    // 2) A previous query (dragged from PreviousQueries)
    const { node, andIdx, orIdx } = props;

    const draggedNode = {
      moved: true,
      andIdx,
      orIdx,

      label: node.label,
      excludeTimestamps: node.excludeTimestamps,

      loading: node.loading,
      error: node.error
    };

    if (node.isPreviousQuery)
      return {
        ...draggedNode,
        id: node.id,
        query: node.query,
        isPreviousQuery: true
      };
    else
      return {
        ...draggedNode,
        ids: node.ids,
        tree: node.tree,
        tables: node.tables
      };
  }
};

/**
 * Specifies the dnd-related props to inject into the component.
 */
const collect = (connect, monitor) => ({
  connectDragSource: connect.dragSource(),
  isDragging: monitor.isDragging()
});

const DraggableQueryNode = DragSource(dndTypes.QUERY_NODE, nodeSource, collect)(
  QueryNode
);

export default DraggableQueryNode;
