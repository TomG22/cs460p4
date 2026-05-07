Steps for building and running the app:
You can use "make run" to run the app immediately or "make build" to build and then run.
If you want to re build the app, use "make clean" beforehand.

Enter your oracle username and password when prompted and then follow the menu in the command line to interact with the app.

The options for our app are laid out as follows:
1. User accounts
  1. Add a user
  2. Update an existing user's membership tier
  3. Delete a user
2. Conversations & messages
  1. Start a new conversation
  2. Add a message to a conversation
  3. Update message feedback
3. Workspaces
  1. Create workspace
  2. Modify workspace name
  3. Move conversation to workspace
4. Personas
  1. Create a persona
  2. Delete a persona
5. Prompt library
  1. Add a prompt template
  2. Update a prompt template
6. Subscription tracking
  1. Update subscription
  2. See a user's rate limit
7. Billing & invoices
  1. Generate new Invoice
  2. Mark Invoice Paid
8. Support tickets
  1. Create ticket
  2. Assign ticket to agent
  3. Resolve or escalate ticket
9. Run a query
  1. Bookmarked messages for a user
  2. Users with unpaid invoices
  3. Most helpful persona
  4. Message activity for a persona
0. Quit


Workload distribution:
Gabe: Database and Infrastructure
DBConnection.java
Prog4.java
schema.sql
data.sql

Andrew: Users, Subscriptions, Billing (Functionalities 1, 6, 7)
UserManager.java
Normalization analysis

Tom: Conversations, Messages, Personas, Prompts (Functionalities 2, 4, 5)
ConversationManager.java
PersonaManager.java
PromptManager.java
ReadMe.txt

Helena: Workspaces, Tickets, Queries (Functionalities 3, 8, Queries 1-4)
WorkspaceManager.java
TicketManager.java
QueryManager.java
Conceptual + logical db design
Query description
