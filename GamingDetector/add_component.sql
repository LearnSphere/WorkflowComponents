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
	'comp_type',
	'comp_user_name',
	'/rdata/Sandbox/learnsphere/d3m/WorkflowComponents//',
	'/rdata/Sandbox/learnsphere/d3m/WorkflowComponents//schemas/_v1_0.xsd',
	'/usr/bin/java -jar',
	'/rdata/Sandbox/learnsphere/d3m/WorkflowComponents//dist/-1.0.jar', 
	1,
	'system',
	'Steven_C_Dang',
	'1.0', 
	'comp_desc'
);
