# Example properties file for generating an R component.
# Contact us (datashop-help@lists.andrew.cmu.edu) if you have questions
# or find yourself wanting to do something that isn't obvious here.

# The name of the new component. [Required]
component.name=CovarianceClustering

# The type of the new component. [Required]
# Options include: import, analysis, transform, visualization, tetrad
component.type=analysis

# The language the component is written in. [Required]
# Options include: Java, R, Python, Jar
component.lang=R

# The Java package for the generated Main class. [Default = edu.cmu.learnsphere]
component.pkg=edu.cmu.pslc.learnsphere.analysis

# The author of the new component. [Default = system]
component.author=Philip Pavlik

# The email of the component author. [Default = datashop-help@lists.andrew.cmu.edu]
component.author.email=ppavlik@memphis.edu

# If the component lang is not Java, you must specify the directory
# which contains the component program files, e.g., example.R
component.program.dir=C:\Users\Liang Zhang\Documents\NetBeansProjects\WorkflowComponents

# If the component lang is not Java, you must specify the file to be run.
# This file is expected to be found in the component.program.dir.
component.program.file=CovarianceClustering.R

# The version of the new component. [Default = 1.0]
component.version=1.0

# Some descriptive text of the new component.
component.description=This component is used to apply the simulatation to help students model anaylsis.

# The number of inputs for the new component. 
component.num_inputs=0

# The number of outputs for the new component. 
component.num_outputs=1

# The number of options for the new component. 
component.num_options=1

# For each input, specify the type, e.g., file, tab-delimited, csv, student-step,transactions
input.0.type=tab-delimited

# For each output, specify the name and type, e.g., file, tab-delimited, csv, student-step
output.0.type=image
output.0.name=curve.png

# For each option, specify the type, name, id and default.
# Common option types include: FileInputHeader, Enum, xs:string, xs:double
# If using an enum, the syntax is "Enum(Foo, Bar, Baz)"
# If the 'default' looks like a regular expression, we'll try to treat it as such.
option.0.type=String
option.0.name=model
option.0.id=Model


