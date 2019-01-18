#!/bin/bash
# Ensure this is pointing to where WorkflowComponents is cloned
wcc=$WCC 

out_file="add_component.sql"
comp_dir_name="ModelRank"

# Remove add_component.sql if it exists
if [ -f $out_file ]; then
    rm $out_file
fi

echo "REPLACE INTO \`workflow_component\` (" >> $out_file
echo -e "\t\`component_type\`," >> $out_file
echo -e "\t\`component_name\`," >> $out_file
echo -e "\t\`tool_dir\`," >> $out_file
echo -e "\t\`schema_path\`," >> $out_file
echo -e "\t\`interpreter_path\`," >> $out_file
echo -e "\t\`tool_path\`," >> $out_file
echo -e "\t\`enabled\`," >> $out_file
echo -e "\t\`author\`," >> $out_file
echo -e "\t\`citation\`," >> $out_file
echo -e "\t\`version\`," >> $out_file
echo -e "\t\`info\`" >> $out_file
echo -e ")" >> $out_file
echo "VALUES (" >> $out_file
echo -e "\t'Analysis'," >> $out_file
echo -e "\t'Rank_Models'," >> $out_file
echo -e "\t'$wcc/$comp_dir_name/'," >> $out_file
echo -e "\t'$wcc/$comp_dir_name/schemas/$comp_dir_name""_v1_0.xsd'," >> $out_file
echo -e "\t'/usr/bin/java -jar'," >> $out_file
echo -e "\t'$wcc/$comp_dir_name/dist/$comp_dir_name-1.0.jar', " >> $out_file
echo -e "\t1," >> $out_file
echo -e "\t'system'," >> $out_file
echo -e "\t'Steven_C_Dang'," >> $out_file 
echo -e "\t'1.0', " >> $out_file
echo -e "\t'Allow user to rank a set of models using performance along a metric'" >> $out_file
echo -e ");" >> $out_file
