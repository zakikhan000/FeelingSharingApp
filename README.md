# FeelingSharingApp
Sure! Here's a **detailed description** of your Final Year Project (FYP) titled:

---

## 💡 **Project Title:**

**FeelShare – A Mental Health Support Platform for Anonymous Emotional Expression**

---

## 🧠 **Project Overview:**

In today’s fast-paced and emotionally overwhelming world, many individuals hesitate to express their true feelings due to the fear of judgment, exposure, or social pressure. *FeelShare* is an innovative mobile application designed to provide a **safe, anonymous, and supportive space** where users can share their thoughts, emotions, and struggles without revealing their identity.

This project combines **Android development**, **AI-based chatbot interaction**, **sentiment analysis**, and **database management** to offer users a compassionate digital friend and a supportive community.

---

## 🎯 **Project Goals and Objectives:**

1. **Enable anonymous expression** of personal thoughts and emotional struggles.
2. **Connect users to a friendly chatbot** for emotional support and mental relief.
3. **Prevent harmful or toxic content** using AI-powered sentiment filtering.
4. **Promote positive mental well-being** by encouraging uplifting content.
5. **Create a virtual support community** where people can interact without fear or pressure.

---

## 📲 **Features & Functionalities:**

### 1. 🔐 **User Interface & Authentication**

* Option for **anonymous access** or **optional user sign-up/login**.
* Simple and clean **UI/UX** design (modern layout using Android Studio).

### 2. 📝 **Post Feelings Anonymously**

* Textbox to write feelings.
* Option to choose mood (Happy, Sad, Angry, Confused, etc.)
* Posts are shown in a **public feed anonymously**.
* Add emoji-based **reactions** (support, hug, advice).

### 3. 💬 **AI Chatbot ("Zaki")**

* Greets the user: *“Hi, I am your friend Zaki. Tell me how do you feel today?”*
* Responds based on **user emotions**.
* Generates different answers each time (no static replies).
* Can **talk in audio** (speech-to-text + text-to-speech).
* Trained on **mental health, stress, anxiety, loneliness** responses.
* Uses **sentiment analysis** to redirect or uplift negative thoughts.

### 4. 🚨 **Content Moderation & Sentiment Analysis**

* Detects:

  * Abusive language
  * Racism or political content
  * Strong negative or suicidal tendencies
* Uses **prefix-based biasing** to shift thoughts positively (e.g., “I hate myself” → “Let’s try to find what’s bothering you…”).

### 5. 📊 **Dashboard / Community Feed**

* Shows posts by others (with timestamp & mood).
* Users can react or comment (anonymously).
* **Search feature** to find similar experiences.

### 6. 🔔 **Notifications**

* Daily reminders like:

  * “Hey! You matter.”
  * “Want to share how you feel today?”
* Updates on replies to their anonymous posts.

---

## 🔌 **Technical Stack**

### 🧱 **Frontend (Android App)**

* **Java or Kotlin**
* **Retrofit** for API communication
* **Speech-to-Text & Text-to-Speech** (Google Speech APIs)

### 🧠 **AI & Chatbot**

* **Python (Flask backend)**
* **Wit.ai or SpaCy + NLTK + Naive Bayes**
* **Scrapy** to collect emotional support responses from blogs/forums

### 🗃️ **Backend**

* **Flask** (REST API)
* **MSSQL** or **PostgreSQL**
* **Raw SQL queries**

### 🗂️ **Database Tables**

* `UserAuthentication` (optional)
* `AnonymousPosts`
* `ChatbotConversations`
* `UserReactions`
* `FilteredWords`
* `MoodLogs`

### ☁️ **Hosting**

* Use **Loophole**, **Render**, or **Replit** for backend deployment
* Use **Firebase** for notifications (optional)

---

## 📐 **System Architecture**

```
[ Android App ]
     |
     | --> [ Flask API (Hosted) ]
                  |
                  |--> [ Chatbot Engine ]
                  |--> [ Sentiment Filter ]
                  |--> [ Database (MSSQL/PostgreSQL) ]
```

---

## 📊 **Data Flow Example**

1. User opens app → Greeted by chatbot.
2. User chooses to post or chat.
3. User writes a post → Goes to backend → Stored anonymously → Shown in feed.
4. User talks to chatbot → Sentiment analyzed → Filtered → Response generated.
5. User gets positive feedback or redirection.

---

## 📈 **Benefits and Impact**

* Encourages **emotional expression** without fear.
* Helps users feel **heard and supported**.
* Protects against **toxic behavior** using sentiment filtering.
* Uses **AI to promote empathy** and kindness.
* Contributes to **mental well-being awareness**.

---

## 📃 **Possible Add-ons (Future Scope)**

* Personal diary mode (private entries).
* Mood tracking charts.
* Chatbot learning from user responses (ML model improvement).
* Admin panel to review flagged content.
* Integration with therapists (optional contact help).

---

Would you like me to prepare this as a **formal proposal document**, **PowerPoint presentation**, or generate a **wireframe/UI design** as well?
