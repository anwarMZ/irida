/**
 * @file React component for use with ag-grid to render the sample name as
 * a hyper link.
 */
import React from "react";
import { FIELDS } from "../../../constants";

const { BASE_URL } = window.TL;

export class SampleNameRenderer extends React.Component {
  constructor(props) {
    super(props);
    this.href = `${BASE_URL}projects/${Number(
      props.data[FIELDS.projectId]
    )}/samples/${Number(props.data[FIELDS.sampleId])}`;
    this.name = props.data[FIELDS.sampleName];
  }

  render() {
    return (
      <a target="_blank" className="t-sample-name" href={this.href}>
        {this.name}
      </a>
    );
  }
}
