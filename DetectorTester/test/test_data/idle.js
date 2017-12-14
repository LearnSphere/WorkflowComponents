//detector template
//console.log("in idle.js");
//process.stdout.write("in idle.js");
//add output variable name below
var variableName = "idle"

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
//
//
//
//
//


//declare and/or initialize any other custom global variables for this detector here
var initTime;
var timerId4;
var timerId5;

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
	    if (offlineMode==false){
		    clearTimeout(timerId4);
		    clearTimeout(timerId5);
		    detector_output.history = e.data.tool_data.tool_event_time
			detector_output.value = "0, > 0 s"
			detector_output.time = new Date(e.data.tool_data.tool_event_time);
			mailer.postMessage(detector_output);
			postMessage(detector_output);
			console.log("output_data = ", detector_output);
		}
		else{
			var currTime = new Date(e.data.tool_data.tool_event_time);
			if (initTime != ""){
				diff = currTime.getTime() - initTime.getTime();
				diff = diff/1000;

				if (diff > 275){
					detector_output.history = e.data.tool_data.tool_event_time
					detector_output.value = "1, > 5 min"
					detector_output.time = new Date(e.data.tool_data.tool_event_time);
				}
				else if (diff > 95){
					detector_output.history = e.data.tool_data.tool_event_time
					detector_output.value = "1, > 2 min"
					detector_output.time = new Date(e.data.tool_data.tool_event_time);
				}
			}
			else{
				detector_output.history = e.data.tool_data.tool_event_time
				detector_output.value = "0, > 0 min"
				detector_output.time = new Date(e.data.tool_data.tool_event_time);
			}

			initTime = new Date(e.data.tool_data.tool_event_time);

		}


	}

	//set conditions under which detector should update
	//external state and history
	if(e.data.actor == 'student' && e.data.tool_data.action != "UpdateVariable"){

		detector_output.time = new Date(e.data.tool_data.tool_event_time);
		detector_output.transaction_id = e.data.transaction_id;

		//custom processing (insert code here)
		if (offlineMode==false){
		    timerId4 = setTimeout(function() { 
		      detector_output.history = e.data.tool_data.tool_event_time
		      detector_output.value = "1, > 2 min"
		      detector_output.time = new Date(e.data.tool_data.tool_event_time);
			  mailer.postMessage(detector_output);
			  postMessage(detector_output);
			  console.log("output_data = ", detector_output);  }, 
		      120000)
		    timerId5 = setTimeout(function() { 
		      detector_output.history = e.data.tool_data.tool_event_time
		      detector_output.value = "1, > 5 min"
		      detector_output.time = new Date(e.data.tool_data.tool_event_time);
			  mailer.postMessage(detector_output);
			  postMessage(detector_output);
			  console.log("output_data = ", detector_output);  }, 
		      300000)
		}
		else if (detector_output.value.split(',')[0]=="1"){
			postMessage(detector_output);
		}


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
    	//console.log("new problem!");
    	initTime = "";
    break;
    case "offlineNewStudent":
    	//console.log("new student!");
    	detector_output.category = event.data.studentId;
    	detector_output.history = "";
		detector_output.value = "0, > 0 s";
		initTime = "";
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
		detectorForget = true;
		//
		//

		if (detectorForget){
			detector_output.history = "onLoad";
			detector_output.value = "0, > 0 s";
		}


		detector_output.time = new Date(event.data.tool_data.tool_event_time);
		mailer.postMessage(detector_output);
		postMessage(detector_output);
		console.log("output_data = ", detector_output);


		if (offlineMode==false){
		    timerId4 = setTimeout(function() { 
		      detector_output.history = "onLoad"
		      detector_output.value = "1, > 2 min"
		      detector_output.time = new Date(event.data.tool_data.tool_event_time);
			  mailer.postMessage(detector_output);
			  postMessage(detector_output);
			  console.log("output_data = ", detector_output);  }, 
		      120000)
		    timerId5 = setTimeout(function() { 
		      detector_output.history = "onLoad"
		      detector_output.value = "1, > 5 min"
		      detector_output.time = new Date(event.data.tool_data.tool_event_time);
			  mailer.postMessage(detector_output);
			  postMessage(detector_output);
			  console.log("output_data = ", detector_output);  }, 
		      300000)
		}

	break;
    default:
	break;

    }

}