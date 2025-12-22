/* Initial beliefs and rules */
time(5000).

/* Initial goals */
!start.

/* Plans */

+!start : true <-
    .my_name(S);
    .print("Agente ", S, " pronto al lavoro!");
    !wait_timer_request;
    !send_restart_notification;
    !start.

+!wait_timer_request: other(Sender) <-
    .wait({ +start[source(Sender)] });
    -start[source(Sender)];
    .print("Start timer request from ", Sender);
    !waitSem.

 +!waitSem <-
     while(time(X) & X > 0) {
         .print(X);
         !!updateTimerPlan(X);
         .wait(1000);
         -+time(X - 1000);
     };
     -+time(5000).

+!send_restart_notification: other(Receiver) <-
    !sendMessageTo(restart, Receiver).

+!updateTimerPlan(X) <-
    updateTimer(X).

+!sendMessageTo(Message, Receiver) <-
  .print("Sending ", Message, " to ", Receiver);
  .send(Receiver, tell, Message).