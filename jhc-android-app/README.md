# JHC Android app

A small Android app that opens https://jhc.lk full screen (pull down to refresh, back button, WhatsApp / phone / mail links, photo upload for Quick Post, offline page).

## Get an APK without installing anything (easiest)
1. Create a free GitHub account and a new empty repository.
2. Upload everything in this folder (keep the `.github` folder).
3. Open the **Actions** tab -> **Build JHC APK** -> **Run workflow**.
4. When it finishes (about 4 minutes) open the run and download **JHC-app-apk**. Unzip it: `app-debug.apk` installs on any Android phone (allow "install unknown apps").

## Or build in Android Studio
File -> Open -> choose this folder -> wait for sync -> Build -> Build APK(s).

## Before putting it on Google Play
* Replace the icon with the real school logo: right-click `app` -> New -> Image Asset.
* Build a signed bundle: Build -> Generate Signed Bundle / APK -> Android App Bundle. Keep the key file safe forever; you need it for every update.
* A Google Play developer account (one-time USD 25) is required.
* Change the address in `MainActivity.java` (`HOME`, `HOST`) only if the website address changes.
