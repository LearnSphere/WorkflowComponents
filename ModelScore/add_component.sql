REPLACE INTO `workflow_component` (
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
	'Model_Score',
	'/datashop/workflow_components/ModelScore/',
	'/datashop/workflow_components/ModelScore/schemas/ModelScore_v1_0.xsd',
	'/usr/bin/java -jar',
	'/datashop/workflow_components/ModelScore/dist/ModelScore-1.0.jar', 
	1,
	'system',
	'Steven_C_Dang',
	'1.0', 
	'Allow user to evaluate the performance of a set of models'
);
