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
	'Visualize_Confusion_Matrix',
	'/datashop/workflow_components/VisualizeConfusionMatrix/',
	'/datashop/workflow_components/VisualizeConfusionMatrix/schemas/VisualizeConfusionMatrix_v1_0.xsd',
	'/usr/bin/java -jar',
	'/datashop/workflow_components/VisualizeConfusionMatrix/dist/VisualizeConfusionMatrix-1.0.jar', 
	1,
	'system',
	'Steven_C_Dang',
	'1.0', 
	'Create Confusion Matrix'
);
