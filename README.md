# Lost and Found Mobile Application

A full-stack Android application that allows users to report, browse, and claim lost or found items. The system uses an Android frontend, a PHP backend API, and a MySQL database to manage data.

Developed as an individual project during the Diploma in Computer Engineering at Temasek Polytechnic.

## Project Overview

The Lost and Found system digitises the process of reporting and recovering lost items. Users can create accounts, post lost or found items, browse existing listings, and submit claims for items posted by other users.

The Android application communicates with a PHP backend through API requests, while MySQL is used to store user, item, and claim information.

## Key Features

* User registration and login
* Registration restricted to valid student/staff email addresses
* Browse lost and found listings
* Create lost or found item posts
* Submit claims for listed items
* View personal item listings
* View submitted claims
* Claim validation logic to prevent duplicate or invalid claims
* Real-time data retrieval using API requests

## Technologies Used

### Android Frontend

* Java
* Android Studio
* Volley

### Backend

* PHP
* Apache (XAMPP)

### Database

* MySQL

## My Contributions

This was an individual project.

My responsibilities included:

* Designing and developing the Android application
* Implementing user authentication and account management
* Developing item posting and claim submission workflows
* Integrating Android frontend components with PHP backend APIs
* Designing and managing the MySQL database
* Implementing claim validation and business logic
* Testing and debugging application functionality

## Screenshots

Screenshots can be found in the `/screenshots` directory.

### Authentication

* Registration Page

![Registration Page](Screenshots/registration.jpg)

---

### Staff & Student Features

* Home Screen

![Home Page](Screenshots/student-home.jpg)

* Create Lost/Found Item

![Create Item](Screenshots/create-item.jpg)

* Item Details

![Item Details](Screenshots/item-list.jpg)

* My Claims

![My Claims](Screenshots/my-claims.jpg)

---

### Administrator Features

* Review Claims Dashboard

![Review Claims](Screenshots/admin-claims-page.jpg)

## Running the Project

### Prerequisites

* Android Studio
* XAMPP
* MySQL

### Installation

#### 1. Set Up the Backend

Move the backend PHP files into:

`C:\xampp\htdocs\LostFoundBackend`

Start Apache and MySQL from XAMPP.

Create a MySQL database and import the project database schema.

Update database credentials in:

`db_connect.php`

#### 2. Configure the Android Application

Open the Android project in Android Studio.

Allow Gradle dependencies to sync completely.

Update the API base URL in the application source code so it points to your backend server.

#### 3. Run the Application

Launch an Android emulator or connect a physical Android device.

Build and run the project from Android Studio.

## Notes

* This project was developed for educational purposes.
* No external hosting or cloud deployment was implemented.
* Database credentials and environment-specific settings are not included.
* The application requires a running PHP backend and MySQL database to function correctly.

## Project Status

Completed (Individual Academic Project)
