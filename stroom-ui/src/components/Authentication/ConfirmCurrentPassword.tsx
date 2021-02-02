import * as React from "react";

import { Formik, FormikProps } from "formik";
import { PasswordFormField } from "components/FormField";
import { useAuthenticationResource } from "./api";
import { usePrompt } from "../Prompt/PromptDisplayBoundary";
import * as Yup from "yup";
import { AuthState, ConfirmPasswordRequest } from "./api/types";
import { Form, Modal } from "react-bootstrap";
import { Dialog } from "components/Dialog/Dialog";
import { OkCancelButtons, OkCancelProps } from "../Dialog/OkCancelButtons";
import { LockFill } from "react-bootstrap-icons";

export interface FormValues {
  userId: string;
  password: string;
}

export interface AuthStateProps {
  authState: AuthState;
  setAuthState: (state: AuthState) => any;
}

export const ConfirmCurrentPasswordForm: React.FunctionComponent<{
  formikProps: FormikProps<FormValues>;
  okCancelProps: OkCancelProps;
}> = ({ formikProps, okCancelProps }) => {
  const { values, handleSubmit, isSubmitting } = formikProps;
  const { onCancel, cancelClicked } = okCancelProps;
  return (
    <Form noValidate={true} onSubmit={handleSubmit}>
      <Modal.Header closeButton={false}>
        <Modal.Title id="contained-modal-title-vcenter">
          <LockFill className="mr-3" />
          Enter Your Current Password
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <input
          type="text"
          id="userId"
          value={values.userId}
          autoComplete="username"
          hidden={true}
        />
        <Form.Row>
          <PasswordFormField
            controlId="password"
            label="Current Password"
            placeholder="Enter Your Current Password"
            autoComplete="current-password"
            autoFocus={true}
            formikProps={formikProps}
          />
        </Form.Row>
      </Modal.Body>
      <Modal.Footer>
        <OkCancelButtons
          onOk={() => undefined}
          onCancel={onCancel}
          okClicked={isSubmitting}
          cancelClicked={cancelClicked}
        />
      </Modal.Footer>
    </Form>
  );
};

const ConfirmCurrentPasswordFormik: React.FunctionComponent<{
  userId: string;
  onClose: (userId: string, password: string) => void;
}> = ({ userId, onClose, children }) => {
  const { confirmPassword } = useAuthenticationResource();
  const { showError } = usePrompt();

  const passwordSchema = Yup.string()
    .label("Password")
    .required("Password is required");

  const validationSchema = Yup.object().shape({
    password: passwordSchema,
  });

  const onCancel = () => {
    onClose(null, null);
  };

  return (
    <Formik
      initialValues={{ userId, password: "" }}
      validationSchema={validationSchema}
      onSubmit={(values, actions) => {
        const request: ConfirmPasswordRequest = {
          password: values.password,
        };

        confirmPassword(request).then((response) => {
          if (!response) {
            actions.setSubmitting(false);
          } else if (response.valid) {
            onClose(values.userId, values.password);
          } else {
            actions.setSubmitting(false);
            showError({
              message: response.message,
            });
          }
        });
      }}
    >
      {(formikProps) => (
        <ConfirmCurrentPasswordForm
          formikProps={formikProps}
          okCancelProps={{ onCancel }}
        >
          {children}
        </ConfirmCurrentPasswordForm>
      )}
    </Formik>
  );
};

export const ConfirmCurrentPassword: React.FunctionComponent<{
  userId: string;
  onClose: (userId: string, password: string) => void;
}> = (props) => {
  return (
    <Dialog
      initWidth={400}
      initHeight={224}
      minWidth={400}
      minHeight={224}
      disableResize={true}
    >
      <ConfirmCurrentPasswordFormik {...props} />
    </Dialog>
  );
};
