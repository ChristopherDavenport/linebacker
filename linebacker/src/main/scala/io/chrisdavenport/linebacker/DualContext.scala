package io.chrisdavenport.linebacker

import cats.effect._
import cats.effect.concurrent.Semaphore
import cats.implicits._
import java.util.concurrent.ExecutorService
import scala.concurrent.ExecutionContext

trait DualContext[F[_]] extends Linebacker[F] {
  def blockingContext: ExecutionContext
  def contextShift: ContextShift[F]

  def block[A](fa: F[A]): F[A] =
    contextShift.evalOn(blockingContext)(fa)
}

object DualContext {
  def apply[F[_]](implicit ev: DualContext[F]) = ev

  def fromContexts[F[_]](
      cs: ContextShift[F],
      blocking: ExecutionContext): DualContext[F] =
    new DualContext[F] {
      override def blockingContext = blocking
      override def contextShift = cs
    }

  def fromExecutorService[F[_]](
      default: ContextShift[F],
      blocking: ExecutorService): DualContext[F] =
    fromContexts(default, ExecutionContext.fromExecutor(blocking))

  def bounded[F[_]: Concurrent](lb: DualContext[F], bound: Long): F[DualContext[F]] = 
    Semaphore[F](bound).map(new BoundedDualContext(lb, _))

  private class BoundedDualContext[F[_]: Concurrent](dc: DualContext[F], s: Semaphore[F]) extends DualContext[F]{
    def blockingContext: ExecutionContext = dc.blockingContext
    def contextShift: ContextShift[F] = dc.contextShift
    override def blockContextShift[A](fa: F[A])(implicit cs: ContextShift[F]): F[A] =
      s.withPermit(dc.blockContextShift(fa))
    override def block[A](fa: F[A]): F[A] =
      s.withPermit(dc.block(fa))
  }
}
