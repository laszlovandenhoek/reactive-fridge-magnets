# Reactive Fridge Magnets

Reimplementation of the now-defunct fridge magnets application at www.lunchtimers.com.

There are many clones on GitHub, but most of them involve NodeJS, and none that I could see apply front-to-back backpressure as the Reactive Streams API allows you to do.

This project aims to achieve this on the JVM, using Akka. The connection to the frontend could be RSocket, maybe? Just plain JSON over WebSocket for 1.0.

frontend build would be nice to have NPM.