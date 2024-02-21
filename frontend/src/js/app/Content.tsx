import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { useSelector } from "react-redux";
import SplitPane from "react-split-pane";

import { History } from "../entity-history/History";
import PreviewV1 from "../preview/Preview";
import ActivateTooltip from "../tooltip/ActivateTooltip";
import Tooltip from "../tooltip/Tooltip";

import { useMemo } from "react";
import { Panel, PanelGroup } from "react-resizable-panels";
import { ResizeHandle } from "../common/ResizeHandle";
import Preview from "../preview-v2/Preview";
import DndProvider from "./DndProvider";
import LeftPane from "./LeftPane";
import RightPane from "./RightPane";
import type { StateT } from "./reducers";

// ADDING TO react-split-pane STYLES
// Because otherwise, vertical panes don't expand properly in Safari
const reactSplitPaneSafariFix = css`
  .vertical {
    height: 100%;
  }
`;

const Root = styled("div")`
  width: 100%;
  height: 100%;
  position: relative;

  ${reactSplitPaneSafariFix};
`;

const Content = () => {
  const displayTooltip = useSelector<StateT, boolean>(
    (state) => state.tooltip.displayTooltip,
  );

  const isPreviewOpen = useSelector<StateT, boolean>(
    (state) => state.preview.isOpen,
  );

  const isPreviewV1Open = useSelector<StateT, boolean>(
    (state) => state.previewV1.isOpen,
  );

  const isHistoryOpen = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isOpen,
  );

  const disableDragHandles = useSelector<StateT, boolean>(
    (state) => state.panes.disableDragHandles,
  );

  const collapsedStyles = useMemo(() => {
    if (displayTooltip) return {};

    return {
      width: "30px",
      minWidth: "30px",
      maxWidth: "30px",
      overflow: "hidden",
    };
  }, [displayTooltip]);

  return (
    <DndProvider>
      <Root>
        {isHistoryOpen && <History />}
        {isPreviewOpen && <Preview />}
        {isPreviewV1Open && <PreviewV1 />}
        <PanelGroup direction="horizontal" units="pixels">
          <Panel
            style={collapsedStyles}
            minSize={200}
            maxSize={600}
            defaultSize={displayTooltip ? 200 : 30}
          >
            {displayTooltip ? <Tooltip /> : <ActivateTooltip />}
          </Panel>
          {!disableDragHandles && <ResizeHandle />}
          <Panel minSize={350} defaultSize={600}>
            <LeftPane />
          </Panel>
          {!disableDragHandles && <ResizeHandle />}
          <Panel minSize={250}>
            <RightPane />
          </SplitPane>
        </SplitPane>
      </Root>
    </DndProvider>
  );
};

// export default withRouter(DragDropContext(HTML5Backend)(ConnectedContent));
export default Content;
