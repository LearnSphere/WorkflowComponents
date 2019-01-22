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
	'Dataset_Augmenter',
	'/datashop/workflow_components/DatasetAugmenter/',
	'/datashop/workflow_components/DatasetAugmenter/schemas/DatasetAugmenter_v1_0.xsd',
	'/usr/bin/java -jar',
	'/datashop/workflow_components/DatasetAugmenter/dist/DatasetAugmenter-1.0.jar', 
	1,
	'system',
	'Steven_C_Dang',
	'1.0', 
	'Select additional datasets to augment a given dataset to improve performance on a given problem'
);
