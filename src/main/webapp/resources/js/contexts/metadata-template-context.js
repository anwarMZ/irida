import React, { createContext, useContext, useEffect, useState } from "react";
import {
  getMetadataTemplateDetails,
  updateTemplateAttribute,
} from "../apis/metadata/metadata-templates";
import { notification } from "antd";

const MetadataTemplateContext = createContext();

function MetadataTemplateProvider({ children, id }) {
  const [template, setTemplate] = useState();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMetadataTemplateDetails({ templateId: id }).then((data) => {
      setTemplate(data);
      setLoading(false);
    });
  }, [setLoading, setTemplate, id]);

  const updateField = (field, value) => {
    updateTemplateAttribute({
      templateId: id,
      field,
      value,
    }).then((message) => {
      notification.success({ message });

      const updated = { ...template };
      updated[field] = value;
      setTemplate(updated);
    });
  };

  return (
    <MetadataTemplateContext.Provider
      value={{ loading, template, updateField }}
    >
      {children}
    </MetadataTemplateContext.Provider>
  );
}

function useMetadataTemplate() {
  const context = useContext(MetadataTemplateContext);
  if (context === undefined) {
    throw new Error(
      "useMetadataTemplate must be used within a MetadataTemplateProvider"
    );
  }
  return context;
}

export { MetadataTemplateProvider, useMetadataTemplate };