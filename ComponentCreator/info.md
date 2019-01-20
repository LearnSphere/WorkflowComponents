




# Component Creator

This is a component that will create the shell of your program to be put into LearnSphere's Tigris Workflow tool.  In other words, this will set up a directory structure with some files that make it easy to test your code and insert it into LearnSphere to share with other users.

In the ComponentCreator's options panel, you can specify inputs, outputs, options, and various other pieces of information about you new component.  You can also use the input to insert your own scripts.  The output of the ComponentCreator is a zip file with all files necessary to run your new component.

[The workflow components document in GitHub](https://github.com/LearnSphere/WorkflowComponents/blob/dev/Workflow%20Components.docx) is where you will find the most comprehensive information on creating new components, however, this document will help you understand how to use the ComponentCreator.

## Table of Contents
- [ComponentCreator Input Zip](#componentcreator-input)
- [ComponentCreator Options Panel](#componentcreator-options-panel)
	- [New Component Background Information](#new-component-background-information)
	- [New Component **Inputs**](#inputs)
	- [New Component **Outputs**](#outputs)
	- [New Component **Options**](#options)
- [Next steps](#next-steps)

# ComponentCreator Input
The input to the component creator is a zip file containing the scripts for your new component.  If you plan on writing your component in Java, then this input is not necessary.  The zip file may contain sub directories and multiple files.  Be sure to include all of the files necessary to run your script.

For example, if you were making the [Row Remover](https://github.com/LearnSphere/WorkflowComponents/tree/dev/RowRemover/program) component, then you would input a zip file containing the rowRemover.R script.

All of your scripts should be contained within a single directory.  Then zip this directory to use as input.  For example, your code might be in a structure such as this:
- **myCodeDir/**
	- myMainScript<span></span>.py
	- subDirWithHelperScripts/
		- scriptHelper<span></span>.py

Then you compress myCodeDir into a zip file so it looks like:
- myCodeZipFile.zip
	- **myCodeDir/**
		- myMainScript<span></span>.py
		- subDirWithHelperScripts/
			- scriptHelper<span></span>.py

Please notice that myCodeDir/ is the only directory or file at its level in the zip file.
# ComponentCreator Options panel

The options panel of the ComponentCreator contains multiple inputs to help create your new component.  
## New Component Background Information

![alt text](https://github.com/LearnSphere/WorkflowComponents/blob/dev/ComponentCreator/images/labelsForBackground.png "How the ComponentCreator options affect your new component")

- ### Component Name
  -- Use this input to name your component.  This name must contain **No Spaces** and consist of **only letters and numbers** as it will be used to name a few java programs and directories.  A common convention to use is upper camel case.  For example, "My new component" would become "MyNewComponent".
- ### Component Type
  -- Use this drop down to select the type of your component according to its function.
- ### Component Language
  -- Specify the language of your scripts.  These can be edited later in case the drop down does not contain the language necessary to run your programs.  This should be the language of the files in the zip file input to the ComponentCreator.
  -- If your new component is a Java component (not including calling an external jar file), you do not need input to the ComponentCreator as your code will go into one of the files that the ComponentCreator generates. See how all of the code the [Column Remover](https://github.com/LearnSphere/WorkflowComponents/tree/dev/ColumnRemover/source/edu/cmu/pslc/learnsphere/transform) component is contained in a single java file that is created by the ComponentCreator.
- ### Component Author Name & Email
  -- It is useful for other users to know who you are, as the author of the component. It allows users to contact you if necessary.
- ### Description of Component
  -- This will appear in the tool-tip when users hover over your component in the left-hand menu.  You can use this to help users understand how to use your component. This information will be inserted into an XML file, see [Row Remover's info.xml](https://github.com/LearnSphere/WorkflowComponents/blob/dev/RowRemover/info.xml) as an example. (This can be edited later)
- ### Program in input to run
  -- Write the name of the script that is in your zip file input to the ComponentCreator that should be run first.  This is probably the program that contains the main method/function.  This is rowRemover.R in the case of the RowRemover component.
- ### Number of Input Nodes / Outputs/ Options
  -- Choose how many input nodes / outputs / options your new component should have.

## Inputs
Note: **X, Y, and Z** are indexes used to differentiate the different Inputs, Outputs, and Options for your new component.  They increment from 0.
- ### Input X type
  -- Write the type of the X'th input to your component.  Common types include
	- tab-delimited
	- student-step
	- xml
	- csv
	- inline-html
	- transaction
	- file
	- text

## Outputs
- ### Output Y type
  -- This is the type of the Y'th file that your component generates.  For example, this would be "tab-delimited" in the case of the **Row Remover** component.  See [Input Type](#input-x-type) for examples of file types.
- ### Output Y File Name
  -- This is the name of the Y'th file that your component generates.  This is necessary so that the LearnSphere interface knows which file in the output directory to allow users to view and download.
## Options

![alt text](https://github.com/LearnSphere/WorkflowComponents/blob/dev/ComponentCreator/images/OptionsFigure.png "What the options look like")

- ### Option Z Type
  -- These are the available option types for components.  They appear in the component options panel just like the options for the ComponentCreator.
  - **String** - This prompts the user to enter text into an input box.
  - **Integer** - This also spawns an input box, but the user must enter an integer value.
  - **Double** - This also spawns an input box, but the user must enter a number value.
  - **FileInputHeader** - This creates a drop down with the column headers from the input files.  For example, this could be the values of the top line of a tab-delimited file.  The user can select just one of them.  
  - **MultiFileInputHeader** - This is the same as **FileInputHeader** with the exception that the user can select zero or multiple headers from the input files.
  - **Enumeration** - This creates a drop down with values that you can specify.  An example of this is the component language option in the ComponentCreator.  You specify the options that should populate the drop down in a comma separated list in [a latter option](#list-of-values-in-enumeration-separated-by-commas-z).
- ### Option Z Name
  -- Create a name for this option.  Option names must be unique and **contain only numbers and letters**.  This is the way that you'll get the option's value in your scripts.  It will be passed to your component via the command line like so:
	  ```python yourPythonScript.py -optionName "Option value"```
	  The above example is simplified, there will be other command line arguments to your script.
- ### Option Z ID
  --This will be what users see next to the option input.  For example, in the Row Remover component, the first option's ID is "What to Do with Rows:".  Option ID's **must be unique and must only consist of letter and numbers**.
- ### Option Z Default Value
  -- This will be the value that is put in when your new component is initiated.  For simple option types like String, Integer, and Double, it is simply the starting value.  
  
  For FileInputHeader and MultiFileInputHeader, this value is a regular expression and can be used to filter the column headers.  For example, in Row Remover, you want all columns to appear in the drop down and so this value is ".\*".  However in AFM, you want only the columns that match the form "KC (anyTextHere)" and so you use the regular expression "\s\*KC\s\*\\((.*)\\)\s\*"  to filter out column headers that don't match that form.
- ### List of values in enumeration separated by commas Z
  -- This option is only available if option Z is of type "Enumeration".  This is where you'll list the values you want in the drop down.  For example, in ComponentCreator's option for option type, you'd enter "String, Integer, FileInputHeader, MultiFileInputHeader, Enumeration, Double"

# Next Steps
You have just run the ComponentCreator and you have a zip file that you don't know what to do with.  Try this:
-  Examine the Output Zip File
  -- Ensure that the contents of your zip file are all there.  You're component should have:
    - a schema file in `YourComponent/schemas/`
    - a java wrapper program in `YourComponent/source/....YourComponentType/YourComponentMain.java`
    - a test xml file in `YourComponent/test/components/ComponentYourComponent.xml`
    - If your scripts were not specified as Java, then you should find the zip file that you used as input to the ComponentCreator in `YourComponent/program/`
    - a build.xml file
    - a build.properties file
    - an info.xml file
-  Move The Output to a Local Copy of the Workflow Components
  -- To test your component locally, copy this new component into a local copy of the [Workflow Components GitHub repository](https://github.com/LearnSphere/WorkflowComponents).
- Fill in the build.properties File
	- This file is used to run your component.  If your component is written in Java, then you will not need to update this file.
	- `component.interpreter.path` is the path to the program that will run your scripts.  If your scripts written in R, then it will look something like this: ```component.interpreter.path=/usr/local/bin/Rscript```
	- `component.program.path` is the program that the component should run.  This will likely be the script with a main method/function.  For example:     ```component.program.path=program/rowRemover.R```
- Add Test Input to `YourComponent/test/test_data/`
	- For testing your component, you'll want to add some input data.  Put files here that should be used as input to the component.
- Edit the Test XML
	- The test XML is located in `YourComponent/test/components/`.  This is where you'll specify a test case for your new components.
		- Add the path to your input files in this XML file where you see "PUT PATH TO TEST INPUT HERE."
		- Put the file name where you see "PUT NAME OF TEST INPUT HERE."
		- If your component has multiple inputs, ensure that you have them listed in the correct order by looking at tags \<inputX\> where X is the input node index.
		- Fill in the options for your test case inside the \<options\> tag.
- Dependencies
	- You will need **Ant** 1.9 or greater.  This can be downloaded from [here](https://ant.apache.org/bindownload.cgi).
	- You will also need **Java** Enterprise Edition Software Development Kit (J2EE SDK).  You can download this [here](https://www.oracle.com/technetwork/java/javaee/downloads/index.html).
	- For more information about dependencies see [the workflow components doc in GitHub](https://github.com/LearnSphere/WorkflowComponents/blob/dev/Workflow%20Components.docx).
- You Can Now Test Your Component Using Ant
	- Open a terminal and go to your new component's directory:
		- ```cd C:/Users/.../WorkflowComponents/YourComponent/```
	- Run `ant runComponent` in this directory to build and test your component.
	- More information on ant and testing your component can be found in [the workflow components doc in GitHub](https://github.com/LearnSphere/WorkflowComponents/blob/dev/Workflow%20Components.docx).
- When you are finished testing your component, contact datashop-help@lists.andrew.cmu.edu to put your component into LearnSphere.