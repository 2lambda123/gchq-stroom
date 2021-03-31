/*
 * Copyright 2020 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from "react";
import { useEffect, useState } from "react";
import BackgroundLogo from "../Layout/BackgroundLogo";
import { SignIn } from "./SignIn";
import { AuthStateProps } from "./ConfirmCurrentPassword";
import { ChangePasswordFormValues, ChangePasswordPage } from "./ChangePassword";
import Background from "../Layout/Background";
import useRouter from "../../lib/useRouter";
import * as queryString from "query-string";
import CustomLoader from "../CustomLoader";
import { FormikHelpers } from "formik/dist/types";
import { usePrompt } from "../Prompt/PromptDisplayBoundary";
import { usePasswordPolicy } from "./usePasswordPolicy";
import { useStroomApi } from "lib/useStroomApi";
import {
  AuthenticationState,
  ChangePasswordRequest,
  ChangePasswordResponse,
} from "api/stroom";
import { AuthState } from "./types";
import { PromptType } from "../Prompt/Prompt";

export interface FormValues {
  userId: string;
  password: string;
}

export interface PageProps {
  allowPasswordResets?: boolean;
}

const Page: React.FunctionComponent = () => {
  let redirectUri: string;

  const { router } = useRouter();
  if (!!router && !!router.location) {
    const query = queryString.parse(router.location.search);
    if (!!query.redirect_uri) {
      redirectUri = query.redirect_uri + "";
    }
  }

  // Client state
  const [authState, setAuthState] = useState<AuthState>();

  const { showPrompt } = usePrompt();
  const { exec } = useStroomApi();

  useEffect(() => {
    if (!authState) {
      exec(
        (api) => api.authentication.getAuthenticationState(),
        (response: AuthenticationState) => {
          setAuthState({
            ...authState,
            userId: response.userId,
            allowPasswordResets: response.allowPasswordResets,
          });
        },
      );
    }
  }, [exec, authState, setAuthState]);

  const passwordPolicyConfig = usePasswordPolicy();

  const props: AuthStateProps = {
    authState,
    setAuthState,
  };

  if (!redirectUri) {
    throw new Error(
      "This page must include a redirect_uri param in the URL and can only be visited as part of an auth flow",
    );
  } else if (!authState) {
    return (
      <CustomLoader
        title="Stroom"
        message="Loading Application. Please wait..."
      />
    );
  } else if (!authState.userId) {
    return <SignIn {...props} />;
  } else if (authState.showInitialChangePassword) {
    const onClose = (success: boolean) => {
      if (success) {
        setAuthState({
          ...authState,
          showInitialChangePassword: false,
        });
      } else {
        setAuthState(undefined);
      }
    };

    const onSubmit = (
      values: ChangePasswordFormValues,
      actions: FormikHelpers<ChangePasswordFormValues>,
    ) => {
      const request: ChangePasswordRequest = {
        userId: authState.userId,
        currentPassword: authState.currentPassword,
        newPassword: values.password,
        confirmNewPassword: values.confirmPassword,
      };

      exec(
        (api) => api.authentication.changePassword(request),
        (response: ChangePasswordResponse) => {
          if (!response) {
            actions.setSubmitting(false);
          } else if (response.changeSucceeded) {
            showPrompt({
              promptProps: {
                type: PromptType.INFO,
                title: "Success",
                message: "Your password has been changed",
              },
              onCloseDialog: () => onClose(true),
            });
          } else {
            actions.setSubmitting(false);
            showPrompt({
              promptProps: {
                type: PromptType.ERROR,
                title: "Error",
                message: response.message,
              },
              onCloseDialog: () => {
                // If the user is asked to sign in again then unset the auth state.
                if (response.forceSignIn) {
                  onClose(false);
                }
              },
            });
          }
        },
      );
    };

    return (
      <ChangePasswordPage
        initialValues={{
          userId: authState.userId,
          password: "",
          confirmPassword: "",
        }}
        passwordPolicyConfig={passwordPolicyConfig}
        onSubmit={onSubmit}
        onClose={onClose}
        {...props}
      />
    );
  } else {
    window.location.href = redirectUri;

    return <CustomLoader title="Stroom" message="Signing in. Please wait..." />;
  }
};

export const SignInManager: React.FunctionComponent<PageProps> = () => (
  <Background>
    <BackgroundLogo>
      <Page />
    </BackgroundLogo>
  </Background>
);

export default SignInManager;
