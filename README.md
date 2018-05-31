# Linebacker [![Build Status](https://travis-ci.org/ChristopherDavenport/linebacker.svg?branch=master)](https://travis-ci.org/ChristopherDavenport/linebacker) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/linebacker_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/linebacker_2.12) [![Gitter chat](https://badges.gitter.im/christopherdavenport/linebacker.png)](https://gitter.im/christopherdavenport/linebacker)

Enabling Functional Blocking where you need it.

## Quick Start

To use linebacker in an existing SBT project with Scala 2.11 or a later version, add the following dependency to your
`build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "linebacker" % "<version>"
```

## Examples

First some imports

```tut:silent
import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect._
import cats.implicits._
import io.chrisdavenport.linebacker.Linebacker
import io.chrisdavenport.linebacker.contexts.Executors
```

Creating And Evaluating Pool Behavior

```tut
val getThread = IO(Thread.currentThread().getName)

Executors.unbound[IO] // Create Executor
    .map(Linebacker.fromExecutorService[IO](_)) // Create Linebacker From Executor
    .use{ implicit linebacker => // Raise Implicitly
      Linebacker[IO].blockEc(getThread) // Block On Linebacker Pool Not Global
        .flatMap(threadName => IO(println(threadName))) >>
      getThread // Running On Global
        .flatMap(threadName => IO(println(threadName)))
    }

ThreadNameExample.checkRun.unsafeRunSync
```

Dual Contexts Are Also Very Useful

```tut
import scala.concurrent.ExecutionContext
import io.chrisdavenport.linebacker.DualContext

Executors.unbound[IO].map(blockingExecutor =>
  DualContext.fromContexts[IO](global,  ExecutionContext.fromExecutorService(blockingExecutor))
)
```