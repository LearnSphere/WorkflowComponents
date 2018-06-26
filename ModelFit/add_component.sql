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
	'Model_Fit',
	'/datashop/workflow_components/ModelFit/',
	'/datashop/workflow_components/ModelFit/schemas/ModelFit_v1_0.xsd',
	'/usr/bin/java -jar',
	'/datashop/workflow_components/ModelFit/dist/ModelFit-1.0.jar', 
	1,
	'system',
	'Steven_C_Dang',
	'1.0', 
	'Allow user to fit a model using a dataset'
);
