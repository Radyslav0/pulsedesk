# PulseDesk – Comment-to-Ticket Triage

A Spring Boot backend that analyses user comments and generates structured support tickets using AI.
 
---

## Overview

PulseDesk is a REST API that:

1. Accepts user comments from any channel (app review, web form, chat, etc.)
2. Sends each comment to the Hugging Face Inference API for zero-shot classification
3. Decides whether the comment warrants a support ticket
4. If so, generates and stores: title, category, priority, summary
5. Exposes REST endpoints to query comments and tickets
6. Includes a single-page UI served at `http://localhost:8080`
---

## Tech Stack

| Layer     | Technology                                   |
|-----------|----------------------------------------------|
| Backend   | Java 21, Spring Boot 3.2                     |
| Database  | H2 (in-memory, embedded)                     |
| AI        | Hugging Face Inference API (bart-large-mnli) |
| Build     | Maven                                        |
| Container | Docker                                       |
 
---

## Project Structure

```
pulsedesk/
├── src/
│   └── main/
│       ├── java/com/ibm/pulsedesk/
│       │   ├── PulseDeskApplication.java
│       │   ├── config/
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── controller/
│       │   │   ├── CommentController.java
│       │   │   └── TicketController.java
│       │   ├── model/
│       │   │   ├── Comment.java
│       │   │   ├── CommentRequest.java
│       │   │   └── Ticket.java
│       │   ├── repository/
│       │   │   ├── CommentRepository.java
│       │   │   └── TicketRepository.java
│       │   └── service/
│       │       ├── CommentService.java
│       │       ├── HuggingFaceService.java
│       │       └── TicketService.java
│       └── resources/
│           ├── application.properties
│           └── static/index.html
├── Dockerfile
├── pom.xml
└── README.md
```
 
---

## Running with Docker

This is the recommended way to run the project.

### 1. Clone the repository

```bash
git clone https://github.com/Radyslav0/pulsedesk.git
cd pulsedesk
```

### 2. Create a .env file in the root directory

```
HF_API_TOKEN=your_huggingface_token_here
```

Get a free token at huggingface.co/settings/tokens (Read access is enough).

The `.env` file is listed in `.gitignore` and will not be committed to the repository.

### 3. Build the Docker image

```bash
docker build -t pulsedesk .
```

### 4. Run the container

```bash
docker run --env-file .env -p 8080:8080 pulsedesk
```

Open `http://localhost:8080` in your browser.
 
---

## Running without Docker

Requirements: Java 21+, Maven 3.8+

```bash
git clone https://github.com/Radyslav0/pulsedesk.git
cd pulsedesk
```

Set the token as an environment variable:

```bash
# Windows
set HF_API_TOKEN=your_token_here
 
# Mac/Linux
export HF_API_TOKEN=your_token_here
```

Then run:

```bash
mvn spring-boot:run
```
 
---

## API Reference

### POST /comments

```bash
curl -X POST http://localhost:8080/comments \
  -H "Content-Type: application/json" \
  -d '{"author":"Alice","content":"The app crashes on login.","channel":"APP_REVIEW"}'
```

Response `201 Created`:

```json
{
  "id": 1,
  "author": "Alice",
  "content": "The app crashes on login.",
  "channel": "APP_REVIEW",
  "createdAt": "2026-04-26T10:00:00",
  "convertedToTicket": true
}
```

### GET /comments

```bash
curl http://localhost:8080/comments
```

### GET /tickets

```bash
curl http://localhost:8080/tickets
```

Response `200 OK`:

```json
[
  {
    "id": 1,
    "title": "[BUG] The app crashes on login.",
    "category": "BUG",
    "priority": "HIGH",
    "summary": "The app crashes on login.",
    "createdAt": "2026-04-26T10:00:01",
    "commentId": 1
  }
]
```

### GET /tickets/{ticketId}

```bash
curl http://localhost:8080/tickets/1
```
 
---

## Web UI

Open `http://localhost:8080` to use the interface:

- Submit comments and see AI triage results
- View generated tickets with category and priority labels
- Browse the full comment history
---

## H2 Console

Available at `http://localhost:8080/h2-console` during development.

JDBC URL: `jdbc:h2:mem:pulsedeskdb`, Username: `sa`, Password: (empty)
 
---

## AI Model

The project uses `facebook/bart-large-mnli` via the Hugging Face Inference API. This is a zero-shot classification model — it receives the comment text and a list of candidate labels, and returns a confidence score for each label. No fine-tuning required.

The task specification suggested generative models (flan-t5-base, Mistral, Falcon). A zero-shot classifier was chosen instead because it returns structured, predictable output directly mapped to ticket fields, without requiring response parsing.

| Field    | Labels used                                                                           |
|----------|---------------------------------------------------------------------------------------|
| Triage   | "support issue", "compliment or general feedback"                                     |
| Category | "bug or crash", "feature request", "billing or payment", "account or login", "other" |
| Priority | "high priority urgent", "medium priority", "low priority"                             |

If the API is unavailable, the service falls back to keyword-based classification so all endpoints remain functional.
 
---

## Sample Test Scenarios

Results may vary depending on phrasing. The model is sensitive to emotional intensity — punctuation and wording affect classification.

| Comment | Result |
|---|---|
| "The app crashes every time I open it." | Ticket, BUG, MEDIUM |
| "The app crashes every time I open it!!!" | Ticket, BUG, HIGH |
| "I was charged twice this month!!" | Ticket, BILLING, HIGH |
| "I was charged twice this month." | may not create a ticket |
| "App doesn't work at all" | Ticket, BUG, MEDIUM |
| "Great app, love it." | No ticket |

Note: the model uses zero-shot classification and has no domain-specific training. More expressive phrasing generally produces higher confidence scores and higher priority.
 
---

