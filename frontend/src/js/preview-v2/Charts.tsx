import styled from "@emotion/styled"
import { PreviewStatistics } from "../api/types";
import Diagram from "./Diagram";

const Root = styled("div")`
`

const SxDiagram = styled(Diagram)`
  width: 300px;
  padding: 5px;
`;

type ChartProps = {
  statistics: PreviewStatistics[];
  className?: string;
}

export default function Charts({ statistics, className }: ChartProps) {

  return (
    <Root className={className}>

      {statistics.map((statistic, i) => {
        return (
          <SxDiagram stat={statistic} />
        )
      })}

    </Root>
  )
}
