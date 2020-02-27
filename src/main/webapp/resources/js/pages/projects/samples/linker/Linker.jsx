import React from "react";
import { render } from "react-dom";
import { Button, Modal, Typography } from "antd";
import { CodeOutlined } from "@ant-design/icons";
import { grey2, grey4, grey9 } from "../../../../styles/colors";
import { getNGSLinkerCode } from "../../../../apis/linker/linker";
import { SPACE_SM } from "../../../../styles/spacing";

const { Paragraph, Text } = Typography;

/**
 * React component to display the ngs-linker command to a user based
 * on the currently selected samples.
 * @returns {*}
 * @constructor
 */
function Linker() {
  function handleSampleIds(e) {
    // Post data to get linker command.
    const { detail } = e;
    getNGSLinkerCode(detail).then(({ data }) => {
      Modal.success({
        className: "t-linker-modal",
        width: 500,
        title: i18n("Linker.title"),
        content: (
          <>
            <Paragraph>{i18n("Linker.details")}</Paragraph>
            <Text type="secondary">
              <span
                dangerouslySetInnerHTML={{ __html: i18n("Linker.note") }}
              />
            </Text>
            <Paragraph
              style={{
                fontFamily: "monospace",
                fontSize: 14,
                marginTop: SPACE_SM,
                padding: 2,
                border: `1px solid ${grey4}`,
                backgroundColor: grey2,
                borderRadius: 2
              }}
              className="t-cmd-text"
              ellipsis={{ rows: 1 }}
              copyable={{ text: data }}
            >
              {data}
            </Paragraph>
          </>
        )
      });
    });
  }

  /*
  These Listeners and Dispatchers are a way to get around the separation between react
  components and the legacy JS code already on the page.
  */
  document.addEventListener("sample-ids-return", handleSampleIds, false);

  function showModal() {
    document.dispatchEvent(new Event("sample-ids"));
  }

  return (
    <Button
      type="link"
      style={{
        margin: `0 inherit`,
        padding: 0,
        color: grey9
      }}
      className="t-linker-btn"
      onClick={showModal}
    >
      {/* constant marginRight here to match old styles */}
      <CodeOutlined style={{ marginRight: 2 }} />
      {i18n("project.samples.export.linker")}
    </Button>
  );
}

render(<Linker />, document.querySelector("#linker"));
