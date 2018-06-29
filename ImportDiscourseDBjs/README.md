#ImportDiscourseDBjs#

Author: Chris Bogart (cbogart@cs.cmu.edu)
License: Apache 2.0

This project is a Learnsphere/Tigris component (http://learnsphere.org/)
It queries DiscourseDB (http://discoursedb.github.io/) for data that the current Learnsphere user has
permission to access.

#Installation#

1. Install the project in the workflow_components directory of a Tigris install.
2. You will need a file called "cert.p12".  Generate it by getting a "cert.pem" file 
  from the DiscourseDB administrators, and calling this command:
 
     openssl pkcs12 -export -in cid.pem -out cert.p12
 
  It will ask you to make up a password.    
 
3. Put cert.p12 in the src directory; this identifies us as a legitimate proxy for discoursedb users
4. Put discoursedb.query.properties in the src directory, substituting in the password you invented in step (2)
5. Put discourseDbSelector.html in the latest oli/temp/deploy/*.war directory
6. Add this component to the Learnsphere database (analysis_db in mysql).  Sample SQL:

    INSERT INTO `workflow_component` (`component_type`, `component_name`, `tool_dir`, `schema_path`, `interpreter_path`, `tool_path`, `enabled`, `author`, `citation`, `version`, `info`) VALUES ('Import', 'ImportDiscourseDBjs', '/datashop/workflow_components/ImportDiscourseDBjs/', '/datashop/workflow_components/ImportDiscourseDBjs/schemas/ImportDiscourseDBjs_v1_0.xsd', '/usr/bin/java -jar', '/datashop/workflow_components/ImportDiscourseDBjs/dist/ImportDiscourseDBjs-1.0.jar', 1, 'cbogart', 'https://github.com/LearnSphere/WorkflowComponents/tree/master/ImportDiscourseDBjs', '1.0', NULL);






