# Codebits 2014 Digital Signage Client

This started out as a barebones webview to test browser behavior, and kind of grew organically from there - it includes a few tweaks to the webview to enable the back button, JS extensions, location, manual APK downloads, SSL certificate validation overrides (so appropriate to this age of Heartbleed), and other things you generally _shouldn't_ do in an Android application, but might be useful to somebody.

## Dependencies

Unlike [our Raspberry Pi client from 2012][dsc], this won't be very useful without a server to go with it. 

We'll be cleaning up the server source ASAP (as well as removing some proprietary bits) and putting it up on [this repo][dss], so watch that for updates.

## Architecture

This app consists of an `InvisibleActivity` that provides a launcher icon and sets up a `PollingService` to run on boot. That in turn contacts the server, grabs playlist updates (basically a sequence of URLs) and hands them over to a `PlayerService` which actually asks a `FullScreenWebviewActivity` to render the signage displays.

All the signage displays we built were HTML5 pages that used the [Codebits API][api], residing on a [MEO Cloud][mc] shared folder, and none come with the client.

To play video, we just stick a `video` tag inside the WebView and run with it -- it was simpler than fiddling with a VideoView in the time we had, and has the additional benefit that we can place SVG overlays atop the `video` tag with program info. It does, however, have the drawback that we can't respond well to breaks in HLS streaming, etc.

## Stuff That Needs Improving

* Due to issues with passing some data in intent extras, we hand over some stuff between services using static class members. This is a hack, and needs to be expunged from the code.
* Besides exposing the device's MAC and IP addresses in the DOM, we need to provide a way for bi-directional communication between the app and the `video` tag for error handling (i.e., skip to the next playlist item if an HTTP live stream breaks or when a video finishes untimely).
* The network protocol we chose for 2014 (constantly retrieving a "live" playlist via HTTP polling) was designed in an attempt to do 'live' random playlists and dynamic insertion of MEO Kanal assets, and was, in retrospect, not much of an improvement. Also, there wasn't much time to implement client status metrics, etc.

We plan on doing three things regarding the network protocol:

* Go back to the original 2012 design (only send playlists when changed, do playlist randomness on the client side, etc.)
* Implement a separate mode for "live" assets (even if it's an entirely separate player)
* Use MQTT instead of HTTP polling for real-time sync of multiple displays into a "signage wall".


## Building

First off, edit `strings.xml.dist` with the appropriate endpoints and rename it to `strings.xml`.

Without an IDE, just set your PATH to the Android tools and use `ant`:

    export PATH=$PATH:$HOME/Developer/Android/sdk/tools
    ant release

If you must use an IDE, this repo includes NetBeans project files. We recommend you use those.

## Running

    # set up emulator
    android avd
    # start emulator
    emulator -avd ARM720 &
    ant release install
    # check logs
    monitor

ant release install

## Recovering from Eclipse

In case you end up importing the project there and wish to return to a saner environment:

    android list targets
    android update project -p . -t 17
    
    
[dsc]: https://github.com/sapo/digital-signage-client
[dss]: https://github.com/sapo/digital-signage-server
[api]: https://codebits.eu/s/api
[mc]: https://meocloud.pt/
