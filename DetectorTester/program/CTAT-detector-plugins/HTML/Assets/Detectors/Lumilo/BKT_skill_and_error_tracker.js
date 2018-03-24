//detector template

//add output variable name below
var variableName = "BKT_skill_and_error_tracker";

//initializations (do not touch)
var detector_output = {name: variableName,
						category: "Dashboard", 
						value: "0, none",
						history: "",
						skill_names: "",
						step_id: "",
						transaction_id: "",
						time: ""
						};
var mailer;

//declare any custom global variables that will be initialized 
//based on "remembered" values across problem boundaries, here
// (initialize these at the bottom of this file, inside of self.onmessage)
var skillLevelsAttemptsErrors;
var onboardSkills;
var outputValue;

//declare and/or initialize any other custom global variables for this detector here...
var stepCounter = {};
var prevStep = "";
var prevEq = "";
var currRow = 1;
var currLeftCorrect = 0;
var currRightCorrect = 0;
var currLeft = "_______";
var currRight = "_______";
var currError = "";
var transitionError = "";
//
//[optional] single out TUNABLE PARAMETERS below
var BKTparams = {p_transit: 0.2, 
				p_slip: 0.1, 
				p_guess: 0.2, 
				p_know: 0.25};


//
//###############################
//###############################
//###############################
//###############################
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

function updateSkillLevelsAttemptsErrors(e, skill, currStepCount){

	if(skill in skillLevelsAttemptsErrors ){
		if(currStepCount==1){
			skillLevelsAttemptsErrors[skill][0] += 1;
		}

		skillLevelsAttemptsErrors[skill][1] = parseFloat(onboardSkills[skill]["p_know"]);

	}
	else{
		skillLevelsAttemptsErrors[skill] = [1, parseFloat(onboardSkills[skill]["p_know"]), [null, null, null, null, null]];
	}
}

// function format_areas_of_struggle_data(e, skill, cleaned_skill_name){
// 	var return_string = "";

// 	if (skillLevelsAttemptsErrors.hasOwnProperty(skill)){

// 		var currSkillName = cleaned_skill_name;
// 		var currSkillAttemptCount = String(skillLevelsAttemptsErrors[skill][0]);
// 		var currSkillLevel = String(skillLevelsAttemptsErrors[skill][1]);
// 		var currSkillErrorHistory = skillLevelsAttemptsErrors[skill][2].join("@");

// 		return_string += currSkillName + "," + currSkillAttemptCount + "," + currSkillErrorHistory + "," + currSkillLevel;
// 	}

// 	return return_string;
// }


function format_areas_of_struggle_data(e){
	var return_string = "";

	//sorting
	var skillLevelsAttemptsErrorsSorted = Object.keys(skillLevelsAttemptsErrors).map(function(key) {
	    return [key, skillLevelsAttemptsErrors[key][1]];
	});

	// Sort the array based on the second element
	skillLevelsAttemptsErrorsSorted.sort(function(first, second) {
	    return first[1] - second[1];
	});

	skillLevelsAttemptsErrorsSorted = skillLevelsAttemptsErrorsSorted.map(function(x) {
	    return x[0];
	});

	console.log("sorted skills: " + JSON.stringify(skillLevelsAttemptsErrorsSorted));


	for (skillIndex in skillLevelsAttemptsErrorsSorted){
		var skill = skillLevelsAttemptsErrorsSorted[skillIndex];
		var cleaned_skill_name = skill.split("/");    
		cleaned_skill_name.pop();
		cleaned_skill_name = cleaned_skill_name.join("/").replaceAll("\\_", " "); 
		var currSkillName = cleaned_skill_name;

		var currSkillAttemptCount = String(skillLevelsAttemptsErrors[skill][0]);
		var currSkillLevel = String(skillLevelsAttemptsErrors[skill][1]);
		var currSkillErrorHistory = skillLevelsAttemptsErrors[skill][2].join("@");

		if (return_string!="")
		{
			return_string += ";"
		}
		else{
			return_string += "!!!!!!!!!!";
		}
		return_string += currSkillName + "," + currSkillAttemptCount + "," + currSkillErrorHistory + "," + currSkillLevel;
	}

	return_string += "!!!!!!!!!!";

	// if (skillLevelsAttemptsErrors.hasOwnProperty(skill)){

	// 	var currSkillName = cleaned_skill_name;
	// 	var currSkillAttemptCount = String(skillLevelsAttemptsErrors[skill][0]);
	// 	var currSkillLevel = String(skillLevelsAttemptsErrors[skill][1]);
	// 	var currSkillErrorHistory = skillLevelsAttemptsErrors[skill][2].join("@");

	// 	return_string += currSkillName + "," + currSkillAttemptCount + "," + currSkillErrorHistory + "," + currSkillLevel;
	// }

	return return_string;
}


//
//###############################
//###############################
//###############################
//###############################
//

function clone(obj) {
    var copy;

    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) return obj;

    // Handle Date
    if (obj instanceof Date) {
        copy = new Date();
        copy.setTime(obj.getTime());
        return copy;
    }

    // Handle Array
    if (obj instanceof Array) {
        copy = [];
        for (var i = 0, len = obj.length; i < len; i++) {
            copy[i] = clone(obj[i]);
        }
        return copy;
    }

    // Handle Object
    if (obj instanceof Object) {
        copy = {};
        for (var attr in obj) {
            if (obj.hasOwnProperty(attr)) copy[attr] = clone(obj[attr]);
        }
        return copy;
    }

    throw new Error("Unable to copy obj! Its type isn't supported.");
}



function receive_transaction( e ){
	//e is the data of the transaction from mailer from transaction assembler

	//set conditions under which transaction should be processed 
	//(i.e., to update internal state and history, without 
	//necessarily updating external state and history)
	if(e.data.actor == 'student' && e.data.tool_data.selection !="done" && e.data.tool_data.action != "UpdateVariable"){
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

		var currStep = e.data.tutor_data.selection;


		//error updates
		if (e.data.tool_data.selection.includes("solve")){

			//TO-DO: generalize so that this works with the undo button
			if (e.data.tool_data.selection == "solveLeft" + currRow && e.data.tutor_data.action_evaluation.toLowerCase() == "correct"){
				currLeftCorrect = 1;
			}
			if (e.data.tool_data.selection == "solveRight" + currRow && e.data.tutor_data.action_evaluation.toLowerCase() == "correct"){
				currRightCorrect = 1;
			}
		
			if (e.data.tutor_data.selection.includes("solveLeft")){
					currLeft = e.data.tool_data.input;
			}
			if (e.data.tutor_data.selection.includes("solveRight")){
					currRight = e.data.tool_data.input;
			}

			if (e.data.tutor_data.selection.includes(String(currRow)) && e.data.tutor_data.action_evaluation.toLowerCase() == "incorrect"){

				if (prevEq == ""){
					//this currently relies on the existing problem naming convention(!)
					//would be ideal to change this to something more robust... specifically:
					//would be nice if we could access varables from tutor state (variable table...)
					prevEq = reinsertParentheses(e.data.context.problem_name.replaceAll(" ", " \+ ").replaceAll("eq", " \= ").replaceAll("\\+", " \+ "));
				}

				if (e.data.tutor_data.selection.includes("solveLeft")){
					currLeft = "`" + currLeft + "~";
				}
				if (e.data.tutor_data.selection.includes("solveRight")){
					currRight = "`" + currRight + "~";
				}

				//needs to be expanded...
				currError = currLeft + " = " + currRight;
				
				transitionError = prevEq + " \\n " + currError;

				console.log("TRANSITION_ERROR: " + transitionError);
			
			}

			if (currLeftCorrect==1 && currRightCorrect==1){
					currRow += 1;
					currLeftCorrect = 0;
					currRightCorrect = 0;
					prevEq = currLeft + " = " + currRight;
					currLeft = "_______";
					currRight = "_______";
			}
		}


		//keep track of num attempts on each step
		if(currStep in stepCounter){
			stepCounter[currStep] += 1;
		}
		else{
			stepCounter[currStep] = 1;
		}
		

		for (var i in currSkills){
			var skill = currSkills[i];

			//
			//BKT
			//
			if (!(skill in onboardSkills)){	//if this skill has not been encountered before
					onboardSkills[skill] = clone(BKTparams);
			}
			if(stepCounter[currStep]==1){
				var p_know_tminus1 = onboardSkills[skill]["p_know"];
				var p_slip = onboardSkills[skill]["p_slip"];
				var p_guess = onboardSkills[skill]["p_guess"];
				var p_transit = onboardSkills[skill]["p_transit"];

				console.log(onboardSkills[skill]["p_know"]);


				if (e.data.tutor_data.action_evaluation.toLowerCase()=="correct"){
					var p_know_given_obs = (p_know_tminus1*(1-p_slip))/( (p_know_tminus1*(1-p_slip)) + ((1-p_know_tminus1)*p_guess) );
				}
				else{
					var p_know_given_obs = (p_know_tminus1*p_slip)/( (p_know_tminus1*p_slip) + ((1-p_know_tminus1)*(1-p_guess)) );
				}
				
				onboardSkills[skill]["p_know"] = p_know_given_obs + (1 - p_know_given_obs)*p_transit;

				//following TutorShop, round down to two decimal places
				onboardSkills[skill]["p_know"] = Math.floor(onboardSkills[skill]["p_know"] * 100) / 100;

				console.log("engine BKT: ", e.data.tutor_data.skills[0].pKnown);
				console.log(onboardSkills[skill]["p_know"]);
			}
		}

		//update skill information for all skills
		for (var i in currSkills){
			var skill = currSkills[i];
			console.log(skill);
			updateSkillLevelsAttemptsErrors(e, skill, stepCounter[currStep]);

			if(e.data.tool_data.selection.includes("solve") && e.data.tutor_data.selection.includes(String(currRow)) && e.data.tutor_data.action_evaluation.toLowerCase() == "incorrect"){
				skillLevelsAttemptsErrors[skill][2].shift();
				skillLevelsAttemptsErrors[skill][2].push(transitionError);

			}
		}

		outputValue = format_areas_of_struggle_data(e)

		detector_output.name = variableName;
		detector_output.value = "0, none";
		detector_output.history = JSON.stringify([skillLevelsAttemptsErrors, onboardSkills, outputValue]);
		detector_output.time = new Date();
		detector_output.transaction_id = e.data.transaction_id;
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
		detectorForget = false;
		//
		//

		if (detectorForget){
			detector_output.history = "";
			detector_output.value = null;
		}


		//optional: If any global variables are based on remembered values across problem boundaries,
		// these initializations should be written here
		//
		//
		if (detector_output.history == "" || detector_output.history == null){
			//in the event that the detector history is empty,
			//initialize variables to your desired 'default' values
			//
			skillLevelsAttemptsErrors = {};
			onboardSkills = {};
			outputValue = "";
		}
		else{
			//if the detector history is not empty, you can access it via:
			//     JSON.parse(detector_output.history);
			//...and initialize your variables to your desired values, based on 
			//this history
			//
			var all_history = JSON.parse(detector_output.history);
			skillLevelsAttemptsErrors = all_history[0];
			onboardSkills = all_history[1];
			outputValue = all_history[2];

		}

		detector_output.time = new Date();
		mailer.postMessage(detector_output);
		postMessage(detector_output);
		console.log("output_data = ", detector_output);
		
	break;
    default:
	break;

    }

}