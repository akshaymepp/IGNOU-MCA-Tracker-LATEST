# IGNOU MCA Tracker — Android

Native Android starter for MCA_NEW students. It retrieves notices from official IGNOU websites, classifies them into project, assignments, exams, results and registration categories, and schedules periodic Android notifications.

## Official sources included
- IGNOU announcements: https://www.ignou.ac.in/announcements/0?nav=6
- MCA_NEW assignments: https://webservices.ignou.ac.in/assignments/schools/socis/mca_new/mca_new.html
- IGNOU Regional Centre Kozhikode: https://rckozhikode.ignou.ac.in/

## Notifications
This build uses WorkManager to poll official sources approximately every 6 hours and issue local Android notifications when new links are detected. Android may defer periodic jobs due to battery optimisation. These are not Firebase server-push notifications. True FCM push requires a Firebase project, app registration and a deployed backend.

## Build APK
1. Install Android Studio and Android SDK Platform 35.
2. Open this folder in Android Studio and allow Gradle sync.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

The included GitHub Actions workflow can also build the APK after this source folder is pushed to a GitHub repository.

## Important
The announcement parser uses page links and keyword matching. IGNOU may change its website layout, and some general notices may or may not apply to MCA_NEW. Always open the original notice and verify its applicability and deadlines.


## Updated notification behavior
- First successful background check silently stores currently listed notices to avoid a flood of old alerts.
- Each newly detected announcement on later checks receives its own Android notification.
- Tapping a notification opens its source URL.
- Android 13+ requires notification permission.
- WorkManager checks approximately every 6 hours; Android may defer jobs due to battery optimisation.
- This uses scheduled on-device polling and local notifications, not instant Firebase server push.


## MCA_NEW project and hall-ticket coverage
The notice filter now includes MCA_NEW project/synopsis items such as synopsis, project proposal, project submission, report, project guidelines, supervisor/guide, approval, project fee, viva, evaluation, internship and dissertation. A dedicated Hall Ticket category is included, and the dashboard has a shortcut to the official IGNOU Samarth portal.

Hall-ticket availability is student-specific. This app opens the official portal for the student to verify/download their own ticket; it does not claim a ticket is available merely because a general TEE notice exists. The official notice parser is keyword-based and can miss notices if IGNOU changes page structure or uses different wording. Always verify each notice on its original official page.

- Announcement dates are displayed as `dd - MMM - yyyy` when the source provides a supported numeric date.


## Exam registration tracking
The app now has a separate `Exam Registration` category. It looks for notices mentioning exam registration, examination forms, TEE forms and examination fees. This is announcement tracking only; it does not submit the examination form or pay fees. Students must use the official IGNOU portal and verify the applicable session and deadline.


### Results tracking
The Results category recognizes result announcements and references to grade cards, revised results, revaluation/re-evaluation, rechecking, TEE results, and marksheets. Detection is based on text found on the configured official IGNOU pages; it is best-effort and does not log in to Samarth or retrieve an individual student's marks. Check the official portal for personal result and grade-card status.
