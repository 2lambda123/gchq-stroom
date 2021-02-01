import * as React from "react";

import { UiConfig } from "./types";

export const defaultValue: UiConfig = {
  theme: {
    backgroundColor: "red",
  },
  uiPreferences: {
    dateFormat: "YYYY-MM-DDTHH:mm:ss.SSS",
  },
};

export default React.createContext<UiConfig>(defaultValue);
