//detector template

//add output variable name below
var variableName = "system_misuse"

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
var attemptWindow = Array.apply(null, Array(windowSize)).map(Number.prototype.valueOf,0);
var initTime;
var lastTrigger;
var intervalID;
var onboardSkills = {};
var e;

//declare and/or initialize any other custom global variables for this detector here...
var stepCounter = {};
var help_model_output;
var help_variables = {"lastAction": "null",
					  "lastActionTime": "",
					  "seenAllHints": {},
					  "lastHintLength": "",
					  "lastSenseOfWhatToDo": false
					 };
var elaborationString = " ";
//
//[optional] single out TUNABLE PARAMETERS below
var windowSize = 10; //arbitrary: need to tune
var threshold = 3; //arbitrary: need to tune
var BKTparams = {p_transit: 0.2, 
				p_slip: 0.1, 
				p_guess: 0.2, 
				p_know: 0.25};
var errorThreshold = 2; //currently somewhat arbitrary
var newStepThreshold = 1; //currently somewhat arbitrary
var familiarityThreshold = 0.4;
var senseOfWhatToDoThreshold = 0.6;
var hintIsHelpfulPlaceholder = true; //currently a dummy value (assumption that hint is always helpful...)
var seedTime = 25;

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
        copy = new Date(e.data.tool_data.tool_event_time);
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

function secondsSince(initTime){	
	var currTime = new Date(e.data.tool_data.tool_event_time);
	diff = currTime.getTime() - initTime.getTime();
	//console.log("time elapsed: ", diff/1000)
	return (diff / 1000);
}

function checkTimeElapsed(initTime) {
  	var timeDiff = secondsSince(initTime);
  	var currTimeMessage = detector_output.value.split(',')[1];
  	//console.log(currTimeMessage);
	if( timeDiff > (300-seedTime)){ 
		if (currTimeMessage!=" > 5 min"){ 
	      detector_output.history = JSON.stringify([attemptWindow, initTime, lastTrigger, onboardSkills]);
	      detector_output.value = "1, > 5 min, " + elaborationString;
	      detector_output.time =  new Date(e.data.tool_data.tool_event_time);
		  if (offlineMode==false){
			mailer.postMessage(detector_output);
		}
		  postMessage(detector_output);
		  //console.log("output_data = ", detector_output);  
		}
	}
	else if( timeDiff > (120-seedTime)){ 
		if (currTimeMessage!=" > 2 min"){ 
		  detector_output.history = JSON.stringify([attemptWindow, initTime, lastTrigger, onboardSkills]);
	      detector_output.value = "1, > 2 min, " + elaborationString;
	      detector_output.time = new Date(e.data.tool_data.tool_event_time);
		  if (offlineMode==false){
			mailer.postMessage(detector_output);
		   }
		  postMessage(detector_output);
		  //console.log("output_data = ", detector_output);  
		}
	}
	else if( timeDiff > (60-seedTime)){
	  if (currTimeMessage!=" > 1 min"){ 
		  detector_output.history = JSON.stringify([attemptWindow, initTime, lastTrigger, onboardSkills]);
	      detector_output.value = "1, > 1 min, " + elaborationString;
	      detector_output.time = new Date(e.data.tool_data.tool_event_time);
		  if (offlineMode==false){
			mailer.postMessage(detector_output);
		}
		  postMessage(detector_output);
		  //console.log("output_data = ", detector_output);
	  }  
	}
	else if( timeDiff > (45-seedTime)){ 
		if (currTimeMessage!=" > 45 s"){ 
		  detector_output.history = JSON.stringify([attemptWindow, initTime, lastTrigger, onboardSkills]);
	      detector_output.value = "1, > 45 s, " + elaborationString;
	      detector_output.time = new Date(e.data.tool_data.tool_event_time);
		  if (offlineMode==false){
			mailer.postMessage(detector_output);
		}
		  postMessage(detector_output);
		  //console.log("output_data = ", detector_output);  
		}
	}
	else{
		//console.log(currTimeMessage == " > " + seedTime.toString() + " s");
		if(currTimeMessage!=" > " + seedTime.toString() + " s"){
		  detector_output.history = JSON.stringify([attemptWindow, initTime, lastTrigger, onboardSkills]);
	      detector_output.value = "1, > " + seedTime.toString() + " s, " + elaborationString;
	      detector_output.time = new Date(e.data.tool_data.tool_event_time);
		  if (offlineMode==false){
			mailer.postMessage(detector_output);
		}
		  postMessage(detector_output);
		  //console.log("output_data = ", detector_output);
		}  
	}
}


//##############

function is_gaming(e){
	return false;
}

function is_abusing_hints(help_model_output){
	if (help_model_output == "hint abuse"){
		return true;
	}
	else{
		return false;
	}
}

function is_not_deliberate(help_model_output){
	if (help_model_output == "not deliberate"){
		return true;
	}
	else{
		return false;
	}
}


//
//
//
//############
//############
//############
//############
//
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
	//console.log("time elapsed: ", diff/1000)
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
	        if (parseFloat(rawSkills[property]['p_know'])<=familiarityThreshold){
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
	        if (parseFloat(rawSkills[property]['p_know'])>=familiarityThreshold){
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
	        if (parseFloat(rawSkills[property]['p_know'])<=familiarityThreshold){
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
	        if (parseFloat(rawSkills[property]['p_know'])<=senseOfWhatToDoThreshold){
	        	return false;
	        }
	    }
	}
	return true;
}

//////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////


//evaluation of each step
function evaluateAction(e){
	var sel = e.data.tutor_data.selection;
	var outcome = e.data.tutor_data.action_evaluation.toLowerCase();

	if (e.data.tutor_data.action_evaluation.toLowerCase() == "hint"){
		//console.log("isHint")
		if (isDeliberate(e)){
			//console.log("isDeliberate")
			if (isLowSkillStep_Some(e) && !lastActionIsError(e)){ //possible modifications...unless wheel-spinning? ... or even closer to Ido's would be to get rid of the "unless last step was error" qualification?
				return("not acceptable/asked hint on low skill step");
			}
			else{
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
					return "hint abuse";
				}
			}
		}
		else{
			//console.log("not deliberate")
			return "hint abuse";
		}

	}
	else{
		if (isLowSkillStep_Some(e) && !lastActionIsError(e)){ //possible modifications...unless wheel-spinning? ... or even closer to Ido's would be to get rid of the "unless last step was error" qualification?
				return("preferred/try step on low skill step");
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
						return "ask teacher for help";
					}
				}
				else{
					return "hint avoidance";
				}
			}
			else{
				return "not deliberate";
			}
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
	else if (e.data.tutor_data.action_evaluation.toLowerCase() == "incorrect"){
		help_variables.lastAction = "error";
	}
	else{
		help_variables.lastAction = "correct";	
	}

	help_variables.lastSenseOfWhatToDo = senseOfWhatToDo(e);

}


//////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////

function receive_transaction( thisTransaction ){
	//e is the data of the transaction from mailer from transaction assembler
	e=thisTransaction;
	//set conditions under which transaction should be processed 
	//(i.e., to update internal state and history, without 
	//necessarily updating external state and history)
	if(e.data.actor == 'student' && e.data.tool_data.selection !="done" && e.data.tool_data.action != "UpdateVariable"){
		//do not touch
		rawSkills = e.data.tutor_data.skills;
		var currSkills = [];
		for (var property in rawSkills) {
		    if (rawSkills.hasOwnProperty(property)) {
		        currSkills.push(rawSkills[property].name + "/" + rawSkills[property].category)
		    }
		}
		detector_output.skill_names = currSkills;
		detector_output.step_id = e.data.tutor_data.step_id;

		//custom processing (insert code here)

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

				//console.log(onboardSkills[skill]["p_know"]);


				if (e.data.tutor_data.action_evaluation.toLowerCase()=="correct"){
					var p_know_given_obs = (p_know_tminus1*(1-p_slip))/( (p_know_tminus1*(1-p_slip)) + ((1-p_know_tminus1)*p_guess) );
				}
				else{
					var p_know_given_obs = (p_know_tminus1*p_slip)/( (p_know_tminus1*p_slip) + ((1-p_know_tminus1)*(1-p_guess)) );
				}
				
				onboardSkills[skill]["p_know"] = p_know_given_obs + (1 - p_know_given_obs)*p_transit;

				//following TutorShop, round down to two decimal places
				onboardSkills[skill]["p_know"] = Math.floor(onboardSkills[skill]["p_know"] * 100) / 100;

				//console.log("engine BKT: ", e.data.tutor_data.skills[0].pKnown);
				//console.log(onboardSkills[skill]["p_know"]);
			}

		}

		//keep track of num attempts on each step
		if(currStep in stepCounter){
			stepCounter[currStep] += 1;
		}
		else{
			stepCounter[currStep] = 1;
		}

		//###########

		if (help_variables.lastAction!="null"){
			help_model_output = evaluateAction(e);
		}
		else{
			help_model_output = "preferred"; //first action in whole tutor is set to "preferred" by default
		}

		//console.log(help_model_output);


		var isGaming = is_gaming(e);
		var isAbusingHints = is_abusing_hints(help_model_output);
		var isNotDeliberate = is_not_deliberate(help_model_output);

		//console.log(isGaming);
		//console.log(isAbusingHints);
		//console.log(isNotDeliberate);

		attemptWindow.shift();
		attemptWindow.push( (isGaming || isAbusingHints || isNotDeliberate) ? 1 : 0 );
		var sumAskTeacherForHelp = attemptWindow.reduce(function(pv, cv) { return pv + cv; }, 0);
		//console.log(attemptWindow);

		updateHistory(e);

	}

	//set conditions under which detector should update
	//external state and history
	if(e.data.actor == 'student' && e.data.tool_data.selection !="done" && e.data.tool_data.action != "UpdateVariable"){
		detector_output.time = new Date(e.data.tool_data.tool_event_time);
		detector_output.transaction_id = e.data.transaction_id;

		//custom processing (insert code here)

		if(isGaming){lastTrigger="isGaming"}
		else if(isAbusingHints){lastTrigger="isAbusingHints"}
		else{lastTrigger="isNotDeliberate?"}

		//   elaboration string
		if (sumAskTeacherForHelp>=threshold){
			if (lastTrigger=="isGaming"){
				elaborationString = "frequent guessing, not trying?";
			}
			else if (lastTrigger=="isAbusingHints"){
				elaborationString = "abusing hints?";
			}
			else{
				elaborationString = "fast attempts in a row, not deliberate?";
			}
		}
		else{
			elaborationString = " ";
		}

		if (detector_output.value.split(',')[0]=="0" && (sumAskTeacherForHelp >= threshold)){
			initTime = new Date(e.data.tool_data.tool_event_time);
			detector_output.history = JSON.stringify([attemptWindow, initTime, lastTrigger, onboardSkills]);
			detector_output.value = "1, > " + seedTime.toString() + " s, " + elaborationString;
			detector_output.time = new Date(e.data.tool_data.tool_event_time);

			console.log("TRUE");
			//intervalID = setInterval( function() { checkTimeElapsed(initTime);} , 3000);

		}
		else if (detector_output.value.split(',')[0]!="0" && (sumAskTeacherForHelp >= threshold)){
			checkTimeElapsed(initTime);
			detector_output.history = JSON.stringify([attemptWindow, initTime, lastTrigger, onboardSkills]);
			detector_output.time = new Date(e.data.tool_data.tool_event_time);
		}
		else{
			detector_output.value = "0, > 0 s, " + elaborationString;
			detector_output.history = JSON.stringify([attemptWindow, initTime, lastTrigger, onboardSkills]);
			detector_output.time = new Date(e.data.tool_data.tool_event_time);

			//clearInterval(intervalID);
		}

		if (offlineMode==false){
			mailer.postMessage(detector_output);
		}
		postMessage(detector_output);
		//console.log("output_data = ", detector_output);
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
		help_model_output = "";
		help_variables = {"lastAction": "null",
					  "lastActionTime": "",
					  "seenAllHints": {},
					  "lastHintLength": "",
					  "lastSenseOfWhatToDo": false
					 };
		elaborationString = " ";

    break;
    case "offlineNewStudent":
    	console.log("new student!");
    	detector_output.category = event.data.studentId;
    	detector_output.history = "";
		detector_output.value = "0, > 0 s";
		attemptWindow = Array.apply(null, Array(windowSize)).map(Number.prototype.valueOf,0);
		initTime = "";
		lastTrigger = "";
		onboardSkills = {};
		stepCounter = {};
		help_model_output = "";
		help_variables = {"lastAction": "null",
					  "lastActionTime": "",
					  "seenAllHints": {},
					  "lastHintLength": "",
					  "lastSenseOfWhatToDo": false
					 };
		elaborationString = " ";

    break;
    case "connectMailer":
		mailer = event.ports[0];
		mailer.onmessage = receive_transaction;
	break;
	case "initialize":
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
		detectorForget = false;
		//
		//

		if (detectorForget){
			detector_output.history = "";
			detector_output.value = 0;
		}


		//optional: If any global variables are based on remembered values across problem boundaries,
		// these initializations should be written here
		//
		//
		if (detector_output.history == "" || detector_output.history == null){
			//in the event that the detector history is empty,
			//initialize variables to your desired 'default' values
			//
			attemptWindow = Array.apply(null, Array(windowSize)).map(Number.prototype.valueOf,0);
			onboardSkills = {};
		}
		else{
			//if the detector history is not empty, you can access it via:
			//     JSON.parse(detector_output.history);
			//...and initialize your variables to your desired values, based on 
			//this history
			//
			var all_history = JSON.parse(detector_output.history);
			attemptWindow = all_history[0];
			initTime = new Date(all_history[1]);
			lastTrigger = all_history[2];
			onboardSkills = all_history[3];

			if(detector_output.value.split(',')[0]!="0"){
				//intervalID = setInterval( function() { checkTimeElapsed(initTime);} , 3000);
			}
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