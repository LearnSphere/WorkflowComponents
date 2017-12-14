//detector template

//add output variable name below
var variableName = "struggle"

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
var e;


//declare any custom global variables that will be initialized 
//based on "remembered" values across problem boundaries, here
// (initialize these at the bottom of this file, inside of self.onmessage)
var attemptWindow;
var intervalID;
var onboardSkills = {};
var initTime;

//declare and/or initialize any other custom global variables for this detector here
var stepCounter = {};
var help_model_output;
var help_variables = {"lastAction": "null",
					  "lastActionTime": "",
					  "seenAllHints": {},
					  "lastHintLength": "",
					  "lastSenseOfWhatToDo": false
					 };
var attemptCorrect;
var elaborationString = " ";
//
//[optional] single out TUNABLE PARAMETERS below
var windowSize = 10;
var threshold = 3;
var BKTparams = {p_transit: 0.2, 
				p_slip: 0.1, 
				p_guess: 0.2, 
				p_know: 0.25};
var errorThreshold = 2; //currently arbitrary
var newStepThreshold = 1; //currently arbitrary
var familiarityThreshold = 0.4;
var senseOfWhatToDoThreshold = 0.6;
var hintIsHelpfulPlaceholder = true; //currently a dummy value (assumption that hint is always helpful...)
var seedTime = 25;


//
//###############################
//#####     Help model     ######
//###############################
//###############################
//

//non-controversial
function lastActionIsHint(e){
	if (help_variables.lastAction == "hint"){return true;}
	else{return false;}
}
function lastActionIsError(e){
	if (help_variables.lastAction == "error"){return true;}
	else{return false;}
}
function seenAllHintLevels(e){
	if (e.data.tutor_data.action_evaluation.toLowerCase() == "hint"){
		if (e.data.tutor_data.selection in help_variables.seenAllHints){
			return help_variables.seenAllHints[e.data.tutor_data.selection];
		}
		else{return false;}
	}
	else{
		if (e.data.tool_data.selection in help_variables.seenAllHints){
			return help_variables.seenAllHints[e.data.tool_data.selection];
		}
		else{return false;}
	}
}
function isCorrect(e){
	if (e.data.tutor_data.action_evaluation.toLowerCase() == "correct"){return true;}
	else{return false;}
}

function secondsSinceLastAction(e){
	var currTime = new Date(e.data.tool_data.tool_event_time);
	diff = currTime.getTime() - help_variables.lastActionTime.getTime();
	console.log("time elapsed: ", diff/1000)
	return (diff / 1000);
}

//less controversial
function isDeliberate(e){
	var hintThreshold = (help_variables.lastHintLength/600)*60;

	if (lastActionIsError(e)){
		return (secondsSinceLastAction(e) > errorThreshold);
	}
	else if (lastActionIsHint(e)){
		return (secondsSinceLastAction(e) > hintThreshold);
	}
	else{
		return (secondsSinceLastAction(e) > newStepThreshold);
	}
}

//more controversial...
function isFamiliar(e){
	var rawSkills = onboardSkills;
	for (var property in rawSkills) {
	    if (rawSkills.hasOwnProperty(property)) {
	        if (parseFloat(rawSkills[property]["p_know"])<=familiarityThreshold){
	        	return false;
	        }
	    }
	}
	return true;
}

function isLowSkillStep_All(e){
	var rawSkills = onboardSkills;
	for (var property in rawSkills) {
	    if (rawSkills.hasOwnProperty(property)) {
	        if (parseFloat(rawSkills[property]["p_know"])>=familiarityThreshold){
	        	return false;
	        }
	    }
	}
	return true;
}

function isLowSkillStep_Some(e){
	var rawSkills = onboardSkills;
	for (var property in rawSkills) {
	    if (rawSkills.hasOwnProperty(property)) {
	        if (parseFloat(rawSkills[property]["p_know"])<=familiarityThreshold){
	        	return true;
	        }
	    }
	}
	return false;
}

function hintIsHelpful(e){
	return hintIsHelpfulPlaceholder;
}
function lastActionUnclearFix(e){
	if (help_variables.lastSenseOfWhatToDo == false){return true;}
	else{return false;}
}
function senseOfWhatToDo(e){
	var sel = e.data.tutor_data.selection;
	var rawSkills = onboardSkills;
	for (var property in rawSkills) {
	    if (rawSkills.hasOwnProperty(property)) {
	        if (parseFloat(rawSkills[property]["p_know"])<=senseOfWhatToDoThreshold){
	        	return false;
	        }
	    }
	}
	return true;
}

//evaluation of each step
function evaluateAction(e){
	var sel = e.data.tutor_data.selection;
	var outcome = e.data.tutor_data.action_evaluation.toLowerCase();

	if (e.data.tutor_data.action_evaluation.toLowerCase() == "hint"){
		console.log("isHint")
		if (isDeliberate(e)){
			console.log("isDeliberate")
			if (!seenAllHintLevels(e) &&
				(!isFamiliar(e) 
				|| (lastActionIsError(e) && lastActionUnclearFix(e)) 
				|| (lastActionIsHint(e) && !hintIsHelpful(e))) ){
				return "preferred/ask hint";
			}
			else if ( (isFamiliar(e) && !senseOfWhatToDo(e) ) 
					|| (lastActionIsHint(e)) ){
				return "acceptable/ask hint";
			}
			else{
				return "not acceptable/hint abuse";
			}
			
		}
		else{
		console.log("not deliberate")
			return "not acceptable/hint abuse";
		}

	}
	else{
		if (isDeliberate(e)){
			if ( (isFamiliar(e) && (!(lastActionIsError(e) && lastActionUnclearFix(e))) )
				|| (lastActionIsHint(e) && hintIsHelpful(e))
				 ){
				return "preferred/try step";
			}
			else if (seenAllHintLevels(e) && 
				     (!(lastActionIsError(e) && lastActionUnclearFix(e))) ){
				return "preferred/try step";
			}
			else if (isCorrect(e)){
				return "acceptable/try step";
			}
			else if (seenAllHintLevels(e)){
				if (lastActionIsError(e) && lastActionUnclearFix(e)){
					return "ask teacher for help/try step";
				}
			}
			else{
				return "not acceptable/hint avoidance";
			}
		}
		else{
			return "not acceptable/not deliberate";
		}
	}

}

function updateHistory(e){
	help_variables.lastActionTime = new Date(e.data.tool_data.tool_event_time);
	if (e.data.tutor_data.action_evaluation.toLowerCase() == "hint"){
		help_variables.lastAction = "hint";
		help_variables.lastHintLength = e.data.tutor_data.tutor_advice.split(' ').length;
		if (help_variables.seenAllHints[e.data.tutor_data.selection] != true){
			help_variables.seenAllHints[e.data.tutor_data.selection] = (e.data.tutor_data.current_hint_number == e.data.tutor_data.total_hints_available);
		}
	}
	if (e.data.tutor_data.action_evaluation.toLowerCase() == "incorrect"){
		help_variables.lastAction = "error";
	}
	if (e.data.tutor_data.action_evaluation.toLowerCase() == "correct"){
		help_variables.lastAction = "correct";
	}

	help_variables.lastSenseOfWhatToDo = senseOfWhatToDo(e);

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

//
//###############################
//###############################
//###############################
//###############################
//

function secondsSince(initTime){	
	var currTime = new Date(e.data.tool_data.tool_event_time);
	diff = currTime.getTime() - initTime.getTime();
	console.log("time elapsed: ", diff/1000)
	return (diff / 1000);
}

function checkTimeElapsed(initTime) {
  	var timeDiff = secondsSince(initTime);
  	var currTimeMessage = detector_output.value.split(',')[1];
  	console.log(currTimeMessage);
	if( timeDiff > (600-seedTime)){ 
		if (currTimeMessage!=" > 10 min"){ 
	      detector_output.history = JSON.stringify([attemptWindow, initTime, onboardSkills]);
	      detector_output.value = "1, > 10 min, " + elaborationString;
	      detector_output.time = new Date(e.data.tool_data.tool_event_time);
		  if (offlineMode==false){
			mailer.postMessage(detector_output);
			}
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);  
		}
	}
	else if( timeDiff > (300-seedTime)){ 
		if (currTimeMessage!=" > 5 min"){ 
	      detector_output.history = JSON.stringify([attemptWindow, initTime, onboardSkills]);
	      detector_output.value = "1, > 5 min, " + elaborationString;
	      detector_output.time = new Date(e.data.tool_data.tool_event_time);
		  if (offlineMode==false){
			mailer.postMessage(detector_output);
			}
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);  
		}
	}
	else if( timeDiff > (120-seedTime)){ 
		if (currTimeMessage!=" > 2 min"){ 
		  detector_output.history = JSON.stringify([attemptWindow, initTime, onboardSkills]);
	      detector_output.value = "1, > 2 min, " + elaborationString;
	      detector_output.time = new Date(e.data.tool_data.tool_event_time);
		  if (offlineMode==false){
			mailer.postMessage(detector_output);
			}
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);  
		}
	}
	else if( timeDiff > (60-seedTime)){
	  if (currTimeMessage!=" > 1 min"){ 
		  detector_output.history = JSON.stringify([attemptWindow, initTime, onboardSkills]);
	      detector_output.value = "1, > 1 min, " + elaborationString;
	      detector_output.time = new Date(e.data.tool_data.tool_event_time);
		  if (offlineMode==false){
			mailer.postMessage(detector_output);
			}
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);
	  }  
	}
	else if( timeDiff > (45-seedTime)){ 
		if (currTimeMessage!=" > 45 s"){ 
		  detector_output.history = JSON.stringify([attemptWindow, initTime, onboardSkills]);
	      detector_output.value = "1, > 45 s, " + elaborationString;
	      detector_output.time = new Date(e.data.tool_data.tool_event_time);
		  if (offlineMode==false){
			mailer.postMessage(detector_output);
			}
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);  
		}
	}
	else{
		console.log(currTimeMessage == " > " + seedTime.toString() + " s");
		if(currTimeMessage!=" > " + seedTime.toString() + " s"){
		  detector_output.history = JSON.stringify([attemptWindow, initTime, onboardSkills]);
	      detector_output.value = "1, > " + seedTime.toString() + " s, " + elaborationString;
	      detector_output.time = new Date(e.data.tool_data.tool_event_time);
		  if (offlineMode==false){
			mailer.postMessage(detector_output);
			}
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);
		}  
	}
}



function receive_transaction( thisTransaction ){
	//e is the data of the transaction from mailer from transaction assembler
	e = thisTransaction;
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
		//

		//########  BKT  ##########
		var currStep = e.data.tutor_data.selection;
		for (var i in currSkills){
			var skill = currSkills[i];

			if(!(currStep in stepCounter)){
				if (!(skill in onboardSkills)){	//if this skill has not been encountered before
					onboardSkills[skill] = clone(BKTparams);
				}

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

		//keep track of num attempts on each step
		if(currStep in stepCounter){
			stepCounter[currStep] += 1;
		}
		else{
			stepCounter[currStep] = 1;
		}

		//#######

		if (help_variables.lastAction!="null"){
			help_model_output = evaluateAction(e);
		}
		else{
			help_model_output = "preferred"; //first action in whole tutor is set to "preferred" by default
		}

		// attemptCorrect = (e.data.tutor_data.action_evaluation.toLowerCase() == "correct") ? 1 : 0;
		// attemptWindow.shift();
		// attemptWindow.push(attemptCorrect);

		// ignore further hint requests if student has already seen all hint levels for this step (i.e., these do not contribute to struggle detector)
		if(seenAllHintLevels(e) && e.data.tutor_data.action_evaluation.toLowerCase() == "hint"){
			console.log("is hint request on step for which student has already seen all hints: no direct/immediate effect on struggle detector");
		}
		else{
			attemptCorrect = (e.data.tutor_data.action_evaluation.toLowerCase() == "correct") ? 1 : 0;
			attemptWindow.shift();
			attemptWindow.push(attemptCorrect);
		}

		if (help_model_output == "ask teacher for help/try step"){
			for(var i=0; i<(windowSize-threshold); i++){
				attemptWindow.shift(); 
				attemptWindow.push(0)};
		}

		var sumCorrect = attemptWindow.reduce(function(pv, cv) { return pv + cv; }, 0);
		console.log(attemptWindow);

		updateHistory(e);
		console.log(help_model_output);
		
	}

	//set conditions under which detector should update
	//external state and history
	if(e.data.actor == 'student' && e.data.tool_data.selection !="done" && e.data.tool_data.action != "UpdateVariable"){
		detector_output.time = new Date(e.data.tool_data.tool_event_time);
		detector_output.transaction_id = e.data.transaction_id;

		//custom processing (insert code here)

		//   elaboration string
		if (sumCorrect<=threshold){
			if(help_model_output == "ask teacher for help/try step"){
				elaborationString = "hints aren't helping";
			}
			else if(help_model_output == "not acceptable/hint avoidance"){
				elaborationString = "not using hints";
			}
			else{
				elaborationString = "lots of errors";
			}
		}
		else{
			elaborationString = " ";
		}


		if (detector_output.value.split(',')[0]=="0" && (sumCorrect <= threshold)){
			initTime = new Date(e.data.tool_data.tool_event_time);
			detector_output.history = JSON.stringify([attemptWindow, initTime, onboardSkills]);
			detector_output.value = "1, > " + seedTime.toString() + " s, " + elaborationString;
			detector_output.time = new Date(e.data.tool_data.tool_event_time);

			//intervalID = setInterval( function() { checkTimeElapsed(initTime);} , 3000);

		}
		else if (detector_output.value.split(',')[0]!="0" && (sumCorrect <= threshold)){
			checkTimeElapsed(initTime);
			detector_output.history = JSON.stringify([attemptWindow, initTime, onboardSkills]);
			detector_output.time = new Date(e.data.tool_data.tool_event_time);
		}
		else{
			detector_output.value = "0, > 0 s, " + elaborationString;
			detector_output.history = JSON.stringify([attemptWindow, initTime, onboardSkills]);
			detector_output.time = new Date(e.data.tool_data.tool_event_time);

			//clearInterval(intervalID);
		}

		if (offlineMode==false){
			mailer.postMessage(detector_output);
		}
		postMessage(detector_output);
		console.log("output_data = ", detector_output);
	}
}


self.onmessage = function ( event ) {
    //console.log(variableName, " self.onmessage:", e, e.data, (e.data?e.data.commmand:null), (e.data?e.data.transaction:null), e.ports);
    switch( event.data.command )
    {
    case "offlineMode":
    	//console.log(event.data.message);
    	offlineMode = true;
    	receive_transaction({data: event.data.message});
    break;
    case "offlineNewProblem":
    	console.log("new problem!");
    	stepCounter = {};
    	help_model_output;
		help_variables = {"lastAction": "null",
					  "lastActionTime": "",
					  "seenAllHints": {},
					  "lastHintLength": "",
					  "lastSenseOfWhatToDo": false
					 };
		attemptCorrect;
    break;
    case "offlineNewStudent":
    	console.log("new student!");
    	detector_output.category = event.data.studentId;
    	detector_output.history = "";
		detector_output.value = "0, > 0 s";
    	attemptWindow = Array.apply(null, Array(windowSize)).map(Number.prototype.valueOf,1);
		onboardSkills = {};
		stepCounter = {};
		initTime;
		elaborationString = " ";
		help_model_output;
		help_variables = {"lastAction": "null",
					  "lastActionTime": "",
					  "seenAllHints": {},
					  "lastHintLength": "",
					  "lastSenseOfWhatToDo": false
					 };
		attemptCorrect;
    break;
    case "connectMailer":
		mailer = e.ports[0];
		mailer.onmessage = receive_transaction;
	break;
	case "initialize":
	console.log("intialize: ", JSON.stringify(event.data.initializer));
		for (initItem in event.data.initializer){
			if (event.data.initializer[initItem].name == variableName){
				detector_output.history = event.data.initializer[initItem].history;
				detector_output.value = event.data.initializer[initItem].value;
			}
		}

		//optional: In "detectorForget", specify conditions under which a detector
		//should NOT remember their most recent value and history (using the variable "detectorForget"). 
		//(e.g., setting the condition to "true" will mean that the detector 
		// will always be reset between problems... and setting the condition to "false"
		// means that the detector will never be reset between problems)
		//
		//
		//
		detectorForget = false;
		//
		//

		if (detectorForget){
			detector_output.history = "";
			detector_output.value = "0, none";
		}


		//optional: If any global variables are based on remembered values across problem boundaries,
		// these initializations should be written here
		//
		//
		if (detector_output.history == "" || detector_output.history == null){
			attemptWindow = Array.apply(null, Array(windowSize)).map(Number.prototype.valueOf,1);
			onboardSkills = {};
		}
		else{
			var all_history = JSON.parse(detector_output.history);
			attemptWindow = all_history[0];
			initTime = new Date(all_history[1]);
			onboardSkills = all_history[2];

			//if(detector_output.value.split(',')[0]!="0"){
			//	intervalID = setInterval( function() { checkTimeElapsed(initTime);} , 3000);
			//}
		}

		if (offlineMode==false){
			detector_output.time = new Date(event.data.tool_data.tool_event_time);
			mailer.postMessage(detector_output);
			postMessage(detector_output);
			console.log("output_data = ", detector_output);
		}

	break;
    default:
	break;

    }

}