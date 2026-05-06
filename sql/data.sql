/*-----------------------------------------------------------------*
| Authors: Gabriel I. Hernandez (gabehernandez07@arizona.edu)     |
|          Andrew Barnica (asbarnica@arizona.edu)                 |
|          Tom Giallanza (giallanza1@arizona.edu)                 |
|          Helena Musial (helenamusial@arizona.edu)               |
| File Name: data.sql                                             |
| Creation Date: 05-04-2026                                       |
| Description: SQL queries to insert data into the tables created |
|              in schema.sql. This data will be used for testing  |
|              as it is sample data for the database              |
| Last Modification: 05-05-2026                                   |
*-----------------------------------------------------------------*/

/*--------------------------------------*
 | Inserting into Membership Tier Table |
 *--------------------------------------*/
INSERT INTO MembershipTier VALUES (SEQ_TIER.NEXTVAL, 'free',       5,  0, 0.00);
INSERT INTO MembershipTier VALUES (SEQ_TIER.NEXTVAL, 'plus',       100,  1, 20.00);
INSERT INTO MembershipTier VALUES (SEQ_TIER.NEXTVAL, 'enterprise', 1000, 1, 100.00);

/*---------------------------------------*
 | Inserting into Application User Table |
 *---------------------------------------*/
INSERT INTO ApplicationUser VALUES (SEQ_USER.NEXTVAL, 1, 'Mark Grayson', 'invincible@gmail.com',   DATE '2021-03-26', 'English');
INSERT INTO ApplicationUser VALUES (SEQ_USER.NEXTVAL, 1, 'Dora',         'theexplorer@gmail.com',  DATE '2026-05-05', 'Spanish');
INSERT INTO ApplicationUser VALUES (SEQ_USER.NEXTVAL, 2, 'Homelander',   'theweakest@gmail.com',   DATE '2019-07-26', 'English');
INSERT INTO ApplicationUser VALUES (SEQ_USER.NEXTVAL, 2, 'Satoru Gojo',  'thestrongest@gmail.com', DATE '2020-10-03', 'Japanese');
INSERT INTO ApplicationUser VALUES (SEQ_USER.NEXTVAL, 3, 'Tony Stark',   'ironman@gmail.com',      DATE '2019-04-26', 'English');

/*--------------------------------------*
 | Inserting into Billing Profile Table |
 *--------------------------------------*/
INSERT INTO BillingProfile VALUES (SEQ_BILLING.NEXTVAL, 1, 'Credit Card', '8124 Raven Blvd, Baltimore MD 491002');
INSERT INTO BillingProfile VALUES (SEQ_BILLING.NEXTVAL, 4, 'Venmo',       '123 Euclid Ave, Tucson AZ 82032');
INSERT INTO BillingProfile VALUES (SEQ_BILLING.NEXTVAL, 5, 'Pay Pal',     '904 Oak St  St, Phoenix AZ 71000');


/*------------------------------*
 | Inserting into Invoice Table |
 *------------------------------*/
INSERT INTO Invoice VALUES (SEQ_INVOICE.NEXTVAL, 3, 20.00,   DATE '2019-08-26', 0);
INSERT INTO Invoice VALUES (SEQ_INVOICE.NEXTVAL, 3, 20.00,   DATE '2023-08-26', 0);
INSERT INTO Invoice VALUES (SEQ_INVOICE.NEXTVAL, 4, 20.00,   DATE '2024-10-31', 1);
INSERT INTO Invoice VALUES (SEQ_INVOICE.NEXTVAL, 5, 100.00,  DATE '2020-04-26', 1);
INSERT INTO Invoice VALUES (SEQ_INVOICE.NEXTVAL, 5, 100.00,  DATE '2021-04-26', 1);

/*------------------------------------*
 | Inserting into Support Agent Table |
 *------------------------------------*/
INSERT INTO SupportAgent VALUES (SEQ_AGENT.NEXTVAL, 'Billy Bob', 'billybobjoe@support.com');
INSERT INTO SupportAgent VALUES (SEQ_AGENT.NEXTVAL, 'Joe Bart',  'joebartolozzi@support.com');


/*-------------------------------------*
 | Inserting into Support Ticket Table |
 *-------------------------------------*/
INSERT INTO SupportTicket VALUES (SEQ_TICKET.NEXTVAL, 2, 1, 'Cannot log in',                     DATE '2026-05-05', 2, 'Resolved: walked client through process');
INSERT INTO SupportTicket VALUES (SEQ_TICKET.NEXTVAL, 1, 2, 'Cannot access billing information', DATE '2024-03-10', 1, 'Resolved: password reset');
INSERT INTO SupportTicket VALUES (SEQ_TICKET.NEXTVAL, 3, 2, 'Messages not saving',               DATE '2024-03-10', NULL, NULL);
INSERT INTO SupportTicket VALUES (SEQ_TICKET.NEXTVAL, 4, 1, 'Cannot create persona',             DATE '2024-03-10', 4, 'Resolved: cleared cache');

/*--------------------------------*
 | Inserting into Workspace Table |
 *--------------------------------*/
INSERT INTO Workspace VALUES (SEQ_WORKSPACE.NEXTVAL, 3, 'The Seven',          1, DATE '2022-04-13');
INSERT INTO Workspace VALUES (SEQ_WORKSPACE.NEXTVAL, 1, 'Invincible Inc',     1, DATE '2024-01-20');
INSERT INTO Workspace VALUES (SEQ_WORKSPACE.NEXTVAL, 4, 'Personal Questions', 0, DATE '2020-10-10');

/*------------------------------------------*
 | Inserting into WorkspaceMembership Table |
 *------------------------------------------*/
INSERT INTO WorkspaceMembership VALUES (3, 1, DATE '2024-04-13');
INSERT INTO WorkspaceMembership VALUES (1, 1, DATE '2024-05-20');
INSERT INTO WorkspaceMembership VALUES (1, 2, DATE '2024-01-20');
INSERT INTO WorkspaceMembership VALUES (4, 3, DATE '2020-10-10');

/*------------------------------*
 | Inserting into Persona Table |
 *------------------------------*/
INSERT INTO Persona VALUES (SEQ_PERSONA.NEXTVAL, 1, 'Undergraduate CS Major', 'Keep responses nice and simple.',       DATE '2022-09-19');
INSERT INTO Persona VALUES (SEQ_PERSONA.NEXTVAL, 3, 'Professional Speaker',   'Make me sound smart and important.',    DATE '2024-06-23');
INSERT INTO Persona VALUES (SEQ_PERSONA.NEXTVAL, 4, 'Non-Sorcerer',           'Help me integrate into human society.', DATE '2021-06-25');

/*-----------------------------------*
 | Inserting into Conversation Table |
 *-----------------------------------*/
INSERT INTO Conversation VALUES (SEQ_CONVERSATION.NEXTVAL, 1, 2, 1, 'Code Review',          DATE '2022-12-17', 0);
INSERT INTO Conversation VALUES (SEQ_CONVERSATION.NEXTVAL, 3, 1, 2, 'Speech practice',      DATE '2024-07-03', 0);
INSERT INTO Conversation VALUES (SEQ_CONVERSATION.NEXTVAL, 4, 3, 3, 'Store Convo practice', DATE '2021-06-26', 0);

/*------------------------------*
 | Inserting into Message Table |
 *------------------------------*/
-- convo with cs major persona
INSERT INTO Message VALUES (SEQ_MESSAGE.NEXTVAL, 1, 'user', 'Can you debug this file?',                                             TIMESTAMP '2022-12-17 21:00:00');
INSERT INTO Message VALUES (SEQ_MESSAGE.NEXTVAL, 1, 'assistant', 'Happy to! Here is a list of potential errors in your code.',      TIMESTAMP '2022-12-17 21:00:05');
INSERT INTO Message VALUES (SEQ_MESSAGE.NEXTVAL, 1, 'user', 'Perfect, can you help me understand this part of the spec?',           TIMESTAMP '2022-12-17 21:03:24');
INSERT INTO Message VALUES (SEQ_MESSAGE.NEXTVAL, 1, 'assistant', 'Of course, here is a brief summary of part 3:',                   TIMESTAMP '2022-12-17 21:03:33');
-- convo with speech helper
INSERT INTO Message VALUES (SEQ_MESSAGE.NEXTVAL, 2, 'user', 'Help me write a draft to get my reputation with the public up',        TIMESTAMP '2024-07-03 10:13:09');
INSERT INTO Message VALUES (SEQ_MESSAGE.NEXTVAL, 2, 'assistant', 'Most certainly, this should get your ratings up in now time.',    TIMESTAMP '2024-07-03 10:13:16');
INSERT INTO Message VALUES (SEQ_MESSAGE.NEXTVAL, 2, 'user', 'NO, rewrite it with more propaganda, they must love me at all costs.', TIMESTAMP '2024-07-03 10:14:02');
INSERT INTO Message VALUES (SEQ_MESSAGE.NEXTVAL, 2, 'assistant', 'Sorry about that, here is a revised version.',                    TIMESTAMP '2024-07-03 10:14:11');
-- convo with non-sorceror
INSERT INTO Message VALUES (SEQ_MESSAGE.NEXTVAL, 3, 'user', 'Let''s simulate a conversation at a store',                            TIMESTAMP '2021-06-26 14:50:32');
INSERT INTO Message VALUES (SEQ_MESSAGE.NEXTVAL, 3, 'assistant', 'I got you, here''s my first line:',                               TIMESTAMP '2021-06-26 14:50:36');

/*-------------------------------*
 | Inserting into Feedback Table |
 *-------------------------------*/
INSERT INTO Feedback VALUES (SEQ_FEEDBACK.NEXTVAL, 2,  1, 1, 'Very helpful debug response.',  TIMESTAMP '2022-12-17 21:05:06');
INSERT INTO Feedback VALUES (SEQ_FEEDBACK.NEXTVAL, 8,  2, 0, 'This machine did not satisfy.', TIMESTAMP '2024-07-03 10:15:55');
INSERT INTO Feedback VALUES (SEQ_FEEDBACK.NEXTVAL, 10, 3, 1, 'Awesome! I feel ready now',     TIMESTAMP '2021-06-26 14:55:30');

/*-------------------------------*
 | Inserting into Bookmark Table |
 *-------------------------------*/
INSERT INTO Bookmark VALUES (1, 4,  1, TIMESTAMP '2022-12-17 21:45:48');
INSERT INTO Bookmark VALUES (4, 10, 3, TIMESTAMP '2021-06-26 14:56:01');

/*-------------------------------------*
 | Inserting into PromptTemplate Table |
 *-------------------------------------*/
INSERT INTO PromptTemplate VALUES (SEQ_PROMPTTEMPLATE.NEXTVAL, 1, 2, 'Summarise Specification', 'Summarise the following in bullet points: [specification]', 'Productivity', 0, DATE '2022-12-17');
INSERT INTO PromptTemplate VALUES (SEQ_PROMPTTEMPLATE.NEXTVAL, 1, 2, 'Debug the Program', 'Debug the following file: [file]', 'Productivity',                                0, DATE '2022-12-17');
INSERT INTO PromptTemplate VALUES (SEQ_PROMPTTEMPLATE.NEXTVAL, 4, 3, 'Conversation Practice', 'Simulate a conversation with me at [place]', 'Social Skills',                 1, DATE '2022-01-28');
