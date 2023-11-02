# Scanner4Zeffy

An Android app which automatically opens the URL of whatever QR code it sees, on the same screen.
This allows for quick scanning of multiple QR codes in a row. BE CAREFUL WITH THIS (see warning below).

I am working on this because we have begun to use a platform called Zeffy for event ticketing.  Ticket buyers receive tickets with QR codes on them, which link to a website to determine validity.
At our first event using it, we found that scanning was very slow.  Zeffy does not have a scanning app, and expects us to use our phones' built-in QR scanning capabilities, or some other kind of generic QR code scanning app.

### This was the process:

1. Launch Camera (or QR Code app)
2. Point camera at QR code
3. Pop-up appears, touch the pop-up to open the link in Browser.
4. Phone switches to browser and opens the link, displaying if ticket is valid or not.

   **REPEAT.**
   **For every single person.**

This process would take up to ten seconds per scan.  A family of four or five would be standing and waiting for almost a minute.

I wanted something better.

## Features

This app aims to smooth out the process and cut out as many time-wasting parts of the process I outlined above as possible.

Here's what it does:

- Scans QR Codes and opens the link with no intervention needed.
- Shows the camera preview and website result on the same screen, reducing time wasted switching to and from the browser.
- Plays a sound and vibrates when a QR code is detected, and will play a negative sound if the ticket has already been scanned.
- Scans the next QR code after a delay of a second.  This could be changed by changing the number "60", which refers to frames in:

```kotlin
  if (frameCounter >= 60)
```
  and
```kotlin
  rebuildQueue(60)
```

## Issues / Coming soon

I want to further improve the responsiveness, and reduce the battery drain of the app.
Below is a laundry list of what I am working on or thinking about.

- Currently, the very first scan you do may two or three seconds more.  It speeds up after that.
- The "QR code found" sound and vibration feedback are not in-sync.
- I may add an option to change the webView to Dark theme to reduce battery usage on phones with OLED displays, though we found the battery usage to be pretty good.
- I will experiment with increasing the frame skipping to reduce processing power. Currently it only scans every third frame to look for a QR code, but I will try to increase that without reducing the responsiveness.
- I may reduce the resolution of the displayed camera preview to again reduce battery usage.
- I may see if I can stop the camera preview entirely for a short period after a QR code is detected.
- Does not play the error sound if a ticket has been cancelled, but the visual feedback may be enough.


## Warning  
This app TRUSTS the QR code it is shown.  QR codes in general can be made to contain malicious links, so I would not recommend using this outside of a situation where you know that you are looking at a most-likely trustworthy QR code.

This app is not associated with Zeffy or 9355-0861 Québec Inc.
