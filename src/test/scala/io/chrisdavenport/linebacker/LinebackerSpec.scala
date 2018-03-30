package io.chrisdavenport.linebacker

import org.specs2._
import cats.effect.IO
import cats.implicits._
import fs2.Stream
import java.lang.Thread
import scala.concurrent.ExecutionContext
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, ThreadFactory}
import _root_.io.chrisdavenport.linebacker.contexts.{Executors => E}

class LinebackerSpec extends Spec {
  override def is = s2"""
  Threads Run On Linebacker $runsOnLinebacker
  Threads Afterwards Run on Provided EC $runsOffLinebackerAfterwards
  """

  def runsOnLinebacker = {
    E.unbound[IO]
      .map(Linebacker.fromExecutorService[IO])
      .flatMap { implicit linebacker =>
        import scala.concurrent.ExecutionContext.Implicits.global
        Stream.eval(
          Linebacker[IO].block(IO(Thread.currentThread().getName))
        )
      }
      .compile
      .last
      .unsafeRunSync must_== Some("linebacker-thread-0")
  }

  def runsOffLinebackerAfterwards = {
    val executor = Executors.newCachedThreadPool(new ThreadFactory {
      private val counter = new AtomicLong(0L)

      def newThread(r: Runnable) = {
        val th = new Thread(r)
        th.setName("test-ec-" + counter.getAndIncrement.toString)
        th.setDaemon(true)
        th
      }
    })
    implicit val ec = ExecutionContext
      .fromExecutorService(executor)

    implicit val linebacker = Linebacker.fromExecutionContext[IO](ec)

    Stream
      .eval(
        Linebacker[IO].block(IO.unit) *>
          IO(Thread.currentThread().getName)
          <* IO(executor.shutdownNow)
      )
      .compile
      .last
      .unsafeRunSync must_== Some("test-ec-1")
  }
}
