//detector template

//add output variable name below
var variableName = "BKT"

//initializations (do not touch)
var detector_output = {name: variableName,
						category: "Dashboard", 
						value: "",
						history: {},
						skill_names: "",
						step_id: "",
						transaction_id: "",
						time: ""
						};
var mailer;


//declare any custom global variables that will be initialized 
//based on "remembered" values across problem boundaries, here
// (initialize these at the bottom of this file, inside of self.onmessage)
var BKTparams = {p_transit: 0.2, 
				p_slip: 0.1, 
				p_guess: 0.2, 
				p_know: 0.25};

//declare and/or initialize any other custom global variables for this detector here...
var pastSteps = {};
//
//[optional] single out TUNABLE PARAMETERS below
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
		var currStep = e.data.tool_data.selection;
		for (var i in currSkills){
			var skill = currSkills[i];

			if(!(currStep in pastSteps)){
				if (!(skill in detector_output.history)){	//if this skill has not been encountered before
					detector_output.history[skill] = clone(BKTparams);
				}

				var p_know_tminus1 = detector_output.history[skill]["p_know"];
				var p_slip = detector_output.history[skill]["p_slip"];
				var p_guess = detector_output.history[skill]["p_guess"];
				var p_transit = detector_output.history[skill]["p_transit"];

				console.log(detector_output.history[skill]["p_know"]);


				if (e.data.tutor_data.action_evaluation.toLowerCase()=="correct"){
					var p_know_given_obs = (p_know_tminus1*(1-p_slip))/( (p_know_tminus1*(1-p_slip)) + ((1-p_know_tminus1)*p_guess) );
				}
				else{
					var p_know_given_obs = (p_know_tminus1*p_slip)/( (p_know_tminus1*p_slip) + ((1-p_know_tminus1)*(1-p_guess)) );
				}
				
				detector_output.history[skill]["p_know"] = p_know_given_obs + (1 - p_know_given_obs)*p_transit;

				//following TutorShop, round down to two decimal places
				detector_output.history[skill]["p_know"] = Math.floor(detector_output.history[skill]["p_know"] * 100) / 100;

				detector_output.value = detector_output.history[skill]["p_know"];

				console.log("engine BKT: ", e.data.tutor_data.skills[0].pKnown);
				console.log(detector_output.history[skill]["p_know"]);
			}

		}

		//update # of attempts at step
		if(!(currStep in pastSteps)){
			pastSteps[currStep] = true;
		}

	}

	//set conditions under which detector should update
	//external state and history
	if(e.data.actor == 'student' && e.data.tool_data.action != "UpdateVariable"){
		detector_output.time = new Date();
		detector_output.transaction_id = e.data.transaction_id;
		detector_output.history = JSON.stringify(detector_output.history);
		mailer.postMessage(detector_output);
		postMessage(detector_output);
		console.log("output_data = ", detector_output);
		detector_output.history = JSON.parse(detector_output.history);
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

			detector_output.history = {};
		}
		else{
			//if the detector history is not empty, you can access it via:
			//     JSON.parse(detector_output.history);
			//...and initialize your variables to your desired values, based on 
			//this history
			//
			detector_output.history = JSON.parse(detector_output.history);
		}

	break;
    default:
	break;
    }
}