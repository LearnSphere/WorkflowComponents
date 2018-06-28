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
	'Visualization',
	'Compare_Model_Scores',
	'/datashop/workflow_components/CompareModelScores/',
	'/datashop/workflow_components/CompareModelScores/schemas/CompareModelScores_v1_0.xsd',
	'/usr/bin/java -jar',
	'/datashop/workflow_components/CompareModelScores/dist/CompareModelScores-1.0.jar', 
	1,
	'system',
	'Steven_C_Dang',
	'1.0', 
	'Generates an interactive html barplot comparing given model scores'
);
