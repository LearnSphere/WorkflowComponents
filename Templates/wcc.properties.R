# Example properties file for generating an R component.
# Contact us (datashop-help@lists.andrew.cmu.edu) if you have questions
# or find yourself wanting to do something that isn't obvious here.

# The name of the new component. [Required]
component.name=AnalysisIAfmCopy

# The type of the new component. [Required]
# Options include: import, analysis, transform, visualization, tetrad
component.type=analysis

# The language the component is written in. [Required]
# Options include: Java, R, Python, Jar
component.lang=R

# The Java package for the generated Main class. [Default = edu.cmu.learnsphere]
component.pkg=edu.cmu.learnsphere.analysis

# The author of the new component. [Default = system]
component.author=ctipper

# The email of the component author. [Default = datashop-help@lists.andrew.cmu.edu]
component.author.email=ctipper@cs.cmu.edu

# If the component lang is not Java, you must specify the directory
# which contains the component program files, e.g., example.R
component.program.dir=/Users/ctipper/dev/GitRepos/WorkflowComponents/AnalysisIAfm/program

# If the component lang is not Java, you must specify the file to be run.
# This file is expected to be found in the component.program.dir.
component.program.file=iAFM.R

# The version of the new component. [Default = 1.0]
component.version=1.0

# Some descriptive text of the new component.
component.description=AFM is a generalization of the log-linear test model (LLTM). It is a specific instance of logistic regression, with student-success (0 or 1) as the dependent variable and with independent variable terms for the student, the KC, and the KC by opportunity interaction. Without the third term (KC by opportunity), AFM is LLTM. If the KC Model is the Unique-Step model (and there is no third term), the model is Item Response Theory, or the Rasch model.

# The number of inputs for the new component. 
component.num_inputs=1

# The number of outputs for the new component. 
component.num_outputs=3

# The number of options for the new component. 
component.num_options=1

# For each input, specify the type, e.g., file, tab-delimited, csv, student-step
input.0.type=student-step

# For each output, specify the type, e.g., file, tab-delimited, csv, student-step
output.0.type=student-step
output.1.type=model-values
output.2.type=parameters

# For each option, specify the type, name, id and default.
# Common option types include: FileInputHeader, Enum, xs:string, xs:double
# If using an enum, the syntax is "Enum(Foo, Bar, Baz)"
# If the 'default' looks like a regular expression, we'll try to treat it as such.
option.0.type=FileInputHeader
option.0.name=model
option.0.id=Model
option.0.default=\\s*KC\\s*\\((.*)\\)\\s*
