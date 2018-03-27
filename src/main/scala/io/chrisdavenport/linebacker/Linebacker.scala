package io.chrisdavenport.linebacker

import cats.implicits._
import cats.effect.{Async, Sync}
import fs2.Stream
import scala.concurrent.ExecutionContext
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

trait Linebacker[F[_]] {

  def blockingPool: ExecutionContext

  final def block[A](fa: F[A])(implicit F: Async[F], ec: ExecutionContext): F[A] =
    Async.shift(blockingPool) *> fa <* Async.shift(ec)
}

object Linebacker {
  def apply[F[_]](implicit ev: Linebacker[F]): Linebacker[F] = ev

  private def buildExecutors[F[_]: Sync]: F[ExecutorService] = Sync[F].delay {
    Executors.newCachedThreadPool(new ThreadFactory {
      private val counter = new AtomicLong(0L)

      def newThread(r: Runnable) = {
        val th = new Thread(r)
        th.setName("linebacker-thread-" + counter.getAndIncrement.toString)
        th.setDaemon(true)
        th
      }
    })
  }

  def build[F[_]: Sync]: F[Linebacker[F]] =
    buildExecutors.map(blockingExecutor =>
      new Linebacker[F] {
        def blockingPool: ExecutionContext =
          ExecutionContext.fromExecutorService(blockingExecutor)
    })

  /**
   * Safe public consumption of creation of a Linebacker
   */
  def stream[F[_]: Sync]: Stream[F, Linebacker[F]] =
    Stream.bracket(buildExecutors)(
      ex =>
        new Linebacker[F] {
          def blockingPool: ExecutionContext =
            ExecutionContext.fromExecutorService(ex)
        }.pure[Stream[F, ?]],
      ex => Sync[F].delay(ex.shutdownNow).void
    )
}
