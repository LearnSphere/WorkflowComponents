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
	'Model_Predict',
	'/datashop/workflow_components/ModelPredict/',
	'/datashop/workflow_components/ModelPredict/schemas/ModelPredict_v1_0.xsd',
	'/usr/bin/java -jar',
	'/datashop/workflow_components/ModelPredict/dist/ModelPredict-1.0.jar', 
	1,
	'system',
	'Steven_C_Dang',
	'1.0', 
	'Allow user to generate predictions on a dataset given a fitted model'
);
