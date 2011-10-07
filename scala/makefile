
# It is dangerous to have Scala weaving and GWT in one project,
# therefore the Scala server classes are compiled from a separate Eclipse project.
# workspace/Grond-client - GAE, GWT, libraries, GIT
# workspace/Grond-server - Scala

SERVER = ../../Grond-server
CLIENT = ..

all: $(SERVER)/bin/gwt.rpc $(CLIENT)/war/WEB-INF/lib/grond-server.jar

$(SERVER)/bin/gwt.rpc: $(CLIENT)/war/grond/*.gwt.rpc
	cat $(CLIENT)/war/grond/*.gwt.rpc > $(SERVER)/bin/gwt.rpc

$(CLIENT)/war/WEB-INF/lib/grond-server.jar: $(SERVER)/bin/gwt.rpc $(CLIENT)/scala/grond/htmlunit/* $(CLIENT)/scala/grond/model/* $(CLIENT)/scala/grond/server/*
	cd $(SERVER)/bin && jar cf grond-server.jar grond gwt.rpc
	mv $(SERVER)/bin/grond-server.jar $(CLIENT)/war/WEB-INF/lib/
