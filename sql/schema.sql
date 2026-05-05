/*---------------------------------------------------------------*
 | Authors: Gabriel I. Hernandez (gabehernandez07@arizona.edu)   |
 |          Andrew Barnica (asbarnica@arizona.edu)               |
 |          Tom Giallanza (giallanza1@arizona.edu)               |
 |          Helena Musial (helenamusial@arizona.edu)             |
 | File Name: schema.sql                                         |
 | Creation Date: 05-02-2026                                     |
 | Description: SQL queries to create the tables associated with |
 |     our relations. Has correct PK-FK relationships and the    |
 |     appropriate constraints.                                  |
 | Last Modification: 05-04-2026                                 |
 *---------------------------------------------------------------*/
-- Ranges for varchar2, number, and using integer can be changed

----------------------------
-- Table Creation Queries --
----------------------------
/*-------------------------------*
 | MembershipTier Table Creation |
 *-------------------------------*/
CREATE TABLE MembershipTier (
    tierID            INTEGER      PRIMARY KEY,
    "name"            VARCHAR2(50) NOT NULL, -- Added quotes to avoid using a keyword, might be doing too much though
    maxMessagesPerDay Number(6)    NOT NULL, -- Guessing 999,999 max messages is enough
    proModelStatus    Number(1)    NOT NULL, -- True or false (1 or 0)
    monthlyFee        Number(10,2) NOT NULL  -- Accounting for cents
);

/*--------------------------------*
 | ApplicationUser Table Creation |
 *--------------------------------*/
CREATE TABLE ApplicationUser (
    userID            INTEGER      PRIMARY KEY,
    tierID            INTEGER, -- FK to MembershipTier
    "name"            VARCHAR2(50) NOT NULL,
    email             VARCHAR2(50) NOT NULL UNIQUE,
    creationDate      DATE         NOT NULL,
    preferredLanguage VARCHAR2(20) NOT NULL,
    CONSTRAINT FK_UserTier FOREIGN KEY (tierID)
        REFERENCES MembershipTier(tierID) ON DELETE SET NULL -- Don't want to delete user if a tier gets removed
);

/*-------------------------------*
 | BillingProfile Table Creation |
 *-------------------------------*/
CREATE TABLE BillingProfile (
    billingID      INTEGER     PRIMARY KEY,
    userID         INTEGER, -- FK to AppUser
    paymentMethod  VARCHAR(15) NOT NULL,
    billingAddress VARCHAR(50) NOT NULL,
    CONSTRAINT FK_BillingUser FOREIGN KEY (userID)
        REFERENCES ApplicationUser(userID) ON DELETE CASCADE -- If userID deleted then delete billing profile too
);

/*------------------------*
 | Invoice Table Creation |
 *------------------------*/
CREATE TABLE Invoice (
    invoiceID     INTEGER      PRIMARY KEY,
    userID        INTEGER, -- FK to AppUser
    amount        NUMBER(10,2) NOT NULL, -- Accounting for cents
    invoiceDate   DATE         NOT NULL,
    paymentStatus VARCHAR(10)  NOT NULL, -- Changed to be like "unpaid"/"paid"/ and maybe "pending"
    CONSTRAINT FK_InvoiceUser FOREIGN KEY (userID)
        REFERENCES ApplicationUser(userID) ON DELETE CASCADE
);

/*-----------------------------*
 | SupportAgent Table Creation |
 *-----------------------------*/
CREATE TABLE SupportAgent (
    agentID    INTEGER      PRIMARY KEY,
    name       VARCHAR2(50) NOT NULL,
    email      VARCHAR2(50) NOT NULL UNIQUE
);

/*------------------------------*
 | SupportTicket Table Creation |
 *------------------------------*/
CREATE TABLE SupportTicket (
    ticketID       INTEGER        PRIMARY KEY,
    userID         INTEGER, -- FK to AppUser
    agentID        INTEGER, -- FK to SuppAgent
    topic          VARCHAR2(1000) NOT NULL,
    dateOpened     DATE           NOT NULL,
    resolutionDays NUMBER(5),
    outcome        VARCHAR2(1000),
    CONSTRAINT FK_UserTicket FOREIGN KEY (userID)
        REFERENCES ApplicationUser(userID) ON DELETE CASCADE,
    CONSTRAINT FK_AgentTicket FOREIGN KEY (agentID)
        REFERENCES SupportAgent(agentID) ON DELETE SET NULL
);

/*--------------------------*
 | Workspace Table Creation |
 *--------------------------*/
CREATE TABLE Workspace (
    workspaceID   INTEGER      PRIMARY KEY,
    creatorID     INTEGER, -- FK to AppUser
    "name"        VARCHAR2(50) NOT NULL,
    privateStatus Number(1)    NOT NULL,
    creationDate  DATE         NOT NULL,
    CONSTRAINT FK_CreatorWorkspace FOREIGN KEY (creatorID)
        REFERENCES ApplicationUser(userID) ON DELETE SET NULL
);

/*------------------------------------*
 | WorkspaceMembership Table Creation |
 *------------------------------------*/
CREATE TABLE WorkspaceMembership (
    userID      INTEGER NOT NULL, -- FK to AppUser
    workspaceID INTEGER NOT NULL, -- FK to Workspace
    dateJoined  DATE    NOT NULL,
    CONSTRAINT  PK_WorkspaceMembership PRIMARY KEY (userID, workspaceID),
    CONSTRAINT  FK_UserWM FOREIGN KEY (userID)
        REFERENCES ApplicationUser(userID) ON DELETE CASCADE,
    CONSTRAINT  FK_WM_Workspace FOREIGN KEY (workspaceID)
        REFERENCES Workspace(workspaceID) ON DELETE CASCADE
);

/*------------------------*
 | Persona Table Creation |
 *------------------------*/
CREATE TABLE Persona (
    personalID   INTEGER PRIMARY KEY,
    creatorID    INTEGER, -- FK to UserApp
    "name"       VARCHAR2(50)  NOT NULL,
    instructions VARCHAR2(500) NOT NULL,
    creationDate DATE          NOT NULL,
    CONSTRAINT FK_PersonaUser FOREIGN KEY (creatorID)
        REFERENCES ApplicationUser(userID) ON DELETE CASCADE
);

/*-----------------------------*
 | Conversation Table Creation |
 *-----------------------------*/
CREATE TABLE Conversation (
    conversationID INTEGER PRIMARY KEY,
    userID         INTEGER, -- FK to AppUser
    workspaceID    INTEGER, -- FK to Workspace
    personaID      INTEGER, -- FK to Persona
    title          VARCHAR2(50) NOT NULL,
    creationDate   DATE         NOT NULL,
    activeStatus   NUMBER(1)    NOT NULL,
    CONSTRAINT FK_ConversationUser FOREIGN KEY (userID)
        REFERENCES ApplicationUser(userID) ON DELETE CASCADE,
    CONSTRAINT FK_ConversationWorkSpace FOREIGN KEY (workspaceID)
        REFERENCES Workspace(workspaceID) ON DELETE SET NULL,
    CONSTRAINT FK_ConversationPersona FOREIGN KEY (personaID)
        REFERENCES Persona(personalID) ON DELETE SET NULL
);

/*------------------------*
 | Message Table Creation |
 *------------------------*/
CREATE TABLE Message (
    messageID      INTEGER        NOT NULL,
    conversationID INTEGER        NOT NULL, -- FK to Conversation
    role           VARCHAR2(20)   NOT NULL,
    content        VARCHAR2(1000) NOT NULL,
    timeSent       TIMESTAMP      NOT NULL,
    CONSTRAINT PK_Message PRIMARY KEY (messageID, conversationID),
    CONSTRAINT FK_MessageConversation FOREIGN KEY (conversationID)
        REFERENCES Conversation(conversationID) ON DELETE CASCADE
);

/*-------------------------*
 | Feedback Table Creation |
 *-------------------------*/
CREATE TABLE Feedback (
    feedbackID     INTEGER        NOT NULL,
    messageID      INTEGER        NOT NULL, -- FK to Message
    conversationID INTEGER,                 -- Added new FK for composite Message PK
    rating         VARCHAR(15)    NOT NULL, -- e.g. Thumbs Up/Thumbs Down
    feedbackText   VARCHAR2(1000) NOT NULL,
    timeSubmitted  TIMESTAMP      NOT NULL,
    CONSTRAINT PK_Feedback PRIMARY KEY (feedbackID, messageID),
    CONSTRAINT FK_FeedbackMessage FOREIGN KEY (messageID, conversationID)
        REFERENCES Message(messageID, conversationID) ON DELETE CASCADE
);

/*-------------------------*
 | Bookmark Table Creation |
 *-------------------------*/
CREATE TABLE Bookmark (
    userID         INTEGER NOT NULL, -- FK to AppUser
    messageID      INTEGER NOT NULL, -- FK to Message
    conversationID INTEGER,          -- Like for feedback, added FK for composite Message PK
    timeBookmarked TIMESTAMP,
    CONSTRAINT PK_Bookmark PRIMARY KEY (userID, messageID), -- Added combined primary key
    CONSTRAINT FK_BookmarkUser FOREIGN KEY (userID)
        REFERENCES ApplicationUser(userID) ON DELETE CASCADE,
    CONSTRAINT FK_BookmarkMessage FOREIGN KEY (messageID, conversationID)
        REFERENCES Message(messageID, conversationID) ON DELETE CASCADE
);

/*-------------------------------*
 | PromptTemplate Table Creation |
 *-------------------------------*/
CREATE TABLE PromptTemplate (
    templateID    INTEGER PRIMARY KEY,
    creatorID     INTEGER, -- FK to AppUser
    workspaceID   INTEGER, -- FK to Workspace
    title         VARCHAR2(50)   NOT NULL,
    content       VARCHAR2(1000) NOT NULL,
    category      VARCHAR2(50)   NOT NULL,
    privateStatus NUMBER(1)      NOT NULL,
    creationDate  DATE,
    CONSTRAINT FK_PromptTemplateUser FOREIGN KEY (creatorID)
        REFERENCES ApplicationUser(userID) ON DELETE CASCADE,
    CONSTRAINT FK_PromptTemplateWorkspace FOREIGN KEY (workspaceID)
        REFERENCES Workspace(workspaceID) ON DELETE SET NULL
);

-------------------------------
-- Sequence Creation Queries --
-------------------------------
CREATE SEQUENCE SEQ_TIER           START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_USER           START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_BILLING        START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_TIER           START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_AGENT          START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_TICKET         START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_WORKSPACE      START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_MEMBERSHIP     START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_PERSONA        START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_CONVERSATION   START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_MESSAGE        START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_FEEDBACK       START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_BOOKMARK       START WITH 1 INCREMENT BY 1
CREATE SEQUENCE SEQ_PROMPTTEMPLATE START WITH 1 INCREMENT BY 1
