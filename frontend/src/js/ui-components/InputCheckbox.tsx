import styled from "@emotion/styled";
import React from "react";
import type { WrappedFieldProps } from "redux-form";

import WithTooltip from "../tooltip/WithTooltip";

const Row = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  cursor: pointer;
`;

const Label = styled("span")`
  margin-left: 10px;
  font-size: ${({ theme }) => theme.font.sm};
  line-height: 1;
`;

const Container = styled("div")`
  flex-shrink: 0;
  position: relative;
  font-size: 22px;
  width: 20px;
  height: 20px;
  border: 2px solid ${({ theme }) => theme.col.blueGrayDark};
  border-radius: ${({ theme }) => theme.borderRadius};
  box-sizing: content-box;
`;

const Checkmark = styled("div")`
  position: absolute;
  top: 0;
  left: 0;
  height: 20px;
  width: 20px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};

  &:after {
    content: "";
    position: absolute;
    left: 6px;
    top: 2px;
    width: 5px;
    height: 10px;
    border: solid white;
    border-width: 0 3px 3px 0;
    transform: rotate(45deg);
  }
`;

interface PropsType extends WrappedFieldProps {
  label: string;
  className?: string;
  tooltip?: string;
  tooltipLazy?: boolean;
}

const InputCheckbox = (props: PropsType) => (
  <Row
    className={props.className}
    onClick={() => props.input.onChange(!props.input.value)}
  >
    <WithTooltip text={props.tooltip} lazy={props.tooltipLazy}>
      <Container>{!!props.input.value && <Checkmark />}</Container>
    </WithTooltip>
    <Label>{props.label}</Label>
  </Row>
);

export default InputCheckbox;
