package io.chrisdavenport

import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ExecutorService, Executors, ForkJoinPool, ThreadFactory}

package object contexts {

  object unsafe {
    def unboundExecutorUnsafe[F[_]: Sync]: F[ExecutorService] = Sync[F].delay {
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
    def fixedPoolExecutorUnsafe[F[_]: Sync](n: Int): F[ExecutorService] =
      Sync[F].delay { Executors.newFixedThreadPool(n) }
    def workStealingPoolUnsafe[F[_]: Sync](n: Int): F[ExecutorService] =
      Sync[F].delay(Executors.newWorkStealingPool(n))
    def forkJoinPoolUnsafe[F[_]: Sync](n: Int): F[ExecutorService] =
      Sync[F].delay(new ForkJoinPool(n))

  }
  import unsafe._

  def unboundExecutor[F[_]: Sync]: Stream[F, ExecutorService] =
    streamExecutorService(unboundExecutorUnsafe)

  def fixedPool[F[_]: Sync](n: Int) =
    streamExecutorService(fixedPoolExecutorUnsafe(n))

  def workStealingPool[F[_]: Sync](n: Int) =
    streamExecutorService(workStealingPoolUnsafe(n))

  def forkJoinPool[F[_]: Sync](n: Int) =
    streamExecutorService(forkJoinPoolUnsafe(n))

  private def streamExecutorService[F[_]: Sync](f: F[ExecutorService]): Stream[F, ExecutorService] =
    Stream.bracket(f)(
      _.pure[Stream[F, ?]],
      es => Sync[F].delay(es.shutdownNow).void
    )

}
