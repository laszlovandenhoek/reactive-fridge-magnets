# Reactive Fridge Magnets

Reimplementation of the now-defunct fridge magnets application at www.lunchtimers.com.

There are many clones on GitHub, but most of them involve NodeJS, and none that I could see apply front-to-back backpressure as the Reactive Streams API allows you to do.

This project aims to achieve this on the JVM, using Akka. The connection to the frontend could be RSocket, maybe? Just plain JSON over WebSocket for 1.0.

# Running

- Install a JDK and sbt (Easy using SDKMAN)
- Install NPM (Easy using `nvm`: `curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.38.0/install.sh | bash`)
- `sbt backend/run`

After the build is done, you can access the application at:

http://localhost:8080

While only one "player" is connected, an automated "Chaos Monkey" will start moving letters around at random.