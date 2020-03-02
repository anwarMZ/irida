import React from "react";
import { DeleteOutlined } from "@ant-design/icons";
import { Button, Popconfirm } from "antd";
import { red6 } from "../../styles/colors";
import { IconQuestionCircle } from "../../components/icons/Icons";

/**
 * Component to render a Delete announcement button.
 * @param id
 * @param deleteAnnouncement
 * @returns {*}
 * @constructor
 */
export function DeleteAnnouncement({ id, deleteAnnouncement }) {
  return (
    <Popconfirm
      placement={"topRight"}
      title={i18n("DeleteAnnouncement.title")}
      onConfirm={() => deleteAnnouncement({ id })}
      okText={i18n("DeleteAnnouncement.ok")}
      icon={<IconQuestionCircle style={{ color: red6 }} />}
    >
      <Button shape={"circle"}>
        <DeleteOutlined />
      </Button>
    </Popconfirm>
  );
}
