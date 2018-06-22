# Linebacker [![Build Status](https://travis-ci.org/ChristopherDavenport/linebacker.svg?branch=master)](https://travis-ci.org/ChristopherDavenport/linebacker) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/linebacker_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/linebacker_2.12) [![Gitter chat](https://badges.gitter.im/christopherdavenport/linebacker.png)](https://gitter.im/christopherdavenport/linebacker)

Enabling Functional Blocking where you need it.

## Quick Start

To use linebacker in an existing SBT project with Scala 2.11 or a later version, add the following dependency to your
`build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "linebacker" % "<version>"
```

## Why Linebacker

Concurrency is hard.

## No Seriously, Why Linebacker

Generally threading models have to deal with the idea that in java/scala some of our fundamental calls
are still blocking. Looking at you JDBC! In order to handle this we generally utilize Executors or
ExecutionContexts. Additionally many libraries now utilize implicit execution contexts for their shifting.
This puts in a position where we need to manually and explicitly pass around two contexts raising one
explicitly where appropriate and then shifting work back and forth from the pools as appropriate.

Here is where we attempt to make these patterns easier. This library provides abstractions for managing
pools and shifting behavior between your pools.

Why should you care? Let us propose you have a single pool on 5 threads and you receive 5 requests that
require communicating with a database. What happens if a 6th call comes in when all these CPU bound threads
are blocked on network IO? Obviously we are waiting for threads.

Some additional resources for why this is important:

<blockquote class="twitter-tweet" data-lang="en"><p lang="en" dir="ltr">Thread pool best practices. <br>For more info, see <a href="https://twitter.com/djspiewak?ref_src=twsrc%5Etfw">@djspiewak</a> &amp; <a href="https://twitter.com/alexelcu?ref_src=twsrc%5Etfw">@alexelcu</a> posts:<a href="https://t.co/pr6McpU3tH">https://t.co/pr6McpU3tH</a><a href="https://t.co/Vz617IMjRB">https://t.co/Vz617IMjRB</a> <a href="https://t.co/gJgzZI6yGJ">pic.twitter.com/gJgzZI6yGJ</a></p>&mdash; Impure Pics (@impurepics) <a href="https://twitter.com/impurepics/status/987758585722621957?ref_src=twsrc%5Etfw">April 21, 2018</a></blockquote>
<script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>

- [Thread Pools](https://gist.github.com/djspiewak/46b543800958cf61af6efa8e072bfd5c) by Daniel Spiewak
- [Best Practice: Should Not Block Threads](https://monix.io/docs/3x/best-practices/blocking.html) by Alexandru Nedelcu/Monix

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

val checkRun = {
  Executors.unbound[IO] // Create Executor
    .map(Linebacker.fromExecutorService[IO](_)) // Create Linebacker From Executor
    .use{ implicit linebacker => // Raise Implicitly
      Linebacker[IO].blockEc(getThread) // Block On Linebacker Pool Not Global
        .flatMap(threadName => IO(println(threadName))) >>
      getThread // Running On Global
        .flatMap(threadName => IO(println(threadName)))
    }
}

checkRun.unsafeRunSync
```

Dual Contexts Are Also Very Useful

```tut
import scala.concurrent.ExecutionContext
import io.chrisdavenport.linebacker.DualContext

Executors.unbound[IO].map(blockingExecutor =>
  DualContext.fromContexts[IO](global,  ExecutionContext.fromExecutorService(blockingExecutor))
)
```