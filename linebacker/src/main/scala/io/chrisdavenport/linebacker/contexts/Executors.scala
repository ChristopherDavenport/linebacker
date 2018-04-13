package io.chrisdavenport.linebacker.contexts

import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ExecutorService, Executors => E, ForkJoinPool, ThreadFactory}

object Executors {

  import unsafe._

  /**
   * Recommended Pool For Non-CPU Load Blocking.
   * For example in a situation where you are
   * transitioning off a CPU loaded task and onto
   * a Hikari pool, you may want to back the
   * resource with the same number of threads.
   */
  def fixedPool[F[_]: Sync](n: Int) =
    streamExecutorService(fixedPoolExecutorUnsafe(n))

  /**
   * Constructs an unbound thread pool that will create
   * a new thread for each submitted job. This is useful
   * if you have a construct that is blocking but
   * self-manages the number of threads you can consume.
   */
  def unbound[F[_]: Sync]: Stream[F, ExecutorService] =
    streamExecutorService(unboundExecutorUnsafe)

  /**
   * A work stealing pool is often a useful blocking
   * pool for CPU bound blocking work that you want
   * to transition off the pool that is handling your
   * requests, generally set to the number of processors
   * to maximize the processor use. Perhaps subtracting
   * 1 as to maximize the other pool for handling
   * requests or other work.
   */
  def workStealingPool[F[_]: Sync](n: Int) =
    streamExecutorService(workStealingPoolUnsafe(n))

  /**
   * Default Pool For Scala, optimized for forked work and then returning to a
   * main pool, generally ideal for your main event loop.
   */
  def forkJoinPool[F[_]: Sync](n: Int) =
    streamExecutorService(forkJoinPoolUnsafe(n))

  private def streamExecutorService[F[_]: Sync](f: F[ExecutorService]): Stream[F, ExecutorService] =
    Stream.bracket(f)(
      _.pure[Stream[F, ?]],
      es => Sync[F].delay(es.shutdownNow).void
    )

  object unsafe {
    def unboundExecutorUnsafe[F[_]: Sync]: F[ExecutorService] = Sync[F].delay {
      E.newCachedThreadPool(new ThreadFactory {
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
      Sync[F].delay { E.newFixedThreadPool(n) }
    def workStealingPoolUnsafe[F[_]: Sync](n: Int): F[ExecutorService] =
      Sync[F].delay(E.newWorkStealingPool(n))
    def forkJoinPoolUnsafe[F[_]: Sync](n: Int): F[ExecutorService] =
      Sync[F].delay(new ForkJoinPool(n))
  }
}
