/*
 * Copyright 2018 Crown Copyright
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
import { compose } from "recompose";
import { connect } from "react-redux";

import StroomDecorator from "../../lib/storybook/StroomDecorator";
import { storiesOf } from "@storybook/react";
import { Field, reduxForm, FormState } from "redux-form";

import DocTypeFilters from "./DocTypeFilters";
import DocRefTypePicker from "./DocRefTypePicker";

import "../../styles/main.css";
import { GlobalStoreState } from "../../startup/reducers";

interface Props {}
interface ConnectState {
  thisForm: FormState;
  initialValues: object;
}
interface ConnectDispatch {}
interface EnhancedProps extends Props, ConnectState, ConnectDispatch {}

const enhance = compose<EnhancedProps, Props>(
  connect<ConnectState, ConnectDispatch, Props, GlobalStoreState>(
    ({ form }) => ({
      thisForm: form.docTypeFilterTest,
      initialValues: {
        docTypes: []
      }
    }),
    {}
  ),
  reduxForm({
    form: "docTypeFilterTest"
  })
);

const TestForm = enhance(({ thisForm }: EnhancedProps) => (
  <form>
    <div>
      <label>Chosen Doc Type</label>
      <Field
        name="docType"
        component={({ input: { onChange, value } }) => (
          <DocRefTypePicker
            pickerId="test1"
            onChange={onChange}
            value={value}
          />
        )}
      />
    </div>
    <div>
      <label>Chosen Doc Types</label>
      <Field
        name="docTypes"
        component={({ input: { onChange, value } }) => (
          <DocTypeFilters onChange={onChange} value={value} />
        )}
      />
    </div>
    {thisForm &&
      thisForm.values && (
        <div>
          <div>Doc Type: {thisForm.values.docType}</div>
          <div>Doc Types: {thisForm.values.docTypes.join(",")}</div>
        </div>
      )}
  </form>
));

storiesOf("Doc Type Filters", module)
  .addDecorator(StroomDecorator)
  .add("Doc Type Filter", () => <TestForm />);
