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

var init = "";


TransactionMailerUsers.create = function(path, txDestURL, scriptsDestURL, authToken, scriptsInitzer)
{

    console.log("TransactionMailerUsers.create(): at entry, init", init );

    TransactionMailerUsers.mailer = new Worker(path+'/'+TransactionMailerUsers.mailerURL);
    
    //listen for detector results
    TransactionMailerUsers.mailer.addEventListener("message", function(e) {
        console.log("sending new message to initWorker: " + JSON.stringify(e.data));
        parent.parent.initWorker.port.postMessage(["newDetectorResult", e.data]);
    }, false);

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
 
    //initialization
    parent.parent.initWorker.port.addEventListener("message", function(e) {
        if (e.data[0] == "init"){
            init = e.data[1]; 

            for(var i = 0; i < TransactionMailerUsers.scripts.length; ++i)
            {
            var s = path + '/' + TransactionMailerUsers.scripts[i];
            var detector = new Worker(s);
            var mc = new MessageChannel();
            TransactionMailerUsers.mailer.postMessage({ command: "connectDetector" }, [ mc.port1 ]);
            detector.postMessage({ command: "connectMailer" }, [ mc.port2 ]);

            detector.postMessage({ command: "initialize", initializer: init });
                console.log("TransactionMailerUsers.create(): sent command: initialize, init ", init );
            
            TransactionMailerUsers.active.push(detector);
            console.log("TransactionMailerUsers.create(): s, active["+i+"]=", s, TransactionMailerUsers.active[i]);
            }
        }
        if (e.data[0] == "newDetectorResult"){
            console.log("from initWorker: " + JSON.stringify(e.data));
        }

    }, false);

    parent.parent.initWorker.port.postMessage(["getInit", 0]);

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
