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
	'Problem_Target_Selector',
	'/datashop/workflow_components/ProblemTargetSelector/',
	'/datashop/workflow_components/ProblemTargetSelector/schemas/ProblemTargetSelector_v1_0.xsd',
	'/usr/bin/java -jar',
	'/datashop/workflow_components/ProblemTargetSelector/dist/ProblemTargetSelector-1.0.jar', 
	1,
	'system',
	'Steven_C_Dang',
	'1.0', 
	'Allow user to select problem target from columns of a dataset from a dataset repository'
);
