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
	'Rank_Models',
	'/datashop/workflow_components/ModelRank/',
	'/datashop/workflow_components/ModelRank/schemas/ModelRank_v1_0.xsd',
	'/usr/bin/java -jar',
	'/datashop/workflow_components/ModelRank/dist/ModelRank-1.0.jar', 
	1,
	'system',
	'Steven_C_Dang',
	'1.0', 
	'Allow user to rank a set of models using performance along a metric'
);
