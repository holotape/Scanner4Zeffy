# Scanner4Zeffy

An Android app which automatically opens the URL of whatever QR code it sees on the same screen.
This allows for quick scanning of multiple QR codes in a row. BE CAREFUL WITH THIS.

I am working on this because we have used a platform called Zeffy for ticketing for events.  At our first event using it, we found that scanning was very slow.  Zeffy does not have a scanning app, and expects user to use their phone's built-in QR scanning capabilities, or some other kind of generic QR code scanning app.

### This was the process:

1. Launch Camera (or QR Code app)
2. Point camera at QR code
3. Pop-up appears, touch the pop-up to open the link in Browser.
4. Phone switches to browser and opens the link, displaying if ticket is valid or not.

   **REPEAT.**

This process would take around ten seconds per scan.  A family of four or five would be standing and waiting for almost a minute.

I wanted something better.

## Features

- Scans QR Codes and opens the link with no intervention needed.
- Shows the camera preview and website result on the same screen, reducing time wasted switch to and from the browser.
- Plays a sound when a QR code is detected, and will play a negative sound if the ticket has already been scanned.
- Scans the next QR code after a delay of a second.  This could be changed by changing the number "60", which refers to frames in:

```kotlin
  if (frameCounter >= 60)
```
  and
```kotlin
  rebuildQueue(60)
```

## Warning  
This app TRUSTS the QR code it is shown.  QR codes in general can be made to contain malicious links, so I would not recommend using this outside of a situation where you know that you are looking at a most-likely trustworthy QR code.
