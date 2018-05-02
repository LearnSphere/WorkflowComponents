INSERT INTO `workflow_component` (
    `component_type`, 
    `component_name`, 
    `tool_dir`, 
    `schema_path`, 
    `interpreter_path`, 
    `tool_path`, 
    `enabled`, 
    `author`, 
    `citation`, 
    `version`, 
    `info`
) 
VALUES (
    'Analysis', 
    'D3m_Pipeline_Search', 
    '/datashop/workflow_components/D3mPipelineSearch/', 
    '/datashop/workflow_components/D3mPipelineSearch/schemas/D3mDatasetSelector_v1_0.xsd', 
    '/usr/bin/java -jar', 
    '/datashop/workflow_components/D3mPipelineSearch/dist/D3mDatasetSelector-1.0.jar', 
    1, 
    'system', 
    'Steven_C_Dang', 
    '1.0', 
    'Component to search for a model and generate a prediction given a d3m dataset'
);
