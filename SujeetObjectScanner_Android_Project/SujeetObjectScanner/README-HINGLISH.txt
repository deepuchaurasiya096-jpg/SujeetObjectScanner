SUJEET OBJECT SCANNER - AASAN SETUP
===================================

Ye sirf OBJECT SCANNER Android app hai. Ismein 3D Scanner, QR Scanner ya Document Scanner nahi hai.

APP KYA KAREGA?
- Phone camera se live view karega.
- ML Kit ka on-device AI model image ke visible content ko labels dega (jaise car, vehicle, plant, bottle, animal, etc.).
- Har label ke saath confidence percentage dikhayega.
- Phone gallery ki photo bhi scan kar sakta hai.
- Last 5 scan results phone par save karega.
- Front/back camera, pause/resume aur flash controls hain (flash supported camera par hi).

IMPORTANT:
Ye image-labeling scanner hai, guaranteed exact identification nahi. Ye exact car model, serial number, ya har object ka naam guarantee nahi karta. Kabhi galat label bhi de sakta hai. Camera images is app ke through upload nahi hote; bundled model phone par kaam karta hai.

ANDROID APK KAISE BANAYEIN?
1. PC par Android Studio install karo.
2. Is ZIP ko Extract karo.
3. Android Studio kholo aur File > Open chuno.
4. Extracted folder "SujeetObjectScanner" select karo (jisme settings.gradle file hai).
5. Gradle Sync hone do. Pehli baar internet zaroori hai, kyunki build libraries download hongi.
6. Phone ko USB cable se connect karo. Phone mein Developer Options aur USB Debugging enable karna hoga.
7. Android Studio mein Run (green triangle) dabao.
8. Phone par Camera permission Allow karo.

APK FILE BANANE KE LIYE:
Android Studio mein Build > Build Bundle(s) / APK(s) > Build APK(s) select karo.
Build successful hone par APK yahan milega:
app/build/outputs/apk/debug/app-debug.apk

TECHNICAL:
Package: com.sujeet.objectscanner
Minimum Android: 6.0 (API 23)
Target SDK: 35
Compile SDK: 36
JDK: 17+

Agar camera nahi khulta: Phone Settings > Apps > Sujeet Object Scanner > Permissions > Camera ko Allow karo.

NOTE: ZIP mein Gradle 8.11.1 ka bootstrap wrapper bhi hai. Pehli baar Gradle ko official Gradle site se download karne ke liye internet lagega.
