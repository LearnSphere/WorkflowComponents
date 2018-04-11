//detector template

//add output variable name below
var variableName = "current_problem_history"

//initializations (do not touch)
var detector_output = {name: variableName,
						category: "Dashboard", 
						value: "0, none",
						history: "0, none",
						skill_names: "",
						step_id: "",
						transaction_id: "",
						time: ""
						};
var mailer;

//declare any custom global variables that will be initialized 
//based on "remembered" values across problem boundaries, here
// (initialize these at the bottom of this file, inside of self.onmessage)
//
//
//
//
//


//declare and/or initialize any other custom global variables for this detector here
var prevStep = "";
var prevEq = "";
var currRow = 1;
var currLeftCorrect = 0;
var currRightCorrect = 0;
var currLeft = "_______";
var currRight = "_______";
var runningSolution = "";
var solutionStages = "";
var runningSolutionMinusCurrentLine = "";
var stepCounter = {};
var usedHint = {}; //for each step, track whether a student has used a hint on this step
//
//
//

String.prototype.replaceAll = function(search, replacement) {
    var target = this;
    return target.replace(new RegExp(search, 'g'), replacement);
};

function reinsertParentheses(e){

	var a = e.split("par");
	var returnString = "";
	var parenList = ['(',')'];	
	for(var i = 0; i < a.length-1; i++){
			returnString += a[i] + parenList[i%2];
		}
	returnString += a[a.length-1];

	return returnString;
}

function receive_transaction( e ){
	//e is the data of the transaction from mailer from transaction assembler

	//set conditions under which transaction should be processed 
	//(i.e., to update internal state and history, without 
	//necessarily updating external state and history)
	if(e.data.actor == 'student' && e.data.tool_data.action != "UpdateVariable"){
		//do not touch
		rawSkills = e.data.tutor_data.skills
		var currSkills = []
		for (var property in rawSkills) {
		    if (rawSkills.hasOwnProperty(property)) {
		        currSkills.push(rawSkills[property].name + "/" + rawSkills[property].category)
		    }
		}
		detector_output.skill_names = currSkills;
		detector_output.step_id = e.data.tutor_data.step_id;

		//custom processing (insert code here)
		//
		//

		//
		var currStep = e.data.tutor_data.selection;
		if(currStep in stepCounter){
			stepCounter[currStep] += 1;
		}
		else{
			stepCounter[currStep] = 1;
		}

		if(e.data.tutor_data.action_evaluation.toLowerCase()=="hint"){
			if(currStep in usedHint){
				usedHint[currStep] += 1;
			}
			else{
				usedHint[currStep] = 1;	
			}
		}


		//
		if (e.data.tool_data.selection.includes("solve")){
		
			//TO-DO: generalize so that this works with the undo button
			if (e.data.tool_data.selection == "solveLeft" + currRow && e.data.tutor_data.action_evaluation.toLowerCase() == "correct"){
				currLeftCorrect = 1;
			}
			if (e.data.tool_data.selection == "solveRight" + currRow && e.data.tutor_data.action_evaluation.toLowerCase() == "correct"){
				currRightCorrect = 1;
			}

			//
			//
			//perform on any attempt
			//
			//
			if (e.data.tool_data.selection.includes("solveLeft")){
				currLeft = e.data.tool_data.input;
				if (e.data.tutor_data.action_evaluation.toLowerCase() == "incorrect"){
					currLeft = "[" + currLeft + "]";
				}
				else{
					if(currStep in usedHint){
						currLeft = "`" + String(usedHint[currStep]) + "~ " + currLeft;
					}
					else{
						currLeft = "          " + currLeft;
					}
				}
			}
			if (e.data.tool_data.selection.includes("solveRight")){
					currRight = e.data.tool_data.input;
				if (e.data.tutor_data.action_evaluation.toLowerCase() == "incorrect"){
					currRight = "[" + currRight + "]";
				}
				else{
					if(currStep in usedHint){
						currRight = currRight + " `" + String(usedHint[currStep]) + "~";
					}
					else{
						currRight = currRight + "          ";
					}
				}
			}

			if (e.data.tool_data.selection.includes(String(currRow))){

					if (prevEq == ""){
						//this currently relies on the existing problem naming convention(!)
						//would be ideal to change this to something more robust... specifically:
						//would be nice if we could access varables from tutor state (variable table...)
						prevEq = reinsertParentheses(e.data.context.problem_name.replaceAll(" ", " \+ ").replaceAll("eq", " \= ").replaceAll("\\+", " \+ "));
						runningSolutionMinusCurrentLine = prevEq;
					}

					//needs to be expanded...
					currAttempt = currLeft + " = " + currRight;

					//runningSolution and solutionStages
					runningSolution = runningSolutionMinusCurrentLine + " \\n " + currAttempt;
					if (solutionStages != ""){
						solutionStages += ",";
					}
					solutionStages += runningSolution;

					if (currLeftCorrect==1 && currRightCorrect==1){
						currRow += 1;
						runningSolutionMinusCurrentLine = runningSolutionMinusCurrentLine + " \\n " + currAttempt;
						currLeftCorrect = 0;
						currRightCorrect = 0;
						prevEq = currLeft + " = " + currRight;
						currLeft = "_______";
						currRight = "_______";
					}
			}

			detector_output.history = solutionStages;
		}

	}

	//set conditions under which detector should update
	//external state and history
	if(e.data.actor == 'student' && e.data.tool_data.action != "UpdateVariable"){
		detector_output.time = new Date();
		detector_output.transaction_id = e.data.transaction_id;

		//custom processing (insert code here)
		//
		//
		//
		//
		//

		mailer.postMessage(detector_output);
		postMessage(detector_output);
		console.log("output_data = ", detector_output);
	}
}


self.onmessage = function ( e ) {
    console.log(variableName, " self.onmessage:", e, e.data, (e.data?e.data.commmand:null), (e.data?e.data.transaction:null), e.ports);
    switch( e.data.command )
    {
    case "connectMailer":
		mailer = e.ports[0];
		mailer.onmessage = receive_transaction;
	break;
	case "initialize":
		for (initItem in e.data.initializer){
			if (e.data.initializer[initItem].name == variableName){
				detector_output.history = e.data.initializer[initItem].history;
				detector_output.value = e.data.initializer[initItem].value;
			}
		}

		//optional: In "detectorForget", specify conditions under which a detector
		//should NOT remember their most recent value and history (using the variable "detectorForget"). 
		//(e.g., setting the condition to "true" will mean that the detector 
		// will always be reset between problems... and setting the condition to "false"
		// means that the detector will never be reset between problems)
		//
		detectorForget = true;
		//
		//

		if (detectorForget){
			detector_output.history = "0, none";
			detector_output.value = "0, none";
		}


		//optional: If any global variables are based on remembered values across problem boundaries,
		// these initializations should be written here
		//
		//
		if (detector_output.history == "" || detector_output.history == null){
			//in the event that the detector history is empty,
			//initialize variables to your desired 'default' values
			//
			//
		}
		else{
			//if the detector history is not empty, you can access it via:
			//     JSON.parse(detector_output.history);
			//...and initialize your variables to your desired values, based on 
			//this history
			//
			//
		}

	break;
    default:
	break;

    }

}