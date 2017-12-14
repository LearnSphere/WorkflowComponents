TransactionMailerUsers =
{
    process_transactions_url: "",
    process_detectors_url: "",
    authenticity_token: "",
    mailerURL: "mail-worker.js",
    mailer: null,
    mailerPort: null,
    scripts: ["Detectors/Lumilo/idle.js", 
    "Detectors/Lumilo/system_misuse.js", 
    "Detectors/Lumilo/struggle__moving_average.js", 
    "Detectors/Lumilo/student_doing_well__moving_average.js", 
    "Detectors/Lumilo/critical_struggle.js", 
    "Detectors/Lumilo/invisible_hand_raise.js",
    "Detectors/current_problem_history.js",
    "Detectors/BKT_skill_and_error_tracker.js"],
    active: []
};

TransactionMailerUsers.create = function(path, txDestURL, scriptsDestURL, authToken, scriptsInitzer)
{
    console.log("TransactionMailerUsers.create(): at entry, scriptsInitzer ", scriptsInitzer );

    TransactionMailerUsers.mailer = new Worker(path+'/'+TransactionMailerUsers.mailerURL);
    
    TransactionMailerUsers.mailer.postMessage({ command: "process_transactions_url", "process_transactions_url": txDestURL, "process_detectors_url": scriptsDestURL, "authenticity_token": authToken});
    TransactionMailerUsers.process_transactions_url = txDestURL;
    TransactionMailerUsers.authenticity_token = authToken;
    TransactionMailerUsers.process_detectors_url = scriptsDestURL;

    var channel = new MessageChannel();
    TransactionMailerUsers.mailer.postMessage(
            { command: "connectTransactionAssembler" },
            [ channel.port1 ]
    );
    TransactionMailerUsers.mailerPort = channel.port2;
    TransactionMailerUsers.mailerPort.onmessage = function(event) {
            console.log("From mailer: "+event);
    };

    for(var i = 0; i < TransactionMailerUsers.scripts.length; ++i)
    {
	var s = path + '/' + TransactionMailerUsers.scripts[i];
	var detector = new Worker(s);
	var mc = new MessageChannel();
	TransactionMailerUsers.mailer.postMessage({ command: "connectDetector" }, [ mc.port1 ]);
	detector.postMessage({ command: "connectMailer" }, [ mc.port2 ]);
	if(scriptsInitzer)
	{
	    detector.postMessage({ command: "initialize", initializer: scriptsInitzer });
	    console.log("TransactionMailerUsers.create(): sent command: initialize, scriptsInitzer ", scriptsInitzer );
	}
	TransactionMailerUsers.active.push(detector);
	console.log("TransactionMailerUsers.create(): s, active["+i+"]=", s, TransactionMailerUsers.active[i]);

    }
    return TransactionMailerUsers;
};

TransactionMailerUsers.sendTransaction = function(tx)
{
    TransactionMailerUsers.mailerPort.postMessage(tx);  // post to listener in other thread

    var tmUsers = TransactionMailerUsers.active;
    for(var i = 0; i < tmUsers; ++i)
    {
	tmUsers[i].postMessage(tx);
    }
};
