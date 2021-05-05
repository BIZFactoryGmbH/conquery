import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC } from "react";
import { useSelector } from "react-redux";

import FormConfigSaver from "./FormConfigSaver";
import type { Form as FormType } from "./config-types";
import Form from "./form/Form";
import { selectFormConfig } from "./stateSelectors";

const Root = styled("div")`
  flex-grow: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0 20px 20px 10px;
`;

const FormsContainer: FC = () => {
  const formConfig = useSelector<StateT, FormType | null>((state) =>
    selectFormConfig(state),
  );

  return (
    <Root>
      {!!formConfig && (
        <>
          <FormConfigSaver />
          <Form config={formConfig} />
        </>
      )}
    </Root>
  );
};

export default FormsContainer;
