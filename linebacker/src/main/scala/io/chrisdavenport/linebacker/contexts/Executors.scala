package io.chrisdavenport.linebacker.contexts

import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory

import cats.effect.{Resource, Sync}
import cats.implicits._
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ExecutorService, ForkJoinPool, ThreadFactory, Executors => E}

object Executors {

  import unsafe._

  /**
   * Recommended Pool For Non-CPU Load Blocking.
   * For example in a situation where you are
   * transitioning off a CPU loaded task and onto
   * a Hikari pool, you may want to back the
   * resource with the same number of threads.
   */
  def fixedPool[F[_]: Sync](n: Int): Resource[F, ExecutorService] =
    fixedPoolC(n)(identity)

  def fixedPoolC[F[_]: Sync](n: Int)(
      configureThreadFactory: F[ThreadFactory] => F[ThreadFactory]): Resource[F, ExecutorService] =
    executorServiceResource(fixedPoolExecutorUnsafe(n, configureThreadFactory))

  /**
   * Constructs an unbound thread pool that will create
   * a new thread for each submitted job. This is useful
   * if you have a construct that is blocking but
   * self-manages the number of threads you can consume.
   */
  def unbound[F[_]: Sync]: Resource[F, ExecutorService] =
    unboundC(identity)

  def unboundC[F[_]: Sync](
      configureThreadFactory: F[ThreadFactory] => F[ThreadFactory]): Resource[F, ExecutorService] =
    executorServiceResource(unboundExecutorUnsafe(configureThreadFactory))

  /**
   * A work stealing pool is often a useful blocking
   * pool for CPU bound blocking work that you want
   * to transition off the pool that is handling your
   * requests, generally set to the number of processors
   * to maximize the processor use. Perhaps subtracting
   * 1 as to maximize the other pool for handling
   * requests or other work.
   */
  def workStealingPool[F[_]: Sync](n: Int): Resource[F, ExecutorService] =
    workStealingPoolC(n)(identity)

  def workStealingPoolC[F[_]: Sync](n: Int)(
      configureThreadFactory: F[ForkJoinWorkerThreadFactory] => F[ForkJoinWorkerThreadFactory]): Resource[
    F,
    ExecutorService] =
    executorServiceResource(workStealingPoolUnsafe(n, configureThreadFactory))

  /**
   * Default Pool For Scala, optimized for forked work and then returning to a
   * main pool, generally ideal for your main event loop.
   */
  def forkJoinPool[F[_]: Sync](n: Int): Resource[F, ExecutorService] =
    forkJoinPoolC(n)(identity)

  def forkJoinPoolC[F[_]: Sync](n: Int)(
      configureThreadFactory: F[ForkJoinWorkerThreadFactory] => F[ForkJoinWorkerThreadFactory]): Resource[
    F,
    ExecutorService] =
    executorServiceResource(forkJoinPoolUnsafe(n, configureThreadFactory))

  private def executorServiceResource[F[_]: Sync](
      f: F[ExecutorService]): Resource[F, ExecutorService] =
    Resource.make[F, ExecutorService](f)(es => Sync[F].delay(es.shutdownNow).void)

  object unsafe {
    def unboundExecutorUnsafe[F[_]: Sync](
        configureThreadFactory: F[ThreadFactory] => F[ThreadFactory]): F[ExecutorService] = {
      //delay used to avoid eager evaluation in case that instance is ignored by the configurer
      val factoryF: F[ThreadFactory] = Sync[F].delay {
        new ThreadFactory {
          private val counter = new AtomicLong(0L)

          def newThread(r: Runnable): Thread = {
            val th = new Thread(r)
            th.setName("linebacker-thread-" + counter.getAndIncrement.toString)
            th.setDaemon(true)
            th
          }
        }
      }

      configureThreadFactory(factoryF).flatMap { factory =>
        Sync[F].delay {
          E.newCachedThreadPool(factory)
        }
      }
    }

    def fixedPoolExecutorUnsafe[F[_]: Sync](
        n: Int,
        configureThreadFactory: F[ThreadFactory] => F[ThreadFactory]): F[ExecutorService] = {
      val factoryF = configureThreadFactory(Sync[F].delay(E.defaultThreadFactory()))

      factoryF.flatMap { factory =>
        Sync[F].delay { E.newFixedThreadPool(n, factory) }
      }
    }

    def workStealingPoolUnsafe[F[_]: Sync](
        n: Int,
        configureThreadFactory: F[ForkJoinWorkerThreadFactory] => F[ForkJoinWorkerThreadFactory]): F[
      ExecutorService] = {
      val factoryF = configureThreadFactory(
        Sync[F].point(ForkJoinPool.defaultForkJoinWorkerThreadFactory))

      factoryF.flatMap { factory =>
        Sync[F].delay {
          new ForkJoinPool(n, factory, null, true)
        }
      }
    }

    def forkJoinPoolUnsafe[F[_]: Sync](
        n: Int,
        configureThreadFactory: F[ForkJoinWorkerThreadFactory] => F[ForkJoinWorkerThreadFactory]): F[
      ExecutorService] = {
      val factoryF = configureThreadFactory(
        Sync[F].point(ForkJoinPool.defaultForkJoinWorkerThreadFactory))

      factoryF.flatMap { factory =>
        Sync[F].delay {
          new ForkJoinPool(n, factory, null, false)
        }
      }
    }
  }
}
