import * as React from "react";
import { AccountResultPage } from "api/stroom";

interface AccountManagerStateApi {
  users: AccountResultPage;
  setUsers: (users: AccountResultPage) => void;
  selectedUser: string;
  setSelectedUser: (userId: string) => void;
}

interface AccountManagerState {
  users: AccountResultPage;
  selectedUser: string;
}

interface SetUsersAction {
  type: "set_user";
  users: AccountResultPage;
}

interface ChangeSelectedUserAction {
  type: "change_selected_user";
  userId: string;
}

const reducer = (
  state: AccountManagerState,
  action: SetUsersAction | ChangeSelectedUserAction,
) => {
  switch (action.type) {
    case "set_user":
      return { ...state, users: action.users };
    case "change_selected_user":
      return { ...state, selectedUser: action.userId };
    default:
      return state;
  }
};

const useAccountManagerState = (): AccountManagerStateApi => {
  const [userState, dispatch] = React.useReducer(reducer, {
    users: {
      values: [],
      pageResponse: {
        offset: 0,
        length: 0,
        total: undefined,
        exact: false,
      },
    },
    selectedUser: "",
  });
  const setUsers = React.useCallback(
    (users: AccountResultPage) => dispatch({ type: "set_user", users }),
    [dispatch],
  );
  const setSelectedUser = React.useCallback(
    (userId: string) => dispatch({ type: "change_selected_user", userId }),
    [dispatch],
  );

  return {
    users: userState.users,
    selectedUser: userState.selectedUser,
    setUsers,
    setSelectedUser,
  };
};

export { useAccountManagerState };
