package io.chrisdavenport.linebacker

import org.specs2._
import cats.effect._
import cats.implicits._
import java.lang.Thread
import scala.concurrent.ExecutionContext
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, ThreadFactory}
import _root_.io.chrisdavenport.linebacker.contexts.{Executors => E}
import scala.concurrent.ExecutionContext.global

class LinebackerSpec extends Spec {
  override def is = s2"""
  Threads Run On Linebacker $runsOnLinebacker
  Threads Afterwards Run on Provided EC $runsOffLinebackerAfterwards
  """

  def runsOnLinebacker = {
    val testRun = E
      .unbound[IO]
      .map(Linebacker.fromExecutorService[IO])
      .use { implicit linebacker =>
        implicit val cs = IO.contextShift(global)
        Linebacker[IO].blockContextShift(IO(Thread.currentThread().getName))
      }

    testRun.unsafeRunSync must_=== "linebacker-thread-0"
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

    implicit val linebacker = Linebacker.fromExecutionContext[IO](global) // Block Onto Global
    implicit val cs = IO.contextShift(ec) // Should return to custom

    val testRun = Linebacker[IO].blockContextShift(IO.unit) *>
      IO(Thread.currentThread().getName) <*
      IO(executor.shutdownNow)

    testRun.unsafeRunSync must_=== "test-ec-0"
  }
}
