OJDBC = /usr/lib/oracle/19.8/client64/lib/ojdbc8.jar
SRC   = src/ConversationManager.java src/DBConnection.java src/PersonaManager.java \
        src/Prog4.java src/PromptManager.java src/QueryManager.java \
        src/TicketManager.java src/UserManager.java src/WorkspaceManager.java

build: build/Prog4.class

build/Prog4.class: $(SRC)
	mkdir -p build
	javac -cp $(OJDBC) -d build $(SRC)

run: build
	java -cp build:$(OJDBC) Prog4

clean:
	rm -rf build
