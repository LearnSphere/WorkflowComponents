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
	'Problem_Metric_Selector',
	'/datashop/workflow_components/ProblemMetricSelector/',
	'/datashop/workflow_components/ProblemMetricSelector/schemas/ProblemMetricSelector_v1_0.xsd',
	'/usr/bin/java -jar',
	'/datashop/workflow_components/ProblemMetricSelector/dist/ProblemMetricSelector-1.0.jar', 
	1,
	'system',
	'Steven_C_Dang',
	'1.0', 
	'Allow user to select a metric to use for the problem'
);
