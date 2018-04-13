# Linebacker [![Build Status](https://travis-ci.org/ChristopherDavenport/linebacker.svg?branch=master)](https://travis-ci.org/ChristopherDavenport/linebacker)

Enabling Functional Blocking where you need it.

## Quick Start

To use linebacker in an existing SBT project with Scala 2.11 or a later version, add the following dependency to your
`build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "linebacker" % "<version>"
```

## Examples

First Some Imports

```tut:silent
import scala.concurrent.ExecutionContext.Implicits.global
import fs2.Stream
import cats.effect._
import cats.implicits._
import io.chrisdavenport.linebacker._
import _root_.io.chrisdavenport.linebacker.contexts.{Executors => E}
```

Creatings And Evaluating Pool Behavior

```tut
val getThread = IO(Thread.currentThread().getName)

object FakeApp {
val checkRun = E.unbound[IO] // Create Executor
                .map(Linebacker.fromExecutorService[IO](_) ) // Create Linebacker From Executor
                .flatMap { implicit linebacker => // Raise Implicitly
                  Stream.eval(
                    Linebacker[IO].block(getThread) // Block On Linebacker Pool Not Global
                  ) ++
                  Stream.eval(getThread) // Running On Global
                }.compile.toVector

}
FakeApp.checkRun.unsafeRunSync
```