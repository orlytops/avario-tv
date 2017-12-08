# avarioHome_android
Avario Home App for Android

Testing the app update:

1. Install first the `v0.22.0` apk.
2. Create a folder named `tablet` on `www/local` this is where the bootstrap.json is located to.
3. Inside the `tablet` folder put the `v0.23.0` apk and rename it to `app-release.apk`.
4. Put the `version.json` inside the `tablet` folder.
5. Go to Settings and click the `Update` button.

Note: `version.json` contains the latest version of the apk available. Every 24 hours the app checks the `version.json` if there is any available update.

App should be in kiosk mode.

There are two ways on how to update the app first is through the interval checking for the updates, it will popup the Alert Dialog. Second is through the Settings.

Instructions for kiosk mode:

1. First install the apk.
2. Remove logged in account in settings to allow setting device owner.
3. Go to your terminal and run  this `adb` script `adb shell dpm set-device-owner com.avariohome.avario/.service.AvarioReceiver` (The app will not be in kiosk mode if you don't do this.)

Note: If you want to remove the kiosk mode to uninstall the app just uncheck the check box `Kiosk Mode` in the settings and deactivate Avario in Device Admin Manager. If you want to turn it back to kiosk mode again please repeat the step 3 stated above.
