import styled from "@emotion/styled";
import type { ComponentProps } from "react";

import SmallTabNavigation from "../../small-tab-navigation/SmallTabNavigation";

const SxSmallTabNavigation = styled(SmallTabNavigation)`
  padding-top: 8px;
  padding-left: 10px;
`;

const FormTavNavigation = (
  props: ComponentProps<typeof SmallTabNavigation>,
) => {
  return <SxSmallTabNavigation size="L" {...props} />;
};

export default FormTavNavigation;
