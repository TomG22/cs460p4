/*--------------------------------*
 | ApplicationUser Table Creation |
 *--------------------------------*/
 -- Ranges for varchar2, number, and using integer can be changed
CREATE TABLE ApplicationUser {
	userID            INTEGER PRIMARY KEY,
	userName          VARCHAR2(50),
	userEmail         VARCHAR2(50),
	creationDate      VARCHAR2(9), -- MM/DD/YYYY
	preferredLanguage VARCHAR2(20),
	tierID            VARCHAR2(50),
    PRIMARY KEY       userID
};

/*-------------------------------*
 | MembershipTier Table Creation |
 *-------------------------------*/
CREATE TABLE MembershipTier {
    tierID            INTEGER PRIMARY KEY,
    tierName          VARCHAR2(50),
    maxMessagesPerDay Number(6), -- Guessing 999,999 max messages is enough
    proModelStatus    Number(1), -- True or false (1 or 0)
    monthlyFee        Number(10,2) -- Accounting for cents
};

/*-------------------------------*
 | BillingProfile Table Creation |
 *-------------------------------*/
CREATE TABLE BillingProfile {
    billingID      INTEGER PRIMARY KEY,
    userID         INTEGER, -- FK to AppUser
    paymentMethod  VARCHAR(15),
    billingAddress VARCHAR(50)
};

/*------------------------*
 | Invoice Table Creation |
 *------------------------*/
CREATE TABLE Invoice {
    invoiceID     INTEGER PRIMARY KEY,
    userID        INTEGER, -- FK to AppUser
    amount        INTEGER,
    invoiceDate   VARCHAR(9), -- MM/DD/YYYY
    paymentStatus NUMBER(1) -- True or false (1 or 0)
};

/*------------------------------*
 | SupportTicket Table Creation |
 *------------------------------*/
CREATE TABLE SupportTicket {
    ticketID       INTEGER PRIMARY KEY,
    userID         INTEGER,     -- FK to AppUser
    agentID        INTEGER,     -- FK to SuppAgent
    topic          VARCHAR2(1000),
    dateOpened     VARCHAR2(9), -- MM/DD/YYYY
    resolutionDays NUMBER(5),
    outcome        VARCHAR2(1000)
};

/*-----------------------------*
 | SupportAgent Table Creation |
 *-----------------------------*/
CREATE TABLE SupportAgent {
    agentID INTEGER PRIMARY KEY,
    agentName VARCHAR2(50),
    agentEmail VARCHAR2(50)
};

/*--------------------------*
 | Workspace Table Creation |
 *--------------------------*/
CREATE TABLE Workspace {
    workspaceID   INTEGER PRIMARY KEY,
    workSpaceName VARCHAR2(50),
    privateStatus Number(1),
    creatorID     INTEGER, -- FK to AppUser
    creationDate  VARCHAR2(9)
};

/*------------------------------------*
 | WorkspaceMembership Table Creation |
 *------------------------------------*/
CREATE TABLE WorkspaceMembership {
    userID      INTEGER,
    workspaceID INTEGER, -- FK to Workspace
    joinDate    VARCHAR2(9)
};

/*-----------------------------*
 | Conversation Table Creation |
 *-----------------------------*/
CREATE TABLE Conversation {
    conversationID INTEGER PRIMARY KEY,
    userID         INTEGER, -- FK to AppUser
    workspaceID    INTEGER, -- FK to Workspace
    personaID      INTEGER, -- FK to Persona
    title          VARCHAR2(50),
    creationDate   VARCHAR2(9),
    activeStatus   NUMBER(1)
};

/*------------------------*
 | Message Table Creation |
 *------------------------*/
CREATE TABLE Message {
    messageID      INTEGER PRIMARY KEY,
    conversationID INTEGER,     -- FK to Conversation
    messageRole    VARCHAR2(20),
    content        VARCHAR2(1000),
    timeSent       VARCHAR2(11) -- XX:XX:XX:AM or XX:XX:XX:PM
};

/*-------------------------*
 | Feedback Table Creation |
 *-------------------------*/
CREATE TABLE Feedback {
    feedbackID INTEGER PRIMARY KEY,
    messageID INTEGER,          -- FK to Message
    rating Number(3),           -- Rating between 0 and 100
    feedbackText VARCHAR2(1000),
    timeSubmitted VARCHAR2(11), -- XX:XX:XX:AM or XX:XX:XX:PM
};

/*-------------------------*
 | Bookmark Table Creation |
 *-------------------------*/
CREATE TABLE Bookmark {
    userID         INTEGER PRIMARY KEY,
    messageID      INTEGER,      -- FK to Message
    timeBookmarked VARCHAR2(11), -- XX:XX:XX:AM or XX:XX:XX:PM
};

/*------------------------*
 | Persona Table Creation |
 *------------------------*/
CREATE TABLE Persona {
    personalID         INTEGER PRIMARY KEY,
    personaName VARCHAR2(50),
    instructions VARCHCAR2(500)
    creatorID INTEGGER, -- FK to UserApp
    creationDat VARCHAR2(11), -- XX:XX:XX:AM or XX:XX:XX:PM
};

/*-------------------------------*
 | PromptTemplate Table Creation |
 *-------------------------------*/
CREATE TABLE PromptTemplate {
    templateID    INTEGER PRIMARY KEY,
    creatorID     INTEGER,        -- FK to AppUser
    workspaceID   INTEGER,        -- FK to Workspace
    title         VARCHAR2(50),   -- FK to Message
    content       VARCHAR2(1000), -- XX:XX:XX:AM or XX:XX:XX:PM
    category      VARCHAR2(50),
    privateStatus NUMBER(1),
    creationDate  VARCHAR2(9) MM/DD/YYYY
};
