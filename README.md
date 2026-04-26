# PulseDesk – Comment-to-Ticket Triage

> **IBM Internship Exercise** – AI-powered backend that analyses user comments and automatically generates structured support tickets.

---

## Overview

PulseDesk is a Spring Boot REST API that:

1. Accepts user comments from any channel (app review, web form, chat, etc.)  
2. Sends each comment to the **Hugging Face Inference API** (`google/flan-t5-base`) for analysis  
3. Decides whether the comment warrants a support ticket  
4. If so, generates and stores: **title · category · priority · summary**  
5. Exposes REST endpoints to query comments and tickets  
6. Includes a **bonus single-page UI** served at `http://localhost:8080`

---

## Tech Stack

| Layer      | Technology                          |
|------------|-------------------------------------|
| Backend    | Java 17 · Spring Boot 3.2           |
| Database   | H2 (in-memory, embedded)            |
| AI / ML    | Hugging Face Inference API (flan-t5-base) |
| Build      | Maven                               |
| Container  | Docker (optional)                   |

---

## Project Structure

```
pulsedesk/
├── src/
│   └── main/
│       ├── java/com/ibm/pulsedesk/
│       │   ├── PulseDeskApplication.java      # Entry point
│       │   ├── config/
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── controller/
│       │   │   ├── CommentController.java     # POST /comments, GET /comments
│       │   │   └── TicketController.java      # GET /tickets, GET /tickets/{id}
│       │   ├── model/
│       │   │   ├── Comment.java               # JPA entity
│       │   │   ├── CommentRequest.java        # Inbound DTO
│       │   │   └── Ticket.java                # JPA entity
│       │   ├── repository/
│       │   │   ├── CommentRepository.java
│       │   │   └── TicketRepository.java
│       │   └── service/
│       │       ├── CommentService.java        # Orchestrates triage flow
│       │       ├── HuggingFaceService.java    # AI API calls + fallback
│       │       └── TicketService.java
│       └── resources/
│           ├── application.properties
│           └── static/index.html              # Bonus UI
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Setup & Running

### Prerequisites

- Java 17+  
- Maven 3.8+  
- A free [Hugging Face](https://huggingface.co/settings/tokens) API token

### 1. Clone & Configure

```bash
git clone https://github.com/YOUR_USERNAME/pulsedesk.git
cd pulsedesk
```

Open `src/main/resources/application.properties` and set your token:

```properties
huggingface.api.token=hf_YOUR_TOKEN_HERE
```

Or pass it as an environment variable (recommended):

```bash
export HF_API_TOKEN=hf_YOUR_TOKEN_HERE
```

### 2. Build & Run

```bash
mvn spring-boot:run
```

The application starts on **http://localhost:8080**

> **No HF token?** The service falls back to a keyword-based heuristic so you can still test all endpoints.

---

## API Reference

### POST `/comments` – Submit a comment

```bash
curl -X POST http://localhost:8080/comments \
  -H "Content-Type: application/json" \
  -d '{"author":"Alice","content":"The app crashes every time I open it!","channel":"APP_REVIEW"}'
```

**Response** `201 Created`:
```json
{
  "id": 1,
  "author": "Alice",
  "content": "The app crashes every time I open it!",
  "channel": "APP_REVIEW",
  "createdAt": "2026-04-26T10:00:00",
  "convertedToTicket": true
}
```

---

### GET `/comments` – List all comments

```bash
curl http://localhost:8080/comments
```

---

### GET `/tickets` – List all tickets

```bash
curl http://localhost:8080/tickets
```

**Response** `200 OK`:
```json
[
  {
    "id": 1,
    "title": "App crash on launch",
    "category": "BUG",
    "priority": "HIGH",
    "summary": "User reports the application crashes immediately upon opening.",
    "createdAt": "2026-04-26T10:00:01",
    "commentId": 1
  }
]
```

---

### GET `/tickets/{ticketId}` – Get one ticket

```bash
curl http://localhost:8080/tickets/1
```

---

## Bonus: Web UI

Open **http://localhost:8080** in your browser to use the interactive UI:

- Submit comments and see instant AI triage results  
- Browse all generated tickets with category/priority badges  
- View the full comment history  

---

## Docker (Optional Deployment)

```bash
# Build image
docker build -t pulsedesk .

# Run (pass your HF token)
docker run -e HF_API_TOKEN=hf_YOUR_TOKEN -p 8080:8080 pulsedesk
```

Then visit **http://localhost:8080**

---

## H2 Console (Development)

The embedded H2 database console is available at:  
**http://localhost:8080/h2-console**  
JDBC URL: `jdbc:h2:mem:pulsedeskdb`  Username: `sa`  Password: *(empty)*

---

## AI Model

The project uses **`google/flan-t5-base`** via the Hugging Face Inference API.  
The model is prompted with targeted instructions for each field:

| Field       | Prompt strategy                                         |
|-------------|---------------------------------------------------------|
| Triage      | Yes/no question – should this become a ticket?          |
| Title       | "Write a short ticket title (max 10 words)"             |
| Category    | Classify into: bug / feature / billing / account / other |
| Priority    | Assess as: low / medium / high                          |
| Summary     | "Write a concise 1-2 sentence summary"                  |

Alternative models (swap in `application.properties`):
- `mistralai/Mistral-7B-Instruct`
- `tiiuae/falcon-7b-instruct`

---

## Sample Test Scenarios

| Comment | Expected outcome |
|---|---|
| `"The app crashes every time I open it!"` | ✅ Ticket · BUG · HIGH |
| `"I was charged twice this month!"` | ✅ Ticket · BILLING · HIGH |
| `"Would love a dark mode option!"` | ✅ Ticket · FEATURE · LOW |
| `"Love the new design, great work!"` | ❌ No ticket (compliment) |

---

## License

Built for the IBM Application Developer Internship technical challenge.
