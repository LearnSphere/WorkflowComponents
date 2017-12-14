//detector template

//add output variable name below
var variableName = "critical_struggle"

//initializations (do not touch)
var detector_output = {name: variableName,
						category: "Dashboard", 
						value: "0, > 0 s",
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
var attemptWindow;
var skillLevelsAttempts;
var intervalID;
var onboardSkills;
var stepCounter = {};

//declare and/or initialize any other custom global variables for this detector here...
var initTime;
var elaborationString = " ";
//
//[optional] single out TUNABLE PARAMETERS below
var windowSize = 6;
var threshold = 1;
var mastery_threshold = 0.8;
var BKTparams = {p_transit: 0.2, 
				p_slip: 0.1, 
				p_guess: 0.2, 
				p_know: 0.25};
var wheelSpinningAttemptThreshold = 10; //following Beck and Gong's wheel-spinning work
var seedTime = 25;



function updateSkillLevelsAttempts(e, rawSkills, skill, currStepCount){
	if( skill in skillLevelsAttempts ){
		if(currStepCount==1){
			skillLevelsAttempts[skill][0] += 1;
		}
		skillLevelsAttempts[skill][1] = parseFloat(rawSkills[skill]["p_know"]);
	}
	else{
		if (skill in rawSkills){
			skillLevelsAttempts[skill] = [1, parseFloat(rawSkills[skill]["p_know"])];
		}
	}
}

function detect_wheel_spinning(e, rawSkills, currStepCount){

	for (var skill in skillLevelsAttempts) {
		if ((skillLevelsAttempts[skill][0] >= 10) && (skillLevelsAttempts[skill][1] < mastery_threshold)){
			console.log("is wheel spinning: " + skill.toString() + " " + skillLevelsAttempts[skill].toString());
			return true;
		}
	}
	return false;

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


function secondsSince(initTime){	
	var currTime = new Date();
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
	      detector_output.history = JSON.stringify([attemptWindow, skillLevelsAttempts, initTime, onboardSkills]);
	      detector_output.value = "1, > 10 min, " + elaborationString;
	      detector_output.time = new Date();
		  mailer.postMessage(detector_output);
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);  
		}
	}
	else if( timeDiff > (300-seedTime)){ 
		if (currTimeMessage!=" > 5 min"){ 
	      detector_output.history = JSON.stringify([attemptWindow, skillLevelsAttempts, initTime, onboardSkills]);
	      detector_output.value = "1, > 5 min, " + elaborationString;
	      detector_output.time = new Date();
		  mailer.postMessage(detector_output);
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);  
		}
	}
	else if( timeDiff > (120-seedTime)){ 
		if (currTimeMessage!=" > 2 min"){ 
		  detector_output.history = JSON.stringify([attemptWindow, skillLevelsAttempts, initTime, onboardSkills]);
	      detector_output.value = "1, > 2 min, " + elaborationString;
	      detector_output.time = new Date();
		  mailer.postMessage(detector_output);
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);  
		}
	}
	else if( timeDiff > (60-seedTime)){
	  if (currTimeMessage!=" > 1 min"){ 
		  detector_output.history = JSON.stringify([attemptWindow, skillLevelsAttempts, initTime, onboardSkills]);
	      detector_output.value = "1, > 1 min, " + elaborationString;
	      detector_output.time = new Date();
		  mailer.postMessage(detector_output);
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);
	  }  
	}
	else if( timeDiff > (45-seedTime)){ 
		if (currTimeMessage!=" > 45 s"){ 
		  detector_output.history = JSON.stringify([attemptWindow, skillLevelsAttempts, initTime, onboardSkills]);
	      detector_output.value = "1, > 45 s, " + elaborationString;
	      detector_output.time = new Date();
		  mailer.postMessage(detector_output);
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);  
		}
	}
	else{
		console.log(currTimeMessage == " > " + seedTime.toString() + " s");
		if(currTimeMessage!=" > " + seedTime.toString() + " s"){
		  detector_output.history = JSON.stringify([attemptWindow, skillLevelsAttempts, initTime, onboardSkills]);
	      detector_output.value = "1, > " + seedTime.toString() + " s, " + elaborationString;
	      detector_output.time = new Date();
		  mailer.postMessage(detector_output);
		  postMessage(detector_output);
		  console.log("output_data = ", detector_output);
		}  
	}
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

		//update skill attempts
		for (var i in currSkills){
			var thisSkill = currSkills[i];
			updateSkillLevelsAttempts(e, onboardSkills, thisSkill, stepCounter[currStep]);
		}

		//########################

		var isWheelSpinning = detect_wheel_spinning(e, onboardSkills, stepCounter[currStep]);

		attemptWindow.shift();
		attemptWindow.push( isWheelSpinning ? 1 : 0 );
		var sumAskTeacherForHelp = attemptWindow.reduce(function(pv, cv) { return pv + cv; }, 0);

		console.log(attemptWindow);
		console.log(isWheelSpinning);
		console.log(skillLevelsAttempts);

	}

	//set conditions under which detector should update
	//external state and history
	if(e.data.actor == 'student' && e.data.tool_data.selection !="done" && e.data.tool_data.action != "UpdateVariable"){
		detector_output.time = new Date();
		detector_output.transaction_id = e.data.transaction_id;

		//custom processing (insert code here)

		//   elaboration string
		if (sumAskTeacherForHelp>=threshold){
			elaborationString = "slow to master some skills";
		}
		else{
			elaborationString = " ";
		}


		if (detector_output.value.split(',')[0]=="0" && (sumAskTeacherForHelp >= threshold)){
			initTime = new Date();
			detector_output.history = JSON.stringify([attemptWindow, skillLevelsAttempts, initTime, onboardSkills]);
			detector_output.value = "1, > " + seedTime.toString() + " s, " + elaborationString;
			detector_output.time = new Date();

			intervalID = setInterval( function() { checkTimeElapsed(initTime);} , 3000);

		}
		else if (detector_output.value.split(',')[0]!="0" && (sumAskTeacherForHelp >= threshold)){
			detector_output.history = JSON.stringify([attemptWindow, skillLevelsAttempts, initTime, onboardSkills]);
			detector_output.time = new Date();
		}
		else{
			detector_output.value = "0, > 0 s, " + elaborationString;
			detector_output.history = JSON.stringify([attemptWindow, skillLevelsAttempts, initTime, onboardSkills]);
			detector_output.time = new Date();

			clearInterval(intervalID);
		}


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
			skillLevelsAttempts = {};
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
			skillLevelsAttempts = all_history[1];
			initTime = new Date(all_history[2]);
			onboardSkills = all_history[3];

			if(detector_output.value.split(',')[0]!="0"){
				intervalID = setInterval( function() { checkTimeElapsed(initTime);} , 3000);
			}
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