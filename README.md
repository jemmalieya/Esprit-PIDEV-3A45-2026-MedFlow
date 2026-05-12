#  MedFlow – Healthcare Management Application

# -Esprit-PIDEV-3A45--2026-MedFlow

## Overview
This project was developed as part of the PIDEV (Projet d’Intégration et de Développement) within the 3rd Year Engineering Program (1st Year of Engineering Cycle) at Esprit School of Engineering – Tunisia (Academic Year 2025–2026).

MedFlow is a complete healthcare management application designed to streamline multiple services including user management, appointments, pharmacy, events, complaints, and blog management.

The project focuses on applying Object-Oriented Programming (OOP) principles, structured architecture, and modern software engineering practices to solve real-world problems.

---

##  Features
-  User Management  
-  Appointment Management (RDV)  
-  Pharmacy Management  
-  Event Management  
-  Complaint Management (Réclamation)  
-  Blog Management  

---

##  Tech Stack

## Frontend
- Java (Desktop Application)

## Backend
- Java
- Maven
- SQL / Database Integration

## Advanced Features

In addition to the standard CRUD operations, MedFlow includes several advanced functionalities that improve automation, security, accessibility, and user experience.

### Artificial Intelligence

- **AI Blog Assistant** using Google Gemini to improve and generate better blog content.
- **AI Event Analysis** using Groq to analyze events and provide intelligent recommendations.
- **Speech-to-Text for Reclamations** using Groq Whisper to convert complaint audio into text.
- **Appointment Urgency Detection** using AI to classify consultation requests by urgency level.
- **AI Product Recommendations** for suggesting pharmacy products based on user behavior and available stock.
- **AI Prescription Suggestions** to assist doctors with medication suggestions based on diagnosis.
- **Dialogflow Assistant** for natural language appointment booking and voice-based interaction.

### External API Integrations

- **Stripe** for secure online payments.
- **Brevo** for automatic email notifications.
- **Twilio** for SMS notifications.
- **Cloudinary** for image storage and management.
- **OCR.Space and PDFBox** for extracting text from medical documents and scanned PDFs.
- **OpenFDA** for checking possible drug interactions.
- **LocationIQ / OpenStreetMap** for event location and geocoding.
- **OpenWeatherMap** for weather information related to events.
- **Google Perspective API** for detecting toxic or inappropriate comments.
- **Google reCAPTCHA** for bot protection.

### Advanced Business Logic

- Appointment management with urgency level and status tracking.
- Pharmacy stock control and drug interaction verification.
- Event risk analysis and participant notification system.
- Blog comment moderation and anti-spam filtering.
- Security risk detection for suspicious user activity.
- Automatic invoice and PDF generation.

### Accessibility

- Offline speech-to-text support using Vosk.
- Text-to-speech support for reading content aloud.
- Voice-enabled assistant for easier interaction.
- Accessible video room management for online events.

### Notifications and Automation

- Automatic email confirmations.
- SMS order notifications.
- Event status notifications.
- Security alert logic.

---

## Architecture
The project follows a layered architecture based on Object-Oriented Programming:
- Presentation Layer → JavaFX UI
- entities → Data models representing database tables  
- services → Business logic and CRUD operations  
- tools → Utility classes (database connection, helpers)  
- mains → Entry point of the application  

## Project Structure

src/
└── main/
    └── java/
        └── tn/
            └── esprit/
                ├── entities/
                ├── mains/
                ├── services/
                └── tools/

## Contributors:
Jemmali Eya 
Saidi Yassmine
Ghribi Wael 
Ben Ghars Mayar 
Mannai Meyssem

## Academic Context 
Developed at **Esprit School of Engineering – Tunisia** 
PIDEV – 3A | 2025–2026 

## Getting Started
-Prerequisites:

Java JDK 17 or higher
IntelliJ IDEA
Maven
SQL Database

-Installation:

Clone the repository
Open the project in IntelliJ IDEA
Install dependencies
Configure the database connection if required
Run the application from the mains package

## Acknowledgments
Esprit School of Engineering – Tunisia
PIDEV academic framework
Team collaboration and contributions
